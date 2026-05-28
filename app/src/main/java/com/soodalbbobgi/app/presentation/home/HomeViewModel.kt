package com.soodalbbobgi.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.health.HcSyncPreferences
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.health.SwimSession
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.SwimLogRequest
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
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
    /** 프로필 카드에 표시할 닉네임 (없으면 빈 문자열). */
    val cardNickname: String = "",
    /** 프로필 카드 하단 한 줄 소개 문구. 사용자 입력 텍스트 또는 기본값. */
    val cardTagline: String = "수영을 사랑하는 수달",
    /** 프로필 카드 좌하단 누적 통계 ("12,540m · 89회" 형식). */
    val cardStats: String = "",
    /** 배경 아이템 이미지 에셋 경로 (없으면 null). */
    val cardBgAsset: String? = null,
    /** 캐릭터 아이템 이미지 에셋 경로 (없으면 null). */
    val cardCharAsset: String? = null,
    /** 테두리 아이템 이미지 에셋 경로 (없으면 null). */
    val cardFrameAsset: String? = null,
    /** 캐릭터 가로 위치 (0..1). */
    val cardCharX: Float = 0.16f,
    /** 캐릭터 세로 위치 (0..1). */
    val cardCharY: Float = 0.06f,
    /** 캐릭터 크기 배율 (0.3..1). */
    val cardCharScale: Float = 0.70f,
)

