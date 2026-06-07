package com.soodalbbobgi.app.domain.repository

import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.model.SwimStats
import kotlinx.coroutines.flow.Flow

interface SwimLogRepository {
    suspend fun addSwimLog(log: SwimLog)
    /** 같은 날짜의 세션 목록 — 시작 시각 순. */
    fun getByDate(date: String): Flow<List<SwimLog>>
    suspend fun getLogsForDateOnce(date: String): List<SwimLog>
    suspend fun getByHcRecordId(hcRecordId: String): SwimLog?
    fun getByDateRange(startDate: String, endDate: String): Flow<List<SwimLog>>
    /** 서버 POST 응답으로 받은 조개 지급량을 그 날짜 첫 행에 반영. (지급은 일 단위) */
    suspend fun updateShellsEarned(date: String, shellsEarned: Int)
    /** 같은 날짜 모든 행의 영법 갱신 — 서버 일 단위 치유용 (단일 세션 날에만 호출). */
    suspend fun updateStrokes(date: String, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int)
    /** 특정 세션 행의 영법별 거리(m)를 갱신한다. */
    suspend fun updateStrokesById(id: Long, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int)
    /** HC 재동기화로 기존 행([id])의 핵심 필드를 [log] 값으로 갱신한다. 편집된 영법은 보존. */
    suspend fun updateFromHc(id: Long, log: SwimLog)
    suspend fun deleteByDate(date: String)
    suspend fun deleteByHcRecordId(hcRecordId: String)
    suspend fun getStats(startDate: String, endDate: String): SwimStats
    suspend fun getLatest(): SwimLog?
}
