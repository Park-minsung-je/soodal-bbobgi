package com.soodalbbobgi.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.RefreshRequest
import com.soodalbbobgi.app.data.remote.dto.UserData
import com.soodalbbobgi.app.domain.model.User
import com.soodalbbobgi.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 스플래시 화면에서 자동 로그인을 시도하고 이동할 화면을 결정한다.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val soodalApi: SoodalApi,
    private val userSession: UserSession,
    private val userRepository: UserRepository,
    private val healthConnectManager: HealthConnectManager,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

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
                // 토큰 만료 시 refresh
                if (tokenStore.isAccessTokenExpired()) {
                    val refreshToken = tokenStore.getRefreshToken()
                    if (refreshToken != null) {
                        val res = soodalApi.refreshToken(RefreshRequest(refreshToken))
                        if (res.success && res.data != null) {
                            tokenStore.saveTokens(res.data.accessToken, res.data.refreshToken, res.data.expiresIn)
                        } else {
                            tokenStore.clearTokens()
                            _destination.value = SplashDestination.Auth
                            return@launch
                        }
                    } else {
                        tokenStore.clearTokens()
                        _destination.value = SplashDestination.Auth
                        return@launch
                    }
                }

                // 서버에서 사용자 정보 확인 → Room에 저장
                val userRes = soodalApi.getMe()
                if (userRes.success && userRes.data != null) {
                    val user = userRes.data
                    userSession.setAuthenticatedUser(user.id)
                    saveUserToRoom(user)

                    val hasHcPermission = healthConnectManager.hasAllPermissions()
                    _destination.value = when {
                        user.nickname == null -> SplashDestination.Onboarding
                        !hasHcPermission -> SplashDestination.Permission
                        else -> SplashDestination.Home
                    }
                } else {
                    tokenStore.clearTokens()
                    _destination.value = SplashDestination.Auth
                }
            } catch (e: Exception) {
                Timber.w(e, "자동 로그인 실패")
                tokenStore.clearTokens()
                _destination.value = SplashDestination.Auth
            }
        }
    }

    /** 서버에서 받은 사용자 정보를 Room DB에 저장한다. */
    private suspend fun saveUserToRoom(data: UserData) {
        userRepository.createUser(User(
            id = data.id,
            nickname = data.nickname ?: "",
            shellBalance = data.shellBalance,
            pearlBalance = data.pearlBalance,
            pityCounter = data.pityCounter,
            lastShellGrantDate = null,
            authProvider = data.authProvider,
        ))
    }
}

enum class SplashDestination {
    Loading, Auth, Onboarding, Permission, Home
}
