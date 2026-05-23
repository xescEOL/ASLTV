package com.xesc.asltv.data.local.dao

import androidx.room.*
import com.xesc.asltv.data.local.entity.ChannelListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelListDao {

    @Query("SELECT * FROM channel_lists ORDER BY isActive DESC, name ASC")
    fun getAllLists(): Flow<List<ChannelListEntity>>

    @Query("SELECT * FROM channel_lists ORDER BY isActive DESC, name ASC")
    suspend fun getAllListsOnce(): List<ChannelListEntity>

    @Query("SELECT * FROM channel_lists WHERE isActive = 1 LIMIT 1")
    fun getActiveList(): Flow<ChannelListEntity?>

    @Query("SELECT * FROM channel_lists WHERE userUrl = :url LIMIT 1")
    suspend fun getByUserUrl(url: String): ChannelListEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: ChannelListEntity)

    @Query("UPDATE channel_lists SET isActive = 0")
    suspend fun deactivateAll()

    @Query("UPDATE channel_lists SET isActive = 1 WHERE userUrl = :url")
    suspend fun setActive(url: String)

    @Query("DELETE FROM channel_lists WHERE userUrl = :url")
    suspend fun delete(url: String)

    @Query("UPDATE channel_lists SET rawJson = :json, currentUrl = :currentUrl, mirrorUrl = :mirrorUrl, epgUrl = :epgUrl, lastUpdated = :timestamp WHERE userUrl = :userUrl")
    suspend fun updateListData(userUrl: String, json: String, currentUrl: String, mirrorUrl: String?, epgUrl: String?, timestamp: Long)
}