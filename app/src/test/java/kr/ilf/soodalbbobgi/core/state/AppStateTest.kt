package kr.ilf.soodalbbobgi.core.state

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.domain.model.Currency
import kr.ilf.soodalbbobgi.domain.model.GachaBoxWithDrops
import kr.ilf.soodalbbobgi.domain.model.GachaDrop
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.domain.model.InventoryItem
import kr.ilf.soodalbbobgi.domain.model.Item
import kr.ilf.soodalbbobgi.domain.model.ProfileCard
import kr.ilf.soodalbbobgi.domain.model.UserProfile
import org.junit.Before
import org.junit.Test

class AppStateTest {
    private lateinit var state: AppState

    @Before
    fun setup() {
        state = AppState()
    }

    @Test
    fun `initial state has empty values`() {
        assertThat(state.profile.value).isNull()
        assertThat(state.currency.value).isEqualTo(Currency())
        assertThat(state.inventory.value).isEmpty()
        assertThat(state.items.value).isEmpty()
        assertThat(state.gachaBoxes.value).isEmpty()
        assertThat(state.shopListings.value).isEmpty()
        assertThat(state.profileCard.value).isNull()
        assertThat(state.pendingShellReward.value).isEqualTo(0)
    }

    @Test
    fun `applyProfile sets profile flow`() {
        val p = UserProfile("u1", "Soo", "male", "30s", "kakao")
        state.applyProfile(p)
        assertThat(state.profile.value).isEqualTo(p)
    }

    @Test
    fun `applyCurrency replaces currency value`() {
        state.applyCurrency(Currency(shellBalance = 5, pearlBalance = 10, pityCounter = 3))
        val c = state.currency.value
        assertThat(c.shellBalance).isEqualTo(5)
        assertThat(c.pearlBalance).isEqualTo(10)
        assertThat(c.pityCounter).isEqualTo(3)
    }

    @Test
    fun `applyInventory replaces inventory list`() {
        val items = listOf(inventoryItem(1L, 101L), inventoryItem(2L, 102L))
        state.applyInventory(items)
        assertThat(state.inventory.value).hasSize(2)
        assertThat(state.inventory.value.map { it.itemId }).containsExactly(101L, 102L)
    }

    @Test
    fun `applyNewInventoryItems appends without removing existing`() {
        state.applyInventory(listOf(inventoryItem(1L, 101L)))
        state.applyNewInventoryItems(listOf(inventoryItem(2L, 102L)))
        assertThat(state.inventory.value).hasSize(2)
    }

    @Test
    fun `mergeItems adds new entries and overwrites duplicates`() {
        val a = item(1L, "A")
        val b = item(2L, "B")
        state.mergeItems(listOf(a, b))
        assertThat(state.items.value).hasSize(2)

        val aRenamed = a.copy(name = "A2")
        state.mergeItems(listOf(aRenamed))
        assertThat(state.items.value[1L]!!.name).isEqualTo("A2")
        assertThat(state.items.value[2L]!!.name).isEqualTo("B")
    }

    @Test
    fun `applyGachaBoxes also merges items from drops into items cache`() {
        val box = GachaBoxWithDrops(
            id = 1L, name = "캐릭터 상자", description = "", category = "char",
            iconAsset = null, shellCost = 1, tenPullCost = 9,
            drops = listOf(
                GachaDrop(item(10L, "수달이"), weight = 1000),
                GachaDrop(item(11L, "튜브 수달"), weight = 500),
            ),
        )
        state.applyGachaBoxes(listOf(box))
        assertThat(state.gachaBoxes.value).hasSize(1)
        assertThat(state.items.value.keys).containsExactly(10L, 11L)
    }

    @Test
    fun `applyProfileCard sets profile card`() {
        val card = ProfileCard(userId = "u1", customText = "안녕")
        state.applyProfileCard(card)
        assertThat(state.profileCard.value).isEqualTo(card)
    }

    @Test
    fun `addPendingShellReward accumulates and consume returns and resets`() {
        state.addPendingShellReward(2)
        state.addPendingShellReward(3)
        assertThat(state.pendingShellReward.value).isEqualTo(5)

        val v = state.consumePendingShellReward()
        assertThat(v).isEqualTo(5)
        assertThat(state.pendingShellReward.value).isEqualTo(0)
    }

    @Test
    fun `clear resets everything`() {
        state.applyProfile(UserProfile("u1", "Soo", null, null, "kakao"))
        state.applyCurrency(Currency(shellBalance = 10))
        state.applyInventory(listOf(inventoryItem(1L, 101L)))
        state.mergeItems(listOf(item(101L, "A")))
        state.addPendingShellReward(2)

        state.clear()

        assertThat(state.profile.value).isNull()
        assertThat(state.currency.value).isEqualTo(Currency())
        assertThat(state.inventory.value).isEmpty()
        assertThat(state.items.value).isEmpty()
        assertThat(state.pendingShellReward.value).isEqualTo(0)
    }

    // ─── 헬퍼 ──────────────────────────────────

    private fun inventoryItem(id: Long, itemId: Long) = InventoryItem(
        id = id, userId = "u1", itemId = itemId, grade = Grade.N,
        category = "char", isEquippedAs = "NONE", acquiredAt = 0L,
    )

    private fun item(id: Long, name: String) = Item(
        id = id, itemKey = "key_$id", name = name, grade = Grade.N,
        category = "char", imageAsset = null,
    )
}
