package kr.ilf.soodalbbobgi.presentation.onboarding

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.ApiResponse
import kr.ilf.soodalbbobgi.data.remote.dto.UpdateUserRequest
import kr.ilf.soodalbbobgi.data.remote.dto.UserData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private lateinit var api: SoodalApi
    private lateinit var appState: AppState
    private lateinit var loader: AppStateLoader
    private lateinit var vm: OnboardingViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        api = mockk(relaxed = true)
        appState = AppState()
        loader = AppStateLoader(api, appState, UserSession())
        vm = OnboardingViewModel(api, loader)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `saveProfile success updates AppState and emits Success`() = runTest {
        val updated = UserData(
            id = "u1", nickname = "수달잉", shellBalance = 0, pearlBalance = 0,
            pityCounter = 0, lastShellGrantDate = null,
            gender = "male", ageRange = "30s",
            authProvider = "kakao", createdAt = 0L,
        )
        coEvery { api.updateMe(any()) } returns ApiResponse(true, updated, null)

        vm.saveProfile("수달잉", "male", "30s")

        assertThat(vm.saveState.value).isEqualTo(OnboardingSaveState.Success)
        assertThat(appState.profile.value?.nickname).isEqualTo("수달잉")
        assertThat(appState.profile.value?.gender).isEqualTo("male")
        assertThat(appState.profile.value?.ageRange).isEqualTo("30s")
        coVerify { api.updateMe(UpdateUserRequest("수달잉", "male", "30s")) }
    }

    @Test
    fun `saveProfile error emits Error and keeps AppState empty`() = runTest {
        coEvery { api.updateMe(any()) } throws RuntimeException("boom")

        vm.saveProfile("수달잉", null, null)

        assertThat(vm.saveState.value).isInstanceOf(OnboardingSaveState.Error::class.java)
        assertThat(appState.profile.value).isNull()
    }
}
