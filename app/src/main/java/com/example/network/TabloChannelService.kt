package com.example.network

import android.util.Log
import com.example.model.TabloChannel
import com.example.model.TabloDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
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
                val (authHeader, dateHeader) = TabloHmac.makeDeviceAuth("GET", path)
                val localReq = Request.Builder()
                    .url(device.localUrl.trimEnd('/') + path)
                    .addHeader("User-Agent", LOCAL_UA)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Date", dateHeader)
                    .get()
                    .build()

                val localResp = client.newCall(localReq).execute()
                if (localResp.isSuccessful) {
                    val pathsArray = JSONArray(localResp.body?.string() ?: "[]")
                    for (i in 0 until minOf(pathsArray.length(), 60)) {
                        val chPath = pathsArray.getString(i)
                        try {
                            val (chAuth, chDate) = TabloHmac.makeDeviceAuth("GET", chPath)
                            val chReq = Request.Builder()
                                .url(device.localUrl.trimEnd('/') + chPath)
                                .addHeader("User-Agent", LOCAL_UA)
                                .addHeader("Authorization", chAuth)
                                .addHeader("Date", chDate)
                                .get()
                                .build()
                            val chResp = client.newCall(chReq).execute()
                            if (chResp.isSuccessful) {
                                val chObj = JSONObject(chResp.body?.string() ?: "{}")
                                val cInfo = chObj.optJSONObject("channel") ?: chObj
                                val ident = cInfo.optString("channel_identifier", cInfo.optString("identifier", chPath))
                                val callSign = cInfo.optString("call_sign", "OTA")
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

                                if (channels.none { it.identifier == ident }) {
                                    channels.add(
                                        TabloChannel(
                                            identifier = ident,
                                            callSign = callSign,
                                            major = major,
                                            minor = minor,
                                            network = network,
                                            kind = "ota",
                                            logoUrl = logoUrl
                                        )
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            // ignore single channel failure
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
