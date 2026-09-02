package kr.ilf.soodalbbobgi.presentation.settings

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kr.ilf.soodalbbobgi.core.notify.SoodalNotifier
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HcSyncPreferences
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.domain.repository.SwimLogRepository
import kr.ilf.soodalbbobgi.work.HcChangeCheckScheduler
import kr.ilf.soodalbbobgi.work.ReminderScheduler
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * 설정 화면의 HC 연결 상태 반영 테스트.
 * HC 권한이 회수돼 있으면 수영 기록 알림이 "켜진 채 잠긴" 상태로 남지 않고 저장값까지 꺼져야 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var notificationPrefs: NotificationPrefs
    private lateinit var hcChangeCheckScheduler: HcChangeCheckScheduler

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        healthConnectManager = mockk(relaxed = true)
        notificationPrefs = mockk(relaxed = true)
        hcChangeCheckScheduler = mockk(relaxed = true)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    /**
     * 테스트용 [SettingsViewModel] 생성 — 인자를 넘기지 않으면 전부 relaxed mock(`AppState`만 실객체).
     * 검증 대상 협력자는 `@Before`에서 만든 필드를 기본값으로 써서 생성 전에 스텁할 수 있다.
     */
    private fun vm(
        api: SoodalApi = mockk(relaxed = true),
        appState: AppState = AppState(),
        appStateLoader: AppStateLoader = mockk(relaxed = true),
        tokenStore: TokenStore = mockk(relaxed = true),
        hcSyncPreferences: HcSyncPreferences = mockk(relaxed = true),
        healthConnectManager: HealthConnectManager = this.healthConnectManager,
        swimLogRepository: SwimLogRepository = mockk(relaxed = true),
        hcSwimSyncer: HcSwimSyncer = mockk(relaxed = true),
        notificationPrefs: NotificationPrefs = this.notificationPrefs,
        reminderScheduler: ReminderScheduler = mockk(relaxed = true),
        hcChangeCheckScheduler: HcChangeCheckScheduler = this.hcChangeCheckScheduler,
        notifier: SoodalNotifier = mockk(relaxed = true),
        appScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
    ) = SettingsViewModel(
        api = api,
        appState = appState,
        appStateLoader = appStateLoader,
        tokenStore = tokenStore,
        hcSyncPreferences = hcSyncPreferences,
        healthConnectManager = healthConnectManager,
        swimLogRepository = swimLogRepository,
        hcSwimSyncer = hcSwimSyncer,
        notificationPrefs = notificationPrefs,
        reminderScheduler = reminderScheduler,
        hcChangeCheckScheduler = hcChangeCheckScheduler,
        notifier = notifier,
        appScope = appScope,
    )

    @Test
    fun `HC 권한이 회수돼 있으면 진입 시 수영 기록 알림을 저장값까지 끈다`() = runTest {
        coEvery { healthConnectManager.hasAllPermissions() } returns false
        every { notificationPrefs.newRecordEnabled } returns true

        val vm = vm()

        assertThat(vm.hcConnected.value).isFalse()
        assertThat(vm.newRecordEnabled.value).isFalse()
        verify { notificationPrefs setProperty "newRecordEnabled" value false }
        verify { hcChangeCheckScheduler.cancel() }
    }

    @Test
    fun `HC가 연결돼 있으면 저장된 알림 설정을 건드리지 않는다`() = runTest {
        coEvery { healthConnectManager.hasAllPermissions() } returns true
        every { notificationPrefs.newRecordEnabled } returns true

        val vm = vm()

        assertThat(vm.hcConnected.value).isTrue()
        assertThat(vm.newRecordEnabled.value).isTrue()
        verify(exactly = 0) { hcChangeCheckScheduler.cancel() }
    }

    @Test
    fun `알림이 원래 꺼져 있으면 회수 감지 시 아무 것도 저장하지 않는다`() = runTest {
        coEvery { healthConnectManager.hasAllPermissions() } returns false
        every { notificationPrefs.newRecordEnabled } returns false

        vm()

        verify(exactly = 0) { notificationPrefs setProperty "newRecordEnabled" value any<Boolean>() }
        verify(exactly = 0) { hcChangeCheckScheduler.cancel() }
    }

    @Test
    fun `권한 플로우에서 거부하고 돌아와도 알림을 끈다`() = runTest {
        coEvery { healthConnectManager.hasAllPermissions() } returns true
        every { notificationPrefs.newRecordEnabled } returns true
        val vm = vm()

        coEvery { healthConnectManager.hasAllPermissions() } returns false
        vm.onHcPermissionFlowReturned()

        assertThat(vm.hcConnected.value).isFalse()
        assertThat(vm.newRecordEnabled.value).isFalse()
        verify { hcChangeCheckScheduler.cancel() }
    }

    @Test
    fun `재개 시 재조회로 회수를 감지한다`() = runTest {
        coEvery { healthConnectManager.hasAllPermissions() } returns true
        every { notificationPrefs.newRecordEnabled } returns true
        val vm = vm()

        coEvery { healthConnectManager.hasAllPermissions() } returns false
        vm.refreshHcStatus()

        assertThat(vm.newRecordEnabled.value).isFalse()
        verify { hcChangeCheckScheduler.cancel() }
    }
}
