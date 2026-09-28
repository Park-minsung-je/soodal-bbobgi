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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
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

    private fun httpError(code: Int, body: String) =
        HttpException(Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())))

    private val errorMessage: String
        get() = (vm.saveState.value as OnboardingSaveState.Error).message

    @Test
    fun `서버가 닉네임 중복으로 거절하면 서버 문구를 그대로 보여준다`() = runTest {
        // 길이·문자는 앱이 먼저 막으므로 실제로 서버가 거절하는 건 중복(409)뿐인데,
        // Retrofit이 예외로 던져 "네트워크 오류"로 보이던 문제 (R46)
        coEvery { api.updateMe(any()) } throws httpError(
            409,
            """{"success":false,"error":{"code":"NICKNAME_TAKEN","message":"이미 사용 중인 닉네임입니다."}}""",
        )

        vm.saveProfile("수달잉", null, null)

        assertThat(errorMessage).isEqualTo("이미 사용 중인 닉네임입니다.")
        assertThat(appState.profile.value).isNull()
    }

    @Test
    fun `네트워크 예외는 기존 문구`() = runTest {
        coEvery { api.updateMe(any()) } throws IOException("down")

        vm.saveProfile("수달잉", null, null)

        assertThat(errorMessage).isEqualTo("네트워크 오류가 발생했어요.")
    }
}
