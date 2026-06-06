package com.example

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {

    private const val GITHUB_API_URL =
        "https://api.github.com/repos/sk-sazzad/finance-tracker/releases/latest"
    private const val APK_DOWNLOAD_URL =
        "https://github.com/sk-sazzad/finance-tracker/releases/tag/latest"

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val latestVersionName: String,
        val apkSizeBytes: Long,
        val downloadUrl: String,
        val apkUpdatedAt: String = ""
    )

    suspend fun checkForUpdate(context: Context): UpdateInfo {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(GITHUB_API_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode != 200) {
                    connection.disconnect()
                    return@withContext UpdateInfo(false, "", 0, APK_DOWNLOAD_URL)
                }

                val response = connection.inputStream.bufferedReader().readText()
                connection.disconnect()

                val json = JSONObject(response)
                val assets = json.getJSONArray("assets")

                if (assets.length() > 0) {
                    val asset = assets.getJSONObject(0)
                    val apkUpdatedAt = asset.getString("updated_at") // ISO date string
                    val apkSizeBytes = asset.getLong("size")
                    val releaseName = json.getString("name")

                    // Get stored last known build time from DataStore
                    val storedTime = getStoredBuildTime(context)
                    val hasUpdate = apkUpdatedAt != storedTime

                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersionName = releaseName,
                        apkSizeBytes = apkSizeBytes,
                        downloadUrl = APK_DOWNLOAD_URL,
                        apkUpdatedAt = apkUpdatedAt
                    )
                } else {
                    UpdateInfo(false, "", 0, APK_DOWNLOAD_URL)
                }
            } catch (e: Exception) {
                // No internet or API error — silently fail
                UpdateInfo(false, "", 0, APK_DOWNLOAD_URL)
            }
        }
    }

    suspend fun markUpdateSeen(context: Context, buildTime: String) {
        // Save the current APK updated_at to DataStore so we don't show again
        context.updateDataStore.edit { it[UPDATE_TIME_KEY] = buildTime }
    }

    private suspend fun getStoredBuildTime(context: Context): String {
        return context.updateDataStore.data.map {
            it[UPDATE_TIME_KEY] ?: ""
        }.first()
    }

    private val UPDATE_TIME_KEY = stringPreferencesKey("last_apk_build_time")

    private val Context.updateDataStore by preferencesDataStore(name = "update_prefs")
}
