package kr.ilf.soodalbbobgi.domain.usecase

import kr.ilf.soodalbbobgi.domain.model.SwimLog
import kr.ilf.soodalbbobgi.domain.model.SwimStats
import kr.ilf.soodalbbobgi.domain.repository.SwimLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
// CurrencyUseCase 의존성 제거: 조개 지급은 서버 권한.

class SwimLogUseCase @Inject constructor(
    private val swimLogRepo: SwimLogRepository,
) {
    /**
     * HC 세션을 로컬에 upsert한다 — 하루 여러 세션 전제, 정체성은 hcRecordId.
     * - 같은 hcRecordId 행이 있으면 핵심 필드만 갱신 (영법 편집·조개는 보존)
     * - 없고 그 날짜에 서버산 행(hcRecordId 없음)이 있으면 그 행에 HC 정체성을 승격
     * - 둘 다 아니면 새 행 insert (같은 날 다른 세션이 있어도 추가)
     * 조개 지급은 서버에서 처리하므로 여기서는 로컬 저장만 담당.
     *
     * @return 새로 저장된 행이 있으면 1, 기존 행 갱신/승격이면 0
     */
    suspend fun syncSwimLog(userId: String, log: SwimLog): Int {
        val hcRecordId = log.hcRecordId
        if (hcRecordId != null) {
            val existing = swimLogRepo.getByHcRecordId(hcRecordId)
            if (existing != null) {
                swimLogRepo.updateFromHc(existing.id, log)
                return 0
            }
            val serverRow = swimLogRepo.getLogsForDateOnce(log.date).firstOrNull { it.hcRecordId == null }
            if (serverRow != null) {
                swimLogRepo.updateFromHc(serverRow.id, log)
                return 0
            }
            swimLogRepo.addSwimLog(log)
            return 1
        }
        // hcRecordId 없는 기록(수동 등) — 날짜에 행이 없을 때만 저장
        if (swimLogRepo.getLogsForDateOnce(log.date).isNotEmpty()) return 0
        swimLogRepo.addSwimLog(log)
        return 1
    }

    /**
     * 서버에서 pull한 일 단위 기록을 로컬에 반영한다.
     * - 그 날짜에 행이 하나도 없으면 insert (HC가 못 채운 과거 복원용)
     * - 단일 세션 날: 조개 차이 반영 + 로컬 영법이 손대지 않은 상태(혼영 외 전부 0)면 서버 분배 적용.
     *   로컬에서 편집한 분배는 보존한다 — 오프라인 편집을 옛 서버 값으로 덮지 않기 위함.
     * - 다중 세션 날: 서버는 일 집계라 세션별 분배를 모르므로 조개만 반영.
     */
    suspend fun saveFromServer(log: SwimLog) {
        val existing = swimLogRepo.getLogsForDateOnce(log.date)
        if (existing.isEmpty()) {
            swimLogRepo.addSwimLog(log)
            return
        }
        if (existing.sumOf { it.shellsEarned } != log.shellsEarned) {
            swimLogRepo.updateShellsEarned(log.date, log.shellsEarned)
        }
        val single = existing.singleOrNull() ?: return

        // 심박은 로컬에 없을 때만 서버 값으로 채운다. 서버 값은 하루치를 합친 것이라
        // 세션별 로컬 값이 더 정확하고, 하루에 세션이 여럿이면 아예 손대지 않는다
        // (위의 singleOrNull이 그 경우를 이미 걸러낸다).
        val localHasNoVitals = single.maxHr == null && single.minHr == null &&
            single.avgHr == null && single.hrSeries == null
        val serverHasVitals = log.maxHr != null || log.minHr != null ||
            log.avgHr != null || log.hrSeries != null
        if (localHasNoVitals && serverHasVitals) {
            swimLogRepo.fillMissingVitals(log.date, log.maxHr, log.minHr, log.avgHr, log.hrSeries)
        }
        val localUntouched = single.strokeFreestyleM == 0 && single.strokeBreastM == 0 &&
            single.strokeBackM == 0 && single.strokeFlyM == 0 && single.strokeKickM == 0
        val strokesDiffer = single.strokeFreestyleM != log.strokeFreestyleM ||
            single.strokeBreastM != log.strokeBreastM ||
            single.strokeBackM != log.strokeBackM ||
            single.strokeFlyM != log.strokeFlyM ||
            single.strokeMixedM != log.strokeMixedM ||
            single.strokeKickM != log.strokeKickM
        if (localUntouched && strokesDiffer) {
            swimLogRepo.updateStrokes(
                log.date,
                log.strokeFreestyleM, log.strokeBreastM, log.strokeBackM,
                log.strokeFlyM, log.strokeMixedM, log.strokeKickM,
            )
        }
    }

    /**
     * 수동 입력 기록을 추가한다 — 같은 날 기존 세션이 있어도 추가 세션으로 저장한다.
     * ([syncSwimLog]의 hcRecordId 없는 경로는 하루 1행 제한이라 수동 다중 세션엔 부적합)
     */
    suspend fun addManualLog(log: SwimLog) = swimLogRepo.addSwimLog(log)

    /** 같은 날짜의 세션 목록을 반환한다 — 서버 일 집계 전송용. */
    suspend fun getLogsForDate(date: String): List<SwimLog> =
        swimLogRepo.getLogsForDateOnce(date)

    /** 그 날짜 모든 행을 서버 보고 완료(synced)로 표시한다. */
    suspend fun markSynced(date: String) = swimLogRepo.markSynced(date)

    /** 그 날짜 모든 행을 미전송으로 되돌린다 — 세션 삭제 후 일 집계 재전송용. */
    suspend fun markUnsynced(date: String) = swimLogRepo.markUnsynced(date)

    /** 서버 보고가 안 된 행이 있는 날짜 목록 — 동기화 때 재전송 대상. */
    suspend fun getUnsyncedDates(): List<String> = swimLogRepo.getUnsyncedDates()

    /** HC sync POST /swim-logs 응답으로 받은 조개 지급량을 같은 날짜 로컬 row에 반영. */
    suspend fun updateShellsEarned(date: String, shellsEarned: Int) =
        swimLogRepo.updateShellsEarned(date, shellsEarned)

    /** 캘린더에서 보정한 영법별 거리(m)를 해당 세션 행에 반영. */
    suspend fun updateStrokes(id: Long, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int) =
        swimLogRepo.updateStrokesById(id, free, breast, back, fly, mixed, kick)

    /** HC 레코드 UID로 로컬 기록을 찾아 날짜를 반환한다. */
    suspend fun getDateByHcRecordId(hcRecordId: String): String? =
        swimLogRepo.getByHcRecordId(hcRecordId)?.date

    /** 날짜로 로컬 수영 기록을 삭제한다. */
    suspend fun deleteByDate(date: String) = swimLogRepo.deleteByDate(date)

    /** 세션 행 하나를 삭제한다 — 캘린더의 세션 단위 삭제용. */
    suspend fun deleteById(id: Long) = swimLogRepo.deleteById(id)

    /** HC 레코드 UID로 로컬 수영 기록을 삭제한다. */
    suspend fun deleteByHcRecordId(hcRecordId: String) = swimLogRepo.deleteByHcRecordId(hcRecordId)

    fun getLogsByDateRange(startDate: String, endDate: String): Flow<List<SwimLog>> =
        swimLogRepo.getByDateRange(startDate, endDate)

    suspend fun getMonthStats(startDate: String, endDate: String): SwimStats =
        swimLogRepo.getStats(startDate, endDate)
}
