package com.example.pengeluaran.util

import android.content.Context
import android.net.Uri
import com.example.pengeluaran.data.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object BackupRestoreHelper {

    fun exportDatabase(context: Context, destUri: Uri): Boolean {
        return try {
            val db = AppDatabase.getDatabase(context)
            // Lakukan checkpoint agar isi WAL ditulis tuntas ke berkas db utama
            val cursor = db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
            cursor.moveToFirst()
            cursor.close()

            val dbFile = context.getDatabasePath("expense_database")
            if (!dbFile.exists()) return false

            context.contentResolver.openOutputStream(destUri)?.use { out ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(out)
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun importDatabase(context: Context, srcUri: Uri): Boolean {
        return try {
            val dbFile = context.getDatabasePath("expense_database")
            val walFile = File(dbFile.path + "-wal")
            val shmFile = File(dbFile.path + "-shm")

            context.contentResolver.openInputStream(srcUri)?.use { input ->
                FileOutputStream(dbFile).use { out ->
                    input.copyTo(out)
                }
            }

            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()
            true
        } catch (_: Exception) {
            false
        }
    }
}
