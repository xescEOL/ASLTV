package com.xesc.asltv.utils

/**
 * Detecta emojis de banderas de país en el nombre del canal.
 * Las banderas son secuencias de Regional Indicator (U+1F1E6..U+1F1FF).
 */
object FlagHelper {
    private val flagRegex = Regex("[\uD83C][\uDDE6-\uDDFF][\uD83C][\uDDE6-\uDDFF]")

    /** Extrae el emoji de bandera del nombre, si existe */
    fun extractFlag(channelName: String): String? =
        flagRegex.find(channelName)?.value

    /** Nombre del canal sin el emoji de bandera */
    fun nameWithoutFlag(channelName: String): String =
        channelName.replace(flagRegex, "").trim()
}