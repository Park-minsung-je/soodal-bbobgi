package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.model.SwimStats
import com.soodalbbobgi.app.domain.repository.SwimLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
// CurrencyUseCase 의존성 제거: 조개 지급은 서버 권한.

class SwimLogUseCase @Inject constructor(
    private val swimLogRepo: SwimLogRepository,
) {
    /**
     * 같은 날짜의 로컬 swim_log가 없으면 저장한다.
     * 조개 지급은 서버에서 처리하므로 여기서는 로컬 저장만 담당.
     *
     * @return 저장된 행이 있으면 1, 이미 있어서 스킵하면 0
     */
    suspend fun syncSwimLog(userId: String, log: SwimLog): Int {
        val existing = swimLogRepo.getByDateOnce(log.date)
        if (existing != null) return 0
        swimLogRepo.addSwimLog(log)
        return 1
    }

    /** 서버에서 pull한 기록을 로컬에 저장한다 (조개 지급 없이). */
    suspend fun saveFromServer(log: SwimLog) {
        val existing = swimLogRepo.getByDateOnce(log.date)
        if (existing == null) {
            swimLogRepo.addSwimLog(log)
        }
    }

    /** HC 레코드 UID로 로컬 기록을 찾아 날짜를 반환한다. */
    suspend fun getDateByHcRecordId(hcRecordId: String): String? =
        swimLogRepo.getByHcRecordId(hcRecordId)?.date

    /** 날짜로 로컬 수영 기록을 삭제한다. */
    suspend fun deleteByDate(date: String) = swimLogRepo.deleteByDate(date)

    /** HC 레코드 UID로 로컬 수영 기록을 삭제한다. */
    suspend fun deleteByHcRecordId(hcRecordId: String) = swimLogRepo.deleteByHcRecordId(hcRecordId)

    fun getLogsByDateRange(startDate: String, endDate: String): Flow<List<SwimLog>> =
        swimLogRepo.getByDateRange(startDate, endDate)

    suspend fun getMonthStats(startDate: String, endDate: String): SwimStats =
        swimLogRepo.getStats(startDate, endDate)
}
