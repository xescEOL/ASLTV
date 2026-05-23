package com.xesc.asltv.data.model

import com.google.gson.annotations.SerializedName

data class ChannelList(
    @SerializedName("name")        val name: String,
    @SerializedName("author")      val author: String = "",
    @SerializedName("url")         val url: String,
    @SerializedName("urlmirror")   val urlMirror: String? = null,
    @SerializedName("epgUrl")      val epgUrl: String? = null,  // Base URL for EPG files
    // Banner opcional en la pantalla principal — si es null no se muestra nada
    @SerializedName("bannerImage") val bannerImage: String? = null,
    @SerializedName("groups")      val groups: List<Group> = emptyList(),
    @SerializedName("apks")        val apks: List<Apk> = emptyList(),
    // APK de la app para actualizaciones automáticas
    @SerializedName("appUpdate")   val appUpdate: AppUpdate? = null
)

data class Group(
    @SerializedName("name")     val name: String,
    @SerializedName("image")    val image: String? = null,
    @SerializedName("stations") val stations: List<Station> = emptyList()
)

data class Station(
    @SerializedName("name")    val name: String,
    @SerializedName("image")   val image: String? = null,
    @SerializedName("url")     val url: String,
    @SerializedName("lite")    val lite: Boolean = false,
    @SerializedName("epgId")   val epgId: String? = null,
    @SerializedName("epgfile") val epgfile: String? = null,
    @SerializedName("epghora") val epgHora: String? = null
)

data class Apk(
    @SerializedName("name")        val name: String,
    @SerializedName("url")         val url: String,
    @SerializedName("armv")        val armv: Int? = null,
    @SerializedName("version")     val version: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("image")       val image: String? = null
)

data class AppUpdate(
    @SerializedName("url")         val url: String,
    @SerializedName("version")     val version: String,
    @SerializedName("description") val description: String? = null
)

/** Compara dos versiones en formato X.Y.Z (ej: 1.0.0) */
fun compareVersions(current: String, new: String): Int {
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val newParts = new.split(".").map { it.toIntOrNull() ?: 0 }
    
    for (i in 0 until maxOf(currentParts.size, newParts.size)) {
        val currentPart = currentParts.getOrNull(i) ?: 0
        val newPart = newParts.getOrNull(i) ?: 0
        
        if (newPart > currentPart) return -1  // Nueva versión es mayor
        if (newPart < currentPart) return 1   // Nueva versión es menor
    }
    
    return 0 // Versiones iguales
}

// ── Modelos de dominio ────────────────────────────────────────────────────────

data class MirrorGroup(val stations: List<Station>) {
    val name: String    get() = stations.firstOrNull()?.name ?: ""
    val image: String?  get() = stations.firstNotNullOfOrNull { it.image?.takeIf { img -> img.isNotBlank() } } ?: stations.firstOrNull()?.image
    
    // Busca el primer epgId no nulo entre todos los espejos (mirrors)
    val epgId: String?  get() = stations.firstNotNullOfOrNull { 
        it.epgId?.takeIf { id -> id.isNotBlank() } 
    }?.sanitizeEpgId()
    
    // Busca el primer epgfile no nulo
    val epgfile: String? get() = stations.firstNotNullOfOrNull { 
        it.epgfile?.takeIf { f -> f.isNotBlank() } 
    }

    val epgHora: Int get() = stations.firstNotNullOfOrNull {
        it.epgHora?.toIntOrNull()
    } ?: 0

    val lite: Boolean   get() = stations.any { it.lite }
    val hasMirrors: Boolean get() = stations.size > 1
    val primaryUrl: String  get() = stations.firstOrNull()?.url?.normalizeAceStream() ?: ""
}

/** Limpia epgIds con formato markdown [id](url) o URLs completas */
fun String.sanitizeEpgId(): String {
    // Caso: "[tv3.es](http://tv3.es)" → "tv3.es"
    val markdownMatch = Regex("""\[([^\]]+)]\([^)]+\)""").find(this)
    if (markdownMatch != null) return markdownMatch.groupValues[1].trim()
    // Caso: "http://tv3.es" → "tv3.es"
    if (startsWith("http://") || startsWith("https://")) {
        return removePrefix("https://").removePrefix("http://").trimEnd('/')
    }
    return trim()
}

fun String.normalizeAceStream(): String {
    val trimmed = this.trim()
    if (trimmed.startsWith("acestream://", ignoreCase = true)) return trimmed
    if (trimmed.startsWith("http://", ignoreCase = true) || 
        trimmed.startsWith("https://", ignoreCase = true) ||
        trimmed.startsWith("rtmp://", ignoreCase = true) ||
        trimmed.startsWith("rtsp://", ignoreCase = true)) return trimmed
    
    // Si es un hash de 40 caracteres, es AceStream
    if (trimmed.length == 40 && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
        return "acestream://$trimmed"
    }
    return trimmed
}

data class SavedListInfo(
    val userUrl: String,
    val name: String,
    val author: String,
    val isActive: Boolean,
    val lastUpdated: Long,
    val groupCount: Int = 0,
    val channelCount: Int = 0,
    val epgUrl: String? = null
)