package com.soodalbbobgi.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.health.HealthConnectManager
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
) : ViewModel() {

    private val _shellReward = MutableStateFlow(0)
    /** 동기화 후 획득한 조개 수. UI에서 팝업 표시 후 0으로 리셋. */
    val shellReward: StateFlow<Int> = _shellReward

    private val _syncing = MutableStateFlow(false)

    init {
        // Home 진입 시 자동으로 HC 동기화 실행
        onSync()
    }

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
     * Health Connect에서 오늘의 수영 세션을 읽어와 저장하고 조개를 지급한다.
     *
     * 1. Health Connect에서 오늘 날짜의 수영 세션을 읽어온다
     * 2. 각 세션을 SwimLog로 변환하여 SwimLogUseCase를 통해 저장한다
     * 3. SwimLogUseCase 내부에서 중복 확인 후 조개를 지급한다
     * 4. 획득한 조개 수를 [shellReward]에 반영한다
     */
    /**
     * Health Connect에서 오늘의 수영 세션을 읽어 서버에 전송한다.
     * 서버가 조개 지급을 판정하고, 결과를 로컬 Room에도 반영한다.
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

                // 새벽 2시 이전이면 전날 기록도 포함 (조개 지급 마감이 익일 2시)
                val now = java.time.LocalDateTime.now()
                val today = now.toLocalDate()
                val zone = ZoneId.systemDefault()
                val fetchFrom = if (now.hour < 2) today.minusDays(1) else today
                val startOfDay = fetchFrom.atStartOfDay(zone).toInstant()
                val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()

                val sessions = healthConnectManager.readSwimSessions(startOfDay, endOfDay)
                Timber.d("Health Connect에서 수영 세션 ${sessions.size}개 읽어옴")

                var totalEarned = 0
                for (session in sessions) {
                    try {
                        // 로컬 Room에 먼저 저장
                        val swimLog = SwimLog(
                            userId = userSession.userId,
                            date = session.date,
                            distanceMeters = session.distanceMeters,
                            durationSeconds = session.durationSeconds,
                            calories = session.calories,
                            source = "health_connect",
                        )
                        swimLogUseCase.syncSwimLog(userSession.userId, swimLog)

                        // 서버에 수영 기록 전송 → 서버가 조개 지급 판정
                        val response = soodalApi.addSwimLog(SwimLogRequest(
                            date = session.date,
                            distanceMeters = session.distanceMeters,
                            durationSeconds = session.durationSeconds,
                            calories = session.calories,
                            strokeFreestyleM = 0,
                            strokeBreastM = 0,
                            strokeBackM = 0,
                            strokeFlyM = 0,
                            strokeMixedM = session.distanceMeters,
                            strokeKickM = 0,
                            source = "health_connect",
                        ))
                        if (response.success && response.data != null) {
                            totalEarned += response.data.shellReward?.earned ?: 0
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "수영 기록 전송 실패: ${session.date}")
                    }
                }

                // 서버에서 최신 사용자 정보 가져와서 로컬 갱신
                try {
                    val userResponse = soodalApi.getMe()
                    if (userResponse.success && userResponse.data != null) {
                        val u = userResponse.data
                        userRepository.updateCurrency(userSession.userId, u.shellBalance, u.pearlBalance)
                    }
                } catch (e: Exception) {
                    Timber.w(e, "사용자 정보 갱신 실패")
                }

                _shellReward.value = totalEarned
            } catch (e: Exception) {
                Timber.e(e, "Health Connect 동기화 실패")
            } finally {
                _syncing.value = false
            }
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
