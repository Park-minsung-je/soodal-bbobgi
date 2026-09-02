package kr.ilf.soodalbbobgi.presentation.onboarding

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.data.asset.AssetManager
import kr.ilf.soodalbbobgi.data.asset.AssetSyncProgress
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HcSyncPreferences
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * OnboardingPermissionViewModel의 최초 동기화 시작 단위 테스트.
 *
 * HC 권한 허용 → 홈 이동 경로에서 선택한 기간 저장, 에셋·HC 동기화 시작,
 * 지급 조개의 홈 팝업 전달이 동작하는지 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPermissionViewModelTest {

    private lateinit var assetManager: AssetManager
    private lateinit var hcSwimSyncer: HcSwimSyncer
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var hcSyncPreferences: HcSyncPreferences
    private lateinit var appState: AppState

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        assetManager = mockk(relaxed = true)
        hcSwimSyncer = mockk(relaxed = true)
        healthConnectManager = mockk(relaxed = true)
        hcSyncPreferences = mockk(relaxed = true)
        appState = mockk(relaxed = true)
        every { assetManager.progress } returns MutableStateFlow(AssetSyncProgress.Idle)
        coEvery { assetManager.sync() } returns Result.success(Unit)
        coEvery { hcSwimSyncer.sync() } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = OnboardingPermissionViewModel(
        assetManager, hcSwimSyncer, healthConnectManager, hcSyncPreferences, appState,
        mockk(relaxed = true), CoroutineScope(UnconfinedTestDispatcher()),
    )

    @Test
    fun `startInitialSync stores the chosen window before syncing`() = runTest {
        vm().startInitialSync(12)

        // 프로세스가 죽어도 첫 동기화가 기간을 기억해야 한다 — 최대 옵션(1년)도 그대로 저장
        verify { hcSyncPreferences.setPendingInitialMonths(12) }
    }

    @Test
    fun `startInitialSync clamps the window to the maximum`() = runTest {
        vm().startInitialSync(99)

        // UI 밖에서 어떤 값이 들어와도 1년을 넘는 읽기를 시작하지 않는다
        verify { hcSyncPreferences.setPendingInitialMonths(OnboardingPermissionViewModel.MAX_INITIAL_MONTHS) }
    }

    @Test
    fun `startInitialSync does not store a window when history is skipped`() = runTest {
        vm().startInitialSync(0)

        verify(exactly = 0) { hcSyncPreferences.setPendingInitialMonths(any()) }
        coVerify { hcSwimSyncer.sync() } // 기간 없이도 오늘 창 동기화는 시작한다
    }

    @Test
    fun `hasHistoryPermission asks Health Connect for the history grant`() = runTest {
        coEvery { healthConnectManager.isHistoryReadGranted() } returns false

        assertThat(vm().hasHistoryPermission()).isFalse()
    }

    @Test
    fun `startInitialSync triggers asset and HC sync`() = runTest {
        vm().startInitialSync(1)

        coVerify { assetManager.sync() }
        coVerify { hcSwimSyncer.sync() }
    }

    @Test
    fun `startInitialSync hands earned shells to the home popup`() = runTest {
        coEvery { hcSwimSyncer.sync() } returns 3

        vm().startInitialSync(1)

        verify { appState.addPendingShellReward(3) }
    }

    @Test
    fun `startInitialSync skips the popup when nothing was earned`() = runTest {
        coEvery { hcSwimSyncer.sync() } returns 0

        vm().startInitialSync(1)

        verify(exactly = 0) { appState.addPendingShellReward(any()) }
    }

    @Test
    fun `startInitialSync marks syncing for the home indicator`() = runTest {
        vm().startInitialSync(1)

        verify { appState.setHcSyncing(true) }
        verify { appState.setHcSyncing(false) }
    }

    @Test
    fun `startInitialSync survives an asset sync failure`() = runTest {
        coEvery { assetManager.sync() } returns Result.failure(RuntimeException("network error"))

        vm().startInitialSync(1)

        coVerify { hcSwimSyncer.sync() }
    }

    @Test
    fun `hasAllPermissions reflects the manager`() = runTest {
        coEvery { healthConnectManager.hasAllPermissions() } returns true

        assertThat(vm().hasAllPermissions()).isTrue()
    }
}
