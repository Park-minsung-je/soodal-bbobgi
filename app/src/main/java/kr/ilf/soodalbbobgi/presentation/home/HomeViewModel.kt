package kr.ilf.soodalbbobgi.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ilf.soodalbbobgi.core.ui.ShellRewardKind
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.auth.AccountPrefs
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.core.util.averageHr
import kr.ilf.soodalbbobgi.core.util.decodeHrSeries
import kr.ilf.soodalbbobgi.domain.model.SwimLog
import kr.ilf.soodalbbobgi.domain.usecase.SwimLogUseCase
import kr.ilf.soodalbbobgi.presentation.calendar.SwimSessionData
import kr.ilf.soodalbbobgi.presentation.calendar.toHomeSessionData
import kr.ilf.soodalbbobgi.presentation.common.WeeklyActivity
import kr.ilf.soodalbbobgi.presentation.common.buildWeeklyActivity
import kr.ilf.soodalbbobgi.presentation.common.swimStreak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class HomeUiState(
    val nickname: String = "",
    val shells: Int = 0,
    val pearls: Int = 0,
    val totalDistance: Int = 0,
    val swimSessions: Int = 0,
    val totalKcal: Int = 0,
    val todayHasRecord: Boolean = false,
    /** 오늘 기록 요약 (없으면 0). */
    val todayDistanceM: Int = 0,
    val todayDurationMin: Int = 0,
    val todayKcal: Int = 0,
    /** 오늘 심박 (HC 기록 있을 때). */
    val todayMaxHr: Int? = null,
    val todayMinHr: Int? = null,
    val todayAvgHr: Int? = null,
    /** 지난달 '같은 기간'(1일~오늘 일자) 통계 — 진행 중인 이번 달과 페이스 비교용. */
    val lastMonthDistance: Int = 0,
    val lastMonthSessions: Int = 0,
    val lastMonthKcal: Int = 0,
    /** 이번 달 주력 영법 (가장 많이 한 영법; 없으면 null). */
    val topStroke: String? = null,
    /** 오늘 세션 목록 (영법 수정용; 시작 시각 순). */
    val todaySessions: List<SwimSessionData> = emptyList(),
    /** 연속 수영 일수 (오늘 미기록 시 어제까지 기준). */
    val streak: Int = 0,
    /** 도감 컬렉션 — 카테고리별 보유/전체 수 (bg=배경, char=캐릭터, frame=테두리). */
    val dexCharOwned: Int = 0, val dexCharTotal: Int = 0,
    val dexBgOwned: Int = 0, val dexBgTotal: Int = 0,
    val dexFrameOwned: Int = 0, val dexFrameTotal: Int = 0,
    /** 최근 7일 활동 + 지난주 대비 추세. */
    val weekly: WeeklyActivity = WeeklyActivity(),
    val syncing: Boolean = false,
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
    /** 캐릭터 중심 가로 위치 (0..1). */
    val cardCharX: Float = 0.5f,
    /** 캐릭터 중심 세로 위치 (0..1). */
    val cardCharY: Float = 0.5f,
    /** 캐릭터 크기 배율 (0.3..1). */
    val cardCharScale: Float = 1.0f,
    /**
     * 저장된 프로필 카드 원본 — 텍스트 요소별 배치/스타일이 많아 개별 필드 대신 통째로 노출한다.
     * null이면 카드 데이터 미로드(기본값 렌더).
     */
    val card: kr.ilf.soodalbbobgi.domain.model.ProfileCard? = null,
)

/**
 * 기존 회원 설정 안내 팝업(R30)에 담을 항목 — 재설치·재로그인으로 꺼져 있을 수 있는 것들.
 *
 * @param hcMissing Health Connect 필수 권한이 전부 허용돼 있지 않음
 * @param newRecordOff 수영 기록 알림이 꺼져 있음 (리마인더는 개인 선택이라 보지 않는다)
 */
