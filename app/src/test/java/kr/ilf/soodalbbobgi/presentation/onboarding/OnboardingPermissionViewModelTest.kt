package kr.ilf.soodalbbobgi.presentation.onboarding

import kr.ilf.soodalbbobgi.data.asset.AssetManager
import kr.ilf.soodalbbobgi.data.asset.AssetSyncProgress
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import io.mockk.coEvery
import io.mockk.coVerify
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
 * OnboardingPermissionViewModel의 권한 허용 후 동기화 트리거 단위 테스트.
 *
 * HC 권한 허용 → Home 이동 경로에서 에셋·HC 동기화가 즉시 실행되는지 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPermissionViewModelTest {

    private lateinit var assetManager: AssetManager
    private lateinit var hcSwimSyncer: HcSwimSyncer

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        assetManager = mockk(relaxed = true)
        hcSwimSyncer = mockk(relaxed = true)
        every { assetManager.progress } returns MutableStateFlow(AssetSyncProgress.Idle)
        coEvery { assetManager.sync() } returns Result.success(Unit)
        coEvery { hcSwimSyncer.sync() } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = OnboardingPermissionViewModel(assetManager, hcSwimSyncer,
        CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `onPermissionGranted triggers assetManager sync`() = runTest {
        val viewModel = vm()
        viewModel.onPermissionGranted()

        // 권한 허용 직후 에셋 동기화가 트리거되어야 한다 (증상 1 — 권한 경로)
        coVerify { assetManager.sync() }
    }

    @Test
    fun `onPermissionGranted triggers HC sync`() = runTest {
        val viewModel = vm()
        viewModel.onPermissionGranted()

        // 권한 허용 직후 HC 동기화가 트리거되어야 한다 (증상 2 — 권한 경로)
        coVerify { hcSwimSyncer.sync() }
    }

    @Test
    fun `onPermissionGranted does not throw when assetManager sync fails`() = runTest {
        coEvery { assetManager.sync() } returns Result.failure(RuntimeException("network error"))

        val viewModel = vm()
        // 실패해도 예외가 전파되지 않아야 한다
        viewModel.onPermissionGranted()

        coVerify { assetManager.sync() }
    }

    @Test
    fun `onPermissionGranted does not throw when HC sync fails`() = runTest {
        coEvery { hcSwimSyncer.sync() } throws RuntimeException("HC error")

        val viewModel = vm()
        // HC 동기화 실패가 앱을 죽이지 않아야 한다
        viewModel.onPermissionGranted()

        coVerify { hcSwimSyncer.sync() }
    }
}
