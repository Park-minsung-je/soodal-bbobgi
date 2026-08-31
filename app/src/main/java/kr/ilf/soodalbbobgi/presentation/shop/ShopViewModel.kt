package kr.ilf.soodalbbobgi.presentation.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.ShopPurchaseRequest
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.domain.model.ShopListingDomain
import kr.ilf.soodalbbobgi.presentation.gacha.GachaResultItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** 상점 카드 UI 모델. AppState의 listings에서 변환된 형태. */
data class ShopItem(
    val shopListingId: Long,
    val productType: String,
    val name: String,
    val description: String,
    val icon: SoodalIcons,
    val imageAsset: String?,
    val grade: Grade?,
    /** 상품 카테고리 (char/bg/frame) — 구매 결과 팝업의 종류 라벨에 사용. */
    val category: String?,
    val price: Int,
    val maxPerUser: Int?,
    val purchasedTotal: Int,
    val maxPerPeriod: Int?,
    val periodType: String?,
    val purchasedThisPeriod: Int,
    val periodResetAt: Long?,
    val isLimited: Boolean,
    /** 판매 종료 시각(epoch ms) — 있으면 기간 한정 상품으로 남은 기간을 표시한다. */
    val endAt: Long? = null,
    val canBuy: Boolean,
    /** 인벤토리 보유 여부 — 아이템 상품만 해당, 상자는 항상 false. */
    val owned: Boolean = false,
)

