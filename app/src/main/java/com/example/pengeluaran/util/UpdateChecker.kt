package com.example.pengeluaran.util

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = ""
)

object UpdateChecker {
    // GANTI DENGAN USERNAME DAN REPO GITHUB ANDA
    private const val GITHUB_OWNER = "ivankw"
    private const val GITHUB_REPO = "expense-tracker"

    private val client = OkHttpClient()

    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "v1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "v1.0.0"
        }
    }

    suspend fun checkLatestRelease(context: Context): UpdateInfo = withContext(Dispatchers.IO) {
        val currentVersion = getCurrentVersion(context).trim().removePrefix("v")
        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext UpdateInfo(hasUpdate = false)

                val body = response.body?.string() ?: return@withContext UpdateInfo(hasUpdate = false)
                val json = JSONObject(body)

                val latestTag = json.optString("tag_name", "").trim()
                val cleanLatest = latestTag.removePrefix("v")
                val releaseNotes = json.optString("body", "Pembaruan versi baru tersedia.")

                // Ambil link APK dari list assets
                var apkUrl = json.optString("html_url", "")
                val assets = json.optJSONArray("assets")
                if (assets != null && assets.length() > 0) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url", apkUrl)
                            break
                        }
                    }
                }

                // Cek apakah versi GitHub berbeda dengan versi saat ini
                val isNewer = isVersionNewer(cleanLatest, currentVersion)

                UpdateInfo(
                    hasUpdate = isNewer,
                    latestVersion = latestTag,
                    releaseNotes = releaseNotes,
                    downloadUrl = apkUrl
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UpdateInfo(hasUpdate = false)
        }
    }

    // Pembanding versi semantik (misal 1.0.1 > 1.0.0)
    private fun isVersionNewer(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.filter { c -> c.isDigit() }.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
