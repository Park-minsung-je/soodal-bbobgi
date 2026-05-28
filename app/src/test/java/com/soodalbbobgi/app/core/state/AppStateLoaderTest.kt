package com.soodalbbobgi.app.core.state

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ApiResponse
import com.soodalbbobgi.app.data.remote.dto.GachaBoxesData
import com.soodalbbobgi.app.data.remote.dto.InventoryData
import com.soodalbbobgi.app.data.remote.dto.ServerCurrency
import com.soodalbbobgi.app.data.remote.dto.ServerGachaBox
import com.soodalbbobgi.app.data.remote.dto.ServerGachaBoxItem
import com.soodalbbobgi.app.data.remote.dto.ServerGachaResult
import com.soodalbbobgi.app.data.remote.dto.ServerInventoryItem
import com.soodalbbobgi.app.data.remote.dto.ServerProfileCard
import com.soodalbbobgi.app.data.remote.dto.ServerShopListing
import com.soodalbbobgi.app.data.remote.dto.ServerShopProduct
import com.soodalbbobgi.app.data.remote.dto.ShopListingsData
import com.soodalbbobgi.app.data.remote.dto.UserData
import com.soodalbbobgi.app.domain.model.Grade
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AppStateLoaderTest {
    private lateinit var api: SoodalApi
    private lateinit var session: UserSession
    private lateinit var state: AppState
    private lateinit var loader: AppStateLoader

    @Before
    fun setup() {
        api = mockk(relaxed = true)
        session = UserSession()
        state = AppState()
        loader = AppStateLoader(api, state, session)
    }

    @Test
    fun `loadAll populates profile currency inventory gachaBoxes profileCard`() = runTest {
        coEvery { api.getMe() } returns ApiResponse(true, sampleUser(), null)
        coEvery { api.getInventory(null) } returns ApiResponse(true, InventoryData(listOf(sampleInventory())), null)
        coEvery { api.getGachaBoxes() } returns ApiResponse(true, GachaBoxesData(listOf(sampleBox())), null)
        coEvery { api.getProfileCard() } returns ApiResponse(true, sampleProfileCard(), null)

        loader.loadAll()

        assertThat(state.profile.value?.nickname).isEqualTo("Soodal")
        assertThat(state.currency.value.shellBalance).isEqualTo(3)
        assertThat(state.currency.value.pearlBalance).isEqualTo(5)
        assertThat(state.inventory.value).hasSize(1)
        assertThat(state.gachaBoxes.value).hasSize(1)
        assertThat(state.items.value).containsKey(101L)
        assertThat(state.profileCard.value?.customText).isEqualTo("Hi")
        assertThat(session.userId).isEqualTo("u1")
    }

    @Test
    fun `refreshCurrency updates only currency`() = runTest {
        state.applyProfile(com.soodalbbobgi.app.domain.model.UserProfile("u1", "Old", null, null, "kakao"))
        coEvery { api.getMe() } returns ApiResponse(true, sampleUser(shells = 9), null)

        loader.refreshCurrency()

        assertThat(state.currency.value.shellBalance).isEqualTo(9)
    }

    @Test
    fun `applyGachaResults adds inventory items and updates currency`() = runTest {
        val results = listOf(
            ServerGachaResult(
                item = sampleBoxItem(id = 101L, name = "수달이"),
                wasNew = true, pearlsEarned = 0, shellsSpent = 1, pityCountAtPull = 1,
                historyId = 1L, inventoryItemId = 9000L,
            ),
            ServerGachaResult(
                item = sampleBoxItem(id = 102L, name = "튜브 수달"),
                wasNew = false, pearlsEarned = 3, shellsSpent = 0, pityCountAtPull = 2,
                historyId = 2L, inventoryItemId = null,
            ),
        )
        val currency = ServerCurrency(shellBalance = 2, pearlBalance = 8, pityCounter = 2)

        loader.applyGachaResults(results, currency)

        // 신규 1개만 인벤토리에 추가됨
        assertThat(state.inventory.value).hasSize(1)
        assertThat(state.inventory.value[0].id).isEqualTo(9000L)
        assertThat(state.inventory.value[0].itemId).isEqualTo(101L)
        // currency 갱신
        assertThat(state.currency.value.shellBalance).isEqualTo(2)
        assertThat(state.currency.value.pearlBalance).isEqualTo(8)
        assertThat(state.currency.value.pityCounter).isEqualTo(2)
        // items 마스터에 두 아이템 다 들어감
        assertThat(state.items.value).containsKey(101L)
        assertThat(state.items.value).containsKey(102L)
    }

    @Test
    fun `applyShellReward updates only shellBalance`() {
        state.applyCurrency(com.soodalbbobgi.app.domain.model.Currency(shellBalance = 1, pearlBalance = 5, pityCounter = 7))

        loader.applyShellReward(newBalance = 4)

        assertThat(state.currency.value.shellBalance).isEqualTo(4)
        assertThat(state.currency.value.pearlBalance).isEqualTo(5)
        assertThat(state.currency.value.pityCounter).isEqualTo(7)
    }

    @Test
    fun `applyServerCurrency replaces shell pearl pity but keeps lastShellGrantDate`() {
        state.applyCurrency(com.soodalbbobgi.app.domain.model.Currency(lastShellGrantDate = "2026-05-27"))
        loader.applyServerCurrency(ServerCurrency(shellBalance = 7, pearlBalance = 2, pityCounter = 8))

        assertThat(state.currency.value.shellBalance).isEqualTo(7)
        assertThat(state.currency.value.pearlBalance).isEqualTo(2)
        assertThat(state.currency.value.pityCounter).isEqualTo(8)
        assertThat(state.currency.value.lastShellGrantDate).isEqualTo("2026-05-27")
    }

    @Test
    fun `refreshShop populates listings and merges item products into master`() = runTest {
        val listing = ServerShopListing(
            id = 1L, productType = "item",
            product = ServerShopProduct(
                id = 200L, itemKey = "char_special", name = "Special",
                grade = "SSR", category = "char", imageAsset = "/x.png", isLimited = true,
            ),
            pearlPrice = 100, maxPerUser = 1, purchasedTotal = 0,
            maxPerPeriod = null, periodType = null, purchasedThisPeriod = 0,
            periodResetAt = null, startAt = null, endAt = null, canBuy = true,
        )
        coEvery { api.getShop() } returns ApiResponse(true, ShopListingsData(listOf(listing)), null)

        loader.refreshShop()

        assertThat(state.shopListings.value).hasSize(1)
        assertThat(state.shopListings.value[0].product.grade).isEqualTo(Grade.SSR)
        assertThat(state.items.value).containsKey(200L)
        assertThat(state.items.value[200L]!!.isLimited).isTrue()
    }

    @Test
    fun `applyProfileUpdate writes both profile and currency from UserData`() {
        loader.applyProfileUpdate(sampleUser(nickname = "Updated", shells = 7, gender = "female"))

        assertThat(state.profile.value?.nickname).isEqualTo("Updated")
        assertThat(state.profile.value?.gender).isEqualTo("female")
        assertThat(state.currency.value.shellBalance).isEqualTo(7)
    }

    @Test
    fun `loadAll failure leaves state untouched`() = runTest {
        coEvery { api.getMe() } throws RuntimeException("offline")

        val result = loader.loadAll()

        assertThat(result.isFailure).isTrue()
        assertThat(state.profile.value).isNull()
    }

    // ─── Fixtures ──────────────────────────────────

    private fun sampleUser(
        nickname: String? = "Soodal",
        shells: Int = 3,
        gender: String? = "male",
    ) = UserData(
        id = "u1", nickname = nickname,
        shellBalance = shells, pearlBalance = 5, pityCounter = 1,
        lastShellGrantDate = "2026-05-27",
        gender = gender, ageRange = "30s",
        authProvider = "kakao", createdAt = 0L,
    )

    private fun sampleInventory() = ServerInventoryItem(
        id = 9000L, itemId = 101L, grade = "N", category = "char",
        isEquippedAs = "NONE", acquiredAt = 0L,
    )

    private fun sampleBoxItem(id: Long, name: String) = ServerGachaBoxItem(
        id = id, itemKey = "key_$id", name = name, grade = "N",
        category = "char", weight = 1000, imageAsset = "/x.png", isLimited = false,
    )

    private fun sampleBox() = ServerGachaBox(
        id = 1L, name = "캐릭터 상자", description = "수달", category = "char",
        iconAsset = null, shellCost = 1, tenPullCost = 9,
        items = listOf(sampleBoxItem(101L, "수달이")),
    )

    private fun sampleProfileCard() = ServerProfileCard(
        backgroundItemId = null, characterItemId = 9000L, borderItemId = null,
        characterX = 0.2f, characterY = 0.1f, characterScale = 0.7f,
        customText = "Hi", textStyle = "REGULAR",
    )
}
