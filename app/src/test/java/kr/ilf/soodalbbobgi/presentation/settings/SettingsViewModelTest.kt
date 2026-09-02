package kr.ilf.soodalbbobgi.presentation.settings

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
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
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HcSyncPreferences
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.ApiResponse
import kr.ilf.soodalbbobgi.data.remote.dto.UserData
import kr.ilf.soodalbbobgi.domain.model.UserProfile
import kr.ilf.soodalbbobgi.domain.repository.SwimLogRepository
import kr.ilf.soodalbbobgi.work.HcChangeCheckScheduler
import kr.ilf.soodalbbobgi.work.ReminderScheduler
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * 설정 화면 ViewModel 테스트.
 * - HC 권한이 회수돼 있으면 수영 기록 알림이 "켜진 채 잠긴" 상태로 남지 않고 저장값까지 꺼져야 한다.
 * - 닉네임 저장은 쿨다운을 앱에서 먼저 막고, 서버 거부(4xx)는 서버 문구를 그대로 보여준다.
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

    // ── HC 재연결 후 동기화 지급분 → 홈 팝업 (R15) ──

    @Test
    fun `onHcPermissionGranted hands earned shells to the home popup`() = runTest {
        val appState = AppState()
        val hcSwimSyncer = mockk<HcSwimSyncer>(relaxed = true)
        coEvery { hcSwimSyncer.sync() } returns 1

        vm(appState = appState, hcSwimSyncer = hcSwimSyncer).onHcPermissionGranted()

        assertThat(appState.pendingShellReward.value).isEqualTo(1)
    }

    // ── 닉네임 저장 · 쿨다운 ──

    private fun userData(nickname: String, changeableAt: Long?) = UserData(
        id = "u1", nickname = nickname, shellBalance = 0, pearlBalance = 0, pityCounter = 0,
        authProvider = "kakao", createdAt = 0L, nicknameChangeableAt = changeableAt,
    )

    private fun profileOf(nickname: String, changeableAt: Long? = null) =
        AppState().apply { applyProfile(UserProfile("u1", nickname, null, null, "kakao", nicknameChangeableAt = changeableAt)) }

    private fun httpError(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    private val SettingsViewModel.errorMessage: String
        get() = (nicknameState.value as NicknameSaveState.Error).message

    @Test
    fun `쿨다운 중 다른 이름은 서버를 부르지 않고 안내를 띄운다`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val vm = vm(api = api, appState = profileOf("수달", changeableAt = 2_000L))

        vm.saveNickname("해달", nowMillis = 1_000L)

        assertThat(vm.errorMessage).endsWith("부터 바꿀 수 있어요.")
        coVerify(exactly = 0) { api.updateMe(any()) }
    }

    @Test
    fun `쿨다운이 지나면 저장하고 응답의 다음 가능 시각을 프로필에 반영한다`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val appState = profileOf("수달", changeableAt = 2_000L)
        coEvery { api.updateMe(any()) } returns ApiResponse(true, userData("해달", 9_999L), null)
        val vm = vm(api = api, appState = appState, appStateLoader = AppStateLoader(api, appState, UserSession()))

        vm.saveNickname("해달", nowMillis = 3_000L)

        assertThat(vm.nicknameState.value).isEqualTo(NicknameSaveState.Success)
        assertThat(appState.profile.value?.nickname).isEqualTo("해달")
        assertThat(appState.profile.value?.nicknameChangeableAt).isEqualTo(9_999L)
    }

    @Test
    fun `서버 NICKNAME_COOLDOWN이면 메시지와 nextAllowedAt을 반영한다`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val appState = profileOf("수달")
        coEvery { api.updateMe(any()) } throws httpError(
            429,
            """{"success":false,"error":{"code":"NICKNAME_COOLDOWN","message":"서버 안내","details":{"nextAllowedAt":7777}}}""",
        )
        val vm = vm(api = api, appState = appState)

        vm.saveNickname("해달", nowMillis = 1_000L)

        assertThat(vm.errorMessage).isEqualTo("서버 안내")
        assertThat(appState.profile.value?.nicknameChangeableAt).isEqualTo(7777L)
    }

    @Test
    fun `서버 NICKNAME_TAKEN은 서버 메시지를 그대로 보여준다`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        coEvery { api.updateMe(any()) } throws httpError(
            409,
            """{"success":false,"error":{"code":"NICKNAME_TAKEN","message":"이미 사용 중인 닉네임입니다."}}""",
        )
        val vm = vm(api = api, appState = profileOf("수달"))

        vm.saveNickname("해달", nowMillis = 1_000L)

        assertThat(vm.errorMessage).isEqualTo("이미 사용 중인 닉네임입니다.")
    }

    @Test
    fun `네트워크 예외는 기존 문구`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        coEvery { api.updateMe(any()) } throws IOException("down")
        val vm = vm(api = api, appState = profileOf("수달"))

        vm.saveNickname("해달", nowMillis = 1_000L)

        assertThat(vm.errorMessage).isEqualTo("네트워크 오류가 발생했어요.")
    }
}
