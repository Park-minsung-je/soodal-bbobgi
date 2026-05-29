package com.soodalbbobgi.app.presentation.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ShopPurchaseRequest
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.ShopListingDomain
import com.soodalbbobgi.app.presentation.gacha.GachaResultItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val price: Int,
    val maxPerUser: Int?,
    val purchasedTotal: Int,
    val maxPerPeriod: Int?,
    val periodType: String?,
    val purchasedThisPeriod: Int,
    val periodResetAt: Long?,
    val isLimited: Boolean,
    val canBuy: Boolean,
)

data class ShopUiState(
    val pearls: Int = 0,
    val listings: List<ShopItem> = emptyList(),
    val confirmItem: ShopItem? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** 박스 구매 결과 — 비어있지 않으면 결과 오버레이를 띄운다. */
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
        appState.currency, appState.shopListings, _local,
    ) { currency, listings, local ->
        ShopUiState(
            pearls = currency.pearlBalance,
            listings = listings.map { it.toUi() },
            confirmItem = local.confirmItem,
            isLoading = local.isLoading,
            error = local.error,
            boxResults = local.boxResults,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopUiState())

    init {
        // 프로세스 사망 후 이 탭으로 복원됐을 수 있으니 먼저 전체 재수화
        viewModelScope.launch { appStateLoader.ensureHydrated() }
        refresh()
    }

    /** 진열 + 잔액 새로고침. */
    fun refresh() {
        viewModelScope.launch {
            _local.update { it.copy(isLoading = true) }
            try {
                appStateLoader.refreshCurrency()
                appStateLoader.refreshShop()
            } catch (e: Exception) {
                Timber.w(e, "상점 새로고침 실패")
                _local.update { it.copy(error = "상점을 불러오지 못했어요") }
            } finally {
                _local.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectForPurchase(item: ShopItem) {
        if (item.canBuy) _local.update { it.copy(confirmItem = item) }
    }

    fun cancelPurchase() { _local.update { it.copy(confirmItem = null) } }
    fun clearError() { _local.update { it.copy(error = null) } }

    /** 박스 구매 결과 오버레이 닫기. */
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
                    } else {
                        _local.update { it.copy(confirmItem = null) }
                    }
                    refresh()
                } else {
                    _local.update { it.copy(error = res.error?.message ?: "구매에 실패했어요", confirmItem = null) }
                }
            } catch (e: Exception) {
                Timber.w(e, "상점 구매 실패")
                _local.update { it.copy(error = "구매에 실패했어요", confirmItem = null) }
            }
        }
    }

    private fun ShopListingDomain.toUi(): ShopItem {
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
            price = pearlPrice,
            maxPerUser = maxPerUser,
            purchasedTotal = purchasedTotal,
            maxPerPeriod = maxPerPeriod,
            periodType = periodType,
            purchasedThisPeriod = purchasedThisPeriod,
            periodResetAt = periodResetAt,
            isLimited = product.isLimited,
            canBuy = canBuy,
        )
    }
}
