package com.example.network

import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TabloHmac {
    private const val HASH_KEY = "6l8jU5N43cEilqItmT3U2M2PFM3qPziilXqau9ys"
    private const val DEVICE_KEY = "ljpg6ZkwShVv8aI12E2LP55Ep8vq1uYDPvX0DdTB"

    fun getDeviceDate(): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("GMT")
        return dateFormat.format(Date())
    }

    fun makeDeviceAuth(method: String, path: String, body: String = ""): Pair<String, String> {
        val date = getDeviceDate()
        val msgHash = if (body.isNotEmpty()) {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(body.toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } else {
            ""
        }

        val payload = "$method\n$path\n$msgHash\n$date"
        val mac = Mac.getInstance("HmacMD5")
        val secretKey = SecretKeySpec(HASH_KEY.toByteArray(Charsets.UTF_8), "HmacMD5")
        mac.init(secretKey)
        val sigBytes = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
        val sig = sigBytes.joinToString("") { "%02x".format(it) }

        return Pair("tablo:$DEVICE_KEY:$sig", date)
    }
}
