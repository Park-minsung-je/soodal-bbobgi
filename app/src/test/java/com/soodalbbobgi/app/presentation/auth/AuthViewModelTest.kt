package com.soodalbbobgi.app.presentation.auth

import android.app.Activity
import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.auth.GoogleAuthManager
import com.soodalbbobgi.app.data.auth.KakaoAuthManager
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ApiError
import com.soodalbbobgi.app.data.remote.dto.ApiResponse
import com.soodalbbobgi.app.data.remote.dto.AuthData
import com.soodalbbobgi.app.data.remote.dto.GoogleAuthRequest
import com.soodalbbobgi.app.data.remote.dto.UserData
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

/**
 * AuthViewModel의 Google 로그인 분기에 대한 단위 테스트.
 *
 * 카카오 분기는 패턴이 동일하므로 한쪽 검증으로 양쪽 신뢰도를 확보한다.
 * Credential Manager/네트워크/Health Connect는 모두 mockk로 대체.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    private lateinit var kakao: KakaoAuthManager
    private lateinit var google: GoogleAuthManager
    private lateinit var api: SoodalApi
    private lateinit var tokenStore: TokenStore
    private lateinit var appStateLoader: AppStateLoader
    private lateinit var hc: HealthConnectManager
    private lateinit var activity: Activity

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        kakao = mockk(relaxed = true)
        google = mockk(relaxed = true)
        api = mockk(relaxed = true)
        tokenStore = mockk(relaxed = true)
        appStateLoader = mockk(relaxed = true)
        hc = mockk(relaxed = true)
        activity = mockk(relaxed = true)
        coEvery { appStateLoader.loadAll() } returns Result.success(Unit)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = AuthViewModel(kakao, google, api, tokenStore, appStateLoader, hc)

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

    private fun sampleUser(nickname: String?) = UserData(
        id = "u_1", nickname = nickname,
        shellBalance = 0, pearlBalance = 0, pityCounter = 0,
        lastShellGrantDate = null, gender = null, ageRange = null,
        authProvider = "google", createdAt = 0L,
    )
}
