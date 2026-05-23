// ============================================================
// FICHERO: data/repository/ApkRepository.kt
// Descarga APKs con progreso, detección ARM, limpieza automática
// ============================================================

package com.xesc.asltv.data.repository

import android.content.Context
import com.xesc.asltv.data.model.Apk
import com.xesc.asltv.utils.ArmDetector
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val downloadDir: File
        get() = File(context.getExternalFilesDir(null), "Downloads").also { it.mkdirs() }

    /**
     * Filtra la lista de APKs para mostrar solo la versión compatible.
     * - Si hay armv7 y armv8, selecciona la del dispositivo
     * - Si hay universal (armv=null), la muestra si no hay versión específica
     * - APKs con distinto nombre siempre se muestran todas
     */
    fun filterCompatibleApks(apks: List<Apk>): List<Apk> {
        val deviceArm = ArmDetector.getArmVersion()

        return apks
            .groupBy { it.name }
            .map { (_, versions) ->
                when {
                    versions.size == 1 -> versions.first()
                    else -> {
                        // Preferencia: 1) versión exacta para el ARM 2) universal 3) primera disponible
                        versions.firstOrNull { it.armv == deviceArm }
                            ?: versions.firstOrNull { it.armv == null }
                            ?: versions.first()
                    }
                }
            }
            .sortedBy { it.name }
    }

    /**
     * Descarga un APK con callback de progreso.
     * @param apk El APK a descargar
     * @param onProgress Callback con progreso 0.0..1.0
     * @param onComplete Callback con el File resultante
     * @param onError Callback con mensaje de error
     */
    suspend fun downloadApk(
        apk: Apk,
        onProgress: (Float) -> Unit,
        onComplete: (File) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val fileName = buildFileName(apk)
            val destFile = File(downloadDir, fileName)

            // Si ya existe (descarga previa), va directo a completado
            if (destFile.exists() && destFile.length() > 0) {
                withContext(Dispatchers.Main) { onComplete(destFile) }
                return@withContext
            }

            val request = Request.Builder().url(apk.url).build()
            val response = okHttpClient.newCall(request).execute()

            if (!response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    onError("Error HTTP ${response.code}: ${response.message}")
                }
                return@withContext
            }

            val body = response.body ?: run {
                withContext(Dispatchers.Main) { onError("Respuesta vacía del servidor") }
                return@withContext
            }

            val totalBytes = body.contentLength()
            val tempFile = File(downloadDir, "$fileName.tmp")

            body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead = 0L
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesRead += read
                        if (totalBytes > 0) {
                            val progress = bytesRead.toFloat() / totalBytes
                            withContext(Dispatchers.Main) { onProgress(progress) }
                        }
                    }
                }
            }

            // Renombra temp → final solo si se descargó completo
            tempFile.renameTo(destFile)
            withContext(Dispatchers.Main) {
                onProgress(1f)
                onComplete(destFile)
            }

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    /** Comprueba si un APK ya está descargado */
    fun isDownloaded(apk: Apk): Boolean {
        val file = File(downloadDir, buildFileName(apk))
        return file.exists() && file.length() > 0
    }

    /** Obtiene el File de un APK descargado (o null si no existe) */
    fun getDownloadedFile(apk: Apk): File? {
        val file = File(downloadDir, buildFileName(apk))
        return if (file.exists()) file else null
    }

    /**
     * Elimina APKs descargados con más de [maxAgeDays] días.
     * Se llama al iniciar la app (desde AceStreamTVApp).
     */
    fun cleanOldApks(maxAgeDays: Int = 7) {
        val cutoff = System.currentTimeMillis() - maxAgeDays * 86_400_000L
        downloadDir.listFiles()
            ?.filter { it.extension == "apk" && it.lastModified() < cutoff }
            ?.forEach { it.delete() }
    }

    /** Lista de APKs descargados con info de tamaño y fecha */
    fun getDownloadedApks(): List<DownloadedApkInfo> {
        return downloadDir.listFiles()
            ?.filter { it.extension == "apk" }
            ?.map { file ->
                DownloadedApkInfo(
                    file = file,
                    name = file.nameWithoutExtension,
                    sizeMb = file.length() / (1024f * 1024f),
                    downloadedAt = file.lastModified()
                )
            } ?: emptyList()
    }

    // ── Privado ───────────────────────────────────────────────────────────────

    private fun buildFileName(apk: Apk): String {
        val safeName = apk.name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val armSuffix = apk.armv?.let { "_armv$it" } ?: ""
        val version = apk.version?.let { "_v$it" } ?: ""
        return "${safeName}${version}${armSuffix}.apk"
    }
}

data class DownloadedApkInfo(
    val file: File,
    val name: String,
    val sizeMb: Float,
    val downloadedAt: Long
)
