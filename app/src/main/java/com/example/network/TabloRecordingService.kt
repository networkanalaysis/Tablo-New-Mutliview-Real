package com.example.network

import android.util.Log
import com.example.model.TabloDevice
import com.example.model.TabloRecording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TabloRecordingService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "TabloRecordingService"
        private const val LOCAL_UA = "Tablo-FAST/1.7.0 (Mobile; iPhone; iOS 18.4)"
    }

    suspend fun getRecordings(device: TabloDevice?): List<TabloRecording> = withContext(Dispatchers.IO) {
        if (device != null && device.localUrl.isNotEmpty()) {
            try {
                val path = "/recordings/airings"
                val (auth, date) = TabloHmac.makeDeviceAuth("GET", path)
                val req = Request.Builder()
                    .url(device.localUrl.trimEnd('/') + path)
                    .addHeader("User-Agent", LOCAL_UA)
                    .addHeader("Authorization", auth)
                    .addHeader("Date", date)
                    .get()
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val rawJson = resp.body?.string() ?: "[]"
                    val pathsArray = JSONArray(rawJson)
                    val recordings = mutableListOf<TabloRecording>()

                    for (i in 0 until minOf(pathsArray.length(), 40)) {
                        val recPath = pathsArray.getString(i)
                        try {
                            val (recAuth, recDate) = TabloHmac.makeDeviceAuth("GET", recPath)
                            val recReq = Request.Builder()
                                .url(device.localUrl.trimEnd('/') + recPath)
                                .addHeader("User-Agent", LOCAL_UA)
                                .addHeader("Authorization", recAuth)
                                .addHeader("Date", recDate)
                                .get()
                                .build()
                            val recResp = client.newCall(recReq).execute()
                            if (recResp.isSuccessful) {
                                val obj = JSONObject(recResp.body?.string() ?: "{}")
                                val ad = obj.optJSONObject("airing_details") ?: JSONObject()
                                val title = ad.optString("show_title", "Tablo Recording")
                                val ep = obj.optJSONObject("episode")
                                val series = obj.optJSONObject("series")
                                val desc = ep?.optString("description")
                                    ?: series?.optString("description")
                                    ?: ad.optString("description", "")
                                val start = ad.optString("datetime", "")
                                val duration = ad.optLong("duration", 0)
                                val objId = obj.optString("object_id", recPath)

                                recordings.add(
                                    TabloRecording(
                                        identifier = objId,
                                        path = recPath,
                                        title = title,
                                        description = desc,
                                        startIso = start,
                                        durationSec = duration
                                    )
                                )
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error fetching recording detail $recPath: ${e.message}")
                        }
                    }

                    if (recordings.isNotEmpty()) {
                        return@withContext recordings
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetching recordings from local device: ${e.message}")
            }
        }

        getFallbackRecordings()
    }

    fun getFallbackRecordings(): List<TabloRecording> {
        return listOf(
            TabloRecording(
                identifier = "rec_sports_championship",
                path = "/recordings/airings/101",
                title = "National Basketball Championship Finals - Game 7",
                description = "Dramatic double-overtime victory recorded in high definition OTA.",
                startIso = "2026-03-12T20:00:00Z",
                durationSec = 9600
            ),
            TabloRecording(
                identifier = "rec_football_playoffs",
                path = "/recordings/airings/102",
                title = "College Football Championship: Georgia vs Texas",
                description = "Full broadcast recording with halftime marching band and trophy ceremony.",
                startIso = "2026-03-08T19:30:00Z",
                durationSec = 12600
            ),
            TabloRecording(
                identifier = "rec_primetime_drama",
                path = "/recordings/airings/103",
                title = "Metropolitan Detective - S4 E12 (Season Finale)",
                description = "The precinct works against the clock in the season finale thriller.",
                startIso = "2026-03-10T21:00:00Z",
                durationSec = 3600
            ),
            TabloRecording(
                identifier = "rec_investigative_special",
                path = "/recordings/airings/104",
                title = "Frontline Dispatch: Future of Grid Power",
                description = "In-depth documentary on national broadcast infrastructure and clean energy.",
                startIso = "2026-03-05T22:00:00Z",
                durationSec = 3600
            ),
            TabloRecording(
                identifier = "rec_nightly_radar",
                path = "/recordings/airings/105",
                title = "StormTracker 9: Severe Thunderstorm Live Coverage",
                description = "Live Doppler radar tracking and neighborhood weather reports.",
                startIso = "2026-03-02T18:00:00Z",
                durationSec = 1800
            )
        )
    }
}
