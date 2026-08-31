package kr.ilf.soodalbbobgi.presentation.onboarding

import com.google.common.truth.Truth.assertThat
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
    private lateinit var healthConnectManager: kr.ilf.soodalbbobgi.data.health.HealthConnectManager

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        assetManager = mockk(relaxed = true)
        hcSwimSyncer = mockk(relaxed = true)
        healthConnectManager = mockk(relaxed = true)
        every { assetManager.progress } returns MutableStateFlow(AssetSyncProgress.Idle)
        coEvery { assetManager.sync() } returns Result.success(Unit)
        coEvery { hcSwimSyncer.sync() } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun vm() = OnboardingPermissionViewModel(assetManager, hcSwimSyncer,
        healthConnectManager, CoroutineScope(UnconfinedTestDispatcher()))

    @Test
    fun `syncAfterPermission triggers assetManager sync`() = runTest {
        val viewModel = vm()
        viewModel.syncAfterPermission()

        // 권한 허용 직후 에셋 동기화가 트리거되어야 한다 (앱 스코프 백그라운드)
        coVerify { assetManager.sync() }
    }

    @Test
    fun `syncAfterPermission triggers HC sync and returns null on success`() = runTest {
        val viewModel = vm()
        val code = viewModel.syncAfterPermission()

        coVerify { hcSwimSyncer.sync() }
        assertThat(code).isNull()
    }

    @Test
    fun `syncAfterPermission returns a code when HC sync fails`() = runTest {
        coEvery { hcSwimSyncer.sync() } throws RuntimeException("HC error")

        val viewModel = vm()
        val code = viewModel.syncAfterPermission()

        // 실패는 예외로 터지지 않고 3자리 코드로 화면에 전달된다
        assertThat(code).isEqualTo("900")
    }

    @Test
    fun `syncAfterPermission survives an asset sync failure`() = runTest {
        coEvery { assetManager.sync() } returns Result.failure(RuntimeException("network error"))

        val viewModel = vm()
        val code = viewModel.syncAfterPermission()

        // 에셋 실패는 백그라운드라 결과에 영향 없음 — HC 성공이면 null
        assertThat(code).isNull()
    }
}
