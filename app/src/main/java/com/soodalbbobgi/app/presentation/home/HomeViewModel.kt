package com.soodalbbobgi.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
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
import java.time.LocalDate
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
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userSession: UserSession,
    private val userRepository: UserRepository,
    private val swimLogUseCase: SwimLogUseCase,
    private val inventoryRepository: InventoryRepository,
    private val currencyUseCase: CurrencyUseCase,
) : ViewModel() {

    private val _shellReward = MutableStateFlow(0)
    /** 동기화 후 획득한 조개 수. UI에서 팝업 표시 후 0으로 리셋. */
    val shellReward: StateFlow<Int> = _shellReward

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.getUser(userSession.userId).filterNotNull(),
        inventoryRepository.getAll(userSession.userId),
        swimLogUseCase.getLogsByDateRange(monthStart(), monthEnd()),
    ) { user, inventory, monthLogs ->
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
            recentItems = inventory.take(8).map { it.toRecentItem() },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /**
     * Health Connect 동기화 후 일일 조개를 지급한다.
     */
    fun onSync() {
        viewModelScope.launch {
            _shellReward.value = 0
            val earned = currencyUseCase.grantDailyShells(
                userSession.userId,
                LocalDate.now().toString(),
            )
            _shellReward.value = earned
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
