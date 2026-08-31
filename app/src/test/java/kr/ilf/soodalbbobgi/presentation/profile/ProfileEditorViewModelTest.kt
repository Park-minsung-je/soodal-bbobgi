package kr.ilf.soodalbbobgi.presentation.profile

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.ApiResponse
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.domain.model.InventoryItem
import kr.ilf.soodalbbobgi.domain.model.Item
import kr.ilf.soodalbbobgi.domain.model.ProfileCard
import kr.ilf.soodalbbobgi.domain.model.SwimStats
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
    private lateinit var swimLogUseCase: SwimLogUseCase

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
        swimLogUseCase = mockk(relaxed = true)
        coEvery { swimLogUseCase.getMonthStats(any(), any()) } returns SwimStats(0, 0, 0)
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

        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()

        assertThat(vm.uiState.value.charItems).hasSize(1)
        assertThat(vm.uiState.value.bgItems).hasSize(1)
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

        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
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
    fun `fresh state defaults to centered character at full scale`() = runTest(testDispatcher) {
        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertThat(s.charX).isEqualTo(0.5f)
        assertThat(s.charY).isEqualTo(0.5f)
        assertThat(s.charScale).isEqualTo(1.0f)
    }

    @Test
    fun `selectItem updates selected id for given category`() = runTest(testDispatcher) {
        appState.applyInventory(listOf(inv(1L, 101L, "char"), inv(2L, 102L, "char")))
        appState.mergeItems(listOf(item(101L, "A", "char"), item(102L, "B", "char")))

        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()

        vm.selectItem(EditorCategory.Character, 2L)
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedCharInventoryId).isEqualTo(2L)
    }

    @Test
    fun `selectItem with null clears background slot`() = runTest(testDispatcher) {
        appState.applyProfileCard(ProfileCard(
            userId = "u1",
            backgroundItemId = 2L,
        ))

        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()
        assertThat(vm.uiState.value.selectedBgInventoryId).isEqualTo(2L)

        vm.selectItem(EditorCategory.Background, null)
        advanceUntilIdle()

        assertThat(vm.uiState.value.selectedBgInventoryId).isNull()
    }

    @Test
    fun `text elements default to spec defaults on fresh state`() = runTest(testDispatcher) {
        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertThat(s.nicknameEl.show).isTrue()
        assertThat(s.nicknameEl.scaleStep).isEqualTo(3)
        assertThat(s.nicknameEl.pill).isEqualTo("WHITE")
        assertThat(s.nicknameEl.color).isEqualTo("#000000")
        assertThat(s.taglineEl.pill).isEqualTo("NONE")
        assertThat(s.statsEl.show).isTrue()
        assertThat(s.statsEl.pill).isEqualTo("BLUR")
        assertThat(s.statsEl.color).isEqualTo("#00F5FF")
    }

    @Test
    fun `text elements seeded from saved profile card`() = runTest(testDispatcher) {
        appState.applyProfileCard(ProfileCard(
            userId = "u1",
            showNickname = false,
            nicknameX = 0.2f, nicknameY = 0.3f, nicknameScaleStep = 5,
            nicknameColor = "#FF0000",
            taglineX = 0.4f, taglineY = 0.6f,
            showStats = false, statsX = 0.7f, statsY = 0.8f,
            statsColor = "#0000FF",
        ))

        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()

        val s = vm.uiState.value
        assertThat(s.nicknameEl.show).isFalse()
        assertThat(s.nicknameEl.x).isEqualTo(0.2f)
        assertThat(s.nicknameEl.y).isEqualTo(0.3f)
        assertThat(s.nicknameEl.scaleStep).isEqualTo(5)
        assertThat(s.nicknameEl.color).isEqualTo("#FF0000")
        assertThat(s.taglineEl.x).isEqualTo(0.4f)
        assertThat(s.taglineEl.y).isEqualTo(0.6f)
        assertThat(s.statsEl.show).isFalse()
        assertThat(s.statsEl.x).isEqualTo(0.7f)
        assertThat(s.statsEl.y).isEqualTo(0.8f)
        assertThat(s.statsEl.color).isEqualTo("#0000FF")
    }

    @Test
    fun `element position setters update uiState and clamp to 0 to 1`() = runTest(testDispatcher) {
        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()

        vm.setElementX(TextElement.Nickname, 0.3f)
        vm.setElementY(TextElement.Nickname, 0.7f)
        advanceUntilIdle()
        assertThat(vm.uiState.value.nicknameEl.x).isEqualTo(0.3f)
        assertThat(vm.uiState.value.nicknameEl.y).isEqualTo(0.7f)

        vm.setElementX(TextElement.Stats, 1.5f)
        vm.setElementY(TextElement.Stats, -0.5f)
        advanceUntilIdle()
        assertThat(vm.uiState.value.statsEl.x).isEqualTo(1.0f)
        assertThat(vm.uiState.value.statsEl.y).isEqualTo(0.0f)
    }

    @Test
    fun `element setters only touch their target element`() = runTest(testDispatcher) {
        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()

        vm.setElementShow(TextElement.Tagline, false)
        vm.setElementScaleStep(TextElement.Tagline, 4)
        vm.setElementPill(TextElement.Tagline, "BLACK")
        vm.setElementColor(TextElement.Tagline, "#ABCDEF")
        advanceUntilIdle()

        val s = vm.uiState.value
        assertThat(s.taglineEl.show).isFalse()
        assertThat(s.taglineEl.scaleStep).isEqualTo(4)
        assertThat(s.taglineEl.pill).isEqualTo("BLACK")
        assertThat(s.taglineEl.color).isEqualTo("#ABCDEF")
        // 다른 요소는 그대로
        assertThat(s.nicknameEl.show).isTrue()
        assertThat(s.statsEl.pill).isEqualTo("BLUR")
    }

    @Test
    fun `statsText reflects month stats in Home format`() = runTest(testDispatcher) {
        coEvery { swimLogUseCase.getMonthStats(any(), any()) } returns SwimStats(1200, 3, 200)

        val vm = ProfileEditorViewModel(session, appState, loader, api, swimLogUseCase)
        startCollect(vm)
        advanceUntilIdle()

        assertThat(vm.uiState.value.statsText).isEqualTo("1200m · 3회")
    }

    @Test
    fun `combineTextStyle maps bold and italic flags to style string`() {
        assertThat(combineTextStyle(bold = false, italic = false)).isEqualTo("REGULAR")
        assertThat(combineTextStyle(bold = true, italic = false)).isEqualTo("BOLD")
        assertThat(combineTextStyle(bold = false, italic = true)).isEqualTo("ITALIC")
        assertThat(combineTextStyle(bold = true, italic = true)).isEqualTo("BOLD_ITALIC")
    }

    @Test
    fun `textStyle flag readers detect bold and italic independently`() {
        assertThat(textStyleHasBold("BOLD")).isTrue()
        assertThat(textStyleHasBold("BOLD_ITALIC")).isTrue()
        assertThat(textStyleHasBold("ITALIC")).isFalse()
        assertThat(textStyleHasBold("REGULAR")).isFalse()

        assertThat(textStyleHasItalic("ITALIC")).isTrue()
        assertThat(textStyleHasItalic("BOLD_ITALIC")).isTrue()
        assertThat(textStyleHasItalic("BOLD")).isFalse()
        assertThat(textStyleHasItalic("REGULAR")).isFalse()
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
