package com.soodalbbobgi.app.presentation.splash

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.asset.AssetManager
import com.soodalbbobgi.app.data.asset.AssetSyncProgress
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.health.HcSyncPreferences
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
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
    private lateinit var assetProgress: MutableStateFlow<AssetSyncProgress>

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        tokenStore = mockk(relaxed = true)
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
        userSession = userSession,
        appState = appState,
        appStateLoader = appStateLoader,
        healthConnectManager = healthConnectManager,
        swimLogUseCase = swimLogUseCase,
        hcSyncPreferences = hcSyncPreferences,
        assetManager = assetManager,
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
}
