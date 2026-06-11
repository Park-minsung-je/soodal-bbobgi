package com.soodalbbobgi.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.asset.AssetManager
import com.soodalbbobgi.app.data.asset.AssetSyncProgress
import com.soodalbbobgi.app.data.auth.ServerFailure
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.auth.classifyServerFailure
import com.soodalbbobgi.app.data.health.HcSwimSyncer
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.RefreshRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 스플래시 화면에서 자동 로그인을 시도하고 이동할 화면을 결정한다.
 *
 * 서버가 진실의 소스라는 아키텍처를 따른다:
 * - 로컬에는 swim_logs만 영속 (HC 동기화 + 오프라인 캘린더)
 * - 그 외 모든 상태는 [AppState] 메모리에 보관
 * - [AppStateLoader.loadAll]이 서버에서 한 번에 받아 채운다
 *
 * 토큰 삭제는 서버가 **명시적으로 거부**(4xx)했을 때만 한다. 네트워크 장애나
 * 서버 다운(5xx)이면 토큰을 보존하고 [serverError]를 올려 사용자에게
 * "서버 문제"임을 알린다 — Auth로 보내 재로그인을 시도하게 하지 않는다.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val soodalApi: SoodalApi,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val healthConnectManager: HealthConnectManager,
    private val hcSwimSyncer: HcSwimSyncer,
    private val assetManager: AssetManager,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    // 서버 장애(연결 불가/5xx)로 진행이 막힌 상태 — 화면이 안내 + 재시도 버튼을 보여준다.
    private val _serverError = MutableStateFlow(false)
    val serverError: StateFlow<Boolean> = _serverError

    /**
     * 에셋 동기화 진행 상태. SplashScreen이 collect해서 진행률/라벨을 표시한다.
     * AssetManager.progress를 그대로 위임 — 별도 State를 두지 않아 단일 출처를 유지한다.
     */
    val assetSyncProgress: StateFlow<AssetSyncProgress> = assetManager.progress

    init {
        // 자동 로그인과 에셋 동기화는 독립이라 병렬 실행한다.
        // 에셋 동기화가 실패해도 화면 진입은 막지 않는다 — AssetImage가 네트워크 fallback으로 graceful degradation.
        syncAssets()
        checkAuth()
    }

    /** 서버 장애 안내에서 "다시 시도"를 눌렀을 때 — 인증 확인(필요시 에셋 동기화도)을 재실행한다. */
    fun retry() {
        if (assetSyncProgress.value is AssetSyncProgress.Error) syncAssets()
        checkAuth()
    }

    /**
     * 백그라운드로 서버 에셋 매니페스트를 동기화한다.
     * 실패해도 destination 결정에 영향을 주지 않으며, AssetImage는 네트워크 URL로 폴백된다.
     */
    private fun syncAssets() {
        viewModelScope.launch {
            val result = assetManager.sync()
            if (result.isFailure) {
                Timber.w(result.exceptionOrNull(), "에셋 동기화 실패 (앱 계속 진행)")
            }
        }
    }

    private fun checkAuth() {
        viewModelScope.launch {
            _serverError.value = false
            val token = tokenStore.getAccessToken()
            if (token == null) {
                _destination.value = SplashDestination.Auth
                return@launch
            }

            try {
                if (tokenStore.isAccessTokenExpired()) {
                    when (refreshAccessToken()) {
                        RefreshResult.SUCCESS -> Unit
                        RefreshResult.REJECTED -> {
                            _destination.value = SplashDestination.Auth
                            return@launch
                        }
                        RefreshResult.UNREACHABLE -> {
                            _serverError.value = true
                            return@launch
                        }
                    }
                }

                // 서버에서 전체 상태 로드 → AppState
                val loaded = appStateLoader.loadAll()
                if (loaded.isFailure) {
                    val cause = loaded.exceptionOrNull()
                    Timber.w(cause, "AppState 로드 실패")
                    if (cause != null && classifyServerFailure(cause) == ServerFailure.REJECTED) {
                        // 서버가 인증을 거부 — 세션 만료로 보고 재로그인 유도
                        tokenStore.clearTokens()
                        _destination.value = SplashDestination.Auth
                    } else {
                        // 일시 장애 — 토큰 보존, 사용자에게 서버 문제임을 알린다
                        _serverError.value = true
                    }
                    return@launch
                }
                if (appState.profile.value == null) {
                    // 호출은 성공했지만 프로필이 비어 있음 — 서버 응답 이상. 세션은 보존한다.
                    Timber.w("loadAll 성공했으나 profile 비어 있음 — 서버 오류로 처리")
                    _serverError.value = true
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
                if (classifyServerFailure(e) == ServerFailure.REJECTED) {
                    tokenStore.clearTokens()
                    _destination.value = SplashDestination.Auth
                } else {
                    _serverError.value = true
                }
            }
        }
    }

    /** 토큰 갱신 시도 결과 — 거부(재로그인)와 장애(토큰 보존)를 구분한다. */
    private enum class RefreshResult { SUCCESS, REJECTED, UNREACHABLE }

    private suspend fun refreshAccessToken(): RefreshResult {
        val refreshToken = tokenStore.getRefreshToken()
        if (refreshToken == null) {
            tokenStore.clearTokens()
            return RefreshResult.REJECTED
        }
        return try {
            val res = soodalApi.refreshToken(RefreshRequest(refreshToken))
            if (res.success && res.data != null) {
                tokenStore.saveTokens(res.data.accessToken, res.data.refreshToken, res.data.expiresIn)
                RefreshResult.SUCCESS
            } else {
                tokenStore.clearTokens()
                RefreshResult.REJECTED
            }
        } catch (e: Exception) {
            Timber.w(e, "토큰 갱신 실패")
            when (classifyServerFailure(e)) {
                ServerFailure.REJECTED -> {
                    tokenStore.clearTokens()
                    RefreshResult.REJECTED
                }
                ServerFailure.UNREACHABLE -> RefreshResult.UNREACHABLE
            }
        }
    }

    /**
     * HC 변경 토큰 기반 수영 데이터 동기화.
     * 새 기록은 로컬 Room + 서버 POST → 서버 응답의 shellReward를 AppState에 반영.
     */
    private suspend fun syncHealthConnect() {
        // 홈 수동 동기화와 동일한 공유 흐름 (HcSwimSyncer)
        val totalEarned = hcSwimSyncer.sync()
        if (totalEarned > 0) {
            appState.addPendingShellReward(totalEarned)
            // 누적 currency 다시 받아 정합성 보장
            appStateLoader.refreshCurrency()
        }
    }
}

enum class SplashDestination {
    Loading, Auth, Onboarding, Permission, Home
}
