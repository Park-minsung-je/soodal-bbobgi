package kr.ilf.soodalbbobgi.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ilf.soodalbbobgi.core.di.ApplicationScope
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.asset.AssetManager
import kr.ilf.soodalbbobgi.data.auth.AccountSwitchGuard
import kr.ilf.soodalbbobgi.data.auth.GoogleAuthManager
import kr.ilf.soodalbbobgi.data.auth.KakaoAuthManager
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.AuthData
import kr.ilf.soodalbbobgi.data.remote.dto.GoogleAuthRequest
import kr.ilf.soodalbbobgi.data.remote.dto.KakaoAuthRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 로그인 화면의 상태와 OAuth 인증 로직을 관리한다.
 * 인증 성공 시 [AppStateLoader.loadAll]로 전체 서버 상태를 받아 메모리에 채운다.
 * 로그인 후 에셋 동기화와 HC 동기화를 즉시 트리거해 재시작 없이 데이터가 보이게 한다.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val kakaoAuthManager: KakaoAuthManager,
    private val googleAuthManager: GoogleAuthManager,
    private val soodalApi: SoodalApi,
    private val tokenStore: TokenStore,
    private val accountSwitchGuard: AccountSwitchGuard,
    private val appStateLoader: AppStateLoader,
    private val appState: AppState,
    private val healthConnectManager: HealthConnectManager,
    private val assetManager: AssetManager,
    private val hcSwimSyncer: HcSwimSyncer,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * 카카오 로그인을 시작한다.
     *
     * @param activity 현재 Activity (카카오 SDK가 로그인 화면을 띄우기 위해 필요)
     */
    fun loginWithKakao(activity: android.app.Activity) {
        if (_uiState.value is AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading("kakao")

        viewModelScope.launch {
            try {
                val kakaoTokenResult = kakaoAuthManager.signIn(activity)
                val kakaoToken = kakaoTokenResult.getOrElse { error ->
                    Timber.e(error, "카카오 로그인 실패")
                    showError(error)
                    return@launch
                }

                val authResponse = soodalApi.authKakao(KakaoAuthRequest(kakaoToken))
                if (authResponse.success && authResponse.data != null) {
                    val data = authResponse.data
                    // 다른 계정이면 온보딩/홈 진입 전에 로컬을 비운다 — 초기화가 토큰도 지우므로 저장보다 앞에 둔다
                    accountSwitchGuard.ensureLocalOwnedBy(data.user.id)
                    tokenStore.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)

                    // 전체 서버 상태 로드 (profile/currency/inventory/gachaBoxes/profileCard)
                    val loaded = appStateLoader.loadAll()
                    if (loaded.isFailure) {
                        Timber.w(loaded.exceptionOrNull(), "AppState 로드 실패")
                    }

                    val hasHcPermission = healthConnectManager.hasAllPermissions()

                    // 로그인 직후 에셋·HC 동기화를 백그라운드로 시작해 앱 재시작 없이 데이터가 보이게 한다.
                    triggerPostLoginSync(hasHcPermission)

                    _uiState.value = AuthUiState.Success(route = resolveRoute(data))
                } else {
                    val errorCode = authResponse.error?.code ?: "UNKNOWN"
                    Timber.e("서버 카카오 인증 실패: $errorCode")
                    showError(RuntimeException("서버 인증 실패: $errorCode"))
                }
            } catch (e: Exception) {
                Timber.e(e, "카카오 인증 중 예외")
                showError(e)
            }
        }
    }

    /**
     * Google 로그인을 시작한다.
     *
     * Credential Manager로 idToken을 받은 뒤 서버 `/auth/google`에 전달.
     * 성공 시 [AppStateLoader.loadAll]로 메모리 채운 뒤 다음 화면을 결정한다.
     *
     * @param activity 현재 Activity (Credential Manager가 계정 선택 시스템 UI를 띄울 때 필요)
     */
    fun loginWithGoogle(activity: android.app.Activity) {
        if (_uiState.value is AuthUiState.Loading) return
        _uiState.value = AuthUiState.Loading("google")

        viewModelScope.launch {
            try {
                val idTokenResult = googleAuthManager.signIn(activity)
                val idToken = idTokenResult.getOrElse { error ->
                    Timber.e(error, "구글 로그인 실패")
                    showError(error)
                    return@launch
                }

                val authResponse = soodalApi.authGoogle(GoogleAuthRequest(idToken))
                if (authResponse.success && authResponse.data != null) {
                    val data = authResponse.data
                    // 다른 계정이면 온보딩/홈 진입 전에 로컬을 비운다 — 초기화가 토큰도 지우므로 저장보다 앞에 둔다
                    accountSwitchGuard.ensureLocalOwnedBy(data.user.id)
                    tokenStore.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)

                    val loaded = appStateLoader.loadAll()
                    if (loaded.isFailure) {
                        Timber.w(loaded.exceptionOrNull(), "AppState 로드 실패")
                    }

                    val hasHcPermission = healthConnectManager.hasAllPermissions()

                    // 로그인 직후 에셋·HC 동기화를 백그라운드로 시작해 앱 재시작 없이 데이터가 보이게 한다.
                    triggerPostLoginSync(hasHcPermission)

                    _uiState.value = AuthUiState.Success(route = resolveRoute(data))
                } else {
                    val errorCode = authResponse.error?.code ?: "UNKNOWN"
                    Timber.e("서버 구글 인증 실패: $errorCode")
                    showError(RuntimeException("서버 인증 실패: $errorCode"))
                }
            } catch (e: Exception) {
                Timber.e(e, "구글 인증 중 예외")
                showError(e)
            }
        }
    }

    /**
     * 로그인 성공 후 이동할 화면을 서버 사실로만 결정한다.
     *
     * 서버가 방금 만든 계정이거나 닉네임이 비어 있으면(가입 도중 이탈) 온보딩, 그 외는 홈.
     * HC 권한 유무는 보지 않는다 — 로컬 완료 표시는 재설치·다른 기기·계정 전환에서 무력하므로
     * 기존 계정은 항상 홈으로 보내고 연결은 설정 > 연동에서 유도한다(R23).
     *
     * @param data 서버 인증 응답 (`isNewUser`, `user.nickname`)
     */
    private fun resolveRoute(data: AuthData): AuthRoute =
        if (data.isNewUser || data.user.nickname.isNullOrBlank()) AuthRoute.Onboarding else AuthRoute.Home

    /**
     * 로그인 성공 직후 에셋 동기화와(권한이 있을 때) HC 동기화를 백그라운드로 실행한다.
     * 각 작업은 독립 코루틴으로 실행되어 실패해도 화면 전환을 막지 않는다.
     * 재로그인은 HC 권한이 남아 있어 이 동기화가 오늘 기록을 가장 먼저 보고한다 —
     * 여기서 지급된 조개는 [AppState.addPendingShellReward]로 홈 팝업에 넘긴다.
     * appScope를 사용하므로 화면 전환으로 ViewModel이 사라져도 동기화가 계속 진행된다.
     *
     * @param hasHcPermission HC 권한 보유 여부 — false이면 HC 동기화를 건너뜀
     */
    internal fun triggerPostLoginSync(hasHcPermission: Boolean) {
        appScope.launch {
            val result = assetManager.sync()
            if (result.isFailure) {
                Timber.w(result.exceptionOrNull(), "로그인 후 에셋 동기화 실패 (앱 계속 진행)")
            }
        }
        if (hasHcPermission) {
            appScope.launch {
                try {
                    val earned = hcSwimSyncer.sync()
                    if (earned > 0) appState.addPendingShellReward(earned)
                } catch (e: Exception) {
                    Timber.w(e, "로그인 후 HC 동기화 실패 (앱 계속 진행)")
                }
            }
        }
    }

    private fun showError(error: Throwable) {
        val message = when {
            error.message?.contains("Unable to resolve host") == true -> "네트워크 연결을 확인해주세요."
            error.message?.contains("timeout") == true -> "서버 응답이 없어요. 잠시 후 다시 시도해주세요."
            error.message?.contains("카카오") == true -> error.message ?: "카카오 로그인에 실패했어요."
            error.message?.contains("구글") == true -> error.message ?: "구글 로그인에 실패했어요."
            else -> "로그인에 실패했어요. 다시 시도해주세요."
        }
        _uiState.value = AuthUiState.Error(message)
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}

/** 로그인 성공 후 이동할 화면. Onboarding은 서버가 새 계정이라 하거나 닉네임이 없는 계정만 받는다. */
enum class AuthRoute {
    Onboarding,
    Home,
}

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data class Loading(val provider: String) : AuthUiState
    data class Success(val route: AuthRoute) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
