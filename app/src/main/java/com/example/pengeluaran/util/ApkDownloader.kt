package com.example.pengeluaran.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

object ApkDownloader {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        fileName: String = "update.apk",
        onProgress: (Boolean) -> Unit = {}
    ) {
        // Cek izin pasang aplikasi tak dikenal untuk Android 8.0+ (Oreo) sampai Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    context,
                    "Izinkan penginstalan aplikasi dari sumber ini untuk melanjutkan pembaruan",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            }
        }

        Toast.makeText(context, "Sedang mengunduh pembaruan...", Toast.LENGTH_SHORT).show()
        onProgress(true)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val targetFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                val request = Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "Mozilla/5.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            onProgress(false)
                            Toast.makeText(context, "Gagal mengunduh: Kode ${response.code}", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val body = response.body ?: run {
                        withContext(Dispatchers.Main) {
                            onProgress(false)
                            Toast.makeText(context, "File unduhan kosong", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    body.byteStream().use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                // Verifikasi file APK: pastikan ukurannya wajar (minimal > 1 MB)
                if (!targetFile.exists() || targetFile.length() < 1024 * 1024) {
                    withContext(Dispatchers.Main) {
                        onProgress(false)
                        Toast.makeText(context, "File APK rusak atau tidak lengkap terunduh", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    onProgress(false)
                    installApk(context, targetFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onProgress(false)
                    Toast.makeText(context, "Terjadi kesalahan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            apkFile
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        context.startActivity(installIntent)
    }
}
