package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.model.SwimStats
import com.soodalbbobgi.app.domain.repository.SwimLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SwimLogUseCase @Inject constructor(
    private val swimLogRepo: SwimLogRepository,
    private val currencyUseCase: CurrencyUseCase,
) {
    suspend fun syncSwimLog(userId: String, log: SwimLog): Int {
        val existing = swimLogRepo.getByDate(log.date).first()
        if (existing != null) return 0

        swimLogRepo.addSwimLog(log)
        return currencyUseCase.grantDailyShells(userId, log.date)
    }

    fun getLogsByDateRange(startDate: String, endDate: String): Flow<List<SwimLog>> =
        swimLogRepo.getByDateRange(startDate, endDate)

    suspend fun getMonthStats(startDate: String, endDate: String): SwimStats =
        swimLogRepo.getStats(startDate, endDate)
}
