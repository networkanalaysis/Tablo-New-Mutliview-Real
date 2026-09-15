package com.example.model

/**
 * Tablo 4th Gen and Multiview domain models.
 */
data class TabloDevice(
    val sid: String,
    val name: String,
    val localUrl: String,
    val lighthouseToken: String = "",
    val accountToken: String = "",
    val clientId: String = ""
)

data class TabloChannel(
    val identifier: String,
    val callSign: String,
    val major: Int = 0,
    val minor: Int = 0,
    val network: String = "",
    val kind: String = "ota", // "ota" | "ott"
    val logoUrl: String? = null,
    val currentProgram: TabloProgram? = null
) : Comparable<TabloChannel> {
    val displayName: String
        get() = if (major > 0) "$major.$minor $callSign" else callSign

    override fun compareTo(other: TabloChannel): Int {
        val thisOta = major > 0
        val otherOta = other.major > 0
        if (thisOta != otherOta) {
            return if (thisOta) -1 else 1
        }
        if (thisOta) {
            val majorComp = major.compareTo(other.major)
            return if (majorComp != 0) majorComp else minor.compareTo(other.minor)
        }
        return callSign.compareTo(other.callSign, ignoreCase = true)
    }
}

data class TabloProgram(
    val title: String,
    val description: String = "",
    val startIso: String = "",
    val durationSec: Long = 1800,
    val channelIdentifier: String = "",
    val genres: List<String> = emptyList(),
    val episodeTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null
)

data class TabloRecording(
    val identifier: String,
    val path: String,
    val title: String,
    val description: String = "",
    val startIso: String = "",
    val durationSec: Long = 0,
    val thumbnail: String? = null
)

data class TabloStream(
    val channelIdentifier: String,
    val playlistUrl: String,
    val token: String? = null,
    val expires: String? = null,
    val keepalive: Int? = null
)

enum class MultiviewLayoutType(val slotCount: Int, val label: String) {
    QUAD_2X2(4, "2x2 Quad (4 Channels)"),
    DUAL_2UP(2, "2-Up Split (2 Channels)"),
    SOLO_1UP(1, "1-Up Single (1 Channel)")
}

data class MultiviewState(
    val layoutType: MultiviewLayoutType = MultiviewLayoutType.QUAD_2X2,
    val activeAudioSlot: Int = 0,
    val soloSlotIndex: Int? = null
)

enum class NavigationDestination(val title: String) {
    MULTIVIEW("Multiview"),
    GUIDE("Guide"),
    LIVE_TV("Live TV"),
    HOME("Home"),
    LIBRARY("Library"),
    SEARCH("Search"),
    SETTINGS("Settings")
}
