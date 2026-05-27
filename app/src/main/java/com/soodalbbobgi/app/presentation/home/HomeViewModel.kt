package com.soodalbbobgi.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.health.HcSyncPreferences
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.health.SwimSession
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.SwimLogRequest
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.repository.InventoryRepository
import com.soodalbbobgi.app.domain.repository.UserRepository
import com.soodalbbobgi.app.domain.usecase.CurrencyUseCase
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.YearMonth
import javax.inject.Inject

data class RecentItem(val name: String, val kind: String, val grade: Grade)

data class HomeUiState(
    val nickname: String = "",
    val shells: Int = 0,
    val pearls: Int = 0,
    val totalDistance: Int = 0,
    val swimSessions: Int = 0,
    val totalKcal: Int = 0,
    val todayHasRecord: Boolean = false,
    val syncing: Boolean = false,
    val recentItems: List<RecentItem> = emptyList(),
)

/**
 * 홈 화면 ViewModel.
 * User, SwimLog, Inventory 데이터를 Room에서 관찰하여 UI 상태를 구성한다.
 * Health Connect에서 수영 데이터를 읽어와 SwimLog로 저장하고 조개를 지급한다.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userSession: UserSession,
    private val userRepository: UserRepository,
    private val swimLogUseCase: SwimLogUseCase,
    private val inventoryRepository: InventoryRepository,
    private val currencyUseCase: CurrencyUseCase,
    private val healthConnectManager: HealthConnectManager,
    private val soodalApi: SoodalApi,
    private val hcSyncPreferences: HcSyncPreferences,
) : ViewModel() {

    private val _shellReward = MutableStateFlow(0)
    /** 동기화 후 획득한 조개 수. UI에서 팝업 표시 후 0으로 리셋. */
    val shellReward: StateFlow<Int> = _shellReward

    private val _syncing = MutableStateFlow(false)

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun clearSyncError() { _syncError.value = null }

    // HC 동기화는 사용자가 동기화 버튼을 누를 때만 실행
    // 서버 사용자 데이터는 Splash에서 Room에 이미 저장됨

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.getUser(userSession.userId).filterNotNull(),
        inventoryRepository.getAll(userSession.userId),
        swimLogUseCase.getLogsByDateRange(monthStart(), monthEnd()),
        _syncing,
    ) { user, inventory, monthLogs, syncing ->
        val stats = swimLogUseCase.getMonthStats(monthStart(), monthEnd())
        val today = LocalDate.now().toString()

        HomeUiState(
            nickname = user.nickname,
            shells = user.shellBalance,
            pearls = user.pearlBalance,
            totalDistance = stats.totalDistanceMeters,
            swimSessions = stats.swimCount,
            totalKcal = stats.totalCalories,
            todayHasRecord = monthLogs.any { it.date == today },
            syncing = syncing,
            recentItems = inventory.take(8).map { it.toRecentItem() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /**
     * HC 변경 토큰 기반으로 수영 데이터를 동기화한다.
     * 추가/삭제 감지 → 로컬 Room + 서버 동기화 → 서버에서 기록 pull.
     */
    fun onSync() {
        viewModelScope.launch {
            _shellReward.value = 0
            _syncing.value = true

            try {
                if (!healthConnectManager.hasAllPermissions()) {
                    Timber.w("Health Connect 권한이 없어 동기화를 건너뜀")
                    _syncing.value = false
                    return@launch
                }

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

                // 서버에서 수영 기록 pull + 사용자 정보 갱신
                pullServerSwimLogs()
                refreshUserData()

                _shellReward.value = totalEarned
            } catch (e: Exception) {
                Timber.e(e, "Health Connect 동기화 실패")
                _syncError.value = when {
                    e.message?.contains("timeout") == true -> "서버 응답이 없어요. 네트워크를 확인해주세요."
                    e.message?.contains("Unable to resolve") == true -> "인터넷 연결을 확인해주세요."
                    else -> "동기화에 실패했어요. 다시 시도해주세요."
                }
            } finally {
                _syncing.value = false
            }
        }
    }

    /** 토큰 없을 때: 전체 읽기 + 초기 토큰 발급. @return 획득한 조개 수. */
    private suspend fun fullReadAndInitToken(): Int {
        val token = healthConnectManager.getChangesToken()

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val zone = ZoneId.systemDefault()
        val fetchFrom = if (now.hour < 2) today.minusDays(1) else today
        val startOfDay = fetchFrom.atStartOfDay(zone).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()

        val sessions = healthConnectManager.readSwimSessions(startOfDay, endOfDay)
        Timber.d("Home HC 전체 읽기: ${sessions.size}개 세션")
        val earned = processAddedSessions(sessions)
        hcSyncPreferences.saveChangesToken(token)
        return earned
    }

    /** 추가된 세션을 로컬+서버에 저장한다. @return 획득한 조개 수. */
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
                    totalEarned += response.data.shellReward?.earned ?: 0
                }
            } catch (e: Exception) {
                Timber.w(e, "수영 기록 전송 실패: ${session.date}")
            }
        }
        return totalEarned
    }

    /** HC에서 삭제된 레코드를 로컬+서버에서 삭제한다. */
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

    /** 서버에서 최신 사용자 정보를 가져와 로컬 갱신한다. */
    private suspend fun refreshUserData() {
        try {
            val userResponse = soodalApi.getMe()
            if (userResponse.success && userResponse.data != null) {
                val u = userResponse.data
                userRepository.updateCurrency(userSession.userId, u.shellBalance, u.pearlBalance)
            }
        } catch (e: Exception) {
            Timber.w(e, "사용자 정보 갱신 실패")
        }
    }

    /** 조개 획득 팝업을 닫은 뒤 보상 값을 리셋한다. */
    fun clearShellReward() {
        _shellReward.value = 0
    }

    private fun monthStart(): String {
        val ym = YearMonth.now()
        return ym.atDay(1).toString()
    }

    private fun monthEnd(): String {
        val ym = YearMonth.now()
        return ym.atEndOfMonth().toString()
    }
}

private fun InventoryItem.toRecentItem(): RecentItem = RecentItem(
    name = "아이템 #$boxItemId",
    kind = category,
    grade = grade,
)
