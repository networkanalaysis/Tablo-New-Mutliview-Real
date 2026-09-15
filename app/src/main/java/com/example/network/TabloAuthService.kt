package com.example.network

import android.util.Log
import com.example.model.TabloDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class TabloAuthService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        private const val TAG = "TabloAuthService"
        private const val CLOUD_HOST = "https://lighthousetv.ewscloud.com"
        private const val USER_AGENT = "Tablo-FAST/2.0.0 (Mobile; iPhone; iOS 16.6)"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun discoverDevices(email: String, pass: String): List<TabloDevice> = withContext(Dispatchers.IO) {
        val loginJson = JSONObject().apply {
            put("email", email)
            put("password", pass)
        }

        val loginReq = Request.Builder()
            .url("$CLOUD_HOST/api/v2/login/")
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Content-Type", "application/json")
            .post(loginJson.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val loginResp = client.newCall(loginReq).execute()
        if (!loginResp.isSuccessful) {
            val err = loginResp.body?.string() ?: "Unknown error"
            throw TabloAuthException("Authentication failed (${loginResp.code}): $err")
        }

        val loginData = JSONObject(loginResp.body?.string() ?: "{}")
        val accessToken = loginData.optString("access_token")
        val tokenType = loginData.optString("token_type", "Bearer")
        val authHeader = "$tokenType $accessToken"

        val accountReq = Request.Builder()
            .url("$CLOUD_HOST/api/v2/account/")
            .addHeader("User-Agent", USER_AGENT)
            .addHeader("Authorization", authHeader)
            .get()
            .build()

        val accountResp = client.newCall(accountReq).execute()
        if (!accountResp.isSuccessful) {
            val err = accountResp.body?.string() ?: "Unknown error"
            throw TabloAuthException("Account lookup failed (${accountResp.code}): $err")
        }

        val accountData = JSONObject(accountResp.body?.string() ?: "{}")
        val profiles = accountData.optJSONArray("profiles")
        val devicesArray = accountData.optJSONArray("devices")

        if (profiles == null || profiles.length() == 0) {
            throw TabloAuthException("No user profiles found for this Tablo account.")
        }
        if (devicesArray == null || devicesArray.length() == 0) {
            throw TabloAuthException("No Tablo devices linked to this account.")
        }

        val profileId = profiles.getJSONObject(0).optString("identifier")
        val clientId = UUID.randomUUID().toString()
        val devices = mutableListOf<TabloDevice>()

        for (i in 0 until devicesArray.length()) {
            val devObj = devicesArray.getJSONObject(i)
            val serverId = devObj.optString("serverId")
            val devName = devObj.optString("name", "Tablo 4th Gen")
            val localUrl = devObj.optString("url", "http://192.168.1.100:8885")

            // Select device to obtain lighthouse token
            var lighthouseToken = ""
            try {
                val selectJson = JSONObject().apply {
                    put("pid", profileId)
                    put("sid", serverId)
                }
                val selectReq = Request.Builder()
                    .url("$CLOUD_HOST/api/v2/account/select/")
                    .addHeader("User-Agent", USER_AGENT)
                    .addHeader("Authorization", authHeader)
                    .post(selectJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                    .build()
                val selectResp = client.newCall(selectReq).execute()
                if (selectResp.isSuccessful) {
                    val selectData = JSONObject(selectResp.body?.string() ?: "{}")
                    lighthouseToken = selectData.optString("token")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed selecting device $serverId: ${e.message}")
            }

            devices.add(
                TabloDevice(
                    sid = serverId,
                    name = devName,
                    localUrl = localUrl,
                    lighthouseToken = lighthouseToken,
                    accountToken = accessToken,
                    clientId = clientId
                )
            )
        }

        devices
    }
}

class TabloAuthException(message: String) : Exception(message)
