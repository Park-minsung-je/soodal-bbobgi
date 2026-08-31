package kr.ilf.soodalbbobgi.presentation.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.domain.model.Item
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
    /** [collectionOrder] 기준 정렬 — 카테고리 → 등급 낮은 순 → 번호 순. */
    val entries: List<CollectionEntry> = emptyList(),
) {
    val ownedCount: Int get() = entries.count { it.owned }
    val totalCount: Int get() = entries.size

    /** 보유 중인 아이템의 등급별 개수. */
    fun ownedByGrade(grade: Grade): Int = entries.count { it.owned && it.grade == grade }

    fun ofKind(kind: String): List<CollectionEntry> = entries.filter { it.kind == kind }
}

/** 도감 카테고리 진열 순서 — 캐릭터 → 배경 → 액자. 목록에 없는 카테고리는 맨 뒤. */
private val KIND_ORDER = mapOf("char" to 0, "bg" to 1, "frame" to 2)

/**
 * 도감 진열 순서 — 카테고리 → **등급 낮은 순(N→R→SR→SSR)** → 번호 순.
 *
 * 상점은 관리자가 배치를 직접 정하지만 도감은 카탈로그 전체를 그대로 보여주므로,
 * 흔한 것부터 귀한 것으로 올라가며 수집 진행을 따라가게 둔다.
 */
internal fun collectionOrder(): Comparator<Item> =
    compareBy<Item> { KIND_ORDER[it.category] ?: KIND_ORDER.size }
        .thenBy { it.grade.ordinal }
        .thenBy { it.id }

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
        // 카드 슬롯 값은 인벤토리 행 id다 (서버의 인벤토리 인다이렉션) —
        // 아이템 마스터 id와 직접 비교하면 엉뚱한 칸에 착용 표시가 붙는다.
        val equippedInvIds = setOfNotNull(card?.backgroundItemId, card?.characterItemId, card?.borderItemId)
        val equippedIds = inventory.filter { it.id in equippedInvIds }.map { it.itemId }.toSet()

        CollectionUiState(
            entries = items.values
                .sortedWith(collectionOrder())
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
