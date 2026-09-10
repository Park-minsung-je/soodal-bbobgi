package kr.ilf.soodalbbobgi.presentation.shop

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.ApiResponse
import kr.ilf.soodalbbobgi.data.remote.dto.ServerCurrency
import kr.ilf.soodalbbobgi.data.remote.dto.ShopPurchaseData
import kr.ilf.soodalbbobgi.data.remote.dto.ShopPurchaseRequest
import kr.ilf.soodalbbobgi.domain.model.Currency
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.domain.model.InventoryItem
import kr.ilf.soodalbbobgi.domain.model.ShopListingDomain
import kr.ilf.soodalbbobgi.domain.model.ShopProduct
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
            icon = kr.ilf.soodalbbobgi.core.ui.SoodalIcons.Otter, imageAsset = null,
            grade = Grade.N, category = "char", price = 5, maxPerUser = 1, purchasedTotal = 1,
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

    @Test
    fun `아이템 구매 성공 시 구매한 상품으로 결과 팝업을 띄운다`() = runTest(testDispatcher) {
        appState.applyCurrency(Currency(pearlBalance = 50))
        appState.applyShopListings(listOf(sampleListing(id = 1, category = "char", price = 5)))
        coEvery { api.shopPurchase(any<ShopPurchaseRequest>()) } returns ApiResponse(
            success = true,
            data = ShopPurchaseData(
                shopListingId = 1L, productType = "item",
                inventoryItemId = 9000L, gachaHistoryId = null,
                acquiredItems = null, // 아이템 구매 응답엔 결과 목록이 없다 — 클라이언트가 구성
                currency = ServerCurrency(shellBalance = 0, pearlBalance = 45, pityCounter = 0),
            ),
            error = null,
        )

        val vm = ShopViewModel(UserSession(), appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        vm.selectForPurchase(vm.uiState.value.listings[0])
        vm.confirmPurchase()
        advanceUntilIdle()

        val results = vm.uiState.value.boxResults
        assertThat(results).hasSize(1)
        assertThat(results[0].name).isEqualTo("Item1")
        assertThat(results[0].kind).isEqualTo("char")
        assertThat(results[0].isNew).isTrue()
        assertThat(results[0].pearlsEarned).isEqualTo(0)
    }

    @Test
    fun `인벤토리에 있는 아이템 상품은 owned로 표시된다`() = runTest(testDispatcher) {
        // sampleListing(id=1)의 product.id = 101
        appState.applyShopListings(listOf(sampleListing(id = 1, category = "bg")))
        appState.applyInventory(listOf(
            InventoryItem(id = 1L, userId = "u", itemId = 101L, grade = Grade.N, category = "bg", acquiredAt = 0L),
        ))
        val vm = ShopViewModel(UserSession(), appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        assertThat(vm.uiState.value.listings[0].owned).isTrue()
    }

    @Test
    fun `인벤토리에 없는 아이템 상품은 owned가 아니다`() = runTest(testDispatcher) {
        appState.applyShopListings(listOf(sampleListing(id = 1, category = "bg")))
        appState.applyInventory(listOf(
            InventoryItem(id = 1L, userId = "u", itemId = 999L, grade = Grade.N, category = "bg", acquiredAt = 0L),
        ))
        val vm = ShopViewModel(UserSession(), appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        assertThat(vm.uiState.value.listings[0].owned).isFalse()
    }

    @Test
    fun `owned 아이템은 selectForPurchase가 무시된다`() = runTest(testDispatcher) {
        appState.applyShopListings(listOf(sampleListing(id = 1, category = "bg")))
        appState.applyInventory(listOf(
            InventoryItem(id = 1L, userId = "u", itemId = 101L, grade = Grade.N, category = "bg", acquiredAt = 0L),
        ))
        val vm = ShopViewModel(UserSession(), appState, loader, api)
        startCollect(vm)
        advanceUntilIdle()

        vm.selectForPurchase(vm.uiState.value.listings[0])
        assertThat(vm.uiState.value.confirmItem).isNull()
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
