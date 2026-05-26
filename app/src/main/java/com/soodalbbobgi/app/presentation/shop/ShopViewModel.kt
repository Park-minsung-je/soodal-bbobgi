package com.soodalbbobgi.app.presentation.shop

import androidx.lifecycle.ViewModel
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val pearls: Int = 12,
    val confirmItem: ShopItem? = null,
    val featured: ShopItem = ShopItem(
        name = "황금 수달",
        icon = SoodalIcons.Otter,
        grade = Grade.SSR,
        price = 30,
        desc = "빛나는 황금빛 수달 캐릭터",
    ),
    val boxes: List<ShopItem> = listOf(
        ShopItem("랜덤 상자", SoodalIcons.Gift, null, 5, desc = "모든 종류 랜덤"),
        ShopItem("배경 상자", SoodalIcons.Aurora, null, 5, desc = "배경 아이템 랜덤"),
        ShopItem("캐릭터 상자", SoodalIcons.Otter, null, 5, desc = "캐릭터 랜덤"),
        ShopItem("테두리 상자", SoodalIcons.Frame, null, 5, desc = "테두리 랜덤"),
    ),
    val directItems: List<ShopItem> = listOf(
        ShopItem("진주 수달", SoodalIcons.Otter, Grade.SR, 10),
        ShopItem("오로라", SoodalIcons.Aurora, Grade.SR, 10),
        ShopItem("시안 라인", SoodalIcons.Frame, Grade.R, 5),
        ShopItem("코랄 수달", SoodalIcons.Otter, Grade.R, 5, isOwned = true),
        ShopItem("한밤", SoodalIcons.Moon, Grade.N, 2),
        ShopItem("수달이", SoodalIcons.Otter, Grade.N, 2, isOwned = true),
    ),
)

@HiltViewModel
class ShopViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState

    fun selectForPurchase(item: ShopItem) {
        if (!item.isOwned) {
            _uiState.value = _uiState.value.copy(confirmItem = item)
        }
    }

    fun cancelPurchase() {
        _uiState.value = _uiState.value.copy(confirmItem = null)
    }

    fun confirmPurchase() {
        val item = _uiState.value.confirmItem ?: return
        val current = _uiState.value
        if (current.pearls < item.price) return

        _uiState.value = current.copy(
            pearls = current.pearls - item.price,
            confirmItem = null,
        )
    }
}
