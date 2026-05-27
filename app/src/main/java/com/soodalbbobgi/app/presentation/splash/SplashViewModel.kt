package com.soodalbbobgi.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.RefreshRequest
import com.soodalbbobgi.app.data.remote.dto.SwimLogRequest
import com.soodalbbobgi.app.data.remote.dto.UserData
import com.soodalbbobgi.app.domain.model.GachaBox
import com.soodalbbobgi.app.domain.model.GachaBoxItem
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.model.User
import com.soodalbbobgi.app.domain.repository.GachaRepository
import com.soodalbbobgi.app.domain.repository.InventoryRepository
import com.soodalbbobgi.app.domain.repository.UserRepository
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
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
    private val gachaRepository: GachaRepository,
    private val inventoryRepository: InventoryRepository,
    private val healthConnectManager: HealthConnectManager,
    private val swimLogUseCase: SwimLogUseCase,
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination

    private val _syncError = MutableStateFlow<String?>(null)
    /** 동기화 실패 시 에러 메시지. UI에서 표시 후 null로 리셋. */
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

                    // 동기화 순서: SERVER_SPEC.md 참고
                    try {
                        // 1. 서버→로컬 (서버 원본)
                        syncGachaBoxes()
                        syncInventory()
                        // 2. 로컬→서버 (로컬 원본)
                        val hasHcPerm = healthConnectManager.hasAllPermissions()
                        if (hasHcPerm) syncHealthConnect()
                    } catch (e: Exception) {
                        Timber.w(e, "동기화 중 오류 (앱은 계속 진행)")
                        _syncError.value = "일부 데이터 동기화에 실패했어요. 나중에 다시 시도됩니다."
                    }
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
        Timber.d("saveUserToRoom: id=${data.id} shells=${data.shellBalance} pearls=${data.pearlBalance}")
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

    /**
     * 서버에서 뽑기 상자 + 아이템 목록을 받아 Room에 저장한다.
     * 뽑기/상점 화면이 이 데이터를 Room에서 읽는다.
     */
    private suspend fun syncGachaBoxes() {
        try {
            val res = soodalApi.getGachaBoxes()
            if (res.success && res.data != null) {
                for (box in res.data.boxes) {
                    gachaRepository.saveBox(GachaBox(
                        id = box.id,
                        name = box.name,
                        description = box.description,
                        category = box.category,
                    ))
                    for (item in box.items) {
                        gachaRepository.saveBoxItem(GachaBoxItem(
                            id = item.id,
                            boxId = box.id,
                            itemKey = item.itemKey,
                            name = item.name,
                            grade = Grade.fromString(item.grade),
                            weight = item.weight,
                            imageAsset = item.imageAsset,
                        ))
                    }
                }
                Timber.d("뽑기 상자 동기화 완료: ${res.data.boxes.size}개")
            }
        } catch (e: Exception) {
            Timber.w(e, "뽑기 상자 동기화 실패")
        }
    }

    /**
     * 서버에서 인벤토리를 받아 Room에 저장한다.
     */
    private suspend fun syncInventory() {
        try {
            val res = soodalApi.getInventory()
            if (res.success && res.data != null) {
                for (item in res.data.items) {
                    inventoryRepository.addItem(InventoryItem(
                        id = item.id,
                        userId = userSession.userId,
                        boxItemId = item.boxItemId,
                        grade = Grade.fromString(item.grade),
                        category = item.category,
                        isEquippedAs = item.isEquippedAs,
                        acquiredAt = item.acquiredAt,
                    ))
                }
                Timber.d("인벤토리 동기화 완료: ${res.data.items.size}개")
            }
        } catch (e: Exception) {
            Timber.w(e, "인벤토리 동기화 실패")
        }
    }

    /**
     * Health Connect에서 수영 데이터를 읽어 로컬 저장 + 서버 전송한다.
     * 앱 실행 시 1회만 호출.
     */
    private suspend fun syncHealthConnect() {
        try {
            if (!healthConnectManager.hasAllPermissions()) return

            val now = LocalDateTime.now()
            val today = now.toLocalDate()
            val zone = ZoneId.systemDefault()
            val fetchFrom = if (now.hour < 2) today.minusDays(1) else today
            val startOfDay = fetchFrom.atStartOfDay(zone).toInstant()
            val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()

            val sessions = healthConnectManager.readSwimSessions(startOfDay, endOfDay)
            Timber.d("Splash HC 동기화: ${sessions.size}개 세션")

            for (session in sessions) {
                try {
                    // 로컬 Room에 저장
                    swimLogUseCase.syncSwimLog(userSession.userId, SwimLog(
                        userId = userSession.userId,
                        date = session.date,
                        distanceMeters = session.distanceMeters,
                        durationSeconds = session.durationSeconds,
                        calories = session.calories,
                        strokeMixedM = session.distanceMeters,
                        source = "health_connect",
                    ))
                    // 서버에 전송
                    soodalApi.addSwimLog(SwimLogRequest(
                        date = session.date,
                        distanceMeters = session.distanceMeters,
                        durationSeconds = session.durationSeconds,
                        calories = session.calories,
                        strokeFreestyleM = 0, strokeBreastM = 0,
                        strokeBackM = 0, strokeFlyM = 0,
                        strokeMixedM = session.distanceMeters, strokeKickM = 0,
                        source = "health_connect",
                    ))
                } catch (e: Exception) {
                    Timber.w(e, "수영 기록 동기화 실패: ${session.date}")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "HC 동기화 실패")
        }
    }
}

enum class SplashDestination {
    Loading, Auth, Onboarding, Permission, Home
}
