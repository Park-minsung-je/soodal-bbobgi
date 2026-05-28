package com.soodalbbobgi.app.presentation.home

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.health.HcSyncPreferences
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.model.Item
import com.soodalbbobgi.app.domain.model.ProfileCard
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.model.SwimStats
import com.soodalbbobgi.app.domain.model.UserProfile
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * HomeViewModel.uiState는 stateIn(WhileSubscribed) 기반이라 구독자가 없으면
 * 업스트림 combine이 돌지 않는다. 각 테스트에서 backgroundScope에 collect를 띄워
 * 실제 매핑 로직이 동작하도록 한다.
 *
 * 검증 목적: 저장된 ProfileCard + 아이템 마스터를 HomeUiState의 카드 필드로
 * 정확히 매핑하는지 확인 (Home 화면 카드 즉시 반영 버그 회귀 방지).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var api: SoodalApi
    private lateinit var appState: AppState
    private lateinit var loader: AppStateLoader
    private lateinit var session: UserSession
    private lateinit var swimLogUseCase: SwimLogUseCase
    private lateinit var hcManager: HealthConnectManager
    private lateinit var hcPrefs: HcSyncPreferences

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk(relaxed = true)
        appState = AppState()
        session = UserSession().apply { setAuthenticatedUser("u1") }
        loader = AppStateLoader(api, appState, session)
        swimLogUseCase = mockk(relaxed = true)
        hcManager = mockk(relaxed = true)
        hcPrefs = mockk(relaxed = true)

        // 기본: 비어 있는 월간 로그 + 0 통계
        every { swimLogUseCase.getLogsByDateRange(any(), any()) } returns flowOf(emptyList())
        coEvery { swimLogUseCase.getMonthStats(any(), any()) } returns
            SwimStats(totalDistanceMeters = 0, swimCount = 0, totalCalories = 0)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun TestScope.startCollect(vm: HomeViewModel) {
        backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
    }

    @Test
    fun `uiState binds saved ProfileCard fields and asset paths from items master`() =
        runTest(testDispatcher) {
            // Given: 사용자 프로필 + 저장된 카드 + 아이템 마스터에 캐릭터 이미지 경로
            appState.applyProfile(UserProfile(
                id = "u1",
                nickname = "수달이",
                gender = null,
                ageRange = null,
                authProvider = "google",
            ))
            // 서버가 ProfileCard 슬롯에 inventory.id (PK)를 저장한다는 점을 반영해서
            // inventory.id != itemId 로 분리한 레이아웃으로 검증한다.
            appState.mergeItems(listOf(
                Item(
                    id = 42L, itemKey = "char_42", name = "수달 캐릭터",
                    grade = Grade.SR, category = "char",
                    imageAsset = "/assets/char/c1.png",
                ),
            ))
            appState.applyInventory(listOf(
                InventoryItem(
                    id = 100L,
                    userId = "u1",
                    itemId = 42L,
                    grade = Grade.SR,
                    category = "char",
                    acquiredAt = 0L,
                ),
            ))
            appState.applyProfileCard(ProfileCard(
                userId = "u1",
                characterItemId = 100L, // 서버가 저장한 inventory.id (← itemId 42 가 아님)
                characterX = 0.3f,
                characterY = 0.4f,
                characterScale = 0.85f,
                customText = "오늘도 한 바퀴",
            ))
            coEvery { swimLogUseCase.getMonthStats(any(), any()) } returns
                SwimStats(totalDistanceMeters = 1200, swimCount = 3, totalCalories = 200)

            val vm = HomeViewModel(
                userSession = session,
                appState = appState,
                appStateLoader = loader,
                swimLogUseCase = swimLogUseCase,
                healthConnectManager = hcManager,
                soodalApi = api,
                hcSyncPreferences = hcPrefs,
            )
            startCollect(vm)
            advanceUntilIdle()

            // Then: 카드 필드가 ProfileCard + Items 마스터에서 정확히 도출돼야 함
            val s = vm.uiState.value
            assertThat(s.cardNickname).isEqualTo("수달이")
            assertThat(s.cardCharAsset).isEqualTo("/assets/char/c1.png")
            assertThat(s.cardBgAsset).isNull()
            assertThat(s.cardFrameAsset).isNull()
            assertThat(s.cardCharX).isEqualTo(0.3f)
            assertThat(s.cardCharY).isEqualTo(0.4f)
            assertThat(s.cardCharScale).isEqualTo(0.85f)
            assertThat(s.cardTagline).isEqualTo("오늘도 한 바퀴")
            assertThat(s.cardStats).isEqualTo("1200m · 3회")
        }
}
