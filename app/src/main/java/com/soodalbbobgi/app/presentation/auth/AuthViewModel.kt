package com.soodalbbobgi.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.BuildConfig
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.auth.KakaoAuthManager
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.KakaoAuthRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 로그인 화면의 상태와 OAuth 인증 로직을 관리한다.
 * Debug 빌드에서는 서버 연결 실패 시 로컬 전용 모드로 자동 폴백한다.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val kakaoAuthManager: KakaoAuthManager,
    private val soodalApi: SoodalApi,
    private val tokenStore: TokenStore,
    private val userSession: UserSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /**
     * 카카오 로그인을 시작한다.
     * 1) 카카오 SDK로 카카오 액세스 토큰 획득
     * 2) 서버 /auth/kakao 호출 → JWT 발급
     * 3) 토큰 저장 후 신규/기존 사용자 구분하여 완료 상태 전환
     */
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
                    handleAuthFallback(error, "kakao")
                    return@launch
                }

                // 2단계: 서버에 카카오 토큰 전달 → JWT 발급
                val authResponse = soodalApi.authKakao(KakaoAuthRequest(kakaoToken))

                if (authResponse.success && authResponse.data != null) {
                    val data = authResponse.data
                    // 3단계: JWT 토큰 저장 및 세션 설정
                    tokenStore.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)
                    userSession.setAuthenticatedUser(data.user.id)
                    _uiState.value = AuthUiState.Success(isNewUser = data.isNewUser)
                } else {
                    val errorCode = authResponse.error?.code ?: "UNKNOWN"
                    Timber.e("서버 카카오 인증 실패: $errorCode")
                    handleAuthFallback(RuntimeException("서버 인증 실패: $errorCode"), "kakao")
                }
            } catch (e: Exception) {
                Timber.e(e, "카카오 인증 중 예외")
                handleAuthFallback(e, "kakao")
            }
        }
    }

    /**
     * 서버 연결 실패 시의 폴백 처리.
     * Debug 빌드에서는 로컬 전용 모드(기존 사용자로 간주)로 자동 진행한다.
     * Release 빌드에서는 에러 상태로 전환하여 사용자에게 알린다.
     *
     * @param error 발생한 예외
     * @param provider 로그인 시도한 OAuth 제공자
     */
    private fun handleAuthFallback(error: Throwable, provider: String) {
        Timber.e(error, "인증 실패 ($provider)")
        val message = when {
            error.message?.contains("Unable to resolve host") == true -> "네트워크 연결을 확인해주세요."
            error.message?.contains("timeout") == true -> "서버 응답이 없어요. 잠시 후 다시 시도해주세요."
            error.message?.contains("카카오") == true -> error.message ?: "카카오 로그인에 실패했어요."
            else -> "로그인에 실패했어요. 다시 시도해주세요."
        }
        _uiState.value = AuthUiState.Error(message)
    }

    /** 에러 상태를 초기화하여 재시도를 허용한다. */
    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }
}

/**
 * 로그인 화면의 UI 상태.
 */
sealed interface AuthUiState {
    /** 초기 대기 상태 */
    data object Idle : AuthUiState

    /**
     * 로그인 진행 중
     * @param provider 현재 진행 중인 OAuth 제공자 ("kakao" | "google")
     */
    data class Loading(val provider: String) : AuthUiState

    /**
     * 로그인 성공
     * @param isNewUser true이면 신규 사용자 → 온보딩으로 이동, false이면 기존 사용자 → 홈으로 이동
     */
    data class Success(val isNewUser: Boolean) : AuthUiState

    /**
     * 로그인 실패
     * @param message 사용자에게 표시할 에러 메시지
     */
    data class Error(val message: String) : AuthUiState
}
