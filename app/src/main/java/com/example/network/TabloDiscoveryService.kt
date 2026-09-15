package com.example.network

import android.util.Log
import com.example.model.TabloDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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
    }

    suspend fun pingDevice(hostOrIp: String, port: Int = DEFAULT_TABLO_PORT): TabloDevice? = withContext(Dispatchers.IO) {
        val cleanHost = hostOrIp.removePrefix("http://").removePrefix("https://").trimEnd('/')
        val url = "http://$cleanHost:$port"
        val req = Request.Builder()
            .url("$url/ping")
            .get()
            .build()
        try {
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                val body = resp.body?.string() ?: "{}"
                val json = JSONObject(body)
                val sid = json.optString("sid", "SID_LOCAL")
                Log.d(TAG, "Discovered Tablo at $url with SID=$sid")
                return@withContext TabloDevice(
                    sid = sid,
                    name = "Tablo 4th Gen ($cleanHost)",
                    localUrl = url,
                    clientId = UUID.randomUUID().toString()
                )
            }
        } catch (e: Exception) {
            Log.d(TAG, "Ping failed for $url: ${e.message}")
        }
        null
    }
}
