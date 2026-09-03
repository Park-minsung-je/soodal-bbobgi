package kr.ilf.soodalbbobgi.presentation.splash

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.asset.AssetManager
import kr.ilf.soodalbbobgi.data.asset.AssetSyncProgress
import kr.ilf.soodalbbobgi.data.auth.AccountPrefs
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.health.HcSyncPreferences
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.domain.model.UserProfile
import kr.ilf.soodalbbobgi.domain.usecase.SwimLogUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.net.ConnectException

/**
 * SplashViewModel의 에셋 동기화 통합에 대한 단위 테스트.
 *
 * 검증 목표:
 * - AssetManager.sync()가 실패해도 destination 해결이 막히지 않는다 (graceful degradation).
 * - sync 코루틴이 진행 중이어도 destination 해결은 독립적으로 진행된다 (병렬 launch 보장).
 * - assetSyncProgress가 AssetManager.progress와 동일 인스턴스로 노출된다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private lateinit var tokenStore: TokenStore
    private lateinit var soodalApi: SoodalApi
    private lateinit var userSession: UserSession
    private lateinit var appState: AppState
    private lateinit var appStateLoader: AppStateLoader
    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var swimLogUseCase: SwimLogUseCase
    private lateinit var hcSyncPreferences: HcSyncPreferences
    private lateinit var assetManager: AssetManager
    private lateinit var accountPrefs: AccountPrefs
    private lateinit var assetProgress: MutableStateFlow<AssetSyncProgress>

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tokenStore = mockk(relaxed = true)
        // relaxed → onboardingCompleted = false (온보딩 미완료 기본값)
        accountPrefs = mockk(relaxed = true)
        soodalApi = mockk(relaxed = true)
        userSession = UserSession()
        appState = AppState()
        appStateLoader = mockk(relaxed = true)
        healthConnectManager = mockk(relaxed = true)
        swimLogUseCase = mockk(relaxed = true)
        hcSyncPreferences = mockk(relaxed = true)
        assetManager = mockk(relaxed = true)
        assetProgress = MutableStateFlow(AssetSyncProgress.Idle)
        every { assetManager.progress } returns assetProgress
        coEvery { assetManager.sync() } returns Result.success(Unit)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = SplashViewModel(
        tokenStore = tokenStore,
        soodalApi = soodalApi,
        appState = appState,
        appStateLoader = appStateLoader,
        healthConnectManager = healthConnectManager,
        hcSwimSyncer = mockk(relaxed = true),
        assetManager = assetManager,
        accountPrefs = accountPrefs,
    )

    @Test
    fun `sync failure does not block destination resolution`() = runTest {
        coEvery { assetManager.sync() } returns Result.failure(RuntimeException("network down"))
        coEvery { tokenStore.getAccessToken() } returns null // → Auth

        val viewModel = vm()

        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Auth)
        coVerify { assetManager.sync() }
    }

    @Test
    fun `sync coroutine running while checkAuth completes does not block destination`() = runTest {
        // sync()를 미완료 상태로 매달아 둔다 — destination 결정이 sync 완료를 기다리지 않음을 검증.
        val syncGate = CompletableDeferred<Result<Unit>>()
        coEvery { assetManager.sync() } coAnswers { syncGate.await() }
        coEvery { tokenStore.getAccessToken() } returns null // → Auth (checkAuth 빠르게 끝남)
        assetProgress.value = AssetSyncProgress.FetchingManifest

        val viewModel = vm()

        // sync는 여전히 진행 중인데 destination은 이미 결정되어 있어야 한다 (병렬 launch).
        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Auth)
        assertThat(viewModel.assetSyncProgress.value)
            .isEqualTo(AssetSyncProgress.FetchingManifest)

        // 이제 sync 완료시켜 후속 상태 전이를 확인.
        assetProgress.value = AssetSyncProgress.Done(version = "v1", downloaded = 0, removed = 0)
        syncGate.complete(Result.success(Unit))

        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Auth)
        assertThat(viewModel.assetSyncProgress.value)
            .isInstanceOf(AssetSyncProgress.Done::class.java)
        coVerify { assetManager.sync() }
    }

    @Test
    fun `assetSyncProgress is delegated to AssetManager progress`() = runTest {
        coEvery { tokenStore.getAccessToken() } returns null

        val viewModel = vm()

        // AssetManager.progress와 같은 source를 노출하는지 (값 동기)
        assetProgress.value = AssetSyncProgress.Downloading(completed = 2, total = 5)
        assertThat(viewModel.assetSyncProgress.value)
            .isEqualTo(AssetSyncProgress.Downloading(2, 5))
    }

    // ── 서버 장애 vs 인증 거부 분기 — 장애로는 절대 토큰을 지우지 않는다 ──

    private fun httpException(code: Int) =
        HttpException(Response.error<Any>(code, "".toResponseBody()))

    @Test
    fun `서버 연결 불가면 serverError를 올리고 토큰을 지우지 않는다`() = runTest {
        every { tokenStore.getAccessToken() } returns "access"
        every { tokenStore.isAccessTokenExpired() } returns false
        coEvery { appStateLoader.loadAll() } returns Result.failure(ConnectException("refused"))

        val viewModel = vm()

        assertThat(viewModel.serverError.value).isTrue()
        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Loading)
        verify(exactly = 0) { tokenStore.clearTokens() }
    }

    @Test
    fun `만료 토큰 갱신 중 네트워크 장애여도 토큰을 보존한다`() = runTest {
        every { tokenStore.getAccessToken() } returns "access"
        every { tokenStore.isAccessTokenExpired() } returns true
        every { tokenStore.getRefreshToken() } returns "refresh"
        coEvery { soodalApi.refreshToken(any()) } throws ConnectException("refused")

        val viewModel = vm()

        assertThat(viewModel.serverError.value).isTrue()
        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Loading)
        verify(exactly = 0) { tokenStore.clearTokens() }
    }

    @Test
    fun `갱신이 401로 거부되면 토큰을 지우고 Auth로 보낸다`() = runTest {
        every { tokenStore.getAccessToken() } returns "access"
        every { tokenStore.isAccessTokenExpired() } returns true
        every { tokenStore.getRefreshToken() } returns "refresh"
        coEvery { soodalApi.refreshToken(any()) } throws httpException(401)

        val viewModel = vm()

        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Auth)
        verify(atLeast = 1) { tokenStore.clearTokens() }
    }

    @Test
    fun `로드가 401로 거부되면 토큰을 지우고 Auth로 보낸다`() = runTest {
        every { tokenStore.getAccessToken() } returns "access"
        every { tokenStore.isAccessTokenExpired() } returns false
        coEvery { appStateLoader.loadAll() } returns Result.failure(httpException(401))

        val viewModel = vm()

        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Auth)
        verify(atLeast = 1) { tokenStore.clearTokens() }
    }

    @Test
    fun `정상 경로면 Home으로 보낸다`() = runTest {
        every { tokenStore.getAccessToken() } returns "access"
        every { tokenStore.isAccessTokenExpired() } returns false
        coEvery { healthConnectManager.hasAllPermissions() } returns true
        coEvery { appStateLoader.loadAll() } coAnswers {
            appState.applyProfile(UserProfile("u1", "수달이", null, null, "google"))
            Result.success(Unit)
        }

        val viewModel = vm()

        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Home)
        assertThat(viewModel.serverError.value).isFalse()
    }

    // ── 온보딩 완료 후 HC 권한 없음 — 권한 화면으로 되돌리지 않는다 (R19) ──

    @Test
    fun `온보딩을 마친 계정은 HC 권한이 없어도 Home으로 보낸다`() = runTest {
        every { tokenStore.getAccessToken() } returns "access"
        every { tokenStore.isAccessTokenExpired() } returns false
        coEvery { healthConnectManager.hasAllPermissions() } returns false
        every { accountPrefs.onboardingCompleted } returns true
        coEvery { appStateLoader.loadAll() } coAnswers {
            appState.applyProfile(UserProfile("u1", "수달이", null, null, "google"))
            Result.success(Unit)
        }

        val viewModel = vm()

        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Home)
    }

    @Test
    fun `온보딩을 마치지 않은 계정은 HC 권한이 없으면 Permission으로 보낸다`() = runTest {
        every { tokenStore.getAccessToken() } returns "access"
        every { tokenStore.isAccessTokenExpired() } returns false
        coEvery { healthConnectManager.hasAllPermissions() } returns false
        every { accountPrefs.onboardingCompleted } returns false
        coEvery { appStateLoader.loadAll() } coAnswers {
            appState.applyProfile(UserProfile("u1", "수달이", null, null, "google"))
            Result.success(Unit)
        }

        val viewModel = vm()

        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Permission)
    }

    @Test
    fun `재시도로 서버가 복구되면 Home으로 진행한다`() = runTest {
        every { tokenStore.getAccessToken() } returns "access"
        every { tokenStore.isAccessTokenExpired() } returns false
        coEvery { healthConnectManager.hasAllPermissions() } returns true
        coEvery { appStateLoader.loadAll() } returns Result.failure(ConnectException("refused"))

        val viewModel = vm()
        assertThat(viewModel.serverError.value).isTrue()

        // 서버 복구 후 재시도
        coEvery { appStateLoader.loadAll() } coAnswers {
            appState.applyProfile(UserProfile("u1", "수달이", null, null, "google"))
            Result.success(Unit)
        }
        viewModel.retry()

        assertThat(viewModel.serverError.value).isFalse()
        assertThat(viewModel.destination.value).isEqualTo(SplashDestination.Home)
    }
}
