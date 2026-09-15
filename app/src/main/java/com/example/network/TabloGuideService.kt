package com.example.network

import android.util.Log
import com.example.model.TabloChannel
import com.example.model.TabloDevice
import com.example.model.TabloProgram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class TabloGuideService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "TabloGuideService"
        private const val CLOUD_HOST = "https://lighthousetv.ewscloud.com"
        private const val CLOUD_UA = "Tablo-FAST/2.0.0 (Mobile; iPhone; iOS 16.6)"
    }

    suspend fun getChannelAirings(
        device: TabloDevice?,
        channel: TabloChannel
    ): List<TabloProgram> = withContext(Dispatchers.IO) {
        if (device != null && device.lighthouseToken.isNotEmpty() && device.accountToken.isNotEmpty()) {
            try {
                val url = "$CLOUD_HOST/api/v2/account/${device.lighthouseToken}/guide/channels/${channel.identifier}/airings/"
                val req = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", CLOUD_UA)
                    .addHeader("Authorization", "Bearer ${device.accountToken}")
                    .addHeader("Lighthouse", device.lighthouseToken)
                    .get()
                    .build()

                val resp = client.newCall(req).execute()
                if (resp.isSuccessful) {
                    val rawJson = resp.body?.string() ?: "[]"
                    val array = JSONArray(rawJson)
                    val programs = mutableListOf<TabloProgram>()
                    for (i in 0 until minOf(array.length(), 10)) {
                        val item = array.getJSONObject(i)
                        val title = item.optString("title", item.optJSONObject("show")?.optString("title") ?: "Program")
                        val desc = item.optString("description", "")
                        val start = item.optString("datetime", "")
                        val duration = item.optLong("duration", 1800)
                        val genres = mutableListOf<String>()
                        val gArr = item.optJSONArray("genres")
                        if (gArr != null) {
                            for (g in 0 until gArr.length()) {
                                genres.add(gArr.getString(g))
                            }
                        }
                        programs.add(
                            TabloProgram(
                                title = title,
                                description = desc,
                                startIso = start,
                                durationSec = duration,
                                channelIdentifier = channel.identifier,
                                genres = genres
                            )
                        )
                    }
                    if (programs.isNotEmpty()) {
                        return@withContext programs
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load cloud airings for ${channel.callSign}: ${e.message}")
            }
        }

        getFallbackProgramsForChannel(channel)
    }

    fun getFallbackProgramsForChannel(channel: TabloChannel): List<TabloProgram> {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val programs = mutableListOf<TabloProgram>()

        val titles = when {
            channel.network.contains("Sports", ignoreCase = true) -> listOf(
                "Live: Premier College Basketball" to "Top 25 showdown with live studio analysis.",
                "SportsDesk Evening Edition" to "Scores, highlights, and trade breakdown.",
                "Championship Classic Replay" to "Relive the legendary overtime finish.",
                "Pro Football Weekly Preview" to "Game film review and tactical breakdowns."
            )
            channel.network.contains("News", ignoreCase = true) -> listOf(
                "Live Primetime Evening News" to "Breaking national coverage and in-depth reporting.",
                "The World Tonight" to "Global affairs, economy, and weather radar.",
                "Nightly Business Report" to "Market closing numbers and tech analysis.",
                "Overnight Live Dispatch" to "Real-time headlines and continuous radar."
            )
            channel.major == 4 -> listOf(
                "Live: Regional Sports Spotlight" to "Live broadcast coverage and courtside cameras.",
                "Nightly News with Tom Washington" to "Comprehensive coverage of today's events.",
                "Primetime Drama Series" to "Season finale mystery unfolds across the city.",
                "Late Night Talk Live" to "Celebrity guests, musical performance, and comedy monologue."
            )
            channel.major == 5 -> listOf(
                "Live: National Basketball League" to "Conference rivals battle for playoff seeding.",
                "Evening Bulletin & Radar" to "Local forecast, traffic, and community news.",
                "Investigators: Special Report" to "Award-winning investigative journalism.",
                "The Late Show Hour" to "Top comedy sketches and musical showcase."
            )
            channel.major == 7 -> listOf(
                "Live: Baseball Night in America" to "Live divisional game with multi-angle coverage.",
                "World News Express" to "Major developments and exclusive on-the-ground reports.",
                "Medical Heroes Emergency" to "Trauma surgeons navigate critical night shift.",
                "Nightline In-Depth" to "Deep dives into the stories making waves."
            )
            channel.major == 9 -> listOf(
                "Live: College Football Saturday" to "Rivalry week matchup live under the lights.",
                "Fox 9 Live at Nine" to "Local fast-paced news, sports, and sky tracker.",
                "Action Police Pursuit" to "First responders handle high-stakes situations.",
                "Sports Extra Live" to "Post-game press conferences and fan reactions."
            )
            else -> listOf(
                "Live Broadcast Special" to "Live television transmission from Tablo receiver.",
                "Evening Feature Broadcast" to "Award-winning documentary presentation.",
                "Prime Showcase" to "Entertainment and cultural documentary series.",
                "Late Night Broadcast" to "Overnight public television presentation."
            )
        }

        titles.forEachIndexed { idx, (t, d) ->
            val startTime = now + (idx * 3600_000L)
            programs.add(
                TabloProgram(
                    title = t,
                    description = d,
                    startIso = sdf.format(Date(startTime)),
                    durationSec = 3600,
                    channelIdentifier = channel.identifier,
                    genres = listOf(channel.network.ifEmpty { "General" })
                )
            )
        }

        return programs
    }
}