data class ShopUiState(
    val shells: Int = 0,
    val pearls: Int = 0,
    val listings: List<ShopItem> = emptyList(),
    val confirmItem: ShopItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** 구매 결과(박스 뽑기·아이템 공용) — 비어있지 않으면 결과 오버레이를 띄운다. */
    val boxResults: List<GachaResultItem> = emptyList(),
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userSession: UserSession,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val soodalApi: SoodalApi,
) : ViewModel() {

    /** confirm/loading/error/박스결과를 한 묶음으로 관리 (combine 인자 수 제한 회피). */
    private data class LocalShopState(
        val confirmItem: ShopItem? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val boxResults: List<GachaResultItem> = emptyList(),
    )

    private val _local = MutableStateFlow(LocalShopState())

    val uiState: StateFlow<ShopUiState> = combine(
        appState.currency, appState.shopListings, appState.inventory, _local,
    ) { currency, listings, inventory, local ->
        // 보유 판정은 구매 이력이 아니라 인벤토리 기준 — 뽑기/기본 지급으로 얻은 아이템도 "보유 중"이어야 한다
        val ownedItemIds = inventory.map { it.itemId }.toSet()
        ShopUiState(
            shells = currency.shellBalance,
            pearls = currency.pearlBalance,
            listings = listings.map { it.toUi(ownedItemIds) },
            confirmItem = local.confirmItem,
            isLoading = local.isLoading,
            error = local.error,
            boxResults = local.boxResults,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopUiState())

    init {
        // 프로세스 사망 후 이 탭으로 복원됐을 수 있으니 먼저 전체 재수화
        viewModelScope.launch { appStateLoader.ensureHydrated() }
        // 진열 새로고침은 여기서 하지 않는다 — 탭이 saveState로 보존돼 이 블록은 첫 진입에만 돈다.
        // 화면이 진입할 때마다 [refresh]를 부른다 (ShopScreen).
    }

    /** 마지막으로 새로고침을 끝낸 시각 — 탭을 오갈 때마다 같은 요청이 반복되지 않게 한다. */
    private var lastRefreshedAt = 0L

    /**
     * 진열 + 잔액 새로고침.
     *
     * @param force true면 [REFRESH_INTERVAL_MS]와 무관하게 무조건 다시 불러온다 (구매 직후 등).
     */
    fun refresh(force: Boolean = false) {
        // 탭을 잠깐 스쳐 지나가는 동안 같은 요청을 여러 번 보내지 않는다.
        if (!force && System.currentTimeMillis() - lastRefreshedAt < REFRESH_INTERVAL_MS) return
        viewModelScope.launch {
            _local.update { it.copy(isLoading = true) }
            // 응답이 순식간에 와도 로딩 딤이 깜빡 스치지 않게 최소 표시 시간을 유지 (홈 동기화와 동일 패턴).
            val startedAt = System.currentTimeMillis()
            try {
                appStateLoader.refreshCurrency()
                appStateLoader.refreshShop()
                // 보유 중 표시용 — 다른 기기에서의 획득도 반영되도록 인벤토리도 갱신
                appStateLoader.refreshInventory()
                lastRefreshedAt = System.currentTimeMillis()
            } catch (e: Exception) {
                Timber.w(e, "상점 새로고침 실패")
                _local.update { it.copy(error = "상점을 불러오지 못했어요") }
            } finally {
                val elapsed = System.currentTimeMillis() - startedAt
                if (elapsed < MIN_LOADING_INDICATOR_MS) delay(MIN_LOADING_INDICATOR_MS - elapsed)
                _local.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectForPurchase(item: ShopItem) {
        if (item.canBuy && !item.owned) _local.update { it.copy(confirmItem = item) }
    }

    fun cancelPurchase() { _local.update { it.copy(confirmItem = null) } }
    fun clearError() { _local.update { it.copy(error = null) } }

    /** 구매 결과 오버레이 닫기 (박스·아이템 공용). */
    fun dismissBoxResults() { _local.update { it.copy(boxResults = emptyList()) } }

    fun confirmPurchase() {
        val item = _local.value.confirmItem ?: return
        if (uiState.value.pearls < item.price) {
            _local.update { it.copy(error = "진주가 부족해요", confirmItem = null) }
            return
        }
        viewModelScope.launch {
            try {
                val res = soodalApi.shopPurchase(ShopPurchaseRequest(shopListingId = item.shopListingId))
                if (res.success && res.data != null) {
                    // currency 즉시 갱신
                    appStateLoader.applyServerCurrency(res.data.currency)
                    // box 구매면 결과 아이템들도 인벤토리에 반영 + 결과 오버레이 표시
                    val acquired = res.data.acquiredItems
                    if (!acquired.isNullOrEmpty()) {
                        appStateLoader.applyGachaResults(acquired, res.data.currency)
                        val results = acquired.map { r ->
                            GachaResultItem(
                                name = r.item.name,
                                grade = Grade.fromString(r.item.grade),
                                kind = r.item.category ?: "",
                                isNew = r.wasNew,
                                pearlsEarned = r.pearlsEarned,
                                imageAsset = r.item.imageAsset,
                            )
                        }
                        _local.update { it.copy(boxResults = results, confirmItem = null) }
                    } else if (item.productType == "item") {
                        // 아이템 직접 구매 — 응답에 아이템 정보가 없으므로 구매한 상품 정보로
                        // 박스와 같은 결과 팝업을 띄운다 (구매 완료 피드백).
                        val result = GachaResultItem(
                            name = item.name,
                            grade = item.grade ?: Grade.N,
                            kind = item.category ?: "",
                            isNew = true,
                            pearlsEarned = 0,
                            imageAsset = item.imageAsset,
                        )
                        _local.update { it.copy(boxResults = listOf(result), confirmItem = null) }
                    } else {
                        _local.update { it.copy(confirmItem = null) }
                    }
                    refresh(force = true)
                } else {
                    // 서버가 거절한 경우 — 화면이 오래된 진열을 들고 있었을 수 있다
                    // (판매 중지·기간 종료·한도 초과). 서버 메시지를 그대로 보여주고 진열을 새로 받는다.
                    _local.update { it.copy(error = res.error?.message ?: "구매에 실패했어요", confirmItem = null) }
                    refresh(force = true)
                }
            } catch (e: Exception) {
                Timber.w(e, "상점 구매 실패")
                _local.update { it.copy(error = "구매에 실패했어요", confirmItem = null) }
            }
        }
    }

    /**
     * 진열 도메인을 UI 모델로 변환한다.
     *
     * @param ownedItemIds 현재 인벤토리의 아이템 ID 집합 — 아이템 상품의 보유 판정에 사용
     */
    private fun ShopListingDomain.toUi(ownedItemIds: Set<Long>): ShopItem {
        val category = product.category ?: ""
        val icon = when (category) {
            "char" -> SoodalIcons.Otter
            "bg" -> SoodalIcons.Aurora
            "frame" -> SoodalIcons.Frame
            else -> SoodalIcons.Gift
        }
        return ShopItem(
            shopListingId = id,
            productType = productType,
            name = product.name,
            description = product.description ?: "",
            icon = icon,
            imageAsset = product.imageAsset ?: product.iconAsset,
            grade = product.grade,
            category = product.category,
            price = pearlPrice,
            maxPerUser = maxPerUser,
            purchasedTotal = purchasedTotal,
            maxPerPeriod = maxPerPeriod,
            periodType = periodType,
            purchasedThisPeriod = purchasedThisPeriod,
            periodResetAt = periodResetAt,
            isLimited = product.isLimited,
            endAt = endAt,
            canBuy = canBuy,
            owned = productType == "item" && product.id in ownedItemIds,
        )
    }

    companion object {
        /** 로딩 딤 최소 유지 시간(ms) — 순간 응답 시 오버레이가 깜빡 스치는 것 방지. */
        private const val MIN_LOADING_INDICATOR_MS = 600L

        /**
         * 같은 요청을 다시 보내기까지의 최소 간격(ms).
         * 탭을 오갈 때마다 서버를 두드리지 않으면서도, 진열이 바뀌면 곧 반영되는 선.
         */
        private const val REFRESH_INTERVAL_MS = 30_000L
    }
}
