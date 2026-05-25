package com.soodalbbobgi.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.soodalbbobgi.app.data.local.entity.GachaHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GachaHistoryDao {
    @Insert
    suspend fun insert(history: GachaHistoryEntity): Long

    @Query("SELECT * FROM gacha_history WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(userId: String, limit: Int = 20): Flow<List<GachaHistoryEntity>>

    @Query("SELECT * FROM gacha_history WHERE userId = :userId AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getByDateRange(userId: String, startTime: Long, endTime: Long): Flow<List<GachaHistoryEntity>>
}
