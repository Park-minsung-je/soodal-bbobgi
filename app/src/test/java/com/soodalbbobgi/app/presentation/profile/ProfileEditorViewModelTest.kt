package com.soodalbbobgi.app.presentation.profile

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ApiResponse
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.model.Item
import com.soodalbbobgi.app.domain.model.ProfileCard
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
 * ProfileEditorViewModel.uiState는 stateIn(WhileSubscribed) 기반이라 구독자가 없으면
 * 업스트림 combine이 돌지 않는다. 각 테스트에서 backgroundScope에 collect를 띄워
 * 실제 매핑/시드 로직이 동작하도록 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditorViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var api: SoodalApi
    private lateinit var appState: AppState
    private lateinit var loader: AppStateLoader
    private lateinit var session: UserSession

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk(relaxed = true)
        coEvery { api.getInventory(null) } returns ApiResponse(true, null, null)
        coEvery { api.getGachaBoxes() } returns ApiResponse(true, null, null)
        coEvery { api.getProfileCard() } returns ApiResponse(true, null, null)
        appState = AppState()
        session = UserSession().apply { setAuthenticatedUser("u1") }
        loader = AppStateLoader(api, appState, session)
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun TestScope.startCollect(vm: ProfileEditorViewModel) {
        backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
    }

    @Test
    fun `uiState builds grids by category from inventory + items master`() = runTest(testDispatcher) {
        appState.applyInventory(listOf(
            inv(1L, 101L, "char"),
            inv(2L, 201L, "bg"),
            inv(3L, 301L, "frame"),
        ))
        appState.mergeItems(listOf(
            item(101L, "수달이", "char"),
            item(201L, "비치 풀장", "bg"),
            item(301L, "야자수 테두리", "frame"),
        ))

        val vm = ProfileEditorViewModel(session, appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        assertThat(vm.uiState.value.charItems).hasSize(1)
        assertThat(vm.uiState.value.bgItems).hasSize(1)
        assertThat(vm.uiState.value.frameItems).hasSize(1)
        assertThat(vm.uiState.value.charItems[0].name).isEqualTo("수달이")
    }

    @Test
    fun `initial state seeded from saved profile card`() = runTest(testDispatcher) {
        appState.applyProfileCard(ProfileCard(
            userId = "u1",
            characterItemId = 1L,
            customText = "안녕",
            characterX = 0.3f, characterY = 0.2f, characterScale = 0.8f,
        ))

        val vm = ProfileEditorViewModel(session, appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertThat(s.selectedCharInventoryId).isEqualTo(1L)
        assertThat(s.customText).isEqualTo("안녕")
        assertThat(s.charX).isEqualTo(0.3f)
        assertThat(s.charY).isEqualTo(0.2f)
        assertThat(s.charScale).isEqualTo(0.8f)
    }

    @Test
    fun `selectItem updates selected id for given category`() = runTest(testDispatcher) {
        appState.applyInventory(listOf(inv(1L, 101L, "char"), inv(2L, 102L, "char")))
        appState.mergeItems(listOf(item(101L, "A", "char"), item(102L, "B", "char")))

        val vm = ProfileEditorViewModel(session, appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        vm.selectItem(EditorCategory.Character, 2L)
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedCharInventoryId).isEqualTo(2L)
    }

    private fun inv(id: Long, itemId: Long, category: String) = InventoryItem(
        id = id, userId = "u1", itemId = itemId, grade = Grade.N,
        category = category, isEquippedAs = "NONE", acquiredAt = 0L,
    )

    private fun item(id: Long, name: String, category: String) = Item(
        id = id, itemKey = "k_$id", name = name, grade = Grade.N,
        category = category, imageAsset = null,
    )
}
