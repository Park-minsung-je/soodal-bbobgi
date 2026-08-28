package com.soodalbbobgi.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.soodalbbobgi.app.data.local.entity.SwimLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SwimLogDao {
    @Insert
    suspend fun insert(log: SwimLogEntity): Long

    /** 같은 날짜의 세션 목록 — 시작 시각 순. */
    @Query("SELECT * FROM swim_logs WHERE date = :date ORDER BY startEpochSec ASC, id ASC")
    fun getByDate(date: String): Flow<List<SwimLogEntity>>

    @Query("SELECT * FROM swim_logs WHERE date = :date ORDER BY startEpochSec ASC, id ASC")
    suspend fun getByDateOnce(date: String): List<SwimLogEntity>

    @Query("SELECT * FROM swim_logs WHERE hcRecordId = :hcRecordId LIMIT 1")
    suspend fun getByHcRecordId(hcRecordId: String): SwimLogEntity?

    @Query("SELECT * FROM swim_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, startEpochSec ASC, id ASC")
    fun getByDateRange(startDate: String, endDate: String): Flow<List<SwimLogEntity>>

    /**
     * 서버 POST 응답으로 받은 조개 지급량을 그 날짜의 첫 행에만 반영한다.
     * (지급은 일 단위라 여러 세션에 중복 기록하면 일 합계가 부풀려진다)
     */
    @Query(
        "UPDATE swim_logs SET shellsEarned = :shellsEarned " +
            "WHERE id = (SELECT id FROM swim_logs WHERE date = :date ORDER BY id ASC LIMIT 1)",
    )
    suspend fun updateShellsEarned(date: String, shellsEarned: Int)

    /**
     * HC 재동기화로 기존 세션 행의 핵심 필드를 갱신한다 (서버산 행의 HC 승격 포함).
     * 영법 분배는 사용자가 손대지 않은 상태(혼영 외 전부 0)일 때만 혼영을 새 거리로 맞추고,
     * 편집된 분배는 보존한다. 심박류는 null이면 기존 값을 유지한다.
     */
    @Query(
        "UPDATE swim_logs SET hcRecordId = :hcRecordId, startEpochSec = :startEpochSec, " +
            "distanceMeters = :distance, durationSeconds = :duration, calories = :calories, " +
            "maxHr = COALESCE(:maxHr, maxHr), minHr = COALESCE(:minHr, minHr), " +
            "avgHr = COALESCE(:avgHr, avgHr), " +
            "activeSeconds = COALESCE(:activeSeconds, activeSeconds), hrSeries = COALESCE(:hrSeries, hrSeries), " +
            "strokeMixedM = CASE WHEN strokeFreestyleM = 0 AND strokeBreastM = 0 AND strokeBackM = 0 " +
            "AND strokeFlyM = 0 AND strokeKickM = 0 THEN :distance ELSE strokeMixedM END " +
            "WHERE id = :id",
    )
    suspend fun updateFromHc(
        id: Long,
        hcRecordId: String?,
        startEpochSec: Long?,
        distance: Int,
        duration: Int,
        calories: Int,
        maxHr: Int?,
        minHr: Int?,
        avgHr: Int?,
        activeSeconds: Int?,
        hrSeries: String?,
    )

    /**
     * 그 날짜 행의 **비어 있는** 심박만 채운다 — 서버 백업에서 복원할 때 쓴다.
     * 로컬에 값이 있으면 그대로 둔다. 서버 값은 하루치를 합친 것이라 로컬 세션 값이 더 정확하다.
     */
    @Query(
        "UPDATE swim_logs SET maxHr = COALESCE(maxHr, :maxHr), minHr = COALESCE(minHr, :minHr), " +
            "avgHr = COALESCE(avgHr, :avgHr), hrSeries = COALESCE(hrSeries, :hrSeries) " +
            "WHERE date = :date",
    )
    suspend fun fillMissingVitals(
        date: String,
        maxHr: Int?,
        minHr: Int?,
        avgHr: Int?,
        hrSeries: String?,
    )

    /** 같은 날짜 모든 행의 영법 갱신 — 서버 일 단위 치유용 (단일 세션 날에만 호출). */
    @Query(
        "UPDATE swim_logs SET strokeFreestyleM = :free, strokeBreastM = :breast, " +
            "strokeBackM = :back, strokeFlyM = :fly, strokeMixedM = :mixed, strokeKickM = :kick " +
            "WHERE date = :date",
    )
    suspend fun updateStrokes(date: String, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int)

    /** 특정 세션 행의 영법별 거리(m)를 갱신한다. (캘린더 세션별 수동 보정) */
    @Query(
        "UPDATE swim_logs SET strokeFreestyleM = :free, strokeBreastM = :breast, " +
            "strokeBackM = :back, strokeFlyM = :fly, strokeMixedM = :mixed, strokeKickM = :kick " +
            "WHERE id = :id",
    )
    suspend fun updateStrokesById(id: Long, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int)

    /** 그 날짜 모든 행을 서버 보고 완료로 표시한다. */
    @Query("UPDATE swim_logs SET synced = 1 WHERE date = :date")
    suspend fun markSynced(date: String)

    /** 그 날짜 모든 행을 미전송으로 되돌린다 — 세션 삭제 후 일 집계 재전송용. */
    @Query("UPDATE swim_logs SET synced = 0 WHERE date = :date")
    suspend fun markUnsynced(date: String)

    /** 서버 보고가 안 된 행이 있는 날짜 목록 — 동기화 때 재전송 대상. */
    @Query("SELECT DISTINCT date FROM swim_logs WHERE synced = 0 ORDER BY date ASC")
    suspend fun getUnsyncedDates(): List<String>

    @Query("DELETE FROM swim_logs WHERE date = :date")
    suspend fun deleteByDate(date: String)

    /** 세션 행 하나를 삭제한다 — 캘린더의 세션 단위 삭제용. */
    @Query("DELETE FROM swim_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM swim_logs WHERE hcRecordId = :hcRecordId")
    suspend fun deleteByHcRecordId(hcRecordId: String)

    /** 모든 수영 기록 삭제 — 로그아웃/계정 탈퇴 시 로컬 정리용. */
    @Query("DELETE FROM swim_logs")
    suspend fun deleteAll()

    @Query("SELECT COALESCE(SUM(distanceMeters), 0) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalDistance(startDate: String, endDate: String): Int

    @Query("SELECT COUNT(DISTINCT date) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getSwimCount(startDate: String, endDate: String): Int

    @Query("SELECT COALESCE(SUM(calories), 0) FROM swim_logs WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalCalories(startDate: String, endDate: String): Int

    @Query("SELECT * FROM swim_logs ORDER BY date DESC, startEpochSec DESC, id DESC LIMIT 1")
    suspend fun getLatest(): SwimLogEntity?
}
