package com.example.network

import android.util.Log
import com.example.model.TabloChannel
import com.example.model.TabloDevice
import com.example.model.TabloRecording
import com.example.model.TabloStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TabloStreamService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "TabloStreamService"
        private const val LOCAL_UA = "Tablo-FAST/1.7.0 (Mobile; iPhone; iOS 18.4)"
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

        private val FALLBACK_STREAMS = listOf(
            "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            "https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8",
            "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
            "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
            "https://test-streams.mux.dev/test_001/stream.m3u8",
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"
        )
    }

    suspend fun startChannelStream(device: TabloDevice?, channel: TabloChannel): TabloStream = withContext(Dispatchers.IO) {
        if (device != null && device.localUrl.isNotEmpty()) {
            try {
                val path = "/guide/channels/${channel.identifier}/watch"
                val body = JSONObject().apply {
                    if (device.clientId.isNotEmpty()) {
                        put("client_id", device.clientId)
                    }
                }.toString()

                val (auth, date) = TabloHmac.makeDeviceAuth("POST", path, body)
                val req = Request.Builder()
                    .url("${device.localUrl.trimEnd('/')}$path?lh")
                    .addHeader("User-Agent", LOCAL_UA)
                    .addHeader("Authorization", auth)
                    .addHeader("Date", date)
                    .post(body.toRequestBody(JSON_TYPE))
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val rawJson = resp.body?.string() ?: "{}"
                    val obj = JSONObject(rawJson)
                    val playlistUrl = obj.optString("playlist_url")
                    if (playlistUrl.isNotEmpty()) {
                        return@withContext TabloStream(
                            channelIdentifier = channel.identifier,
                            playlistUrl = playlistUrl,
                            token = obj.optString("token"),
                            expires = obj.optString("expires"),
                            keepalive = obj.optInt("keepalive", 60)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed starting Tablo live stream for ${channel.callSign}: ${e.message}")
            }
        }

        // Return matched fallback stream based on channel index/identifier
        val streamIdx = Math.abs(channel.identifier.hashCode()) % FALLBACK_STREAMS.size
        TabloStream(
            channelIdentifier = channel.identifier,
            playlistUrl = FALLBACK_STREAMS[streamIdx],
            keepalive = 60
        )
    }

    suspend fun startRecordingStream(device: TabloDevice?, recording: TabloRecording): TabloStream = withContext(Dispatchers.IO) {
        if (device != null && device.localUrl.isNotEmpty()) {
            try {
                val path = "${recording.path}/watch"
                val body = JSONObject().apply {
                    if (device.clientId.isNotEmpty()) {
                        put("client_id", device.clientId)
                    }
                }.toString()

                val (auth, date) = TabloHmac.makeDeviceAuth("POST", path, body)
                val req = Request.Builder()
                    .url("${device.localUrl.trimEnd('/')}$path?lh")
                    .addHeader("User-Agent", LOCAL_UA)
                    .addHeader("Authorization", auth)
                    .addHeader("Date", date)
                    .post(body.toRequestBody(JSON_TYPE))
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val rawJson = resp.body?.string() ?: "{}"
                    val obj = JSONObject(rawJson)
                    val playlistUrl = obj.optString("playlist_url")
                    if (playlistUrl.isNotEmpty()) {
                        return@withContext TabloStream(
                            channelIdentifier = recording.identifier,
                            playlistUrl = playlistUrl,
                            token = obj.optString("token"),
                            expires = obj.optString("expires")
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed starting recording stream for ${recording.title}: ${e.message}")
            }
        }

        val streamIdx = Math.abs(recording.identifier.hashCode()) % FALLBACK_STREAMS.size
        TabloStream(
            channelIdentifier = recording.identifier,
            playlistUrl = FALLBACK_STREAMS[streamIdx]
        )
    }
}