/**
 * 홈 화면 ViewModel.
 * 사용자 프로필/잔액/인벤토리는 [AppState] 메모리에서 관찰하고,
 * 수영 통계는 swim_logs Room Flow에서 본다.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userSession: UserSession,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val swimLogUseCase: SwimLogUseCase,
    private val healthConnectManager: HealthConnectManager,
    private val soodalApi: SoodalApi,
    private val hcSyncPreferences: HcSyncPreferences,
) : ViewModel() {

    private val _shellReward = MutableStateFlow(0)
    val shellReward: StateFlow<Int> = _shellReward

    private val _syncing = MutableStateFlow(false)
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun clearSyncError() { _syncError.value = null }

    init {
        // 스플래시에서 누적된 조개 보상 팝업으로 표시
        val pending = appState.consumePendingShellReward()
        if (pending > 0) _shellReward.value = pending
    }

    val uiState: StateFlow<HomeUiState> = combine(
        appState.profile,
        appState.currency,
        appState.inventory,
        appState.items,
        appState.profileCard,
        swimLogUseCase.getLogsByDateRange(monthStart(), monthEnd()),
        _syncing,
    ) { values ->
        val profile = values[0] as com.soodalbbobgi.app.domain.model.UserProfile?
        val currency = values[1] as com.soodalbbobgi.app.domain.model.Currency
        val inventory = values[2] as List<com.soodalbbobgi.app.domain.model.InventoryItem>
        val itemsMap = values[3] as Map<Long, com.soodalbbobgi.app.domain.model.Item>
        val profileCard = values[4] as com.soodalbbobgi.app.domain.model.ProfileCard?
        val monthLogs = values[5] as List<SwimLog>
        val syncing = values[6] as Boolean

        val stats = swimLogUseCase.getMonthStats(monthStart(), monthEnd())
        val today = LocalDate.now().toString()
        // "최근 획득"은 가챠/상점으로 얻은 아이템만 노출. 신규 가입 시 자동 지급되는
        // 기본 아이템(isDefault=true)은 '획득'이 아니라 출발점이라 제외.
        val recent = inventory
            .filterNot { inv -> itemsMap[inv.itemId]?.isDefault == true }
            .take(8)
            .map { inv ->
                val meta = itemsMap[inv.itemId]
                RecentItem(name = meta?.name ?: "아이템 #${inv.itemId}", kind = inv.category, grade = inv.grade)
            }

        // 저장된 프로필 카드를 ProfileCardComposite에 전달할 형태로 매핑.
        // 장착된 아이템의 imageAsset 경로를 ItemsMap에서 조회해 카드 합성에 사용.
        val cardTaglineText = profileCard?.customText?.takeIf { it.isNotBlank() }
            ?: "수영을 사랑하는 수달"
        val cardStatsText = "${stats.totalDistanceMeters}m · ${stats.swimCount}회"

        HomeUiState(
            nickname = profile?.nickname ?: "",
            shells = currency.shellBalance,
            pearls = currency.pearlBalance,
            totalDistance = stats.totalDistanceMeters,
            swimSessions = stats.swimCount,
            totalKcal = stats.totalCalories,
            todayHasRecord = monthLogs.any { it.date == today },
            syncing = syncing,
            recentItems = recent,
            cardNickname = profile?.nickname ?: "",
            cardTagline = cardTaglineText,
            cardStats = cardStatsText,
            cardBgAsset = resolveCardAsset(profileCard?.backgroundItemId, inventory, itemsMap),
            cardCharAsset = resolveCardAsset(profileCard?.characterItemId, inventory, itemsMap),
            cardFrameAsset = resolveCardAsset(profileCard?.borderItemId, inventory, itemsMap),
            cardCharX = profileCard?.characterX ?: 0.16f,
            cardCharY = profileCard?.characterY ?: 0.06f,
            cardCharScale = profileCard?.characterScale ?: 0.70f,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /**
     * 저장된 ProfileCard의 슬롯 값으로 표시할 이미지 에셋 경로를 해결한다.
     *
     * 서버 `authService.js`가 신규 가입 시 `profile_cards` 슬롯에 인벤토리 행의 PK
     * (`inv.id`)를 저장한다. 필드 이름은 `characterItemId`지만 내용물은 사실상
     * inventory id다. 따라서 ProfileEditor와 동일한 경로(인벤토리 id → itemId → 마스터)로
     * 두 단계 조회를 거쳐야 올바른 imageAsset을 얻는다.
     *
     * @param inventoryId ProfileCard 슬롯 값 (실제로는 inventory.id)
     * @param inventory 사용자 인벤토리 목록
     * @param items 아이템 마스터 캐시
     * @return 매니페스트 상대 경로 (없거나 매칭 실패 시 null)
     */
    private fun resolveCardAsset(
        inventoryId: Long?,
        inventory: List<com.soodalbbobgi.app.domain.model.InventoryItem>,
        items: Map<Long, com.soodalbbobgi.app.domain.model.Item>,
    ): String? {
        if (inventoryId == null) return null
        val inv = inventory.firstOrNull { it.id == inventoryId } ?: return null
        return items[inv.itemId]?.imageAsset
    }

    /** 수동 동기화 — Splash와 동일한 HC sync 흐름. */
    fun onSync() {
        viewModelScope.launch {
            _shellReward.value = 0
            _syncing.value = true
            try {
                if (!healthConnectManager.hasAllPermissions()) {
                    Timber.w("Health Connect 권한이 없어 동기화를 건너뜀")
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
                pullServerSwimLogs()
                appStateLoader.refreshCurrency()
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

    private suspend fun fullReadAndInitToken(): Int {
        val token = healthConnectManager.getChangesToken()
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val zone = ZoneId.systemDefault()
        val fetchFrom = if (now.hour < 2) today.minusDays(1) else today
        val startOfDay = fetchFrom.atStartOfDay(zone).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(zone).toInstant()
        val sessions = healthConnectManager.readSwimSessions(startOfDay, endOfDay)
        val earned = processAddedSessions(sessions)
        hcSyncPreferences.saveChangesToken(token)
        return earned
    }

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
                    val earned = response.data.shellReward?.earned ?: 0
                    totalEarned += earned
                    // 서버가 지급한 조개량을 로컬 swim_log에도 즉시 반영 (캘린더 표시용)
                    if (earned > 0) {
                        swimLogUseCase.updateShellsEarned(session.date, earned)
                    }
                    response.data.shellReward?.newBalance?.let { appStateLoader.applyShellReward(it) }
                }
            } catch (e: Exception) {
                Timber.w(e, "수영 기록 전송 실패: ${session.date}")
            }
        }
        return totalEarned
    }

    private suspend fun processDeletedRecords(deletedRecordIds: List<String>) {
        for (hcRecordId in deletedRecordIds) {
            try {
                val date = swimLogUseCase.getDateByHcRecordId(hcRecordId) ?: continue
                swimLogUseCase.deleteByHcRecordId(hcRecordId)
                soodalApi.deleteSwimLog(date)
            } catch (e: Exception) {
                Timber.w(e, "수영 기록 삭제 동기화 실패: $hcRecordId")
            }
        }
    }

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
            }
        } catch (e: Exception) {
            Timber.w(e, "서버 수영 기록 pull 실패")
        }
    }

    fun clearShellReward() { _shellReward.value = 0 }

    private fun monthStart(): String = YearMonth.now().atDay(1).toString()
    private fun monthEnd(): String = YearMonth.now().atEndOfMonth().toString()
}
