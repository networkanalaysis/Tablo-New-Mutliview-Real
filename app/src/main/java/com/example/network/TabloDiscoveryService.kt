package com.example.network

import android.util.Log
import com.example.model.TabloDevice
import com.example.model.TabloDiscoveredCpe
import com.example.model.TabloServerInfo
import com.example.model.TabloTuner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.UUID
import java.util.concurrent.TimeUnit

class TabloDiscoveryService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "TabloDiscovery"
        const val DEFAULT_TABLO_PORT = 8885
        const val ASSOC_SERVER_URL = "https://api.tablotv.com/assocserver/getipinfo/"
        const val UDP_BROADCAST_PORT = 8881
        const val UDP_LISTEN_PORT = 8882
    }

    /**
     * Primary discovery: Tablo Association Server (https://api.tablotv.com/assocserver/getipinfo/)
     * Documented by official and community API docs to discover local Tablos by private IP.
     */
    suspend fun discoverViaAssocServer(): List<TabloDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<TabloDevice>()
        try {
            val req = Request.Builder()
                .url(ASSOC_SERVER_URL)
                .addHeader("User-Agent", "Tablo-Multiview/1.0 (Android TV)")
                .get()
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: "{}"
                val json = JSONObject(body)
                val cpes = json.optJSONArray("cpes") ?: JSONArray()
                for (i in 0 until cpes.length()) {
                    val cpe = cpes.getJSONObject(i)
                    val serverId = cpe.optString("serverid", cpe.optString("server_id", ""))
                    val name = cpe.optString("name", "Tablo TV")
                    val privateIp = cpe.optString("private_ip", "")
                    val httpPort = cpe.optInt("http", DEFAULT_TABLO_PORT)
                    val version = cpe.optString("server_version", "")
                    val board = cpe.optString("board", "")

                    if (privateIp.isNotEmpty()) {
                        val localUrl = "http://$privateIp:$httpPort"
                        Log.d(TAG, "Discovered Tablo via assocserver: $name at $localUrl (SID: $serverId)")
                        devices.add(
                            TabloDevice(
                                sid = serverId.ifEmpty { "SID_${privateIp.replace(".", "_")}" },
                                name = name,
                                localUrl = localUrl,
                                version = version,
                                board = board,
                                clientId = UUID.randomUUID().toString()
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Assocserver discovery error: ${e.message}")
        }
        devices
    }

    /**
     * Local UDP discovery: Broadcast on port 8881, listen on 8882.
     * Documented in unofficial Tablo API documentation.
     */
    suspend fun discoverViaUdp(timeoutMs: Int = 1500): List<TabloDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<TabloDevice>()
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket().apply {
                broadcast = true
                soTimeout = timeoutMs
            }

            val queryMsg = "{\"command\":\"discover\"}"
            val queryBytes = queryMsg.toByteArray(Charsets.UTF_8)
            val packet = DatagramPacket(
                queryBytes,
                queryBytes.size,
                InetAddress.getByName("255.255.255.255"),
                UDP_BROADCAST_PORT
            )
            socket.send(packet)

            val recvBuffer = ByteArray(4096)
            val recvPacket = DatagramPacket(recvBuffer, recvBuffer.size)
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    socket.receive(recvPacket)
                    val data = String(recvPacket.data, 0, recvPacket.length, Charsets.UTF_8)
                    val senderIp = recvPacket.address.hostAddress ?: ""
                    Log.d(TAG, "Received UDP discovery response from $senderIp: $data")

                    val json = JSONObject(data)
                    val serverId = json.optString("server_id", json.optString("serverId", "SID_UDP"))
                    val name = json.optString("name", "Tablo ($senderIp)")
                    val port = json.optInt("http", DEFAULT_TABLO_PORT)
                    val localUrl = "http://$senderIp:$port"

                    if (devices.none { it.localUrl == localUrl }) {
                        devices.add(
                            TabloDevice(
                                sid = serverId,
                                name = name,
                                localUrl = localUrl,
                                clientId = UUID.randomUUID().toString()
                            )
                        )
                    }
                } catch (timeout: java.net.SocketTimeoutException) {
                    break
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "UDP discovery completed/exception: ${e.message}")
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
        devices
    }

    /**
     * Probes a specific host/IP on port 8885 using documented /server/info, falling back to /ping.
     */
    suspend fun pingDevice(hostOrIp: String, port: Int = DEFAULT_TABLO_PORT): TabloDevice? = withContext(Dispatchers.IO) {
        val cleanHost = hostOrIp.removePrefix("http://").removePrefix("https://").trimEnd('/')
        val url = "http://$cleanHost:$port"

        // Try /server/info first (documented REST API)
        try {
            val req = Request.Builder()
                .url("$url/server/info")
                .addHeader("User-Agent", "Tablo-Multiview/1.0 (Android TV)")
                .get()
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: "{}"
                val json = JSONObject(body)
                val sid = json.optString("server_id", json.optString("sid", "SID_LOCAL"))
                val name = json.optString("name", "Tablo ($cleanHost)")
                val version = json.optString("version", "")
                val modelObj = json.optJSONObject("model")
                val tunerCount = modelObj?.optInt("tuners", 2) ?: json.optInt("tuners", 2)

                Log.d(TAG, "Discovered Tablo via /server/info at $url (SID=$sid, tuners=$tunerCount)")
                return@withContext TabloDevice(
                    sid = sid,
                    name = name,
                    localUrl = url,
                    version = version,
                    tunerCount = tunerCount,
                    clientId = UUID.randomUUID().toString()
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "/server/info probe failed for $url: ${e.message}")
        }

        // Fallback to /ping
        try {
            val req = Request.Builder()
                .url("$url/ping")
                .addHeader("User-Agent", "Tablo-Multiview/1.0 (Android TV)")
                .get()
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: "{}"
                val json = JSONObject(body)
                val sid = json.optString("sid", "SID_LOCAL")
                Log.d(TAG, "Discovered Tablo via /ping at $url (SID=$sid)")
                return@withContext TabloDevice(
                    sid = sid,
                    name = "Tablo ($cleanHost)",
                    localUrl = url,
                    clientId = UUID.randomUUID().toString()
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "/ping probe failed for $url: ${e.message}")
        }

        null
    }

    /**
     * Comprehensive discovery: checks assocserver, UDP broadcast, and optional local fallback.
     */
    suspend fun discoverAllDevices(): List<TabloDevice> = withContext(Dispatchers.IO) {
        val found = mutableListOf<TabloDevice>()

        // 1. Try assocserver first (most reliable on modern Wi-Fi networks)
        val assocDevs = discoverViaAssocServer()
        found.addAll(assocDevs)

        // 2. Try UDP broadcast if assocserver found nothing
        if (found.isEmpty()) {
            val udpDevs = discoverViaUdp(1200)
            found.addAll(udpDevs)
        }

        // Enrich server info for discovered devices if possible
        found.map { dev ->
            val info = fetchServerInfo(dev.localUrl)
            if (info != null) {
                dev.copy(
                    name = info.name.ifEmpty { dev.name },
                    version = info.version.ifEmpty { dev.version },
                    tunerCount = info.tunerCount
                )
            } else {
                dev
            }
        }
    }

    /**
     * Queries GET /server/info from Tablo local REST API (port 8885).
     */
    suspend fun fetchServerInfo(localUrl: String): TabloServerInfo? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("${localUrl.trimEnd('/')}/server/info")
                .addHeader("User-Agent", "Tablo-Multiview/1.0 (Android TV)")
                .get()
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val json = JSONObject(resp.body?.string() ?: "{}")
                val sid = json.optString("server_id", "")
                val name = json.optString("name", "Tablo TV")
                val version = json.optString("version", "")
                val localAddr = json.optString("local_address", "")
                val tz = json.optString("timezone", "")
                val avail = json.optString("availability", "")

                val modelObj = json.optJSONObject("model")
                val modelType = modelObj?.optString("type", "") ?: json.optString("model", "")
                val tuners = modelObj?.optInt("tuners", 2) ?: json.optInt("tuners", 2)

                return@withContext TabloServerInfo(
                    serverId = sid,
                    name = name,
                    version = version,
                    localAddress = localAddr,
                    model = modelType,
                    tunerCount = tuners,
                    timezone = tz,
                    availability = avail
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to fetch /server/info from $localUrl: ${e.message}")
        }
        null
    }

    /**
     * Queries GET /server/tuners from Tablo local REST API (port 8885).
     * Documented response is an array of tuner states: [{"in_use": false, "channel": null, "recording": null}, ...]
     */
    suspend fun fetchTuners(localUrl: String): List<TabloTuner> = withContext(Dispatchers.IO) {
        val tuners = mutableListOf<TabloTuner>()
        try {
            val req = Request.Builder()
                .url("${localUrl.trimEnd('/')}/server/tuners")
                .addHeader("User-Agent", "Tablo-Multiview/1.0 (Android TV)")
                .get()
                .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val raw = resp.body?.string() ?: "[]"
                val arr = JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val inUse = obj.optBoolean("in_use", false)
                    val ch = if (obj.isNull("channel")) null else obj.optString("channel")
                    val rec = if (obj.isNull("recording")) null else obj.optString("recording")
                    tuners.add(
                        TabloTuner(
                            index = i,
                            inUse = inUse,
                            channelPath = ch,
                            recordingPath = rec
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Failed to fetch /server/tuners from $localUrl: ${e.message}")
        }
        tuners
    }
}

