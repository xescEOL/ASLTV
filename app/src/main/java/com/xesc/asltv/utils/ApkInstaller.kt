package com.xesc.asltv.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object ApkInstaller {

    fun installApk(context: Context, apkFile: File) {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
        } else {
            Uri.fromFile(apkFile)
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        url: String,
        version: String,
        onProgress: (Float) -> Unit = {}
    ) = withContext(Dispatchers.IO) {
        try {
            // Create download directory
            val downloadDir = File(context.getExternalFilesDir(null), "Downloads")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }
            
            // Create APK file
            val fileName = "app-update-$version.apk"
            val apkFile = File(downloadDir, fileName)
            
            // Download the APK
            val connection = URL(url).openConnection()
            connection.connect()
            val contentLength = connection.contentLength
            
            val inputStream = connection.getInputStream()
            val outputStream = FileOutputStream(apkFile)
            
            val buffer = ByteArray(4096)
            var bytesRead: Int
            var totalBytesRead = 0L
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                
                // Report progress
                if (contentLength > 0) {
                    val progress = (totalBytesRead.toFloat() / contentLength.toFloat()) * 100f
                    onProgress(progress / 100f) // Convert to 0.0-1.0 range
                }
            }
            
            outputStream.close()
            inputStream.close()
            
            // Install the APK
            withContext(Dispatchers.Main) {
                installApk(context, apkFile)
            }
            
        } catch (e: Exception) {
            throw Exception("Error downloading APK: ${e.message}", e)
        }
    }

    /** Elimina APKs descargados con más de [maxAgeDays] días */
    fun cleanOldApks(context: Context, maxAgeDays: Int = 7) {
        val dir = File(context.getExternalFilesDir(null), "Downloads")
        if (!dir.exists()) return
        val cutoff = System.currentTimeMillis() - maxAgeDays * 86400000L
        dir.listFiles()
            ?.filter { it.extension == "apk" && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }
}