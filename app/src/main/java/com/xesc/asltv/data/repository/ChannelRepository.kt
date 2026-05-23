package com.xesc.asltv.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.xesc.asltv.data.local.dao.ChannelListDao
import com.xesc.asltv.data.local.entity.ChannelListEntity
import com.xesc.asltv.data.model.AppUpdate
import com.xesc.asltv.data.model.ChannelList
import com.xesc.asltv.data.model.compareVersions
import com.xesc.asltv.data.remote.ApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val dao: ChannelListDao,
    private val api: ApiService,
    private val gson: Gson,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val UPDATE_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 horas
    }

    /** Flow con la lista activa parseada */
    fun getActiveChannelList(): Flow<ChannelList?> =
        dao.getActiveList().map { entity ->
            entity?.let {
                val parsed = parseJson(it.rawJson)
                Log.d("ChannelRepository", "Parsed ChannelList: bannerImage=${parsed?.bannerImage}, epgUrl=${parsed?.epgUrl}, groups=${parsed?.groups?.size}")
                parsed
            }
        }

    /** Flow con todas las listas guardadas */
    fun getAllSavedLists(): Flow<List<ChannelListEntity>> = dao.getAllLists()

    /**
     * Añade una nueva lista por URL.
     * Descarga el JSON, guarda en DB y la activa si es la primera.
     */
    suspend fun addList(userUrl: String): Result<Unit> = runCatching {
        Log.d("ChannelRepository", "Intentando añadir lista: $userUrl")
        val json = try {
            downloadJson(userUrl)
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error descargando: ${e.message}")
            throw Exception("Error de conexió: ${e.message}")
        }
        
        val channelList = parseJson(json) ?: throw Exception("JSON inválido o mal formado")

        val existing = dao.getAllLists().first()
        val isFirst = existing.isEmpty()

        val entity = ChannelListEntity(
            userUrl = userUrl,
            name = channelList.name,
            author = channelList.author,
            currentUrl = channelList.url,
            mirrorUrl = channelList.urlMirror,
            epgUrl = channelList.epgUrl,
            rawJson = json,
            lastUpdated = System.currentTimeMillis(),
            isActive = isFirst
        )
        dao.insert(entity)
        Log.d("ChannelRepository", "Lista añadida con éxito: ${channelList.name}")
    }

    /**
     * Refresca la lista siempre (sin restricción de tiempo) o se fuerza.
     * Intenta primero la URL del JSON actual; si falla, usa el mirror.
     */
    suspend fun refreshList(userUrl: String, force: Boolean = true): Result<Unit> = runCatching {
        val entity = dao.getByUserUrl(userUrl) ?: return@runCatching
        val now = System.currentTimeMillis()

        // Siempre refresca cuando se llama (force=true por defecto para startup)
        if (!force && (now - entity.lastUpdated) < UPDATE_INTERVAL_MS) return@runCatching

        // Intenta URL principal del JSON
        val (json, usedUrl) = tryDownload(entity.currentUrl, entity.mirrorUrl)
            ?: throw Exception("No se pudo descargar la lista")

        val channelList = parseJson(json) ?: throw Exception("JSON inválido")

        // Guarda: la nueva URL principal/mirror vienen del JSON descargado
        dao.updateListData(
            userUrl = userUrl,
            json = json,
            currentUrl = channelList.url,
            mirrorUrl = channelList.urlMirror,
            epgUrl = channelList.epgUrl,
            timestamp = now
        )
    }

    suspend fun setActiveList(userUrl: String) {
        dao.deactivateAll()
        dao.setActive(userUrl)
    }

    suspend fun deleteList(userUrl: String) = dao.delete(userUrl)

    /**
     * Comprueba si hay una actualización de la app disponible en la lista activa.
     * @return AppUpdate si hay una versión más nueva, null en caso contrario
     */
    suspend fun checkForAppUpdate(): AppUpdate? {
        try {
            Log.d("ChannelRepository", "Iniciando comprobación de actualización")
            
            val channelList = getActiveChannelList().first()
            Log.d("ChannelRepository", "ChannelList obtenido: ${channelList?.name}")
            
            if (channelList == null) {
                Log.d("ChannelRepository", "No hi ha llista activa")
                return null
            }
            
            val appUpdate = channelList.appUpdate
            Log.d("ChannelRepository", "AppUpdate del JSON: ${appUpdate?.version}")
            
            if (appUpdate == null) {
                Log.d("ChannelRepository", "No hay appUpdate en el JSON")
                return null
            }
            
            val currentVersion = getAppVersion()
            Log.d("ChannelRepository", "Versió actual: $currentVersion")
            Log.d("ChannelRepository", "Versió del JSON: ${appUpdate.version}")
            
            val comparison = compareVersions(currentVersion, appUpdate.version)
            Log.d("ChannelRepository", "Comparació: $comparison")
            
            if (comparison < 0) { // Versión del JSON es más nueva
                Log.d("ChannelRepository", "Nova versió disponible: $currentVersion -> ${appUpdate.version}")
                return appUpdate
            } else {
                Log.d("ChannelRepository", "No hi ha nova versió disponible")
            }
            
            return null
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error comprobando actualización: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private suspend fun downloadJson(url: String): String =
        api.fetchList(url).string()

    private suspend fun tryDownload(primary: String, mirror: String?): Pair<String, String>? {
        return try {
            val json = downloadJson(primary)
            Pair(json, primary)
        } catch (e: Exception) {
            if (mirror != null) {
                try {
                    val json = downloadJson(mirror)
                    Pair(json, mirror)
                } catch (e2: Exception) { null }
            } else null
        }
    }

    private fun parseJson(json: String): ChannelList? =
        try {
            gson.fromJson(json, ChannelList::class.java)
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error parseando JSON. Content snippet: ${json.take(200)}")
            Log.e("ChannelRepository", "Gson error: ${e.message}")
            null
        }

    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            Log.e("ChannelRepository", "Error obteniendo versión de la app: ${e.message}")
            "1.0.0" // Versión por defecto
        }
    }
}