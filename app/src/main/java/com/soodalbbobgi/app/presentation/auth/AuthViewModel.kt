package com.soodalbbobgi.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.di.ApplicationScope
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.asset.AssetManager
import com.soodalbbobgi.app.data.auth.GoogleAuthManager
import com.soodalbbobgi.app.data.auth.KakaoAuthManager
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.health.HcSwimSyncer
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.GoogleAuthRequest
import com.soodalbbobgi.app.data.remote.dto.KakaoAuthRequest
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
    private val appStateLoader: AppStateLoader,
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
                    tokenStore.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)

                    // 전체 서버 상태 로드 (profile/currency/inventory/gachaBoxes/profileCard)
                    val loaded = appStateLoader.loadAll()
                    if (loaded.isFailure) {
                        Timber.w(loaded.exceptionOrNull(), "AppState 로드 실패")
                    }

                    val needsNickname = data.isNewUser || data.user.nickname.isNullOrBlank()
                    val hasHcPermission = healthConnectManager.hasAllPermissions()

                    // 로그인 직후 에셋·HC 동기화를 백그라운드로 시작해 앱 재시작 없이 데이터가 보이게 한다.
                    triggerPostLoginSync(hasHcPermission)

                    _uiState.value = when {
                        needsNickname -> AuthUiState.Success(route = AuthRoute.Onboarding)
                        !hasHcPermission -> AuthUiState.Success(route = AuthRoute.Permission)
                        else -> AuthUiState.Success(route = AuthRoute.Home)
                    }
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
                    tokenStore.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)

                    val loaded = appStateLoader.loadAll()
                    if (loaded.isFailure) {
                        Timber.w(loaded.exceptionOrNull(), "AppState 로드 실패")
                    }

                    val needsNickname = data.isNewUser || data.user.nickname.isNullOrBlank()
                    val hasHcPermission = healthConnectManager.hasAllPermissions()

                    // 로그인 직후 에셋·HC 동기화를 백그라운드로 시작해 앱 재시작 없이 데이터가 보이게 한다.
                    triggerPostLoginSync(hasHcPermission)

                    _uiState.value = when {
                        needsNickname -> AuthUiState.Success(route = AuthRoute.Onboarding)
                        !hasHcPermission -> AuthUiState.Success(route = AuthRoute.Permission)
                        else -> AuthUiState.Success(route = AuthRoute.Home)
                    }
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
     * 로그인 성공 직후 에셋 동기화와(권한이 있을 때) HC 동기화를 백그라운드로 실행한다.
     * 각 작업은 독립 코루틴으로 실행되어 실패해도 화면 전환을 막지 않는다.
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
                    hcSwimSyncer.sync()
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

/** 로그인 성공 후 이동할 화면 */
enum class AuthRoute {
    Onboarding,
    Permission,
    Home,
}

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data class Loading(val provider: String) : AuthUiState
    data class Success(val route: AuthRoute) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
