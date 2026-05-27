package com.soodalbbobgi.app.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.health.HcSyncPreferences
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.health.SwimSession
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
    private val hcSyncPreferences: HcSyncPreferences,
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
     * Health Connect 변경 토큰 기반으로 수영 데이터를 동기화한다.
     *
     * 1. 저장된 토큰이 있으면 Changes API로 변경분만 처리
     * 2. 토큰이 없거나 만료되면 전체 읽기 후 토큰 발급
     * 3. 추가된 세션 → 로컬 Room + 서버 POST
     * 4. 삭제된 세션 → 로컬 Room 삭제 + 서버 DELETE
     * 5. 서버에서 수영 기록 pull → 로컬에 없는 것만 저장 (다른 기기 대응)
     */
    private suspend fun syncHealthConnect() {
        try {
            if (!healthConnectManager.hasAllPermissions()) return

            val storedToken = hcSyncPreferences.getChangesToken()

            if (storedToken != null) {
                val result = healthConnectManager.getChanges(storedToken)
                if (result != null) {
                    processAddedSessions(result.addedSessions)
                    processDeletedRecords(result.deletedRecordIds)
                    hcSyncPreferences.saveChangesToken(result.nextToken)
                } else {
                    fullReadAndInitToken()
                }
            } else {
                fullReadAndInitToken()
            }

            // 서버에서 수영 기록 pull (다른 기기에서 등록한 기록 반영)
            pullServerSwimLogs()
        } catch (e: Exception) {
            Timber.w(e, "HC 동기화 실패")
        }
    }

    /** 토큰 없을 때: 전체 읽기 + 초기 토큰 발급 */
    private suspend fun fullReadAndInitToken() {
        val token = healthConnectManager.getChangesToken()

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val zone = ZoneId.systemDefault()
        val fetchFrom = if (now.hour < 2) today.minusDays(1) else today
        val startOfDay = fetchFrom.atStartOfDay(zone).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()

        val sessions = healthConnectManager.readSwimSessions(startOfDay, endOfDay)
        Timber.d("Splash HC 전체 읽기: ${sessions.size}개 세션")
        processAddedSessions(sessions)

        hcSyncPreferences.saveChangesToken(token)
    }

    /** 추가/수정된 수영 세션을 로컬 Room + 서버에 저장한다. */
    private suspend fun processAddedSessions(sessions: List<SwimSession>) {
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
    }

    /** HC에서 삭제된 레코드를 로컬 Room + 서버에서 삭제한다. */
    private suspend fun processDeletedRecords(deletedRecordIds: List<String>) {
        for (hcRecordId in deletedRecordIds) {
            try {
                val date = swimLogUseCase.getDateByHcRecordId(hcRecordId) ?: continue
                swimLogUseCase.deleteByHcRecordId(hcRecordId)
                soodalApi.deleteSwimLog(date)
                Timber.d("수영 기록 삭제 완료: $date (HC: $hcRecordId)")
            } catch (e: Exception) {
                Timber.w(e, "수영 기록 삭제 동기화 실패: $hcRecordId")
            }
        }
    }

    /** 서버에서 수영 기록을 가져와 로컬에 없는 것만 저장한다. */
    private suspend fun pullServerSwimLogs() {
        try {
            val today = LocalDate.now()
            val startDate = today.minusDays(30).toString()
            val endDate = today.toString()

            val response = soodalApi.getSwimLogs(startDate, endDate)
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
