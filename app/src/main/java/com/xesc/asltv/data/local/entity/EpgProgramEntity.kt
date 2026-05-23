package com.xesc.asltv.data.local.entity

import androidx.room.Entity

@Entity(tableName = "epg_programs", primaryKeys = ["channelId", "startTime"])
data class EpgProgramEntity(
    val channelId: String,      // matches Station.epgId
    val startTime: Long,        // millis UTC
    val endTime: Long,
    val title: String,
    val description: String = "",
    val category: String = "",
    val imageUrl: String? = null
) {
    fun withOffset(hours: Int): EpgProgramEntity {
        if (hours == 0) return this
        val offsetMillis = hours * 3600000L
        return copy(
            startTime = startTime + offsetMillis,
            endTime = endTime + offsetMillis
        )
    }
}