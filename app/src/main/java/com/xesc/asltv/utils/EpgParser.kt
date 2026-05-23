package com.xesc.asltv.utils

import android.util.Log
import android.util.Xml
import com.xesc.asltv.data.local.entity.EpgProgramEntity
import com.xesc.asltv.data.model.sanitizeEpgId
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.TimeZone

/**
 * Parser de formato XMLTV.
 */
object EpgParser {

    private const val TAG = "EpgParser"

    /**
     * Parsea un stream XMLTV y notifica cada programa encontrado.
     */
    fun parseStreaming(inputStream: InputStream, onProgramParsed: (EpgProgramEntity) -> Unit) {
        var count = 0
        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(inputStream, "UTF-8")
            }

            var builder: ProgramBuilder? = null
            var inTitle = false
            var inDesc = false
            var inCategory = false

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "programme" -> {
                            builder = ProgramBuilder(
                                channelId = parser.getAttributeValue(null, "channel")?.sanitizeEpgId() ?: "",
                                startTime = parseDate(parser.getAttributeValue(null, "start")),
                                endTime   = parseDate(parser.getAttributeValue(null, "stop"))
                            )
                        }
                        "title"    -> if (builder != null) inTitle = true
                        "desc"     -> if (builder != null) inDesc = true
                        "category" -> if (builder != null) inCategory = true
                    }

                    XmlPullParser.TEXT -> builder?.let { b ->
                        val text = parser.text ?: ""
                        when {
                            inTitle    -> b.title += text
                            inDesc     -> b.description += text
                            inCategory -> b.category += text
                        }
                    }

                    XmlPullParser.END_TAG -> when (parser.name) {
                        "programme" -> {
                            builder?.build()?.takeIf {
                                it.channelId.isNotBlank() && it.title.isNotBlank()
                            }?.let { 
                                onProgramParsed(it)
                                count++
                            }
                            builder = null
                        }
                        "title"    -> inTitle = false
                        "desc"     -> inDesc = false
                        "category" -> inCategory = false
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            android.util.Log.e("EpgParser", "Error al parsear XMLTV (streaming): ${e.message}")
        }
        Log.d("EpgParser", "Parsed $count programs via streaming")
    }

    /**
     * Parsea un stream XMLTV y devuelve la lista de programas.
     */
    fun parse(inputStream: InputStream): List<EpgProgramEntity> {
        val programs = mutableListOf<EpgProgramEntity>()

        try {
            val parser = Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(inputStream, "UTF-8")
            }

            var builder: ProgramBuilder? = null
            var inTitle = false
            var inDesc = false
            var inCategory = false

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> when (parser.name) {
                        "programme" -> {
                            builder = ProgramBuilder(
                                channelId = parser.getAttributeValue(null, "channel")?.sanitizeEpgId() ?: "",
                                startTime = parseDate(parser.getAttributeValue(null, "start")),
                                endTime   = parseDate(parser.getAttributeValue(null, "stop"))
                            )
                        }
                        "title"    -> if (builder != null) inTitle = true
                        "desc"     -> if (builder != null) inDesc = true
                        "category" -> if (builder != null) inCategory = true
                    }

                    XmlPullParser.TEXT -> builder?.let { b ->
                        val text = parser.text ?: ""
                        when {
                            inTitle    -> b.title += text
                            inDesc     -> b.description += text
                            inCategory -> b.category += text
                        }
                    }

                    XmlPullParser.END_TAG -> when (parser.name) {
                        "programme" -> {
                            builder?.build()?.takeIf {
                                it.channelId.isNotBlank() && it.title.isNotBlank()
                            }?.let { programs.add(it) }
                            builder = null
                        }
                        "title"    -> inTitle = false
                        "desc"     -> inDesc = false
                        "category" -> inCategory = false
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            android.util.Log.e("EpgParser", "Error al parsear XMLTV: ${e.message}")
        }

        val uniqueChannelIds = programs.map { it.channelId }.distinct()
        Log.d("EpgParser", "Parsed ${programs.size} programs, ${uniqueChannelIds.size} unique channels. Sample channelIds: ${uniqueChannelIds.take(10)}")
        
        // Buscar channelIds que contengan "deporte" o "teledeporte" (case insensitive)
        val matchingIds = uniqueChannelIds.filter { it.lowercase().contains("deporte") || it.lowercase().contains("teledeporte") }
        Log.d("EpgParser", "ChannelIds matching 'deporte' or 'teledeporte': $matchingIds")

        return programs
    }

    private fun parseDate(value: String?): Long {
        if (value.isNullOrBlank()) return 0L
        val v = value.trim()

        return try {
            // El formato XMLTV suele ser YYYYMMDDHHMMSS [+/-]HHMM
            // Ejemplo: "20240928193000 +0200" o "20240928193000"
            // Para evitar problemas de desfases de 2h con proveedores que envían el offset mal
            // o que no lo envían, tratamos la hora del XML siempre como HORA LOCAL del dispositivo.
            val datePart = if (v.length >= 14) v.substring(0, 14) else return 0L
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            val ldt = LocalDateTime.parse(datePart, formatter)

            // Forzamos que la hora leída sea tratada como la hora local del sistema.
            // Esto garantiza que si el XML dice "21:00", el usuario vea "21:00" en su reloj local.
            ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing date '$value': ${e.message}")
            0L
        }
    }

    private data class ProgramBuilder(
        val channelId: String,
        val startTime: Long,
        val endTime: Long,
        var title: String = "",
        var description: String = "",
        var category: String = ""
    ) {
        fun build() = EpgProgramEntity(
            channelId   = channelId,
            startTime   = startTime,
            endTime     = endTime,
            title       = title.trim(),
            description = description.trim(),
            category    = category.trim()
        )
    }
}
