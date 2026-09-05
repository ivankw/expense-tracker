package com.example.pengeluaran.util

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

enum class UpdateStatus {
    HAS_UPDATE,     // Ada versi yang lebih tinggi di repo
    UP_TO_DATE,     // Versi lokal sama persis dengan yang ada di repo
    ERROR           // Gagal terhubung/akses API repo
}

data class UpdateResult(
    val status: UpdateStatus,
    val currentVersion: String,
    val latestVersion: String = "",
    val releaseNotes: String = "",
    val downloadUrl: String = "",
    val errorMessage: String = ""
)

object UpdateChecker {
    // SESUAIKAN DENGAN USERNAME DAN REPO GITHUB ANDA
    private const val GITHUB_OWNER = "ivankw"
    private const val GITHUB_REPO = "expense-tracker"

    private val client = OkHttpClient()

    fun getCurrentVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    suspend fun checkRelease(context: Context): UpdateResult = withContext(Dispatchers.IO) {
        val currentVerRaw = getCurrentVersion(context).trim()
        val currentVerClean = currentVerRaw.removePrefix("v")
        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext UpdateResult(
                        status = UpdateStatus.ERROR,
                        currentVersion = currentVerRaw,
                        errorMessage = "Gagal mengambil data dari GitHub (${response.code})"
                    )
                }

                val body = response.body?.string() ?: return@withContext UpdateResult(
                    status = UpdateStatus.ERROR,
                    currentVersion = currentVerRaw,
                    errorMessage = "Data rilis kosong"
                )

                val json = JSONObject(body)
                val latestTagRaw = json.optString("tag_name", "").trim()
                val latestVerClean = latestTagRaw.removePrefix("v")
                val releaseNotes = json.optString("body", "Pembaruan rutin aplikasi.")

                // Ambil tautan unduh APK
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

                // Logika pencocokan versi
                val diff = compareVersions(latestVerClean, currentVerClean)
                when {
                    diff > 0 -> UpdateResult(
                        status = UpdateStatus.HAS_UPDATE,
                        currentVersion = currentVerRaw,
                        latestVersion = latestTagRaw,
                        releaseNotes = releaseNotes,
                        downloadUrl = apkUrl
                    )
                    else -> UpdateResult(
                        status = UpdateStatus.UP_TO_DATE,
                        currentVersion = currentVerRaw,
                        latestVersion = latestTagRaw
                    )
                }
            }
        } catch (e: Exception) {
            UpdateResult(
                status = UpdateStatus.ERROR,
                currentVersion = currentVerRaw,
                errorMessage = e.localizedMessage ?: "Tidak dapat terhubung ke server"
            )
        }
    }

    /**
     * Membandingkan dua semver string.
     * Mengembalikan 1 jika v1 > v2, -1 jika v1 < v2, dan 0 jika v1 == v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }

        val length = maxOf(parts1.size, parts2.size)
        for (i in 0 until length) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }
            if (num1 > num2) return 1
            if (num1 < num2) return -1
        }
        return 0
    }
}
