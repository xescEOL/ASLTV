package com.xesc.asltv.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channel_lists")
data class ChannelListEntity(
    @PrimaryKey val userUrl: String,    // URL que puso el usuario (immutable)
    val name: String,
    val author: String,
    val currentUrl: String,             // URL activa (puede cambiar según el JSON)
    val mirrorUrl: String?,
    val epgUrl: String?,
    val rawJson: String,                // JSON completo guardado
    val lastUpdated: Long,              // timestamp millis
    val isActive: Boolean = false       // lista seleccionada actualmente
)