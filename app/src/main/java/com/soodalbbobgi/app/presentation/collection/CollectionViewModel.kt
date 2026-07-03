package com.soodalbbobgi.app.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 컬렉션 화면의 아이템 한 칸 — 보유/장착 여부를 포함한 표시 데이터.
 */
data class CollectionEntry(
    val id: Long,
    val name: String,
    /** 카테고리 (char/bg/frame). */
    val kind: String,
    val grade: Grade,
    val imageAsset: String?,
    val owned: Boolean,
    /** 현재 프로필 카드에 장착 중인지. */
    val equipped: Boolean,
)

data class CollectionUiState(
    /** 카테고리 순(char→bg→frame), 카테고리 안에서는 id 순 정렬. */
    val entries: List<CollectionEntry> = emptyList(),
) {
    val ownedCount: Int get() = entries.count { it.owned }
    val totalCount: Int get() = entries.size

    /** 보유 중인 아이템의 등급별 개수. */
    fun ownedByGrade(grade: Grade): Int = entries.count { it.owned && it.grade == grade }

    fun ofKind(kind: String): List<CollectionEntry> = entries.filter { it.kind == kind }
}

/**
 * 내 컬렉션(도감) 화면 — 아이템 카탈로그 전체에 보유/장착 상태를 얹어 노출한다.
 */
@HiltViewModel
class CollectionViewModel @Inject constructor(
    appState: AppState,
) : ViewModel() {

    val uiState: StateFlow<CollectionUiState> = combine(
        appState.items,
        appState.inventory,
        appState.profileCard,
    ) { items, inventory, card ->
        val ownedIds = inventory.map { it.itemId }.toSet()
        val equippedIds = setOfNotNull(card?.backgroundItemId, card?.characterItemId, card?.borderItemId)
        val kindOrder = mapOf("char" to 0, "bg" to 1, "frame" to 2)
        CollectionUiState(
            entries = items.values
                .sortedWith(compareBy({ kindOrder[it.category] ?: 3 }, { it.id }))
                .map {
                    CollectionEntry(
                        id = it.id,
                        name = it.name,
                        kind = it.category,
                        grade = it.grade,
                        imageAsset = it.imageAsset,
                        owned = it.id in ownedIds,
                        equipped = it.id in equippedIds,
                    )
                },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CollectionUiState())
}
