package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.model.TabloChannel
import com.example.model.TabloDevice
import com.example.model.TabloProgram
import com.example.model.TabloRecording
import com.example.model.TabloServerInfo
import com.example.model.TabloStream
import com.example.model.TabloTuner
import com.example.network.TabloAuthService
import com.example.network.TabloChannelService
import com.example.network.TabloDiscoveryService
import com.example.network.TabloGuideService
import com.example.network.TabloRecordingService
import com.example.network.TabloStreamService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TabloRepository(
    private val context: Context,
    private val authService: TabloAuthService = TabloAuthService(),
    private val discoveryService: TabloDiscoveryService = TabloDiscoveryService(),
    private val channelService: TabloChannelService = TabloChannelService(),
    private val guideService: TabloGuideService = TabloGuideService(),
    private val recordingService: TabloRecordingService = TabloRecordingService(),
    private val streamService: TabloStreamService = TabloStreamService()
) {
    companion object {
        private const val TAG = "TabloRepository"
        private const val PREFS_NAME = "tablo_multiview_prefs"
        private const val KEY_DEVICE_SID = "device_sid"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_DEVICE_LOCAL_URL = "device_local_url"
        private const val KEY_LIGHTHOUSE_TOKEN = "lighthouse_token"
        private const val KEY_ACCOUNT_TOKEN = "account_token"
        private const val KEY_CLIENT_ID = "client_id"
        private const val KEY_FAVORITES = "favorite_channels"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _activeDevice = MutableStateFlow<TabloDevice?>(null)
    val activeDevice: StateFlow<TabloDevice?> = _activeDevice.asStateFlow()

    private val _serverInfo = MutableStateFlow<TabloServerInfo?>(null)
    val serverInfo: StateFlow<TabloServerInfo?> = _serverInfo.asStateFlow()

    private val _tuners = MutableStateFlow<List<TabloTuner>>(emptyList())
    val tuners: StateFlow<List<TabloTuner>> = _tuners.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<TabloDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<TabloDevice>> = _discoveredDevices.asStateFlow()

    private val _channels = MutableStateFlow<List<TabloChannel>>(emptyList())
    val channels: StateFlow<List<TabloChannel>> = _channels.asStateFlow()

    private val _recordings = MutableStateFlow<List<TabloRecording>>(emptyList())
    val recordings: StateFlow<List<TabloRecording>> = _recordings.asStateFlow()

    private val _favorites = MutableStateFlow<Set<String>>(emptySet())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    init {
        loadPersistedState()
        refreshAll()
    }

    private fun loadPersistedState() {
        val favs = prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
        _favorites.value = favs

        val sid = prefs.getString(KEY_DEVICE_SID, null)
        val localUrl = prefs.getString(KEY_DEVICE_LOCAL_URL, null)
        if (sid != null && localUrl != null) {
            val name = prefs.getString(KEY_DEVICE_NAME, "Tablo 4th Gen") ?: "Tablo 4th Gen"
            val lhToken = prefs.getString(KEY_LIGHTHOUSE_TOKEN, "") ?: ""
            val accToken = prefs.getString(KEY_ACCOUNT_TOKEN, "") ?: ""
            val clientId = prefs.getString(KEY_CLIENT_ID, UUID.randomUUID().toString()) ?: UUID.randomUUID().toString()

            _activeDevice.value = TabloDevice(
                sid = sid,
                name = name,
                localUrl = localUrl,
                lighthouseToken = lhToken,
                accountToken = accToken,
                clientId = clientId
            )
        }
    }

    fun saveDevice(device: TabloDevice?) {
        _activeDevice.value = device
        prefs.edit().apply {
            if (device != null) {
                putString(KEY_DEVICE_SID, device.sid)
                putString(KEY_DEVICE_NAME, device.name)
                putString(KEY_DEVICE_LOCAL_URL, device.localUrl)
                putString(KEY_LIGHTHOUSE_TOKEN, device.lighthouseToken)
                putString(KEY_ACCOUNT_TOKEN, device.accountToken)
                putString(KEY_CLIENT_ID, device.clientId)
            } else {
                remove(KEY_DEVICE_SID)
                remove(KEY_DEVICE_NAME)
                remove(KEY_DEVICE_LOCAL_URL)
                remove(KEY_LIGHTHOUSE_TOKEN)
                remove(KEY_ACCOUNT_TOKEN)
            }
            apply()
        }
    }

    fun refreshAll() {
        repoScope.launch {
            _isLoading.value = true
            try {
                val dev = _activeDevice.value
                if (dev != null && dev.localUrl.isNotEmpty()) {
                    // Fetch /server/info and /server/tuners
                    try {
                        val sInfo = discoveryService.fetchServerInfo(dev.localUrl)
                        _serverInfo.value = sInfo
                        val tunerList = discoveryService.fetchTuners(dev.localUrl)
                        _tuners.value = tunerList
                    } catch (e: Exception) {
                        Log.d(TAG, "Server info / tuners fetch warning: ${e.message}")
                    }
                }

                // Fetch channels
                val chList = channelService.getChannels(_activeDevice.value)
                // Enrich current programs for channels
                val enrichedChannels = chList.map { ch ->
                    val airings = guideService.getFallbackProgramsForChannel(ch)
                    ch.copy(currentProgram = airings.firstOrNull())
                }
                _channels.value = enrichedChannels

                // Fetch recordings
                val recList = recordingService.getRecordings(_activeDevice.value)
                _recordings.value = recList

                val tunersCount = _serverInfo.value?.tunerCount ?: _activeDevice.value?.tunerCount ?: 2
                val busyTuners = _tuners.value.count { it.inUse }

                _statusMessage.value = if (_activeDevice.value != null) {
                    "Connected to ${_activeDevice.value?.name} (${_channels.value.size} channels, $tunersCount tuners ($busyTuners in use))"
                } else {
                    "Ready (${_channels.value.size} broadcast channels)"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Refresh failed: ${e.message}", e)
                _statusMessage.value = "Error refreshing: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    suspend fun scanLocalNetwork(): List<TabloDevice> {
        _isLoading.value = true
        _statusMessage.value = "Discovering Tablo devices on local network..."
        return try {
            val found = discoveryService.discoverAllDevices()
            _discoveredDevices.value = found
            if (found.isNotEmpty()) {
                _statusMessage.value = "Found ${found.size} Tablo device(s)"
            } else {
                _statusMessage.value = "No Tablo devices found via automatic discovery."
            }
            found
        } catch (e: Exception) {
            _statusMessage.value = "Discovery error: ${e.message}"
            emptyList()
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun refreshTuners(): List<TabloTuner> {
        val dev = _activeDevice.value ?: return emptyList()
        val list = discoveryService.fetchTuners(dev.localUrl)
        _tuners.value = list
        return list
    }

    suspend fun login(email: String, pass: String): List<TabloDevice> {
        _isLoading.value = true
        _statusMessage.value = "Authenticating with Tablo Cloud..."
        return try {
            val devices = authService.discoverDevices(email, pass)
            if (devices.isNotEmpty()) {
                saveDevice(devices.first())
                refreshAll()
            }
            devices
        } catch (e: Exception) {
            _statusMessage.value = "Login error: ${e.message}"
            throw e
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun connectManualIp(ipOrHost: String): TabloDevice? {
        _isLoading.value = true
        _statusMessage.value = "Connecting to Tablo at $ipOrHost..."
        return try {
            val dev = discoveryService.pingDevice(ipOrHost)
            if (dev != null) {
                saveDevice(dev)
                refreshAll()
            } else {
                _statusMessage.value = "Could not find Tablo at $ipOrHost:8885"
            }
            dev
        } finally {
            _isLoading.value = false
        }
    }

    fun logout() {
        saveDevice(null)
        refreshAll()
    }

    suspend fun getStreamForChannel(channel: TabloChannel): TabloStream {
        return streamService.startChannelStream(_activeDevice.value, channel)
    }

    suspend fun getStreamForRecording(recording: TabloRecording): TabloStream {
        return streamService.startRecordingStream(_activeDevice.value, recording)
    }

    suspend fun getGuideAiringsForChannel(channel: TabloChannel): List<TabloProgram> {
        return guideService.getChannelAirings(_activeDevice.value, channel)
    }

    fun getFallbackProgramsForChannel(channel: TabloChannel): List<TabloProgram> {
        return guideService.getFallbackProgramsForChannel(channel)
    }

    fun toggleFavorite(channelIdentifier: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(channelIdentifier)) {
            current.remove(channelIdentifier)
        } else {
            current.add(channelIdentifier)
        }
        _favorites.value = current
        prefs.edit().putStringSet(KEY_FAVORITES, current).apply()
    }

    fun saveSlotChannel(slotIndex: Int, channelIdentifier: String?) {
        prefs.edit().putString("slot_${slotIndex}_channel", channelIdentifier).apply()
    }

    fun getSavedSlotChannel(slotIndex: Int): String? {
        return prefs.getString("slot_${slotIndex}_channel", null)
    }
}
