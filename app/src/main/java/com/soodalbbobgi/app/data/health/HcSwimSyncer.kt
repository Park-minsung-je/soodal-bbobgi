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

/**
 * HC ↔ 로컬 ↔ 서버 수영 기록 동기화 오케스트레이터 — Splash 자동 동기화와 홈 수동 동기화가 공유한다.
 *
 * 하루 여러 세션을 전제로 하며 HC 세션의 정체성은 hcRecordId다. 흐름:
 * 1) 변경 토큰으로 삭제 변경을 처리하고 토큰을 갱신한다 (추가/수정 변경은 2가 어차피 커버)
 * 2) 최근 30일 HC 세션을 모두 읽어 hcRecordId 기준 upsert — 재설치·유실 후에도 HC가 과거 복원의 1차 소스.
 *    날짜에 기록이 처음 생기면 서버에 일 집계를 POST한다 (조개는 서버가 일 단위로 지급)
 * 3) 서버 pull — HC가 못 채운 날짜(워치 미착용, 30일 이전 등)만 복원
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
        val storedToken = hcSyncPreferences.getChangesToken()
        if (storedToken != null) {
            val result = healthConnectManager.getChanges(storedToken)
            if (result != null) {
                processDeletedRecords(result.deletedRecordIds)
                hcSyncPreferences.saveChangesToken(result.nextToken)
            } else {
                // 토큰 만료 — 새로 발급 (복원은 reconcile이 담당)
                hcSyncPreferences.saveChangesToken(healthConnectManager.getChangesToken())
            }
        } else {
            hcSyncPreferences.saveChangesToken(healthConnectManager.getChangesToken())
        }
        val earned = reconcileRecentSessions()
        pullServerSwimLogs()
        return earned
    }

    /**
     * 최근 30일 HC 세션을 로컬과 대조해 빠진 세션은 추가하고 기존 세션은 갱신한다.
     * 변경 이벤트 유실(같은 날 두 세션 중 하나 누락 등)이 있어도 여기서 복구된다.
     */
    private suspend fun reconcileRecentSessions(): Int {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val start = today.minusDays(29).atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        val sessions = healthConnectManager.readSwimSessions(start, end)

        var totalEarned = 0
        for ((date, daySessions) in sessions.groupBy { it.date }) {
            var added = 0
            var hadRows = true
            try {
                hadRows = swimLogUseCase.getLogsForDate(date).isNotEmpty()
                for (session in daySessions) {
                    added += swimLogUseCase.syncSwimLog(userSession.userId, session.toLog())
                }
            } catch (e: Exception) {
                Timber.w(e, "수영 세션 저장 실패: $date")
            }
            // 날짜에 기록이 처음 생겼을 때만 서버에 일 집계 보고 (조개 지급 요청).
            // 이미 행이 있던 날짜는 서버도 알고 있거나(서버산) 이전에 보고된 날이다.
            if (!hadRows && added > 0) {
                totalEarned += postDayAggregate(date)
            }
        }
        return totalEarned
    }

    /** 그 날짜의 세션 합계를 서버에 POST하고 지급된 조개 수를 반환한다. */
    private suspend fun postDayAggregate(date: String): Int {
        return try {
            val rows = swimLogUseCase.getLogsForDate(date)
            if (rows.isEmpty()) return 0
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
            if (response.success && response.data != null) {
                val earned = response.data.shellReward?.earned ?: 0
                // 서버가 지급한 조개량을 로컬 swim_log에도 즉시 반영 (캘린더 표시용)
                if (earned > 0) {
                    swimLogUseCase.updateShellsEarned(date, earned)
                }
                response.data.shellReward?.newBalance?.let { appStateLoader.applyShellReward(it) }
                earned
            } else 0
        } catch (e: Exception) {
            Timber.w(e, "수영 기록 전송 실패: $date")
            0
        }
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
