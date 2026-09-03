package kr.ilf.soodalbbobgi.presentation.home

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.ui.ShellRewardKind
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.auth.AccountPrefs
import kr.ilf.soodalbbobgi.data.health.HcSyncPreferences
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.domain.model.InventoryItem
import kr.ilf.soodalbbobgi.domain.model.Item
import kr.ilf.soodalbbobgi.domain.model.ProfileCard
import kr.ilf.soodalbbobgi.domain.model.SwimLog
import kr.ilf.soodalbbobgi.domain.model.SwimStats
import kr.ilf.soodalbbobgi.domain.model.UserProfile
import kr.ilf.soodalbbobgi.domain.usecase.SwimLogUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * HomeViewModel.uiState는 stateIn(WhileSubscribed) 기반이라 구독자가 없으면
 * 업스트림 combine이 돌지 않는다. 각 테스트에서 backgroundScope에 collect를 띄워
 * 실제 매핑 로직이 동작하도록 한다.
 *
 * 검증 목적: 저장된 ProfileCard + 아이템 마스터를 HomeUiState의 카드 필드로
 * 정확히 매핑하는지 확인 (Home 화면 카드 즉시 반영 버그 회귀 방지).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var api: SoodalApi
    private lateinit var appState: AppState
    private lateinit var loader: AppStateLoader
    private lateinit var session: UserSession
    private lateinit var swimLogUseCase: SwimLogUseCase
    private lateinit var hcManager: HealthConnectManager
    private lateinit var hcPrefs: HcSyncPreferences
    private lateinit var accountPrefs: AccountPrefs

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk(relaxed = true)
        appState = AppState()
        session = UserSession().apply { setAuthenticatedUser("u1") }
        loader = AppStateLoader(api, appState, session)
        swimLogUseCase = mockk(relaxed = true)
        hcManager = mockk(relaxed = true)
        hcPrefs = mockk(relaxed = true)
        accountPrefs = mockk(relaxed = true)

        // 기본: 비어 있는 월간 로그 + 0 통계
        every { swimLogUseCase.getLogsByDateRange(any(), any()) } returns flowOf(emptyList())
        coEvery { swimLogUseCase.getMonthStats(any(), any()) } returns
            SwimStats(totalDistanceMeters = 0, swimCount = 0, totalCalories = 0)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun TestScope.startCollect(vm: HomeViewModel) {
        backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
    }

    /** 공통 픽스처로 [HomeViewModel]을 만든다 — 동기화 오케스트레이터는 relaxed mock. */
    private fun newVm() = HomeViewModel(
        userSession = session,
        appState = appState,
        appStateLoader = loader,
        swimLogUseCase = swimLogUseCase,
        healthConnectManager = hcManager,
        hcSwimSyncer = mockk(relaxed = true),
        accountPrefs = accountPrefs,
    )

    // ── 기존 회원 설정 안내 팝업 (R30) ─────────────────────────────────────

    @Test
    fun `HC 필수 권한이 없으면 연결 안내를 띄운다`() = runTest(testDispatcher) {
        coEvery { hcManager.hasAllPermissions() } returns false
        every { accountPrefs.setupNudgeDismissed } returns false

        val vm = newVm()
        advanceUntilIdle()

        assertThat(vm.setupNudge.value).isTrue()
    }

    @Test
    fun `HC 권한이 모두 있으면 알림 설정과 무관하게 안내를 띄우지 않는다`() = runTest(testDispatcher) {
        coEvery { hcManager.hasAllPermissions() } returns true
        every { accountPrefs.setupNudgeDismissed } returns false

        val vm = newVm()
        advanceUntilIdle()

        assertThat(vm.setupNudge.value).isFalse()
    }

    @Test
    fun `다시 보지 않음을 저장한 기기에서는 조건이 맞아도 안내를 띄우지 않는다`() = runTest(testDispatcher) {
        coEvery { hcManager.hasAllPermissions() } returns false
        every { accountPrefs.setupNudgeDismissed } returns true

        val vm = newVm()
        advanceUntilIdle()

        assertThat(vm.setupNudge.value).isFalse()
    }

    @Test
    fun `온보딩 직후 진입은 한 번 건너뛰고 플래그를 소비해 다음 진입부터 띄운다`() = runTest(testDispatcher) {
        coEvery { hcManager.hasAllPermissions() } returns false
        every { accountPrefs.setupNudgeDismissed } returns false
        appState.suppressSetupNudgeOnce = true

        val first = newVm()
        advanceUntilIdle()
        assertThat(first.setupNudge.value).isFalse()
        assertThat(appState.suppressSetupNudgeOnce).isFalse()

        val second = newVm()
        advanceUntilIdle()
        assertThat(second.setupNudge.value).isTrue()
    }

    @Test
    fun `다시 보지 않음으로 닫으면 기기에 저장하고 안내를 내린다`() = runTest(testDispatcher) {
        coEvery { hcManager.hasAllPermissions() } returns false
        every { accountPrefs.setupNudgeDismissed } returns false
        val vm = newVm()
        advanceUntilIdle()
        assertThat(vm.setupNudge.value).isTrue()

        vm.dismissSetupNudge(dontShowAgain = true)

        verify { accountPrefs.setupNudgeDismissed = true }
        assertThat(vm.setupNudge.value).isFalse()
    }

    @Test
    fun `나중에로 닫으면 저장 없이 안내만 내린다`() = runTest(testDispatcher) {
        coEvery { hcManager.hasAllPermissions() } returns false
        every { accountPrefs.setupNudgeDismissed } returns false
        val vm = newVm()
        advanceUntilIdle()

        vm.dismissSetupNudge(dontShowAgain = false)

        verify(exactly = 0) { accountPrefs.setupNudgeDismissed = any() }
        assertThat(vm.setupNudge.value).isFalse()
    }

    // ── 대기 조개 → 팝업 구독 (R15 회귀) ──────────────────────────────────

    @Test
    fun `홈 진입 전에 쌓인 대기 조개는 생성 즉시 팝업으로 올린다`() = runTest(testDispatcher) {
        appState.addPendingShellReward(2)

        val vm = newVm()

        assertThat(vm.shellReward.value).isEqualTo(2)
        assertThat(vm.shellRewardKind.value).isEqualTo(ShellRewardKind.SwimRecord)
        assertThat(appState.pendingShellReward.value).isEqualTo(0)
    }

    @Test
    fun `홈 진입 후에 끝난 최초 동기화 지급분도 팝업으로 올린다`() = runTest(testDispatcher) {
        val vm = newVm()
        assertThat(vm.shellReward.value).isEqualTo(0)

        appState.addPendingShellReward(3)
        advanceUntilIdle()

        assertThat(vm.shellReward.value).isEqualTo(3)
    }

    @Test
    fun `최초 동기화 스크림이 도는 동안은 조개 팝업을 미루고 끝난 뒤 올린다`() = runTest(testDispatcher) {
        appState.setHcSyncing(true)
        val vm = newVm()

        // 로그인 직후 동기화가 먼저 지급한 조개 — 스크림 위로 팝업이 겹치면 안 된다 (R31)
        appState.addPendingShellReward(2)
        advanceUntilIdle()
        assertThat(vm.shellReward.value).isEqualTo(0)
        assertThat(appState.pendingShellReward.value).isEqualTo(2)

        appState.setHcSyncing(false)
        advanceUntilIdle()
        assertThat(vm.shellReward.value).isEqualTo(2)
        assertThat(appState.pendingShellReward.value).isEqualTo(0)
    }

    // ── 최초 동기화 진행 표시 (R17 조작 차단의 근거 상태) ────────────────────

    @Test
    fun `hcSyncing은 AppState의 최초 동기화 진행 상태를 그대로 반영한다`() = runTest(testDispatcher) {
        val vm = newVm()
        assertThat(vm.hcSyncing.value).isFalse()

        appState.setHcSyncing(true)
        advanceUntilIdle()
        assertThat(vm.hcSyncing.value).isTrue()

        appState.setHcSyncing(false)
        advanceUntilIdle()
        assertThat(vm.hcSyncing.value).isFalse()
    }

    @Test
    fun `uiState binds saved ProfileCard fields and asset paths from items master`() =
        runTest(testDispatcher) {
            // Given: 사용자 프로필 + 저장된 카드 + 아이템 마스터에 캐릭터 이미지 경로
            appState.applyProfile(UserProfile(
                id = "u1",
                nickname = "수달이",
                gender = null,
                ageRange = null,
                authProvider = "google",
            ))
            // 서버가 ProfileCard 슬롯에 inventory.id (PK)를 저장한다는 점을 반영해서
            // inventory.id != itemId 로 분리한 레이아웃으로 검증한다.
            appState.mergeItems(listOf(
                Item(
                    id = 42L, itemKey = "char_42", name = "수달 캐릭터",
                    grade = Grade.SR, category = "char",
                    imageAsset = "/assets/char/c1.png",
                ),
            ))
            appState.applyInventory(listOf(
                InventoryItem(
                    id = 100L,
                    userId = "u1",
                    itemId = 42L,
                    grade = Grade.SR,
                    category = "char",
                    acquiredAt = 0L,
                ),
            ))
            appState.applyProfileCard(ProfileCard(
                userId = "u1",
                characterItemId = 100L, // 서버가 저장한 inventory.id (← itemId 42 가 아님)
                characterX = 0.3f,
                characterY = 0.4f,
                characterScale = 0.85f,
                customText = "오늘도 한 바퀴",
            ))
            coEvery { swimLogUseCase.getMonthStats(any(), any()) } returns
                SwimStats(totalDistanceMeters = 1200, swimCount = 3, totalCalories = 200)

            val vm = HomeViewModel(
                userSession = session,
                appState = appState,
                appStateLoader = loader,
                swimLogUseCase = swimLogUseCase,
                healthConnectManager = hcManager,
                hcSwimSyncer = mockk(relaxed = true),
                        accountPrefs = accountPrefs,
            )
            startCollect(vm)
            advanceUntilIdle()

            // Then: 카드 필드가 ProfileCard + Items 마스터에서 정확히 도출돼야 함
            val s = vm.uiState.value
            assertThat(s.cardNickname).isEqualTo("수달이")
            assertThat(s.cardCharAsset).isEqualTo("/assets/char/c1.png")
            assertThat(s.cardBgAsset).isNull()
            assertThat(s.cardFrameAsset).isNull()
            assertThat(s.cardCharX).isEqualTo(0.3f)
            assertThat(s.cardCharY).isEqualTo(0.4f)
            assertThat(s.cardCharScale).isEqualTo(0.85f)
            assertThat(s.cardTagline).isEqualTo("오늘도 한 바퀴")
            assertThat(s.cardStats).isEqualTo("1200m · 3회")
        }
}
