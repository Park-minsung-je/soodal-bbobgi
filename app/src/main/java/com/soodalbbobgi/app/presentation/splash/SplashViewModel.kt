package com.soodalbbobgi.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.health.HcSyncPreferences
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.health.SwimSession
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.RefreshRequest
import com.soodalbbobgi.app.data.remote.dto.SwimLogRequest
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * 스플래시 화면에서 자동 로그인을 시도하고 이동할 화면을 결정한다.
 *
 * 서버가 진실의 소스라는 아키텍처를 따른다:
 * - 로컬에는 swim_logs만 영속 (HC 동기화 + 오프라인 캘린더)
 * - 그 외 모든 상태는 [AppState] 메모리에 보관
 * - [AppStateLoader.loadAll]이 서버에서 한 번에 받아 채운다
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val soodalApi: SoodalApi,
    private val userSession: UserSession,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val healthConnectManager: HealthConnectManager,
    private val swimLogUseCase: SwimLogUseCase,
    private val hcSyncPreferences: HcSyncPreferences,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val token = tokenStore.getAccessToken()
            if (token == null) {
                _destination.value = SplashDestination.Auth
                return@launch
            }

            try {
                if (tokenStore.isAccessTokenExpired() && !refreshAccessToken()) {
                    _destination.value = SplashDestination.Auth
                    return@launch
                }

                // 서버에서 전체 상태 로드 → AppState
                val loaded = appStateLoader.loadAll()
                if (loaded.isFailure || appState.profile.value == null) {
                    Timber.w("AppState 로드 실패 → Auth로 복귀")
                    tokenStore.clearTokens()
                    _destination.value = SplashDestination.Auth
                    return@launch
                }

                // HC 동기화 (가능하면)
                try {
                    if (healthConnectManager.hasAllPermissions()) syncHealthConnect()
                } catch (e: Exception) {
                    Timber.w(e, "HC 동기화 중 오류 (앱 계속 진행)")
                    _syncError.value = "수영 데이터 동기화에 실패했어요."
                }

                val profile = appState.profile.value
                val hasHcPermission = healthConnectManager.hasAllPermissions()
                _destination.value = when {
                    profile?.nickname.isNullOrBlank() -> SplashDestination.Onboarding
                    !hasHcPermission -> SplashDestination.Permission
                    else -> SplashDestination.Home
                }
            } catch (e: Exception) {
                Timber.w(e, "자동 로그인 실패")
                tokenStore.clearTokens()
                _destination.value = SplashDestination.Auth
            }
        }
    }

    private suspend fun refreshAccessToken(): Boolean {
        val refreshToken = tokenStore.getRefreshToken() ?: return false.also { tokenStore.clearTokens() }
        return try {
            val res = soodalApi.refreshToken(RefreshRequest(refreshToken))
            if (res.success && res.data != null) {
                tokenStore.saveTokens(res.data.accessToken, res.data.refreshToken, res.data.expiresIn)
                true
            } else {
                tokenStore.clearTokens()
                false
            }
        } catch (e: Exception) {
            Timber.w(e, "토큰 갱신 실패")
            tokenStore.clearTokens()
            false
        }
    }

    /**
     * HC 변경 토큰 기반 수영 데이터 동기화.
     * 새 기록은 로컬 Room + 서버 POST → 서버 응답의 shellReward를 AppState에 반영.
     */
    private suspend fun syncHealthConnect() {
        val storedToken = hcSyncPreferences.getChangesToken()
        var totalEarned = 0

        if (storedToken != null) {
            val result = healthConnectManager.getChanges(storedToken)
            if (result != null) {
                totalEarned = processAddedSessions(result.addedSessions)
                processDeletedRecords(result.deletedRecordIds)
                hcSyncPreferences.saveChangesToken(result.nextToken)
            } else {
                totalEarned = fullReadAndInitToken()
            }
        } else {
            totalEarned = fullReadAndInitToken()
        }

        pullServerSwimLogs()

        if (totalEarned > 0) {
            appState.addPendingShellReward(totalEarned)
            // 누적 currency 다시 받아 정합성 보장
            appStateLoader.refreshCurrency()
        }
    }

    private suspend fun fullReadAndInitToken(): Int {
        val token = healthConnectManager.getChangesToken()
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val zone = ZoneId.systemDefault()
        val fetchFrom = if (now.hour < 2) today.minusDays(1) else today
        val startOfDay = fetchFrom.atStartOfDay(zone).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()
        val sessions = healthConnectManager.readSwimSessions(startOfDay, endOfDay)
        Timber.d("Splash HC 전체 읽기: ${sessions.size}개 세션")
        val earned = processAddedSessions(sessions)
        hcSyncPreferences.saveChangesToken(token)
        return earned
    }

    private suspend fun processAddedSessions(sessions: List<SwimSession>): Int {
        var totalEarned = 0
        for (session in sessions) {
            try {
                swimLogUseCase.syncSwimLog(userSession.userId, SwimLog(
                    userId = userSession.userId,
                    date = session.date,
                    distanceMeters = session.distanceMeters,
                    durationSeconds = session.durationSeconds,
                    calories = session.calories,
                    strokeMixedM = session.distanceMeters,
                    source = "health_connect",
                    hcRecordId = session.hcRecordId,
                ))
                val response = soodalApi.addSwimLog(SwimLogRequest(
                    date = session.date,
                    distanceMeters = session.distanceMeters,
                    durationSeconds = session.durationSeconds,
                    calories = session.calories,
                    strokeFreestyleM = 0, strokeBreastM = 0,
                    strokeBackM = 0, strokeFlyM = 0,
                    strokeMixedM = session.distanceMeters, strokeKickM = 0,
                    source = "health_connect",
                ))
                if (response.success && response.data != null) {
                    val earned = response.data.shellReward?.earned ?: 0
                    totalEarned += earned
                    response.data.shellReward?.newBalance?.let { appStateLoader.applyShellReward(it) }
                }
            } catch (e: Exception) {
                Timber.w(e, "수영 기록 동기화 실패: ${session.date}")
            }
        }
        return totalEarned
    }

    private suspend fun processDeletedRecords(deletedRecordIds: List<String>) {
        for (hcRecordId in deletedRecordIds) {
            try {
                val date = swimLogUseCase.getDateByHcRecordId(hcRecordId) ?: continue
                swimLogUseCase.deleteByHcRecordId(hcRecordId)
                soodalApi.deleteSwimLog(date)
                Timber.d("수영 기록 삭제 완료: $date")
            } catch (e: Exception) {
                Timber.w(e, "수영 기록 삭제 동기화 실패: $hcRecordId")
            }
        }
    }

    private suspend fun pullServerSwimLogs() {
        try {
            val today = LocalDate.now()
            val response = soodalApi.getSwimLogs(
                startDate = today.minusDays(30).toString(),
                endDate = today.toString(),
            )
            if (response.success && response.data != null) {
                for (serverLog in response.data.items) {
                    swimLogUseCase.saveFromServer(SwimLog(
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
                    ))
                }
                Timber.d("서버 수영 기록 pull 완료: ${response.data.items.size}개")
            }
        } catch (e: Exception) {
            Timber.w(e, "서버 수영 기록 pull 실패")
        }
    }
}

enum class SplashDestination {
    Loading, Auth, Onboarding, Permission, Home
}
