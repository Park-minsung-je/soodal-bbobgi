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

    /**
     * 서버에서 pull한 기록을 로컬에 반영한다.
     * - 같은 날짜 로컬 row가 없으면 insert
     * - 있으면 shellsEarned 차이가 있을 때 update (HC sync 직후 0으로 들어간 로컬 row를 서버 값으로 갱신)
     * - 로컬 영법이 손대지 않은 상태(혼영 외 전부 0)면 서버 분배를 적용 (영법 수정은 서버에 저장되므로 서버가 진실).
     *   로컬에서 편집한 분배(혼영 외 값 존재)는 보존한다 — 오프라인 편집을 옛 서버 값으로 덮지 않기 위함.
     */
    suspend fun saveFromServer(log: SwimLog) {
        val existing = swimLogRepo.getByDateOnce(log.date)
        if (existing == null) {
            swimLogRepo.addSwimLog(log)
            return
        }
        if (existing.shellsEarned != log.shellsEarned) {
            swimLogRepo.updateShellsEarned(log.date, log.shellsEarned)
        }
        val localUntouched = existing.strokeFreestyleM == 0 && existing.strokeBreastM == 0 &&
            existing.strokeBackM == 0 && existing.strokeFlyM == 0 && existing.strokeKickM == 0
        val strokesDiffer = existing.strokeFreestyleM != log.strokeFreestyleM ||
            existing.strokeBreastM != log.strokeBreastM ||
            existing.strokeBackM != log.strokeBackM ||
            existing.strokeFlyM != log.strokeFlyM ||
            existing.strokeMixedM != log.strokeMixedM ||
            existing.strokeKickM != log.strokeKickM
        if (localUntouched && strokesDiffer) {
            swimLogRepo.updateStrokes(
                log.date,
                log.strokeFreestyleM, log.strokeBreastM, log.strokeBackM,
                log.strokeFlyM, log.strokeMixedM, log.strokeKickM,
            )
        }
    }

    /** HC sync POST /swim-logs 응답으로 받은 조개 지급량을 같은 날짜 로컬 row에 반영. */
    suspend fun updateShellsEarned(date: String, shellsEarned: Int) =
        swimLogRepo.updateShellsEarned(date, shellsEarned)

    /** 캘린더에서 보정한 영법별 거리(m)를 같은 날짜 로컬 row에 반영. */
    suspend fun updateStrokes(date: String, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int) =
        swimLogRepo.updateStrokes(date, free, breast, back, fly, mixed, kick)

    /** HC 원본에서 읽은 심박/실운동시간을 같은 날짜 로컬 row에 반영. null인 값은 기존 값 유지. */
    suspend fun updateVitals(date: String, maxHr: Int?, minHr: Int?, activeSeconds: Int?) =
        swimLogRepo.updateVitals(date, maxHr, minHr, activeSeconds)

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
