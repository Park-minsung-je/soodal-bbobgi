package kr.ilf.soodalbbobgi.presentation.profile

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.domain.model.InventoryItem
import kr.ilf.soodalbbobgi.domain.model.Item
import kr.ilf.soodalbbobgi.domain.model.ProfileCard
import kr.ilf.soodalbbobgi.domain.model.SwimStats
import kr.ilf.soodalbbobgi.domain.model.UserProfile
import kr.ilf.soodalbbobgi.domain.usecase.SwimLogUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * ProfileFullscreenViewModel.cardState는 stateIn(WhileSubscribed) 기반이라 구독자가
 * 없으면 업스트림 combine이 돌지 않는다. 각 테스트에서 backgroundScope에 collect를
 * 띄워 매핑 로직이 동작하도록 한다.
 *
 * 검증 목적: 저장된 ProfileCard + 인벤토리 + 아이템 마스터를 FullscreenCardState로
 * 정확히 매핑하는지 확인 (Fullscreen 카드 빈 표시 회귀 방지). 인벤토리 인다이렉션
 * (서버가 inv.id를 슬롯에 저장) 동작도 함께 검증한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileFullscreenViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var appState: AppState
    private lateinit var context: Context
    private lateinit var swimLogUseCase: SwimLogUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appState = AppState()
        context = mockk(relaxed = true)
        swimLogUseCase = mockk(relaxed = true)
        // 기본: 0 통계 (개별 테스트에서 필요 시 덮어씀)
        coEvery { swimLogUseCase.getMonthStats(any(), any()) } returns
            SwimStats(totalDistanceMeters = 0, swimCount = 0, totalCalories = 0)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun TestScope.startCollect(vm: ProfileFullscreenViewModel) {
        backgroundScope.launch(testDispatcher) { vm.cardState.collect {} }
    }

    @Test
    fun `cardState binds saved ProfileCard via inventory indirection`() =
        runTest(testDispatcher) {
            appState.applyProfile(UserProfile(
                id = "u1",
                nickname = "수달이",
                gender = null,
                ageRange = null,
                authProvider = "google",
            ))
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
                characterItemId = 100L, // 서버가 저장한 inventory.id
                characterX = 0.3f,
                characterY = 0.4f,
                characterScale = 0.85f,
                customText = "오늘도 한 바퀴",
            ))

            val vm = ProfileFullscreenViewModel(
                context = context,
                appState = appState,
                swimLogUseCase = swimLogUseCase,
            )
            startCollect(vm)
            advanceUntilIdle()

            val s = vm.cardState.value!!
            assertThat(s.nickname).isEqualTo("수달이")
            assertThat(s.charAsset).isEqualTo("/assets/char/c1.png")
            assertThat(s.bgAsset).isNull()
            assertThat(s.charX).isEqualTo(0.3f)
            assertThat(s.charY).isEqualTo(0.4f)
            assertThat(s.charScale).isEqualTo(0.85f)
            assertThat(s.tagline).isEqualTo("오늘도 한 바퀴")
        }

    @Test
    fun `cardState reflects month stats in Home format`() =
        runTest(testDispatcher) {
            coEvery { swimLogUseCase.getMonthStats(any(), any()) } returns
                SwimStats(totalDistanceMeters = 1200, swimCount = 3, totalCalories = 200)

            val vm = ProfileFullscreenViewModel(
                context = context,
                appState = appState,
                swimLogUseCase = swimLogUseCase,
            )
            startCollect(vm)
            advanceUntilIdle()

            assertThat(vm.cardState.value!!.statsText).isEqualTo("1200m · 3회")
        }

    @Test
    fun `cardState falls back to defaults when nothing is saved`() =
        runTest(testDispatcher) {
            val vm = ProfileFullscreenViewModel(
                context = context,
                appState = appState,
                swimLogUseCase = swimLogUseCase,
            )
            startCollect(vm)
            advanceUntilIdle()

            val s = vm.cardState.value!!
            assertThat(s.nickname).isEmpty()
            assertThat(s.tagline).isEqualTo("수영을 사랑하는 수달")
            assertThat(s.bgAsset).isNull()
            assertThat(s.charAsset).isNull()
        }
}
