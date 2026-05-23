package com.xesc.asltv.data.local.dao

import androidx.room.*
import com.xesc.asltv.data.local.entity.EpgProgramEntity

@Dao
interface EpgDao {

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId = :channelId 
        AND startTime > :now 
        ORDER BY startTime ASC 
        LIMIT :limit
    """)
    suspend fun getUpcomingPrograms(channelId: String, limit: Int, now: Long = System.currentTimeMillis()): List<EpgProgramEntity>

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND startTime <= :now AND endTime > :now LIMIT 1")
    suspend fun getCurrentProgram(channelId: String, now: Long = System.currentTimeMillis()): EpgProgramEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(programs: List<EpgProgramEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAllSync(programs: List<EpgProgramEntity>)

    @Query("DELETE FROM epg_programs WHERE endTime < :before")
    suspend fun deleteOldPrograms(before: Long = System.currentTimeMillis() - 86400000L)

    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId AND endTime > :from AND startTime < :to ORDER BY startTime ASC")
    suspend fun getProgramsInRange(channelId: String, from: Long, to: Long): List<EpgProgramEntity>

    @Query("DELETE FROM epg_programs WHERE channelId = :channelId")
    suspend fun clearChannel(channelId: String)

    @Query("DELETE FROM epg_programs")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM epg_programs 
        WHERE channelId IN (:channelIds) 
        AND endTime > :now 
        AND startTime < :until 
        ORDER BY startTime ASC
    """)
    suspend fun getProgramsForChannels(channelIds: List<String>, now: Long, until: Long): List<EpgProgramEntity>
}