package com.xesc.asltv.data.repository

import android.util.Log
import com.xesc.asltv.data.local.dao.EpgDao
import com.xesc.asltv.data.local.entity.EpgProgramEntity
import com.xesc.asltv.data.remote.ApiService
import com.xesc.asltv.utils.EpgParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpgRepository @Inject constructor(
    private val epgDao: EpgDao,
    private val api: ApiService
) {
    // Track loading state globally
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val syncProgress = _syncProgress.asStateFlow()

    fun updateSyncState(syncing: Boolean, progress: Float = 0f) {
        _isSyncing.value = syncing
        _syncProgress.value = progress
    }

    // Track loaded EPG files to avoid reloading
    private val loadedEpgFiles = mutableSetOf<String>()

    /**
     * Descarga y parsea el XMLTV desde [baseUrl] + [epgfile], guarda en Room.
     * Llama esto cuando el usuario actualiza la lista o al iniciar la app.
     */
    suspend fun refreshEpg(baseUrl: String, epgfile: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            if (epgfile.isBlank()) return@runCatching 0
            
            var actualEpgFile = epgfile
            // Mapeo ES2 a ES1 si falla o para evitar 404s conocidos
            if (epgfile.contains("epg_ripper_ES2.xml.gz")) {
                actualEpgFile = epgfile.replace("epg_ripper_ES2", "epg_ripper_ES1")
                Log.d("EpgRepository", "Redirecting ES2 to ES1: $actualEpgFile")
            }

            val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val fullUrl = "$base$actualEpgFile"
            Log.d("EpgRepository", "Fetching EPG from: $fullUrl")
            
            // Skip if already loaded in this session's memory cache
            if (loadedEpgFiles.contains(fullUrl)) {
                return@runCatching 0
            }
            
            try {
                val response = api.fetchEpg(fullUrl)
                val inputStream = response.byteStream()
                
                val decompressedStream = if (fullUrl.endsWith(".gz")) {
                    GZIPInputStream(inputStream)
                } else {
                    inputStream
                }

                if (loadedEpgFiles.isEmpty()) {
                    Log.d("EpgRepository", "First EPG load. Cleaning DB.")
                    epgDao.deleteAll()
                }

                var totalParsed = 0
                val batch = mutableListOf<EpgProgramEntity>()
                
                epgDao.deleteOldPrograms()

                EpgParser.parseStreaming(decompressedStream) { program ->
                    batch.add(program)
                    totalParsed++
                    
                    if (batch.size >= 500) {
                        val toInsert = ArrayList(batch)
                        batch.clear()
                        epgDao.insertAllSync(toInsert)
                    }
                }
                
                if (batch.isNotEmpty()) {
                    epgDao.insertAllSync(batch)
                }

                loadedEpgFiles.add(fullUrl)
                totalParsed
            } catch (e: Exception) {
                Log.e("EpgRepository", "Error fetching EPG: ${e.message}")
                throw e
            }
        }
    }

    suspend fun getProgramsForChannels(epgIds: List<String>, now: Long, until: Long): List<EpgProgramEntity> {
        if (epgIds.isEmpty()) return emptyList()
        // No filtramos por tiempo aquí para permitir que el ViewModel aplique offsets
        // pero limitamos un poco el rango por rendimiento (ej: +/- 24h)
        val padding = 24 * 3600000L
        return epgDao.getProgramsForChannels(epgIds, now - padding, until + padding)
    }

    suspend fun getCurrentPrograms(epgIds: List<String>): Map<String, EpgProgramEntity> {
        if (epgIds.isEmpty()) return emptyMap()
        val now = System.currentTimeMillis()
        return epgDao.getProgramsForChannels(epgIds, now, now + 1)
            .filter { it.startTime <= now && it.endTime > now }
            .associateBy { it.channelId }
    }

    /**
     * Clear loaded EPG files cache (call when switching channel lists)
     */
    fun clearLoadedCache() {
        loadedEpgFiles.clear()
    }

    /** Programa que se emite ahora mismo para un canal */
    suspend fun getCurrentProgram(epgId: String): EpgProgramEntity? =
        epgDao.getCurrentProgram(epgId)

    /** Próximos programas para un canal */
    suspend fun getUpcomingPrograms(epgId: String, limit: Int = 2): List<EpgProgramEntity> =
        epgDao.getUpcomingPrograms(epgId, limit = limit)

    /**
     * Info EPG completa para mostrar en la tarjeta del canal:
     * programa actual + próximos 20 programas, aplicando offset horario.
     */
    data class ChannelEpgInfo(
        val current: EpgProgramEntity?,
        val upcoming: List<EpgProgramEntity>
    )

    suspend fun getChannelEpgInfo(epgId: String?, offsetHours: Int = 0): ChannelEpgInfo {
        if (epgId.isNullOrBlank()) return ChannelEpgInfo(null, emptyList())
        val now = System.currentTimeMillis()
        
        // Ampliamos el rango de búsqueda para compensar el offset (hasta 24h)
        val rangeStart = now - 24 * 3600000L
        val rangeEnd = now + 24 * 3600000L
        
        val programs = epgDao.getProgramsInRange(epgId, rangeStart, rangeEnd)
        
        // Aplicamos el offset negativo: si el usuario pone +3, restamos 3h a la EPG
        // para "adelantar" los programas a la hora actual.
        val shifted = if (offsetHours != 0) {
            programs.map { it.withOffset(-offsetHours) }
        } else {
            programs
        }
        
        val current = shifted.firstOrNull { it.startTime <= now && it.endTime > now }
        val upcoming = shifted.filter { it.startTime >= (current?.endTime ?: now) }
            .filter { it != current } // Evitar duplicar el actual en la lista de próximos
            .take(20)
        
        return ChannelEpgInfo(current, upcoming)
    }
}
