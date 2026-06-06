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

    @Query("SELECT * FROM swim_logs WHERE date = :date LIMIT 1")
    suspend fun getByDateOnce(date: String): SwimLogEntity?

    @Query("SELECT * FROM swim_logs WHERE hcRecordId = :hcRecordId LIMIT 1")
    suspend fun getByHcRecordId(hcRecordId: String): SwimLogEntity?

    @Query("SELECT * FROM swim_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<SwimLogEntity>>

    /** 서버 POST 응답으로 받은 조개 지급량을 같은 날짜의 로컬 row에 반영. */
    @Query("UPDATE swim_logs SET shellsEarned = :shellsEarned WHERE date = :date")
    suspend fun updateShellsEarned(date: String, shellsEarned: Int)

    /** HC 원본에서 읽은 심박/실운동시간/심박 시계열을 같은 날짜 로컬 row에 반영. null인 값은 기존 값을 유지한다. */
    @Query(
        "UPDATE swim_logs SET maxHr = COALESCE(:maxHr, maxHr), minHr = COALESCE(:minHr, minHr), " +
            "activeSeconds = COALESCE(:activeSeconds, activeSeconds), hrSeries = COALESCE(:hrSeries, hrSeries) " +
            "WHERE date = :date",
    )
    suspend fun updateVitals(date: String, maxHr: Int?, minHr: Int?, activeSeconds: Int?, hrSeries: String?)

    /** 같은 날짜 기록의 영법별 거리(m)를 갱신한다. (캘린더 영법 비율 수동 보정) */
    @Query(
        "UPDATE swim_logs SET strokeFreestyleM = :free, strokeBreastM = :breast, " +
            "strokeBackM = :back, strokeFlyM = :fly, strokeMixedM = :mixed, strokeKickM = :kick " +
            "WHERE date = :date",
    )
    suspend fun updateStrokes(date: String, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int)

    @Query("DELETE FROM swim_logs WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM swim_logs WHERE hcRecordId = :hcRecordId")
    suspend fun deleteByHcRecordId(hcRecordId: String)

    @Query("SELECT COALESCE(SUM(distanceMeters), 0) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDistance(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(*) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getSwimCount(startDate: String, endDate: String): Int

    @Query("SELECT COALESCE(SUM(calories), 0) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalCalories(startDate: String, endDate: String): Int

    @Query("SELECT * FROM swim_logs ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): SwimLogEntity?
}
