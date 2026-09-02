package kr.ilf.soodalbbobgi.presentation.settings

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
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
import kr.ilf.soodalbbobgi.data.auth.GoogleAuthManager
import kr.ilf.soodalbbobgi.data.auth.KakaoAuthManager
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HcSyncPreferences
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.local.LocalDataResetter
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.ApiError
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
        kakao: KakaoAuthManager = mockk(relaxed = true),
        google: GoogleAuthManager = mockk(relaxed = true),
        hcSyncPreferences: HcSyncPreferences = mockk(relaxed = true),
        healthConnectManager: HealthConnectManager = this.healthConnectManager,
        swimLogRepository: SwimLogRepository = mockk(relaxed = true),
        hcSwimSyncer: HcSwimSyncer = mockk(relaxed = true),
        notificationPrefs: NotificationPrefs = this.notificationPrefs,
        reminderScheduler: ReminderScheduler = mockk(relaxed = true),
        hcChangeCheckScheduler: HcChangeCheckScheduler = this.hcChangeCheckScheduler,
        notifier: SoodalNotifier = mockk(relaxed = true),
        resetter: LocalDataResetter = mockk(relaxed = true),
        appScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
    ) = SettingsViewModel(
        api = api,
        appState = appState,
        appStateLoader = appStateLoader,
        tokenStore = tokenStore,
        kakaoAuthManager = kakao,
        googleAuthManager = google,
        hcSyncPreferences = hcSyncPreferences,
        healthConnectManager = healthConnectManager,
        swimLogRepository = swimLogRepository,
        hcSwimSyncer = hcSwimSyncer,
        notificationPrefs = notificationPrefs,
        reminderScheduler = reminderScheduler,
        hcChangeCheckScheduler = hcChangeCheckScheduler,
        notifier = notifier,
        localDataResetter = resetter,
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

    // ── 로그아웃 · 탈퇴 초기 상태화 (R16) ──

    @Test
    fun `탈퇴는 서버 삭제 성공 시에만 HC 권한을 회수하고 에셋을 제외한 로컬 전부를 지운 뒤 signedOut을 올린다`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val resetter = mockk<LocalDataResetter>(relaxed = true)
        coEvery { api.deleteMe() } returns ApiResponse(true, Unit, null)

        val vm = vm(api = api, resetter = resetter)
        vm.deleteAccount()

        coVerify(exactly = 1) { healthConnectManager.revokeAllPermissions() }
        coVerify(exactly = 1) { resetter.clearAll(keepAssets = true) }
        assertThat(vm.signedOut.value).isTrue()
        assertThat(vm.accountAction.value).isEqualTo(AccountActionState.Idle)
    }

    @Test
    fun `탈퇴 서버 실패면 로컬을 건드리지 않는다`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val resetter = mockk<LocalDataResetter>(relaxed = true)
        coEvery { api.deleteMe() } returns ApiResponse(false, null, ApiError("E", "x"))

        val vm = vm(api = api, resetter = resetter)
        vm.deleteAccount()

        coVerify(exactly = 0) { resetter.clearAll(any()) }
        verify(exactly = 0) { resetter.clearSession() }
        coVerify(exactly = 0) { healthConnectManager.revokeAllPermissions() }
        assertThat(vm.signedOut.value).isFalse()
        assertThat(vm.accountAction.value).isInstanceOf(AccountActionState.Error::class.java)
    }

    @Test
    fun `로그아웃은 서버 실패여도 세션만 끊고 로컬 데이터는 남긴다`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val tokenStore = mockk<TokenStore>(relaxed = true)
        val resetter = mockk<LocalDataResetter>(relaxed = true)
        every { tokenStore.getRefreshToken() } returns "rt"
        coEvery { api.logout(any()) } throws IOException("offline")

        val vm = vm(api = api, tokenStore = tokenStore, resetter = resetter)
        vm.logout()

        verify(exactly = 1) { resetter.clearSession() }
        coVerify(exactly = 0) { resetter.clearAll(any()) }
        coVerify(exactly = 0) { healthConnectManager.revokeAllPermissions() }
        assertThat(vm.signedOut.value).isTrue()
    }

    // ── 탈퇴·로그아웃 시 프로바이더 기기 세션 정리 (R6 앱) ──

    private fun kakaoUser() = AppState().apply { applyProfile(UserProfile("u1", "수달", null, null, "kakao")) }

    private fun googleUser() = AppState().apply { applyProfile(UserProfile("u1", "수달", null, null, "google")) }

    @Test
    fun `deleteAccount on a kakao user clears the kakao session after the server delete`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val kakao = mockk<KakaoAuthManager>(relaxed = true)
        val google = mockk<GoogleAuthManager>(relaxed = true)
        val resetter = mockk<LocalDataResetter>(relaxed = true)
        coEvery { api.deleteMe() } returns ApiResponse(true, Unit, null)
        coEvery { kakao.signOutLocally() } returns Result.success(Unit)

        val vm = vm(api = api, appState = kakaoUser(), kakao = kakao, google = google, resetter = resetter)
        vm.deleteAccount()

        // 서버 성공 직후·HC 회수 앞 — provider 값을 읽어야 하므로 로컬 초기화(clearAll)보다 먼저여야 한다
        coVerifyOrder {
            api.deleteMe()
            kakao.signOutLocally()
            healthConnectManager.revokeAllPermissions()
            resetter.clearAll(keepAssets = true)
        }
        coVerify(exactly = 0) { google.clearCredentialState() }
        assertThat(vm.signedOut.value).isTrue()
    }

    @Test
    fun `deleteAccount on a google user clears the credential state`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val kakao = mockk<KakaoAuthManager>(relaxed = true)
        val google = mockk<GoogleAuthManager>(relaxed = true)
        coEvery { api.deleteMe() } returns ApiResponse(true, Unit, null)
        coEvery { google.clearCredentialState() } returns Result.success(Unit)

        vm(api = api, appState = googleUser(), kakao = kakao, google = google).deleteAccount()

        coVerify(exactly = 1) { google.clearCredentialState() }
        coVerify(exactly = 0) { kakao.signOutLocally() }
    }

    @Test
    fun `deleteAccount proceeds when the provider cleanup fails`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val kakao = mockk<KakaoAuthManager>(relaxed = true)
        val resetter = mockk<LocalDataResetter>(relaxed = true)
        coEvery { api.deleteMe() } returns ApiResponse(true, Unit, null)
        coEvery { kakao.signOutLocally() } returns Result.failure(RuntimeException("no token"))

        val vm = vm(api = api, appState = kakaoUser(), kakao = kakao, resetter = resetter)
        vm.deleteAccount()

        coVerify(exactly = 1) { resetter.clearAll(keepAssets = true) }
        assertThat(vm.signedOut.value).isTrue()
        assertThat(vm.accountAction.value).isEqualTo(AccountActionState.Idle)
    }

    @Test
    fun `deleteAccount server failure touches neither provider nor local`() = runTest {
        val api = mockk<SoodalApi>(relaxed = true)
        val kakao = mockk<KakaoAuthManager>(relaxed = true)
        val resetter = mockk<LocalDataResetter>(relaxed = true)
        coEvery { api.deleteMe() } returns ApiResponse(false, null, ApiError("INTERNAL_ERROR", "x"))

        val vm = vm(api = api, appState = kakaoUser(), kakao = kakao, resetter = resetter)
        vm.deleteAccount()

        coVerify(exactly = 0) { kakao.signOutLocally() }
        coVerify(exactly = 0) { resetter.clearAll(any()) }
        assertThat(vm.accountAction.value).isInstanceOf(AccountActionState.Error::class.java)
    }

    @Test
    fun `logout clears the provider session too`() = runTest {
        val kakao = mockk<KakaoAuthManager>(relaxed = true)
        val resetter = mockk<LocalDataResetter>(relaxed = true)
        coEvery { kakao.signOutLocally() } returns Result.success(Unit)

        val vm = vm(appState = kakaoUser(), kakao = kakao, resetter = resetter)
        vm.logout()

        // 세션 정리(clearSession)가 메모리 프로필을 비우므로 provider 정리는 그 앞이어야 한다
        coVerifyOrder {
            kakao.signOutLocally()
            resetter.clearSession()
        }
        assertThat(vm.signedOut.value).isTrue()
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
