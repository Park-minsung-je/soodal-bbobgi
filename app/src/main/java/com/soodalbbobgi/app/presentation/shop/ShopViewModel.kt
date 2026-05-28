package com.soodalbbobgi.app.presentation.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ServerShopListing
import com.soodalbbobgi.app.data.remote.dto.ShopPurchaseRequest
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/** 상점 진열 카드 UI 모델 (서버 응답을 화면용으로 변환한 것) */
data class ShopItem(
    val shopListingId: Long,
    val productType: String,             // 'item' | 'box'
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

/**
 * 상점 화면 ViewModel.
 * 서버의 shop_listings 응답을 받아 진열하고, 구매 요청은 shopListingId 기반으로 보낸다.
 */
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userSession: UserSession,
    private val userRepository: UserRepository,
    private val soodalApi: SoodalApi,
) : ViewModel() {

    private val userId get() = userSession.userId

    private val _listings = MutableStateFlow<List<ShopItem>>(emptyList())
    private val _confirmItem = MutableStateFlow<ShopItem?>(null)
    private val _loading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val pearlsFlow = userRepository.getUser(userId)
        .filterNotNull()
        .map { it.pearlBalance }

    val uiState: StateFlow<ShopUiState> = combine(
        pearlsFlow, _listings, _confirmItem, _loading, _error,
    ) { pearls, listings, confirm, loading, error ->
        ShopUiState(
            pearls = pearls,
            listings = listings,
            confirmItem = confirm,
            isLoading = loading,
            error = error,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopUiState())

    init { refresh() }

    /** 상점 진열 + 사용자 진주 잔액 새로고침. */
    fun refresh() {
        viewModelScope.launch {
            _loading.value = true
            try {
                val userRes = soodalApi.getMe()
                if (userRes.success && userRes.data != null) {
                    val u = userRes.data
                    userRepository.updateCurrency(u.id, u.shellBalance, u.pearlBalance)
                }
                val shopRes = soodalApi.getShop()
                if (shopRes.success && shopRes.data != null) {
                    _listings.value = shopRes.data.listings.map { it.toUi() }
                }
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

    /** 서버에 구매 요청. 성공 시 잔액 갱신 + 진열 갱신. */
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
                    val c = res.data.currency
                    userRepository.updateCurrency(userId, c.shellBalance, c.pearlBalance)
                    userRepository.updatePityCounter(userId, c.pityCounter)
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

    private fun ServerShopListing.toUi(): ShopItem {
        val name = product.name
        val grade = product.grade?.let { Grade.fromString(it) }
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
            name = name,
            description = product.description ?: "",
            icon = icon,
            imageAsset = product.imageAsset ?: product.iconAsset,
            grade = grade,
            price = pearlPrice,
            maxPerUser = maxPerUser,
            purchasedTotal = purchasedTotal,
            maxPerPeriod = maxPerPeriod,
            periodType = periodType,
            purchasedThisPeriod = purchasedThisPeriod,
            periodResetAt = periodResetAt,
            isLimited = product.isLimited == true,
            canBuy = canBuy,
        )
    }
}
