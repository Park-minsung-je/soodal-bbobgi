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
    suspend fun deleteByDate(date: String)
    suspend fun deleteByHcRecordId(hcRecordId: String)
    suspend fun getStats(startDate: String, endDate: String): SwimStats
    suspend fun getLatest(): SwimLog?
}
