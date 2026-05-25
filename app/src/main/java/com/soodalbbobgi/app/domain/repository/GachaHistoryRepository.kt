package com.soodalbbobgi.app.domain.repository

import com.soodalbbobgi.app.domain.model.GachaHistory
import kotlinx.coroutines.flow.Flow

interface GachaHistoryRepository {
    suspend fun record(history: GachaHistory): Long
    fun getRecent(userId: String, limit: Int = 20): Flow<List<GachaHistory>>
}
