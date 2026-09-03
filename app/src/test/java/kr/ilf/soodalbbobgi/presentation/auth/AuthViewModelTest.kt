package kr.ilf.soodalbbobgi.presentation.auth

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.asset.AssetManager
import kr.ilf.soodalbbobgi.data.asset.AssetSyncProgress
import kr.ilf.soodalbbobgi.data.auth.AccountPrefs
import kr.ilf.soodalbbobgi.data.auth.AccountSwitchGuard
import kr.ilf.soodalbbobgi.data.auth.GoogleAuthManager
import kr.ilf.soodalbbobgi.data.auth.KakaoAuthManager
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.ApiError
import kr.ilf.soodalbbobgi.data.remote.dto.ApiResponse
import kr.ilf.soodalbbobgi.data.remote.dto.AuthData
import kr.ilf.soodalbbobgi.data.remote.dto.GoogleAuthRequest
import kr.ilf.soodalbbobgi.data.remote.dto.UserData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
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
 * AuthViewModel의 Google 로그인 분기에 대한 단위 테스트.
 *
 * 카카오 분기는 패턴이 동일하므로 한쪽 검증으로 양쪽 신뢰도를 확보한다.
 * Credential Manager/네트워크/Health Connect는 모두 mockk로 대체.
 * 재설치 직후 로그인 후 에셋·HC 동기화 트리거도 함께 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private lateinit var kakao: KakaoAuthManager
    private lateinit var google: GoogleAuthManager
    private lateinit var api: SoodalApi
    private lateinit var tokenStore: TokenStore
    private lateinit var guard: AccountSwitchGuard
    private lateinit var accountPrefs: AccountPrefs
    private lateinit var appStateLoader: AppStateLoader
    private lateinit var appState: AppState
    private lateinit var hc: HealthConnectManager
    private lateinit var assetManager: AssetManager
    private lateinit var hcSwimSyncer: HcSwimSyncer
    private lateinit var activity: Activity

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        kakao = mockk(relaxed = true)
        google = mockk(relaxed = true)
        api = mockk(relaxed = true)
        tokenStore = mockk(relaxed = true)
        guard = mockk(relaxed = true)
        // relaxed → onboardingCompleted = false (온보딩 미완료 기본값)
        accountPrefs = mockk(relaxed = true)
        appStateLoader = mockk(relaxed = true)
        appState = AppState()
        hc = mockk(relaxed = true)
        assetManager = mockk(relaxed = true)
        hcSwimSyncer = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        coEvery { appStateLoader.loadAll() } returns Result.success(Unit)
        coEvery { assetManager.sync() } returns Result.success(Unit)
        every { assetManager.progress } returns MutableStateFlow(AssetSyncProgress.Idle)
        coEvery { hcSwimSyncer.sync() } returns 0
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = AuthViewModel(kakao, google, api, tokenStore, guard, accountPrefs, appStateLoader, appState, hc,
        assetManager, hcSwimSyncer, CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `loginWithGoogle on new user routes to Onboarding`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(GoogleAuthRequest("idtok")) } returns ApiResponse(
            success = true,
            data = AuthData(
                accessToken = "at", refreshToken = "rt", expiresIn = 3600L,
                isNewUser = true,
                user = sampleUser(nickname = null),
            ),
            error = null,
        )

        val viewModel = vm()
        viewModel.loginWithGoogle(activity)

        coVerify { tokenStore.saveTokens("at", "rt", 3600L) }
        coVerify { appStateLoader.loadAll() }
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(AuthUiState.Success::class.java)
        assertThat((state as AuthUiState.Success).route).isEqualTo(AuthRoute.Onboarding)
    }

    @Test
    fun `loginWithGoogle on existing user without HC permission routes to Permission`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData(
                accessToken = "at", refreshToken = "rt", expiresIn = 3600L,
                isNewUser = false,
                user = sampleUser(nickname = "수달이"),
            ),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns false

        val viewModel = vm()
        viewModel.loginWithGoogle(activity)

        val state = viewModel.uiState.value
        assertThat((state as AuthUiState.Success).route).isEqualTo(AuthRoute.Permission)
    }

    @Test
    fun `loginWithGoogle on existing user with HC permission routes to Home`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData(
                accessToken = "at", refreshToken = "rt", expiresIn = 3600L,
                isNewUser = false,
                user = sampleUser(nickname = "수달이"),
            ),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true

        val viewModel = vm()
        viewModel.loginWithGoogle(activity)

        val state = viewModel.uiState.value
        assertThat((state as AuthUiState.Success).route).isEqualTo(AuthRoute.Home)
    }

    // ── 온보딩 완료 후 HC 권한 없음 — 권한 화면으로 되돌리지 않는다 (R19) ──

    @Test
    fun `loginWithGoogle on onboarded user without HC permission routes to Home`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns false
        every { accountPrefs.onboardingCompleted } returns true

        val viewModel = vm()
        viewModel.loginWithGoogle(activity)

        val state = viewModel.uiState.value
        assertThat((state as AuthUiState.Success).route).isEqualTo(AuthRoute.Home)
    }

    @Test
    fun `loginWithGoogle on user who never finished onboarding without HC permission routes to Permission`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns false
        every { accountPrefs.onboardingCompleted } returns false

        val viewModel = vm()
        viewModel.loginWithGoogle(activity)

        val state = viewModel.uiState.value
        assertThat((state as AuthUiState.Success).route).isEqualTo(AuthRoute.Permission)
    }

    @Test
    fun `loginWithGoogle reads the onboarding flag only after the account guard has run`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns false
        every { accountPrefs.onboardingCompleted } returns true

        vm().loginWithGoogle(activity)

        // 다른 계정이면 가드가 prefs를 지우므로, 완료 플래그는 가드 뒤에 읽어야 이전 계정 값을 쓰지 않는다.
        coVerifyOrder {
            guard.ensureLocalOwnedBy(sampleUser("수달이").id)
            accountPrefs.onboardingCompleted
        }
    }

    @Test
    fun `loginWithGoogle when signIn fails sets Error state`() = runTest {
        coEvery { google.signIn(activity) } returns Result.failure(RuntimeException("user canceled"))

        val viewModel = vm()
        viewModel.loginWithGoogle(activity)

        assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Error::class.java)
        coVerify(exactly = 0) { api.authGoogle(any()) }
    }

    @Test
    fun `loginWithGoogle on server error sets Error state and does not save tokens`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = false, data = null,
            error = ApiError("INVALID_TOKEN", "audience mismatch"),
        )

        val viewModel = vm()
        viewModel.loginWithGoogle(activity)

        assertThat(viewModel.uiState.value).isInstanceOf(AuthUiState.Error::class.java)
        coVerify(exactly = 0) { tokenStore.saveTokens(any(), any(), any()) }
    }

    @Test
    fun `loginWithGoogle ignored while already loading`() = runTest {
        coEvery { google.signIn(activity) } coAnswers {
            kotlinx.coroutines.delay(50)
            Result.success("idtok")
        }
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true

        val viewModel = vm()
        viewModel.loginWithGoogle(activity)
        viewModel.loginWithGoogle(activity) // 두 번째 호출은 무시되어야 함

        // 첫 번째 호출만 dispatch
        coVerify(exactly = 1) { google.signIn(activity) }
    }

    // ── 재설치 직후 동기화 트리거 검증 ──────────────────────────────────────

    @Test
    fun `loginWithGoogle triggers assetManager sync after success`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true

        vm().loginWithGoogle(activity)

        // 에셋 동기화가 로그인 성공 후 반드시 호출되어야 한다 (증상 1 수정 확인)
        coVerify { assetManager.sync() }
    }

    @Test
    fun `loginWithGoogle triggers HC sync when permission granted`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true

        vm().loginWithGoogle(activity)

        // HC 권한이 있으면 수영 기록 동기화도 트리거되어야 한다 (증상 2 수정 확인)
        coVerify { hcSwimSyncer.sync() }
    }

    @Test
    fun `loginWithGoogle skips HC sync when no permission`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true,
            data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")),
            error = null,
        )
        coEvery { hc.hasAllPermissions() } returns false

        vm().loginWithGoogle(activity)

        // HC 권한이 없으면 HC 동기화를 건너뛰어야 한다
        coVerify(exactly = 0) { hcSwimSyncer.sync() }
        // 에셋 동기화는 권한 무관하게 항상 실행되어야 한다
        coVerify { assetManager.sync() }
    }

    @Test
    fun `loginWithGoogle does not trigger sync on server error`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = false, data = null,
            error = ApiError("INVALID_TOKEN", "bad token"),
        )

        vm().loginWithGoogle(activity)

        // 로그인 실패 시 동기화 트리거 없음
        coVerify(exactly = 0) { assetManager.sync() }
        coVerify(exactly = 0) { hcSwimSyncer.sync() }
    }

    // ── 로그인 직후 동기화 지급분 → 홈 팝업 (R15) ────────────────────────────

    @Test
    fun `loginWithGoogle hands post-login shells to the home popup`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true, data = AuthData("at", "rt", 3600L, true, sampleUser(null)), error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true
        coEvery { hcSwimSyncer.sync() } returns 2

        vm().loginWithGoogle(activity)

        // 재로그인은 HC 권한이 남아 있어 로그인 직후 동기화가 오늘 기록을 먼저 보고한다 —
        // 온보딩을 거쳐 홈에 닿았을 때 이 지급분이 팝업으로 떠야 한다 (R15).
        assertThat(appState.pendingShellReward.value).isEqualTo(2)
    }

    @Test
    fun `loginWithGoogle leaves no pending popup when the sync earned nothing`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true, data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")), error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true
        coEvery { hcSwimSyncer.sync() } returns 0

        vm().loginWithGoogle(activity)

        assertThat(appState.pendingShellReward.value).isEqualTo(0)
    }

    @Test
    fun `loginWithKakao hands post-login shells to the home popup`() = runTest {
        coEvery { kakao.signIn(activity) } returns Result.success("kakaotok")
        coEvery { api.authKakao(any()) } returns ApiResponse(
            success = true, data = AuthData("at", "rt", 3600L, true, sampleUser(null)), error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true
        coEvery { hcSwimSyncer.sync() } returns 2

        vm().loginWithKakao(activity)

        assertThat(appState.pendingShellReward.value).isEqualTo(2)
    }

    @Test
    fun `loginWithKakao leaves no pending popup when the sync earned nothing`() = runTest {
        coEvery { kakao.signIn(activity) } returns Result.success("kakaotok")
        coEvery { api.authKakao(any()) } returns ApiResponse(
            success = true, data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")), error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true
        coEvery { hcSwimSyncer.sync() } returns 0

        vm().loginWithKakao(activity)

        assertThat(appState.pendingShellReward.value).isEqualTo(0)
    }

    // ── 계정 전환 가드 (R16) ──────────────────────────────────────────────────

    @Test
    fun `loginWithGoogle runs the account guard before saving tokens and loading state`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = true, data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")), error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true

        vm().loginWithGoogle(activity)

        // 초기화가 토큰과 AppState도 지우므로 가드는 토큰 저장·상태 로드보다 먼저 돌아야 한다.
        coVerifyOrder {
            guard.ensureLocalOwnedBy(sampleUser("수달이").id)
            tokenStore.saveTokens("at", "rt", 3600L)
            appStateLoader.loadAll()
        }
    }

    @Test
    fun `loginWithGoogle does not touch the account guard on server error`() = runTest {
        coEvery { google.signIn(activity) } returns Result.success("idtok")
        coEvery { api.authGoogle(any()) } returns ApiResponse(
            success = false, data = null, error = ApiError("INVALID_TOKEN", "bad"),
        )

        vm().loginWithGoogle(activity)

        coVerify(exactly = 0) { guard.ensureLocalOwnedBy(any()) }
    }

    @Test
    fun `loginWithKakao runs the account guard before saving tokens`() = runTest {
        coEvery { kakao.signIn(activity) } returns Result.success("kakaotok")
        coEvery { api.authKakao(any()) } returns ApiResponse(
            success = true, data = AuthData("at", "rt", 3600L, false, sampleUser("수달이")), error = null,
        )
        coEvery { hc.hasAllPermissions() } returns true

        vm().loginWithKakao(activity)

        coVerifyOrder {
            guard.ensureLocalOwnedBy(sampleUser("수달이").id)
            tokenStore.saveTokens("at", "rt", 3600L)
        }
    }

    private fun sampleUser(nickname: String?) = UserData(
        id = "u_1", nickname = nickname,
        shellBalance = 0, pearlBalance = 0, pityCounter = 0,
        lastShellGrantDate = null, gender = null, ageRange = null,
        authProvider = "google", createdAt = 0L,
    )
}
