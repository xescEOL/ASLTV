package com.xesc.asltv.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.xesc.asltv.data.local.dao.*
import com.xesc.asltv.data.local.entity.*

@Database(
    entities = [ChannelListEntity::class, EpgProgramEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun channelListDao(): ChannelListDao
    abstract fun epgDao(): EpgDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "acestreamtv.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}