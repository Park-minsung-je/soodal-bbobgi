package com.soodalbbobgi.app.presentation.shop

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ApiResponse
import com.soodalbbobgi.app.data.remote.dto.ServerCurrency
import com.soodalbbobgi.app.data.remote.dto.ShopPurchaseData
import com.soodalbbobgi.app.data.remote.dto.ShopPurchaseRequest
import com.soodalbbobgi.app.domain.model.Currency
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.ShopListingDomain
import com.soodalbbobgi.app.domain.model.ShopProduct
import io.mockk.coEvery
import io.mockk.coVerify
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
 * ShopViewModel.uiState는 stateIn(WhileSubscribed)로 만들어졌기 때문에
 * 구독자가 없으면 초기값만 노출된다. 각 테스트에서 backgroundScope에 collect를 띄워
 * 업스트림 combine 블록이 실제로 돌도록 한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var api: SoodalApi
    private lateinit var appState: AppState
    private lateinit var loader: AppStateLoader

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        api = mockk(relaxed = true)
        coEvery { api.getMe() } returns ApiResponse(true, null, null)
        coEvery { api.getShop() } returns ApiResponse(true, null, null)
        appState = AppState()
        loader = AppStateLoader(api, appState, UserSession())
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun TestScope.startCollect(vm: ShopViewModel) {
        backgroundScope.launch(testDispatcher) { vm.uiState.collect {} }
    }

    @Test
    fun `uiState maps AppState shopListings into ShopItem with derived icon`() = runTest(testDispatcher) {
        appState.applyShopListings(listOf(sampleListing(id = 1, category = "char")))
        val vm = ShopViewModel(UserSession(), appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        assertThat(vm.uiState.value.listings).hasSize(1)
        assertThat(vm.uiState.value.listings[0].shopListingId).isEqualTo(1L)
    }

    @Test
    fun `selectForPurchase ignores when canBuy is false`() = runTest(testDispatcher) {
        val vm = ShopViewModel(UserSession(), appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        val item = ShopItem(
            shopListingId = 1L, productType = "item", name = "X", description = "",
            icon = com.soodalbbobgi.app.core.ui.SoodalIcons.Otter, imageAsset = null,
            grade = Grade.N, price = 5, maxPerUser = 1, purchasedTotal = 1,
            maxPerPeriod = null, periodType = null, purchasedThisPeriod = 0,
            periodResetAt = null, isLimited = false, canBuy = false,
        )
        vm.selectForPurchase(item)
        assertThat(vm.uiState.value.confirmItem).isNull()
    }

    @Test
    fun `confirmPurchase with insufficient pearls sets error and clears confirm`() = runTest(testDispatcher) {
        appState.applyCurrency(Currency(pearlBalance = 1))
        appState.applyShopListings(listOf(sampleListing(id = 1, category = "char", price = 5)))
        val vm = ShopViewModel(UserSession(), appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        val item = vm.uiState.value.listings[0]
        vm.selectForPurchase(item)
        vm.confirmPurchase()
        advanceUntilIdle()

        assertThat(vm.uiState.value.error).isEqualTo("진주가 부족해요")
        assertThat(vm.uiState.value.confirmItem).isNull()
        coVerify(exactly = 0) { api.shopPurchase(any()) }
    }

    @Test
    fun `confirmPurchase success applies new currency`() = runTest(testDispatcher) {
        appState.applyCurrency(Currency(pearlBalance = 50))
        appState.applyShopListings(listOf(sampleListing(id = 1, category = "char", price = 5)))
        coEvery { api.shopPurchase(any<ShopPurchaseRequest>()) } returns ApiResponse(
            success = true,
            data = ShopPurchaseData(
                shopListingId = 1L, productType = "item",
                inventoryItemId = 9000L, gachaHistoryId = null,
                acquiredItems = null,
                currency = ServerCurrency(shellBalance = 0, pearlBalance = 45, pityCounter = 0),
            ),
            error = null,
        )

        val vm = ShopViewModel(UserSession(), appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        val item = vm.uiState.value.listings[0]
        vm.selectForPurchase(item)
        vm.confirmPurchase()
        advanceUntilIdle()

        assertThat(appState.currency.value.pearlBalance).isEqualTo(45)
    }

    private fun sampleListing(id: Long, category: String, price: Int = 5) = ShopListingDomain(
        id = id, productType = "item",
        product = ShopProduct(
            id = 100L + id, name = "Item$id", grade = Grade.N, category = category,
            imageAsset = "/x.png", isLimited = false,
        ),
        pearlPrice = price, maxPerUser = null, purchasedTotal = 0,
        maxPerPeriod = null, periodType = null, purchasedThisPeriod = 0,
        periodResetAt = null, startAt = null, endAt = null, canBuy = true,
    )
}
