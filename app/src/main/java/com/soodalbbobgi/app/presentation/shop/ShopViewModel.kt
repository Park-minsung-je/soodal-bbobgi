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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
)

@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userSession: UserSession,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val soodalApi: SoodalApi,
) : ViewModel() {

    private val _confirmItem = MutableStateFlow<ShopItem?>(null)
    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ShopUiState> = combine(
        appState.currency, appState.shopListings, _confirmItem, _loading, _error,
    ) { currency, listings, confirm, loading, error ->
        ShopUiState(
            pearls = currency.pearlBalance,
            listings = listings.map { it.toUi() },
            confirmItem = confirm,
            isLoading = loading,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopUiState())

    init { refresh() }

    /** 진열 + 잔액 새로고침. */
    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                appStateLoader.refreshCurrency()
                appStateLoader.refreshShop()
            } catch (e: Exception) {
                Timber.w(e, "상점 새로고침 실패")
                _error.value = "상점을 불러오지 못했어요"
            } finally {
                _loading.value = false
            }
        }
    }

    fun selectForPurchase(item: ShopItem) {
        if (item.canBuy) _confirmItem.value = item
    }

    fun cancelPurchase() { _confirmItem.value = null }
    fun clearError() { _error.value = null }

    fun confirmPurchase() {
        val item = _confirmItem.value ?: return
        if (uiState.value.pearls < item.price) {
            _error.value = "진주가 부족해요"
            _confirmItem.value = null
            return
        }
        viewModelScope.launch {
            try {
                val res = soodalApi.shopPurchase(ShopPurchaseRequest(shopListingId = item.shopListingId))
                if (res.success && res.data != null) {
                    // currency 즉시 갱신
                    appStateLoader.applyServerCurrency(res.data.currency)
                    // box 구매면 결과 아이템들도 인벤토리에 반영
                    res.data.acquiredItems?.let { acquired ->
                        appStateLoader.applyGachaResults(acquired, res.data.currency)
                    }
                    refresh()
                } else {
                    _error.value = res.error?.message ?: "구매에 실패했어요"
                }
            } catch (e: Exception) {
                Timber.w(e, "상점 구매 실패")
                _error.value = "구매에 실패했어요"
            } finally {
                _confirmItem.value = null
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
