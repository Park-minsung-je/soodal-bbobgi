package kr.ilf.soodalbbobgi.presentation.collection

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.domain.model.InventoryItem
import kr.ilf.soodalbbobgi.domain.model.Item
import kr.ilf.soodalbbobgi.domain.model.ProfileCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * 도감의 보유/착용 판정 검증 — 특히 카드 슬롯 값(인벤토리 행 id)을
 * 아이템 마스터 id로 올바르게 변환하는지 (인벤토리 인다이렉션).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CollectionViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var appState: AppState

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appState = AppState()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `착용 표시는 인벤토리 행 id를 아이템 id로 변환해 판정한다`() = runTest(testDispatcher) {
        appState.mergeItems(listOf(
            Item(id = 1L, itemKey = "char_default", name = "기본 수달", grade = Grade.N, category = "char", imageAsset = null),
            Item(id = 2L, itemKey = "char_1", name = "알로하 수달", grade = Grade.R, category = "char", imageAsset = null),
            Item(id = 13L, itemKey = "bg_0", name = "비치 풀장", grade = Grade.N, category = "bg", imageAsset = null),
        ))
        appState.applyInventory(listOf(
            InventoryItem(id = 1L, userId = "u", itemId = 1L, grade = Grade.N, category = "char", acquiredAt = 0L),
            InventoryItem(id = 2L, userId = "u", itemId = 13L, grade = Grade.N, category = "bg", acquiredAt = 0L),
        ))
        // 카드 슬롯에는 인벤토리 행 id가 들어간다: 캐릭터=inv 1(→item 1), 배경=inv 2(→item 13)
        appState.applyProfileCard(ProfileCard(userId = "u", characterItemId = 1L, backgroundItemId = 2L))

        val vm = CollectionViewModel(appState)
        backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
        advanceUntilIdle()

        val entries = vm.uiState.value.entries.associateBy { it.id }
        assertThat(entries[1L]!!.equipped).isTrue()   // 기본 수달 착용
        assertThat(entries[13L]!!.equipped).isTrue()  // 비치 풀장 착용
        // 버그였던 케이스: 배경 슬롯의 inv id=2가 아이템 id 2(알로하)로 오인되면 안 된다
        assertThat(entries[2L]!!.equipped).isFalse()
        assertThat(entries[2L]!!.owned).isFalse()
    }
}
