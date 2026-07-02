package com.soodalbbobgi.app.data.health

import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.SwimLogRequest
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HC ↔ 로컬 ↔ 서버 수영 기록 동기화 오케스트레이터 — Splash 자동 동기화와 홈 수동 동기화가 공유한다.
 *
 * 하루 여러 세션을 전제로 하며 HC 세션의 정체성은 hcRecordId다. 매 동기화는 가볍게:
 * 1) 변경 토큰으로 추가/수정/삭제 변경분만 처리 (토큰이 없거나 만료면 오늘 기록만 읽고 새 토큰 발급)
 * 2) 서버에 아직 보고 안 된(synced=false) 날짜의 일 집계를 전송 — 실패해도 다음 동기화에 재시도
 * 3) 서버 pull — 로컬에 없는 과거 날짜를 서버 백업에서 복원
 */
@Singleton
class HcSwimSyncer @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val swimLogUseCase: SwimLogUseCase,
    private val soodalApi: SoodalApi,
    private val hcSyncPreferences: HcSyncPreferences,
    private val userSession: UserSession,
    private val appStateLoader: AppStateLoader,
) {

    /**
     * 전체 동기화를 수행한다.
     *
     * @return 이번 동기화로 서버가 지급한 조개 수
     */
    suspend fun sync(): Int {
        syncChanges()
        val earned = pushUnsyncedDates()
        pullServerSwimLogs()
        return earned
    }

    /**
     * 수동 입력 기록을 오늘 세션으로 등록하고 서버에 보고한다.
     * (인증은 v1에서 즉시 통과 — 추후 AI 사진 인증으로 대체 예정)
     *
     * @param distanceMeters 수영 거리(m)
     * @param durationMin 수영 시간(분)
     * @param calories 칼로리(kcal) — null이면 거리 기반 추정
     * @param maxHr 최대 심박(bpm, 선택)
     * @param minHr 최소 심박(bpm, 선택)
     * @param date 기록 날짜 — 기본은 오늘, 캘린더에서 과거 날짜 입력 가능 (미래는 호출부에서 차단)
     * @param startTime 시작 시각 (선택) — 없으면 오늘은 등록 시각, 과거 날짜는 정오로 근사
     * @param strokeFreeM~strokeKickM 영법별 거리(m) — 합계의 잔여분은 혼영으로 배정된다
     * @return 서버가 지급한 조개 수 (그 날 첫 기록일 때만 > 0)
     */
    suspend fun registerManual(
        distanceMeters: Int,
        durationMin: Int,
        calories: Int? = null,
        maxHr: Int? = null,
        minHr: Int? = null,
        date: LocalDate = LocalDate.now(),
        startTime: LocalTime? = null,
        strokeFreeM: Int = 0,
        strokeBreastM: Int = 0,
        strokeBackM: Int = 0,
        strokeFlyM: Int = 0,
        strokeKickM: Int = 0,
    ): Int {
        val now = java.time.LocalDateTime.now()
        val zone = ZoneId.systemDefault()
        val startEpoch = when {
            startTime != null -> date.atTime(startTime).atZone(zone).toEpochSecond()
            // 과거 날짜는 시작 시각을 알 수 없으므로 정오로 둔다 (시간대 라벨용 근사값).
            date == now.toLocalDate() -> now.atZone(zone).toEpochSecond()
            else -> date.atTime(12, 0).atZone(zone).toEpochSecond()
        }
        val strokeSum = strokeFreeM + strokeBreastM + strokeBackM + strokeFlyM + strokeKickM
        swimLogUseCase.addManualLog(
            SwimLog(
                userId = userSession.userId,
                date = date.toString(),
                startEpochSec = startEpoch,
                distanceMeters = distanceMeters,
                durationSeconds = durationMin * 60,
                // 미입력 시 거리 기반 대략 추정 (자유형 완만 페이스 기준).
                calories = calories ?: (distanceMeters * 0.21f).toInt(),
                strokeFreestyleM = strokeFreeM,
                strokeBreastM = strokeBreastM,
                strokeBackM = strokeBackM,
                strokeFlyM = strokeFlyM,
                strokeKickM = strokeKickM,
                strokeMixedM = (distanceMeters - strokeSum).coerceAtLeast(0),
                source = "manual",
                maxHr = maxHr,
                minHr = minHr,
            )
        )
        return pushUnsyncedDates()
    }

    /** HC 변경분(추가/수정/삭제)을 반영한다. 토큰이 없거나 만료면 오늘 기록만 읽고 새 토큰 발급. */
    private suspend fun syncChanges() {
        val storedToken = hcSyncPreferences.getChangesToken()
        if (storedToken != null) {
            val result = healthConnectManager.getChanges(storedToken)
            if (result != null) {
                for (session in result.addedSessions) upsert(session)
                processDeletedRecords(result.deletedRecordIds)
                hcSyncPreferences.saveChangesToken(result.nextToken)
                return
            }
        }
        val token = healthConnectManager.getChangesToken()
        val zone = ZoneId.systemDefault()
        val now = java.time.LocalDateTime.now()
        // 자정 직후 동기화하면 어제 밤 수영이 빠지지 않게 새벽 2시까지는 어제부터 읽는다
        val fetchFrom = if (now.hour < 2) now.toLocalDate().minusDays(1) else now.toLocalDate()
        val start = fetchFrom.atStartOfDay(zone).toInstant()
        val end = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant()
        for (session in healthConnectManager.readSwimSessions(start, end)) upsert(session)
        hcSyncPreferences.saveChangesToken(token)
    }

    private suspend fun upsert(session: SwimSession) {
        try {
            swimLogUseCase.syncSwimLog(userSession.userId, session.toLog())
        } catch (e: Exception) {
            Timber.w(e, "수영 세션 저장 실패: ${session.date}")
        }
    }

    /**
     * 서버에 아직 보고 안 된(synced=false) 행이 있는 날짜의 일 집계를 전송한다.
     * 네트워크 실패면 미전송으로 남아 다음 동기화에 자동 재시도된다.
     */
    private suspend fun pushUnsyncedDates(): Int {
        var totalEarned = 0
        for (date in swimLogUseCase.getUnsyncedDates()) {
            try {
                val rows = swimLogUseCase.getLogsForDate(date)
                if (rows.isNotEmpty()) {
                    totalEarned += postDayAggregate(date, rows)
                }
            } catch (e: retrofit2.HttpException) {
                if (e.code() in 400..499) {
                    // 서버의 명시적 거부(409 중복 등) — 재시도 무의미, 전송됨 처리
                    swimLogUseCase.markSynced(date)
                    Timber.w("수영 기록 전송 거부(HTTP %d) — 전송됨 처리: %s", e.code(), date)
                } else {
                    Timber.w(e, "수영 기록 전송 실패: $date")
                }
            } catch (e: Exception) {
                Timber.w(e, "수영 기록 전송 실패: $date")
            }
        }
        return totalEarned
    }

    /**
     * 그 날짜의 세션 합계를 서버에 POST하고 지급된 조개 수를 반환한다.
     * 서버 응답을 받았으면(수락이든 거부든) 전송됨으로 표시한다 —
     * 거부(이미 있는 날짜 등)를 무한 재시도하지 않기 위함. 예외는 호출자가 처리.
     */
    private suspend fun postDayAggregate(date: String, rows: List<SwimLog>): Int {
        // 영법은 행에 저장된 값을 그대로 합산한다 — 수동 입력의 영법 구성이 서버에도 보존된다.
        // (HC 행은 저장 시 혼영=거리로 들어가므로 합계는 항상 거리와 일치한다)
        val response = soodalApi.addSwimLog(
            SwimLogRequest(
                date = date,
                distanceMeters = rows.sumOf { it.distanceMeters },
                durationSeconds = rows.sumOf { it.durationSeconds },
                calories = rows.sumOf { it.calories },
                strokeFreestyleM = rows.sumOf { it.strokeFreestyleM },
                strokeBreastM = rows.sumOf { it.strokeBreastM },
                strokeBackM = rows.sumOf { it.strokeBackM },
                strokeFlyM = rows.sumOf { it.strokeFlyM },
                strokeMixedM = rows.sumOf { it.strokeMixedM },
                strokeKickM = rows.sumOf { it.strokeKickM },
                source = if (rows.all { it.source == "manual" }) "manual" else "health_connect",
            )
        )
        swimLogUseCase.markSynced(date)
        if (!response.success || response.data == null) return 0
        val earned = response.data.shellReward?.earned ?: 0
        // 서버가 지급한 조개량을 로컬 swim_log에도 즉시 반영 (캘린더 표시용)
        if (earned > 0) {
            swimLogUseCase.updateShellsEarned(date, earned)
        }
        response.data.shellReward?.newBalance?.let { appStateLoader.applyShellReward(it) }
        return earned
    }

    private suspend fun processDeletedRecords(deletedRecordIds: List<String>) {
        for (hcRecordId in deletedRecordIds) {
            try {
                val date = swimLogUseCase.getDateByHcRecordId(hcRecordId) ?: continue
                swimLogUseCase.deleteByHcRecordId(hcRecordId)
                // 같은 날 다른 세션이 남아 있으면 서버 일 기록은 유지한다
                if (swimLogUseCase.getLogsForDate(date).isEmpty()) {
                    soodalApi.deleteSwimLog(date)
                }
                Timber.d("수영 기록 삭제 동기화: $date ($hcRecordId)")
            } catch (e: Exception) {
                Timber.w(e, "수영 기록 삭제 동기화 실패: $hcRecordId")
            }
        }
    }

    /** HC가 못 채운 날짜만 서버 백업에서 복원한다. */
    private suspend fun pullServerSwimLogs() {
        try {
            val today = LocalDate.now()
            val response = soodalApi.getSwimLogs(
                startDate = today.minusDays(30).toString(),
                endDate = today.toString(),
            )
            if (response.success && response.data != null) {
                for (serverLog in response.data.items) {
                    swimLogUseCase.saveFromServer(
                        SwimLog(
                            userId = userSession.userId,
                            date = serverLog.date,
                            distanceMeters = serverLog.distanceMeters,
                            durationSeconds = serverLog.durationSeconds,
                            calories = serverLog.calories,
                            strokeFreestyleM = serverLog.strokeFreestyleM,
                            strokeBreastM = serverLog.strokeBreastM,
                            strokeBackM = serverLog.strokeBackM,
                            strokeFlyM = serverLog.strokeFlyM,
                            strokeMixedM = serverLog.strokeMixedM,
                            strokeKickM = serverLog.strokeKickM,
                            source = serverLog.source,
                            shellsEarned = serverLog.shellsEarned,
                            synced = true, // 서버에서 온 기록 — 되돌려 보낼 필요 없음
                        )
                    )
                }
                Timber.d("서버 수영 기록 pull 완료: ${response.data.items.size}개")
            }
        } catch (e: Exception) {
            Timber.w(e, "서버 수영 기록 pull 실패")
        }
    }

    private fun SwimSession.toLog() = SwimLog(
        userId = userSession.userId,
        date = date,
        startEpochSec = startEpochSec,
        distanceMeters = distanceMeters,
        durationSeconds = durationSeconds,
        calories = calories,
        strokeMixedM = distanceMeters,
        source = "health_connect",
        hcRecordId = hcRecordId,
        maxHr = maxHr,
        minHr = minHr,
        activeSeconds = activeSeconds,
        hrSeries = hrSeries,
    )
}
