package com.example.pengeluaran.util

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ApkDownloader {

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun downloadAndInstall(context: Context, downloadUrl: String, fileName: String = "update.apk") {
        val appContext = context.applicationContext

        // Buka pengaturan Unknown Sources jika belum diizinkan (bebas requestCode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!appContext.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    appContext,
                    "Izinkan penginstalan aplikasi dari sumber ini terlebih dahulu",
                    Toast.LENGTH_LONG
                ).show()

                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${appContext.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(settingsIntent)
                return
            }
        }

        val destinationFile = File(
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            fileName
        )

        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("Mengunduh Pembaruan")
            .setDescription("Sedang mengunduh file APK...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setMimeType("application/vnd.android.package-archive")

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        Toast.makeText(appContext, "Mulai mengunduh file update...", Toast.LENGTH_SHORT).show()

        val onCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(recvContext: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L) ?: -1L
                if (id == downloadId) {
                    try {
                        appContext.unregisterReceiver(this)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor: Cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex != -1 && cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL) {
                            installApk(appContext, destinationFile)
                        } else {
                            Toast.makeText(appContext, "Gagal mengunduh APK.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    cursor.close()
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(onCompleteReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            appContext.registerReceiver(onCompleteReceiver,Sesi percakapan kita sebelumnya belum tersambung ke obrolan ini, sehingga saya belum bisa melihat kode, dokumen, atau topik spesifik apa yang terakhir kali kita kerjakan bersama.

Bisa tolong sebutkan judul proyek, topik, atau cuplikan bagian yang sedang kita garap? Saya akan langsung melanjutkan dan memberikan versi terbarunya.
