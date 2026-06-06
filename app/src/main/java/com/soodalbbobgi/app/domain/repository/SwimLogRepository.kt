package com.soodalbbobgi.app.domain.repository

import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.model.SwimStats
import kotlinx.coroutines.flow.Flow

interface SwimLogRepository {
    suspend fun addSwimLog(log: SwimLog)
    fun getByDate(date: String): Flow<SwimLog?>
    suspend fun getByDateOnce(date: String): SwimLog?
    suspend fun getByHcRecordId(hcRecordId: String): SwimLog?
    fun getByDateRange(startDate: String, endDate: String): Flow<List<SwimLog>>
    /** 서버 POST 응답으로 받은 조개 지급량을 같은 날짜의 로컬 row에 반영. */
    suspend fun updateShellsEarned(date: String, shellsEarned: Int)
    /** 같은 날짜 기록의 영법별 거리(m)를 갱신한다. */
    suspend fun updateStrokes(date: String, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int)
    /** HC 원본에서 읽은 심박/실운동시간을 같은 날짜 row에 반영한다. null인 값은 기존 값 유지. */
    suspend fun updateVitals(date: String, maxHr: Int?, minHr: Int?, activeSeconds: Int?)
    suspend fun deleteByDate(date: String)
    suspend fun deleteByHcRecordId(hcRecordId: String)
    suspend fun getStats(startDate: String, endDate: String): SwimStats
    suspend fun getLatest(): SwimLog?
}
