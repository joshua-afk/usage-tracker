package com.joshua_afk.usagetracker.sync

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.serialization.encodeToString
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

sealed interface PostResult {
    data object Success : PostResult
    data class HttpError(val code: Int) : PostResult
    data class NetworkError(val message: String) : PostResult
}

class SyncClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    /** Blocking; call off the main thread. Only a 2xx response counts as [PostResult.Success]. */
    fun post(url: String, token: String, payload: SyncPayload): PostResult {
        val body = ApiJson.encodeToString(payload).toRequestBody(JSON_TYPE)
        val request = try {
            Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .post(body)
                .build()
        } catch (e: IllegalArgumentException) {
            return PostResult.NetworkError("Invalid server URL")
        }
        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) PostResult.Success else PostResult.HttpError(response.code)
            }
        } catch (e: IOException) {
            PostResult.NetworkError(e.message ?: e.javaClass.simpleName)
        }
    }

    private companion object {
        val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
