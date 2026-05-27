package com.soodalbbobgi.app.presentation.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.domain.repository.GachaRepository
import com.soodalbbobgi.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShopItem(
    val name: String,
    val icon: SoodalIcons,
    val grade: Grade?,
    val price: Int,
    val isOwned: Boolean = false,
    val desc: String = "",
)

data class ShopUiState(
    val pearls: Int = 0,
    val confirmItem: ShopItem? = null,
    val featured: ShopItem = ShopItem("", SoodalIcons.Otter, null, 0),
    val boxes: List<ShopItem> = emptyList(),
    val directItems: List<ShopItem> = emptyList(),
)

/**
 * 상점 화면 ViewModel.
 * User(진주 잔액)와 GachaBox(상자 목록)를 Room DB에서 관찰하고,
 * [CurrencyUseCase]를 통해 진주 구매를 처리한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val userSession: UserSession,
    private val userRepository: UserRepository,
    private val gachaRepository: GachaRepository,
    private val soodalApi: SoodalApi,
) : ViewModel() {

    private val userId get() = userSession.userId

    private val _confirmItem = MutableStateFlow<ShopItem?>(null)

    /** 진주 잔액을 User Flow에서 관찰 */
    private val pearlsFlow = userRepository.getUser(userId)
        .filterNotNull()
        .map { it.pearlBalance }

    /** 활성 상자 목록을 ShopItem으로 변환 */
    private val boxesFlow = gachaRepository.getAllActiveBoxes().map { boxes ->
        boxes.map { box ->
            ShopItem(
                name = box.name,
                icon = when (box.category) {
                    "bg" -> SoodalIcons.Aurora
                    "char" -> SoodalIcons.Otter
                    "frame" -> SoodalIcons.Frame
                    else -> SoodalIcons.Gift
                },
                grade = null,
                price = 5,
                desc = box.description,
            )
        }
    }

    /** 각 상자의 SSR/SR 아이템을 직접 구매 목록으로 변환 */
    private val directItemsFlow = gachaRepository.getAllActiveBoxes().flatMapLatest { boxes ->
        if (boxes.isEmpty()) return@flatMapLatest flowOf(emptyList<ShopItem>())

        val itemFlows = boxes.map { box ->
            gachaRepository.getBoxItems(box.id).map { items ->
                items
                    .filter { it.grade == Grade.SSR || it.grade == Grade.SR }
                    .map { item ->
                        ShopItem(
                            name = item.name,
                            icon = when (box.category) {
                                "bg" -> SoodalIcons.Aurora
                                "char" -> SoodalIcons.Otter
                                "frame" -> SoodalIcons.Frame
                                else -> SoodalIcons.Gift
                            },
                            grade = item.grade,
                            price = item.grade.pearlValue,
                        )
                    }
            }
        }
        combine(itemFlows) { arrays -> arrays.flatMap { it } }
    }

    init {
        // 화면 진입 시 서버에서 최신 데이터 갱신
        refreshFromServer()
    }

    private fun refreshFromServer() {
        viewModelScope.launch {
            try {
                val userRes = soodalApi.getMe()
                if (userRes.success && userRes.data != null) {
                    val u = userRes.data
                    userRepository.updateCurrency(u.id, u.shellBalance, u.pearlBalance)
                }
            } catch (_: Exception) { }
        }
    }

    val uiState: StateFlow<ShopUiState> = combine(
        pearlsFlow,
        boxesFlow,
        directItemsFlow,
        _confirmItem,
    ) { pearls, boxes, directItems, confirm ->
        val featured = directItems.firstOrNull { it.grade == Grade.SSR }
            ?: directItems.firstOrNull()
            ?: ShopItem("", SoodalIcons.Otter, null, 0)

        ShopUiState(
            pearls = pearls,
            confirmItem = confirm,
            featured = featured,
            boxes = boxes,
            directItems = directItems,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ShopUiState())

    /** 구매 확인 다이얼로그를 표시한다. 이미 보유한 아이템은 무시. */
    fun selectForPurchase(item: ShopItem) {
        if (!item.isOwned) {
            _confirmItem.value = item
        }
    }

    /** 구매를 취소한다. */
    fun cancelPurchase() {
        _confirmItem.value = null
    }

    /** 구매를 확정하여 서버에서 진주 차감 + 아이템 지급을 처리한다. */
    fun confirmPurchase() {
        val item = _confirmItem.value ?: return
        if (uiState.value.pearls < item.price) return

        viewModelScope.launch {
            try {
                val response = soodalApi.shopPurchase(
                    com.soodalbbobgi.app.data.remote.dto.ShopPurchaseRequest(
                        boxItemId = 0, // TODO: ShopItem에 boxItemId 추가 필요
                        price = item.price,
                    )
                )
                if (response.success && response.data != null) {
                    val currency = response.data.currency
                    userRepository.updateCurrency(userId, currency.shellBalance, currency.pearlBalance)
                }
            } catch (e: Exception) {
                timber.log.Timber.w(e, "상점 구매 실패")
            } finally {
                _confirmItem.value = null
            }
        }
    }
}
