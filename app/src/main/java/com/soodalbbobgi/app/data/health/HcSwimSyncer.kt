package com.soodalbbobgi.app.data.health

import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.SwimLogRequest
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

// 페이스/휴식구간 알고리즘 버전 — 계산 방식이 바뀌면 +1 해서
// 기존 기록을 한 번만 다시 계산하게 한다.
internal const val HC_ALGO_VERSION = 2

/**
 * HC ↔ 로컬 ↔ 서버 수영 기록 동기화 오케스트레이터 — Splash 자동 동기화와 홈 수동 동기화가 공유한다.
 *
 * 하루 여러 세션을 전제로 하며 HC 세션의 정체성은 hcRecordId다. 매 동기화는 가볍게:
 * 1) 변경 토큰으로 추가/수정/삭제 변경분만 처리 (토큰이 없거나 만료면 오늘 기록만 읽고 새 토큰 발급)
 * 2) 알고리즘 버전이 바뀐 빌드의 첫 동기화에만 — 최근 30일 기록의 페이스·휴식구간을 한 번 재계산
 * 3) 서버에 아직 보고 안 된(synced=false) 날짜의 일 집계를 전송 — 실패해도 다음 동기화에 재시도
 * 4) 서버 pull — 로컬에 없는 과거 날짜를 서버 백업에서 복원
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
        refreshIfAlgoChanged()
        val earned = pushUnsyncedDates()
        pullServerSwimLogs()
        return earned
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

    /**
     * 페이스·휴식구간 알고리즘이 바뀐 빌드의 첫 동기화에만 최근 30일 기록을 다시 읽어
     * 기존 행의 실운동시간과 휴식구간을 재계산한다. 평소 동기화에는 실행되지 않는다.
     */
    private suspend fun refreshIfAlgoChanged() {
        if (hcSyncPreferences.getAlgoVersion() == HC_ALGO_VERSION) return
        try {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val start = today.minusDays(29).atStartOfDay(zone).toInstant()
            val end = today.plusDays(1).atStartOfDay(zone).toInstant()
            for (session in healthConnectManager.readSwimSessions(start, end)) upsert(session)
            hcSyncPreferences.saveAlgoVersion(HC_ALGO_VERSION)
            Timber.d("알고리즘 v%d 재계산 완료", HC_ALGO_VERSION)
        } catch (e: Exception) {
            // 버전을 저장하지 않아 다음 동기화에 다시 시도된다
            Timber.w(e, "알고리즘 변경 재계산 실패")
        }
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
        val response = soodalApi.addSwimLog(
            SwimLogRequest(
                date = date,
                distanceMeters = rows.sumOf { it.distanceMeters },
                durationSeconds = rows.sumOf { it.durationSeconds },
                calories = rows.sumOf { it.calories },
                strokeFreestyleM = 0, strokeBreastM = 0,
                strokeBackM = 0, strokeFlyM = 0,
                strokeMixedM = rows.sumOf { it.distanceMeters }, strokeKickM = 0,
                source = "health_connect",
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
