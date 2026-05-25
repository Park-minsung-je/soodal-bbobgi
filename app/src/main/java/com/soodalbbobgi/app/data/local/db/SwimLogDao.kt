package com.soodalbbobgi.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soodalbbobgi.app.data.local.entity.SwimLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SwimLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SwimLogEntity)

    @Query("SELECT * FROM swim_logs WHERE date = :date LIMIT 1")
    fun getByDate(date: String): Flow<SwimLogEntity?>

    @Query("SELECT * FROM swim_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<SwimLogEntity>>

    @Query("SELECT COALESCE(SUM(distanceMeters), 0) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDistance(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(*) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getSwimCount(startDate: String, endDate: String): Int

    @Query("SELECT COALESCE(SUM(calories), 0) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalCalories(startDate: String, endDate: String): Int

    @Query("SELECT * FROM swim_logs ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): SwimLogEntity?
}