data class SetupNudge(val hcMissing: Boolean, val newRecordOff: Boolean)

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
    private val hcSwimSyncer: HcSwimSyncer,
    private val notificationPrefs: NotificationPrefs,
    private val accountPrefs: AccountPrefs,
) : ViewModel() {

    private val _shellReward = MutableStateFlow(0)
    val shellReward: StateFlow<Int> = _shellReward

    /** 조개를 받은 계기 — 팝업 문구를 가른다. [_shellReward]와 항상 같이 바뀐다. */
    private val _shellRewardKind = MutableStateFlow(ShellRewardKind.SwimRecord)
    val shellRewardKind: StateFlow<ShellRewardKind> = _shellRewardKind

    private val _syncing = MutableStateFlow(false)
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError

    fun clearSyncError() { _syncError.value = null }

    /** 최초 HC 가져오기 진행 여부 — 온보딩이 시작한 동기화를 홈이 표시한다. */
    val hcSyncing: StateFlow<Boolean> = appState.hcSyncing

    private val _setupNudge = MutableStateFlow<SetupNudge?>(null)
    /**
     * 기존 회원 설정 안내 팝업(R30) — null이면 띄울 것 없음.
     * 홈 진입마다 한 번 판단하며, 화면은 조개 팝업이 닫힌 뒤에 그린다.
     */
    val setupNudge: StateFlow<SetupNudge?> = _setupNudge

    init {
        viewModelScope.launch { evaluateSetupNudge() }

        // 대기 중인 조개 보상을 팝업으로 — 스플래시 누적분과, 온보딩이 시작해 홈 진입
        // 후에야 끝나는 최초 동기화 지급분을 모두 받도록 일회성 소비가 아니라 구독한다.
        // 최초 동기화 스크림이 도는 동안은 소비하지 않고 기다렸다가, 스크림이 걷힌 뒤 팝업을 올린다
        // (로그인 직후 동기화가 먼저 지급해도 스크림 위로 팝업이 겹치지 않게).
        viewModelScope.launch {
            combine(appState.pendingShellReward, appState.hcSyncing) { pending, syncing ->
                if (syncing) 0 else pending
            }.collect { pending ->
                if (pending > 0) {
                    val earned = appState.consumePendingShellReward()
                    if (earned > 0) {
                        _shellRewardKind.value = ShellRewardKind.SwimRecord
                        _shellReward.value = earned
                    }
                }
            }
        }

        // 프로세스 사망 후 Splash를 거치지 않고 복원된 경우 메모리 상태를 재수화한다.
        viewModelScope.launch { appStateLoader.ensureHydrated() }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        appState.profile,
        appState.currency,
        appState.inventory,
        appState.items,
        appState.profileCard,
        // 최근 60일: 오늘 기록 + 주간 활동(14일) + 스트릭 계산을 모두 커버한다.
        swimLogUseCase.getLogsByDateRange(LocalDate.now().minusDays(59).toString(), LocalDate.now().toString()),
        _syncing,
    ) { values ->
        val profile = values[0] as kr.ilf.soodalbbobgi.domain.model.UserProfile?
        val currency = values[1] as kr.ilf.soodalbbobgi.domain.model.Currency
        val inventory = values[2] as List<kr.ilf.soodalbbobgi.domain.model.InventoryItem>
        val itemsMap = values[3] as Map<Long, kr.ilf.soodalbbobgi.domain.model.Item>
        val profileCard = values[4] as kr.ilf.soodalbbobgi.domain.model.ProfileCard?
        val recentLogs = values[5] as List<SwimLog>
        val syncing = values[6] as Boolean

        val stats = swimLogUseCase.getMonthStats(monthStart(), monthEnd())
        val todayDate = LocalDate.now()
        // 하루 여러 세션 가능 — 오늘 요약은 세션 합계로 표시
        val todayLogs = recentLogs.filter { it.date == todayDate.toString() }
        // 이번 달은 진행 중이므로 지난달 전체가 아니라 '같은 기간'(1일~오늘 일자)의
        // 페이스와 비교한다 — 월 중반에 항상 뒤처져 보이는 왜곡 방지.
        val lastMonth = swimLogUseCase.getMonthStats(
            lastMonthStart(),
            lastMonthSamePeriodEnd(todayDate).toString(),
        )
        val monthLogs = recentLogs.filter { it.date >= monthStart() && it.date <= monthEnd() }
        val topStroke = listOf(
            "자유형" to monthLogs.sumOf { it.strokeFreestyleM },
            "평영" to monthLogs.sumOf { it.strokeBreastM },
            "배영" to monthLogs.sumOf { it.strokeBackM },
            "접영" to monthLogs.sumOf { it.strokeFlyM },
        ).filter { it.second > 0 }.maxByOrNull { it.second }?.first
        val todayMax = todayLogs.mapNotNull { it.maxHr }.maxOrNull()
        val todayMin = todayLogs.mapNotNull { it.minHr }.minOrNull()
        // 시계열이 없으면(수동 입력) 저장된 평균 심박으로 폴백한다.
        val todayAvg = averageHr(todayLogs.flatMap { decodeHrSeries(it.hrSeries) })
            ?: todayLogs.mapNotNull { it.avgHr }.takeIf { it.isNotEmpty() }?.average()?.toInt()
        val todaySessionData = todayLogs
            .sortedWith(compareBy(nullsLast()) { it.startEpochSec })
            .map { it.toHomeSessionData() }
        val weekly = buildWeeklyActivity(
            recentLogs.filter { it.date >= todayDate.minusDays(13).toString() },
            todayDate,
        )
        val streak = swimStreak(recentLogs.map { LocalDate.parse(it.date) }.toSet(), todayDate)

        // 저장된 프로필 카드를 ProfileCardComposite에 전달할 형태로 매핑.
        // 장착된 아이템의 imageAsset 경로를 ItemsMap에서 조회해 카드 합성에 사용.
        val cardTaglineText = profileCard?.customText?.takeIf { it.isNotBlank() }
            ?: "수영을 사랑하는 수달"
        val cardStatsText = "${stats.totalDistanceMeters}m · ${stats.swimCount}회"

        // 도감: 카테고리별 보유(중복 제거) / 전체 카탈로그 수.
        val ownedByCat = inventory.groupBy { it.category }.mapValues { e -> e.value.map { it.itemId }.toSet().size }
        val totalByCat = itemsMap.values.groupBy { it.category }.mapValues { it.value.size }

        HomeUiState(
            nickname = profile?.nickname ?: "",
            shells = currency.shellBalance,
            pearls = currency.pearlBalance,
            totalDistance = stats.totalDistanceMeters,
            swimSessions = stats.swimCount,
            totalKcal = stats.totalCalories,
            todayHasRecord = todayLogs.isNotEmpty(),
            todayDistanceM = todayLogs.sumOf { it.distanceMeters },
            todayDurationMin = todayLogs.sumOf { it.durationSeconds } / 60,
            todayKcal = todayLogs.sumOf { it.calories },
            todayMaxHr = todayMax,
            todayMinHr = todayMin,
            todayAvgHr = todayAvg,
            lastMonthDistance = lastMonth.totalDistanceMeters,
            lastMonthSessions = lastMonth.swimCount,
            lastMonthKcal = lastMonth.totalCalories,
            topStroke = topStroke,
            todaySessions = todaySessionData,
            streak = streak,
            dexCharOwned = ownedByCat["char"] ?: 0, dexCharTotal = totalByCat["char"] ?: 0,
            dexBgOwned = ownedByCat["bg"] ?: 0, dexBgTotal = totalByCat["bg"] ?: 0,
            dexFrameOwned = ownedByCat["frame"] ?: 0, dexFrameTotal = totalByCat["frame"] ?: 0,
            weekly = weekly,
            syncing = syncing,
            cardNickname = profile?.nickname ?: "",
            cardTagline = cardTaglineText,
            cardStats = cardStatsText,
            cardBgAsset = resolveCardAsset(profileCard?.backgroundItemId, inventory, itemsMap),
            cardCharAsset = resolveCardAsset(profileCard?.characterItemId, inventory, itemsMap),
            cardFrameAsset = resolveCardAsset(profileCard?.borderItemId, inventory, itemsMap),
            cardCharX = profileCard?.characterX ?: 0.5f,
            cardCharY = profileCard?.characterY ?: 0.5f,
            cardCharScale = profileCard?.characterScale ?: 1.0f,
            card = profileCard,
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
        inventory: List<kr.ilf.soodalbbobgi.domain.model.InventoryItem>,
        items: Map<Long, kr.ilf.soodalbbobgi.domain.model.Item>,
    ): String? {
        if (inventoryId == null) return null
        val inv = inventory.firstOrNull { it.id == inventoryId } ?: return null
        return items[inv.itemId]?.imageAsset
    }

    /** 수동 동기화 — Splash와 공유하는 [HcSwimSyncer] 흐름. */
    fun onSync() {
        viewModelScope.launch {
            _shellReward.value = 0
            _syncing.value = true
            // 동기화가 순식간에 끝나도 로딩 표시는 최소 1초 유지 — 깜빡임 방지.
            val startedAt = System.currentTimeMillis()
            try {
                if (!healthConnectManager.hasAllPermissions()) {
                    Timber.w("Health Connect 권한이 없어 동기화를 건너뜀")
                    return@launch
                }
                val totalEarned = hcSwimSyncer.sync()
                appStateLoader.refreshCurrency()
                _shellRewardKind.value = ShellRewardKind.SwimRecord
                _shellReward.value = totalEarned
            } catch (e: Exception) {
                Timber.e(e, "Health Connect 동기화 실패")
                _syncError.value = when {
                    e.message?.contains("timeout") == true -> "서버 응답이 없어요. 네트워크를 확인해주세요."
                    e.message?.contains("Unable to resolve") == true -> "인터넷 연결을 확인해주세요."
                    else -> "동기화에 실패했어요. 다시 시도해주세요."
                }
            } finally {
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < MIN_SYNC_INDICATOR_MS) delay(MIN_SYNC_INDICATOR_MS - elapsed)
                _syncing.value = false
            }
        }
    }

    fun clearShellReward() { _shellReward.value = 0 }

    /**
     * 홈 진입 시 HC 연결·수영 기록 알림 상태를 보고 설정 안내 팝업(R30)을 띄울지 정한다.
     *
     * 온보딩 직후 진입은 억제 플래그를 소비하며 한 번 건너뛰고, "다시 보지 않음"을
     * 저장한 기기에서는 판단 자체를 하지 않는다. 플래그는 조건과 무관하게 먼저 소비해
     * 다음 진입에 남지 않게 한다.
     */
    private suspend fun evaluateSetupNudge() {
        val suppressed = appState.consumeSuppressSetupNudgeOnce()
        if (suppressed || accountPrefs.setupNudgeDismissed) return
        val hcMissing = !healthConnectManager.hasAllPermissions()
        val newRecordOff = !notificationPrefs.newRecordEnabled
        if (hcMissing || newRecordOff) {
            _setupNudge.value = SetupNudge(hcMissing = hcMissing, newRecordOff = newRecordOff)
        }
    }

    /**
     * 설정 안내 팝업을 닫는다 — "나중에"·"설정으로" 어느 쪽이든 체크 상태를 저장한다.
     *
     * @param dontShowAgain "다시 보지 않음" 체크 여부. true면 기기에 저장해 이후 진입에서 띄우지 않는다
     */
    fun dismissSetupNudge(dontShowAgain: Boolean) {
        if (dontShowAgain) accountPrefs.setupNudgeDismissed = true
        _setupNudge.value = null
    }

    /**
     * 수동 입력 기록 등록 — 로컬 저장 후 서버 보고까지 동기화 흐름을 재사용한다.
     * 성공 시 조개 지급 팝업(첫 기록일 때)이 뜬다. 인증은 v1에서 즉시 통과.
     */
    fun onManualRegister(input: ManualEntryInput) {
        viewModelScope.launch {
            _shellReward.value = 0
            _syncing.value = true
            try {
                val earned = hcSwimSyncer.registerManual(
                    distanceMeters = input.distanceM, durationMin = input.durationMin,
                    calories = input.calories, maxHr = input.maxHr, minHr = input.minHr,
                    avgHr = input.avgHr,
                    startTime = input.startTime,
                    strokeFreeM = input.freeM, strokeBreastM = input.breastM,
                    strokeBackM = input.backM, strokeFlyM = input.flyM, strokeKickM = input.kickM,
                )
                appStateLoader.refreshCurrency()
                _shellRewardKind.value = ShellRewardKind.SwimRecord
                _shellReward.value = earned
            } catch (e: Exception) {
                Timber.e(e, "수동 기록 등록 실패")
                _syncError.value = "기록 등록에 실패했어요. 다시 시도해주세요."
            } finally {
                _syncing.value = false
            }
        }
    }

    companion object {
        /** 동기화 로딩 표시 최소 유지 시간(ms). */
        private const val MIN_SYNC_INDICATOR_MS = 1000L
    }

    private fun monthStart(): String = YearMonth.now().atDay(1).toString()
    private fun monthEnd(): String = YearMonth.now().atEndOfMonth().toString()
    private fun lastMonthStart(): String = YearMonth.now().minusMonths(1).atDay(1).toString()

    /**
     * 오늘 세션의 영법 분배를 로컬에 저장하고 서버에도 반영한다.
     *
     * 서버 반영은 캘린더와 같은 경로를 쓴다 — 예전엔 홈에서 고친 영법이 로컬에만 남아
     * 기기를 바꾸면 사라졌다. 오늘 기록을 처음 채우면 서버가 조개 1개를 얹어 준다.
     */
    fun saveStrokes(logId: Long, free: Int, breast: Int, back: Int, fly: Int, kick: Int, mixed: Int) {
        viewModelScope.launch {
            swimLogUseCase.updateStrokes(logId, free, breast, back, fly, mixed, kick)
            val earned = hcSwimSyncer.pushStrokes(LocalDate.now().toString())
            if (earned > 0) {
                _shellRewardKind.value = ShellRewardKind.StrokeBonus
                _shellReward.value = earned
            }
        }
    }
}
