package com.example.network

import android.util.Log
import com.example.model.TabloChannel
import com.example.model.TabloDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TabloChannelService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "TabloChannelService"
        private const val CLOUD_HOST = "https://lighthousetv.ewscloud.com"
        private const val CLOUD_UA = "Tablo-FAST/2.0.0 (Mobile; iPhone; iOS 16.6)"
        private const val LOCAL_UA = "Tablo-FAST/1.7.0 (Mobile; iPhone; iOS 18.4)"
    }

    suspend fun getChannels(device: TabloDevice?): List<TabloChannel> = withContext(Dispatchers.IO) {
        if (device == null) {
            return@withContext getFallbackChannels()
        }

        val channels = mutableListOf<TabloChannel>()

        // 1. Try cloud channels if token is present
        if (device.lighthouseToken.isNotEmpty() && device.accountToken.isNotEmpty()) {
            try {
                val cloudUrl = "$CLOUD_HOST/api/v2/account/${device.lighthouseToken}/guide/channels/"
                val req = Request.Builder()
                    .url(cloudUrl)
                    .addHeader("User-Agent", CLOUD_UA)
                    .addHeader("Authorization", "Bearer ${device.accountToken}")
                    .addHeader("Lighthouse", device.lighthouseToken)
                    .get()
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val rawJson = resp.body?.string() ?: "[]"
                    val jsonArray = JSONArray(rawJson)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optString("identifier")
                        if (id.isEmpty()) continue
                        val callSign = obj.optString("call_sign", obj.optString("display_name", "CH"))
                        val major = obj.optInt("major", 0)
                        val minor = obj.optInt("minor", 0)
                        val network = obj.optString("network", "")
                        val kind = obj.optString("kind", "ott")

                        var logoUrl: String? = null
                        val logos = obj.optJSONArray("logos")
                        if (logos != null && logos.length() > 0) {
                            for (l in 0 until logos.length()) {
                                val lg = logos.getJSONObject(l)
                                val url = lg.optString("url")
                                val kindLogo = lg.optString("kind")
                                if (kindLogo == "originalLarge" || kindLogo == "lightLarge") {
                                    logoUrl = url
                                    break
                                }
                                if (logoUrl == null && url.isNotEmpty()) {
                                    logoUrl = url
                                }
                            }
                        }

                        channels.add(
                            TabloChannel(
                                identifier = id,
                                callSign = callSign,
                                major = major,
                                minor = minor,
                                network = network,
                                kind = kind,
                                logoUrl = logoUrl
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching cloud channels: ${e.message}")
            }
        }

        // 2. Try local device channels (OTA)
        if (device.localUrl.isNotEmpty()) {
            try {
                val path = "/guide/channels"
                val localReqBuilder = Request.Builder()
                    .url(device.localUrl.trimEnd('/') + path)
                    .addHeader("User-Agent", LOCAL_UA)
                    .get()

                // Optional HMAC auth for 4th Gen local devices if available
                try {
                    val (authHeader, dateHeader) = TabloHmac.makeDeviceAuth("GET", path)
                    localReqBuilder.addHeader("Authorization", authHeader)
                    localReqBuilder.addHeader("Date", dateHeader)
                } catch (_: Exception) {}

                val localResp = client.newCall(localReqBuilder.build()).execute()
                if (localResp.isSuccessful) {
                    val pathsArray = JSONArray(localResp.body?.string() ?: "[]")
                    val pathsToFetch = mutableListOf<String>()
                    for (i in 0 until minOf(pathsArray.length(), 100)) {
                        pathsToFetch.add(pathsArray.getString(i))
                    }

                    if (pathsToFetch.isNotEmpty()) {
                        // Attempt POST /batch first (documented high-performance batch retrieval)
                        var batchSucceeded = false
                        try {
                            val batchArray = JSONArray(pathsToFetch)
                            val jsonMediaType = "application/json; charset=utf-8".toMediaType()
                            val batchReq = Request.Builder()
                                .url(device.localUrl.trimEnd('/') + "/batch")
                                .addHeader("User-Agent", LOCAL_UA)
                                .post(batchArray.toString().toRequestBody(jsonMediaType))
                                .build()

                            val batchResp = client.newCall(batchReq).execute()
                            if (batchResp.isSuccessful) {
                                val batchJson = JSONObject(batchResp.body?.string() ?: "{}")
                                for (chPath in pathsToFetch) {
                                    val chObj = batchJson.optJSONObject(chPath) ?: continue
                                    parseChannelFromObject(chObj, chPath)?.let { parsed ->
                                        if (channels.none { it.identifier == parsed.identifier }) {
                                            channels.add(parsed)
                                        }
                                    }
                                }
                                batchSucceeded = channels.isNotEmpty()
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Batch channel retrieval failed, falling back to sequential: ${e.message}")
                        }

                        // Fallback: fetch individual channels if batch failed
                        if (!batchSucceeded) {
                            for (chPath in pathsToFetch) {
                                try {
                                    val chReq = Request.Builder()
                                        .url(device.localUrl.trimEnd('/') + chPath)
                                        .addHeader("User-Agent", LOCAL_UA)
                                        .get()
                                        .build()
                                    val chResp = client.newCall(chReq).execute()
                                    if (chResp.isSuccessful) {
                                        val chObj = JSONObject(chResp.body?.string() ?: "{}")
                                        parseChannelFromObject(chObj, chPath)?.let { parsed ->
                                            if (channels.none { it.identifier == parsed.identifier }) {
                                                channels.add(parsed)
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching local OTA channels: ${e.message}")
            }
        }

        if (channels.isEmpty()) {
            getFallbackChannels()
        } else {
            channels.sorted()
        }
    }

    private fun parseChannelFromObject(chObj: JSONObject, fallbackIdentifier: String): TabloChannel? {
        val cInfo = chObj.optJSONObject("channel") ?: chObj
        val ident = cInfo.optString("channel_identifier", cInfo.optString("identifier", fallbackIdentifier))
        val callSign = cInfo.optString("call_sign", cInfo.optString("display_name", "OTA"))
        val major = cInfo.optInt("major", 0)
        val minor = cInfo.optInt("minor", 0)
        val network = cInfo.optString("network", "")

        var logoUrl: String? = null
        val logos = cInfo.optJSONArray("logos")
        if (logos != null && logos.length() > 0) {
            for (l in 0 until logos.length()) {
                val lg = logos.getJSONObject(l)
                val u = lg.optString("url")
                if (u.isNotEmpty()) {
                    logoUrl = u
                    break
                }
            }
        }

        return TabloChannel(
            identifier = ident,
            callSign = callSign,
            major = major,
            minor = minor,
            network = network,
            kind = "ota",
            logoUrl = logoUrl
        )
    }

    fun getFallbackChannels(): List<TabloChannel> {
        return listOf(
            TabloChannel(
                identifier = "ota_4_1_nbc",
                callSign = "WNBC",
                major = 4,
                minor = 1,
                network = "NBC",
                kind = "ota",
                logoUrl = null
            ),
            TabloChannel(
                identifier = "ota_5_1_cbs",
                callSign = "WCBS",
                major = 5,
                minor = 1,
                network = "CBS",
                kind = "ota",
                logoUrl = null
            ),
            TabloChannel(
                identifier = "ota_7_1_abc",
                callSign = "WABC",
                major = 7,
                minor = 1,
                network = "ABC",
                kind = "ota",
                logoUrl = null
            ),
            TabloChannel(
                identifier = "ota_9_1_fox",
                callSign = "WNYW",
                major = 9,
                minor = 1,
                network = "FOX",
                kind = "ota",
                logoUrl = null
            ),
            TabloChannel(
                identifier = "ota_11_1_pix",
                callSign = "WPIX",
                major = 11,
                minor = 1,
                network = "CW",
                kind = "ota",
                logoUrl = null
            ),
            TabloChannel(
                identifier = "ota_13_1_pbs",
                callSign = "WNET",
                major = 13,
                minor = 1,
                network = "PBS",
                kind = "ota",
                logoUrl = null
            ),
            TabloChannel(
                identifier = "ott_sports_fast_1",
                callSign = "STADIUM",
                major = 0,
                minor = 0,
                network = "Sports",
                kind = "ott",
                logoUrl = null
            ),
            TabloChannel(
                identifier = "ott_news_fast_1",
                callSign = "LIVE NEWS",
                major = 0,
                minor = 0,
                network = "News",
                kind = "ott",
                logoUrl = null
            )
        )
    }
}
