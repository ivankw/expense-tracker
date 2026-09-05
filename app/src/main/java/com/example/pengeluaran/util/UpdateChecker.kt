package com.example.pengeluaran.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

enum class UpdateStatus {
    HAS_UPDATE,
    UP_TO_DATE,
    ERROR
}

data class UpdateResult(
    val status: UpdateStatus,
    val currentVersion: String,
    val latestVersion: String,
    val downloadUrl: String? = null,
    val releaseNotes: String? = null,
    val errorMessage: String? = null
)

object UpdateChecker {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Sesuaikan format: "username_github/nama_repo" (contoh: "octocat/expense-tracker")
    var githubRepoPath: String = "expense-tracker/expense-tracker"

    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    suspend fun checkRelease(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        val currentVer = getCurrentVersion(context)
        try {
            val url = "https://api.github.com/repos/$githubRepoPath/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext UpdateResult(
                    status = UpdateStatus.ERROR,
                    currentVersion = currentVer,
                    latestVersion = currentVer,
                    errorMessage = "HTTP ${response.code}: ${response.message}"
                )
            }

            val bodyString: String = response.body?.string().orEmpty()
            if (bodyString.isBlank()) {
                return@withContext UpdateResult(
                    status = UpdateStatus.ERROR,
                    currentVersion = currentVer,
                    latestVersion = currentVer,
                    errorMessage = "Respon server kosong"
                )
            }

            val jsonObject = JSONObject(bodyString)
            val tagName = jsonObject.optString("tag_name", "").trim()
            val releaseNotes = jsonObject.optString("body", "-")
            val latestVer = tagName.removePrefix("v")

            var downloadUrl: String? = null
            val assets = jsonObject.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk")) {
                        downloadUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
            }

            val hasUpdate = isNewerVersion(currentVer, latestVer)

            UpdateResult(
                status = if (hasUpdate) UpdateStatus.HAS_UPDATE else UpdateStatus.UP_TO_DATE,
                currentVersion = currentVer,
                latestVersion = tagName.ifBlank { latestVer },
                downloadUrl = downloadUrl,
                releaseNotes = releaseNotes
            )
        } catch (e: Exception) {
            UpdateResult(
                status = UpdateStatus.ERROR,
                currentVersion = currentVer,
                latestVersion = currentVer,
                errorMessage = e.localizedMessage ?: "Gagal terhubung ke server"
            )
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        if (current == latest || latest.isBlank()) return false
        val currentParts = current.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val latestParts = latest.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(currentParts.size, latestParts.size)
        for (i in 0 until maxLen) {
            val c = currentParts.getOrElse(i) { 0 }
            val l = latestParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
