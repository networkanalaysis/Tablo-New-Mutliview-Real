package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.TabloRepository
import com.example.model.MultiviewLayoutType
import com.example.model.NavigationDestination
import com.example.model.TabloChannel
import com.example.model.TabloRecording
import com.example.playback.MultiviewPlaybackManager
import com.example.ui.guide.GuideScreen
import com.example.ui.home.HomeScreen
import com.example.ui.library.LibraryScreen
import com.example.ui.livetv.LiveTvScreen
import com.example.ui.multiview.MultiviewScreen
import com.example.ui.navigation.TvNavHeader
import com.example.ui.picker.ChannelPickerDialog
import com.example.ui.search.SearchScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.CanvasDark
import com.example.ui.theme.TabloTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: TabloRepository
    private lateinit var playbackManager: MultiviewPlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = TabloRepository(applicationContext)
        playbackManager = MultiviewPlaybackManager(applicationContext)

        setContent {
            TabloTheme {
                TabloApp(
                    repository = repository,
                    playbackManager = playbackManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackManager.release()
    }
}

@Composable
fun TabloApp(
    repository: TabloRepository,
    playbackManager: MultiviewPlaybackManager
) {
    val scope = rememberCoroutineScope()

    var currentDestination by remember { mutableStateOf(NavigationDestination.MULTIVIEW) }
    var targetPickerSlotIndex by remember { mutableStateOf<Int?>(null) }

    val activeDevice by repository.activeDevice.collectAsState()
    val channels by repository.channels.collectAsState()
    val recordings by repository.recordings.collectAsState()
    val favorites by repository.favorites.collectAsState()
    val isLoading by repository.isLoading.collectAsState()
    val statusMessage by repository.statusMessage.collectAsState()

    val layoutType by playbackManager.layoutType.collectAsState()
    val soloSlotIndex by playbackManager.soloSlotIndex.collectAsState()

    // Handle BACK key navigation
    BackHandler(enabled = currentDestination != NavigationDestination.MULTIVIEW || soloSlotIndex != null) {
        if (soloSlotIndex != null) {
            playbackManager.exitSoloMode()
        } else if (currentDestination != NavigationDestination.MULTIVIEW) {
            currentDestination = NavigationDestination.MULTIVIEW
        }
    }

    // Initialize 4 channels into the 4 slots on first load
    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(channels) {
        if (!isInitialized && channels.isNotEmpty()) {
            isInitialized = true
            // Load channels into the 4 slots
            val defaultChannels = channels.take(4)
            defaultChannels.forEachIndexed { index, channel ->
                val savedChannelId = repository.getSavedSlotChannel(index)
                val chToLoad = channels.find { it.identifier == savedChannelId } ?: channel
                scope.launch {
                    try {
                        val stream = repository.getStreamForChannel(chToLoad)
                        playbackManager.setChannelForSlot(index, chToLoad, stream)
                    } catch (e: Exception) {
                        // handled by player error state
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CanvasDark,
        topBar = {
            if (soloSlotIndex == null) {
                TvNavHeader(
                    currentDestination = currentDestination,
                    onNavigate = { dest -> currentDestination = dest },
                    activeDevice = activeDevice,
                    layoutType = layoutType,
                    onLayoutChange = { lType -> playbackManager.setLayoutType(lType) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CanvasDark)
        ) {
            when (currentDestination) {
                NavigationDestination.MULTIVIEW -> {
                    MultiviewScreen(
                        playbackManager = playbackManager,
                        onOpenChannelPickerForSlot = { slotIdx ->
                            targetPickerSlotIndex = slotIdx
                        }
                    )
                }

                NavigationDestination.GUIDE -> {
                    GuideScreen(
                        repository = repository,
                        channels = channels,
                        favorites = favorites,
                        onPlayChannelInSlot = { channel, slotIdx ->
                            scope.launch {
                                val stream = repository.getStreamForChannel(channel)
                                playbackManager.setChannelForSlot(slotIdx, channel, stream)
                                repository.saveSlotChannel(slotIdx, channel.identifier)
                                currentDestination = NavigationDestination.MULTIVIEW
                            }
                        }
                    )
                }

                NavigationDestination.LIVE_TV -> {
                    LiveTvScreen(
                        channels = channels,
                        favorites = favorites,
                        onToggleFavorite = { repository.toggleFavorite(it) },
                        onWatchInSlot = { channel, slotIdx ->
                            scope.launch {
                                val stream = repository.getStreamForChannel(channel)
                                playbackManager.setChannelForSlot(slotIdx, channel, stream)
                                repository.saveSlotChannel(slotIdx, channel.identifier)
                                currentDestination = NavigationDestination.MULTIVIEW
                            }
                        }
                    )
                }

                NavigationDestination.HOME -> {
                    HomeScreen(
                        channels = channels,
                        recordings = recordings,
                        favorites = favorites,
                        onOpenMultiview = { currentDestination = NavigationDestination.MULTIVIEW },
                        onPlayChannel = { channel, slotIdx ->
                            scope.launch {
                                val stream = repository.getStreamForChannel(channel)
                                playbackManager.setChannelForSlot(slotIdx, channel, stream)
                                repository.saveSlotChannel(slotIdx, channel.identifier)
                                currentDestination = NavigationDestination.MULTIVIEW
                            }
                        },
                        onPlayRecording = { recording ->
                            scope.launch {
                                val stream = repository.getStreamForRecording(recording)
                                val syntheticChannel = TabloChannel(
                                    identifier = recording.identifier,
                                    callSign = recording.title,
                                    network = "DVR"
                                )
                                playbackManager.setChannelForSlot(0, syntheticChannel, stream)
                                playbackManager.enterSoloMode(0)
                                currentDestination = NavigationDestination.MULTIVIEW
                            }
                        }
                    )
                }

                NavigationDestination.LIBRARY -> {
                    LibraryScreen(
                        recordings = recordings,
                        onPlayRecording = { recording ->
                            scope.launch {
                                val stream = repository.getStreamForRecording(recording)
                                val syntheticChannel = TabloChannel(
                                    identifier = recording.identifier,
                                    callSign = recording.title,
                                    network = "DVR"
                                )
                                playbackManager.setChannelForSlot(0, syntheticChannel, stream)
                                playbackManager.enterSoloMode(0)
                                currentDestination = NavigationDestination.MULTIVIEW
                            }
                        }
                    )
                }

                NavigationDestination.SEARCH -> {
                    SearchScreen(
                        channels = channels,
                        recordings = recordings,
                        onSelectChannel = { channel ->
                            scope.launch {
                                val stream = repository.getStreamForChannel(channel)
                                playbackManager.setChannelForSlot(0, channel, stream)
                                playbackManager.enterSoloMode(0)
                                currentDestination = NavigationDestination.MULTIVIEW
                            }
                        },
                        onSelectRecording = { recording ->
                            scope.launch {
                                val stream = repository.getStreamForRecording(recording)
                                val syntheticChannel = TabloChannel(
                                    identifier = recording.identifier,
                                    callSign = recording.title,
                                    network = "DVR"
                                )
                                playbackManager.setChannelForSlot(0, syntheticChannel, stream)
                                playbackManager.enterSoloMode(0)
                                currentDestination = NavigationDestination.MULTIVIEW
                            }
                        }
                    )
                }

                NavigationDestination.SETTINGS -> {
                    SettingsScreen(
                        repository = repository,
                        activeDevice = activeDevice,
                        isLoading = isLoading,
                        statusMessage = statusMessage
                    )
                }
            }

            // Channel Picker Dialog
            targetPickerSlotIndex?.let { slotIdx ->
                ChannelPickerDialog(
                    targetSlotIndex = slotIdx,
                    channels = channels,
                    favorites = favorites,
                    onToggleFavorite = { repository.toggleFavorite(it) },
                    onChannelSelected = { selectedCh ->
                        targetPickerSlotIndex = null
                        scope.launch {
                            val stream = repository.getStreamForChannel(selectedCh)
                            playbackManager.setChannelForSlot(slotIdx, selectedCh, stream)
                            repository.saveSlotChannel(slotIdx, selectedCh.identifier)
                        }
                    },
                    onDismiss = { targetPickerSlotIndex = null }
                )
            }
        }
    }
}
