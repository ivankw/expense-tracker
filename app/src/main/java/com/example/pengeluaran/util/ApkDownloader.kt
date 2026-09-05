package com.example.pengeluaran.util

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import java.util.concurrent.TimeUnit

object ApkDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun downloadAndInstall(context: Context, downloadUrl: String, fileName: String) {
        Toast.makeText(context, "Memulai unduhan pembaruan...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url(downloadUrl)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Gagal mengunduh: HTTP ${response.code}", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "File unduhan kosong", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val downloadDir = context.getExternalFilesDir(null) ?: context.filesDir
                val apkFile = File(downloadDir, fileName)
                if (apkFile.exists()) apkFile.delete()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Unduhan selesai, membuka installer...", Toast.LENGTH_SHORT).show()
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal mengunduh pembaruan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "File APK tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal memasang APK: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
