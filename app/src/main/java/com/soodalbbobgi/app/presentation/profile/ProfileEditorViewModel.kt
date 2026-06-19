package com.soodalbbobgi.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ServerProfileCard
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.model.ProfileCard
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.YearMonth
import javax.inject.Inject

/** 에디터에서 표시할 아이템 단위 (인벤토리 + items 마스터 메타 결합) */
data class EditorItemUi(
    val inventoryId: Long,
    val itemId: Long,
    val name: String,
    val grade: Grade,
    val imageAsset: String,
    val isSelected: Boolean,
)

enum class EditorCategory(val key: String, val label: String) {
    Background("bg", "배경"),
    Character("char", "캐릭터"),
    Frame("frame", "테두리"),
    Text("text", "텍스트"),
}

// 폰트 스타일을 굵게/이탤릭 독립 토글로 다룬다 — 저장은 기존 textStyle 문자열을 그대로 쓴다.
/** 굵게·이탤릭 토글 조합 → textStyle 문자열. */
fun combineTextStyle(bold: Boolean, italic: Boolean): String = when {
    bold && italic -> "BOLD_ITALIC"
    bold -> "BOLD"
    italic -> "ITALIC"
    else -> "REGULAR"
}

/** textStyle 문자열에 굵게가 포함됐는지. */
fun textStyleHasBold(style: String): Boolean = style == "BOLD" || style == "BOLD_ITALIC"

/** textStyle 문자열에 이탤릭이 포함됐는지. */
fun textStyleHasItalic(style: String): Boolean = style == "ITALIC" || style == "BOLD_ITALIC"

data class ProfileEditorUiState(
    val activeTab: EditorCategory = EditorCategory.Background,
    val bgItems: List<EditorItemUi> = emptyList(),
    val charItems: List<EditorItemUi> = emptyList(),
    val frameItems: List<EditorItemUi> = emptyList(),
    val selectedBgInventoryId: Long? = null,
    val selectedCharInventoryId: Long? = null,
    val selectedFrameInventoryId: Long? = null,
    val charX: Float = 0.5f,
    val charY: Float = 0.5f,
    val charScale: Float = 1.0f,
    val customText: String = "",
    val textStyle: String = "REGULAR",
    /** 텍스트 블록 내부 줄 정렬 ("LEFT" | "RIGHT"). */
    val textAlign: String = "RIGHT",
    /** 텍스트 블록 가로 위치 (0~1). */
    val textX: Float = 0.95f,
    /** 텍스트 블록 세로 중심 위치 (0~1). */
    val textY: Float = 0.5f,
    /** 텍스트 블록 크기 단계 (1~5). */
    val textScaleStep: Int = 3,
    /** 기록(통계) 줄 표시 여부. */
    val showStats: Boolean = true,
    /** 닉네임 색상 ("#RRGGBB"). */
    val nicknameColor: String = "#FFFFFF",
    /** 소개 줄 색상 ("#RRGGBB"). */
    val taglineColor: String = "#FFFFFF",
    /** 기록 줄 색상 ("#RRGGBB"). */
    val statsColor: String = "#00F5FF",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    /** Live Preview 카드 닉네임 — 현재 로그인 사용자의 닉네임. 미로드 상태에선 빈 문자열. */
    val nickname: String = "",
    /** [customText]가 비었을 때 카드에 표시할 기본 한 줄 소개 (Home과 동일 문구). */
    val taglineFallback: String = "수영을 사랑하는 수달",
    /** Live Preview 카드 통계 문구 — 이번 달 누적 거리·횟수 (Home과 동일 포맷). */
    val statsText: String = "",
)

/**
 * 프로필 에디터 ViewModel.
 *
 * AppState의 inventory + items master + profileCard를 결합해 그리드/Live Preview를
 * 구성한다. 진입 시 [AppStateLoader.refreshInventory/refreshGachaBoxes/refreshProfileCard]로
 * 메모리를 최신화. 저장 시 서버 PUT + AppState에 즉시 반영.
 */
@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    private val userSession: UserSession,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val soodalApi: SoodalApi,
    private val swimLogUseCase: SwimLogUseCase,
) : ViewModel() {

    private val _editState = MutableStateFlow(EditState())

    private data class EditState(
        val activeTab: EditorCategory = EditorCategory.Background,
        val selectedBgInventoryId: Long? = null,
        val selectedCharInventoryId: Long? = null,
        val selectedFrameInventoryId: Long? = null,
        val charX: Float = 0.5f,
        val charY: Float = 0.5f,
        val charScale: Float = 1.0f,
        val customText: String = "",
        val textStyle: String = "REGULAR",
        val textAlign: String = "RIGHT",
        val textX: Float = 0.95f,
        val textY: Float = 0.5f,
        val textScaleStep: Int = 3,
        val showStats: Boolean = true,
        val nicknameColor: String = "#FFFFFF",
        val taglineColor: String = "#FFFFFF",
        val statsColor: String = "#00F5FF",
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val saveError: String? = null,
        val initialized: Boolean = false,
    )

    val uiState: StateFlow<ProfileEditorUiState> = combine(
        appState.inventory,
        appState.items,
        appState.profileCard,
        appState.profile,
        _editState,
    ) { inventory, itemsMap, savedCard, profile, edit ->
        val state = if (!edit.initialized && savedCard != null) {
            val initialized = edit.copy(
                selectedBgInventoryId = savedCard.backgroundItemId,
                selectedCharInventoryId = savedCard.characterItemId,
                selectedFrameInventoryId = savedCard.borderItemId,
                charX = savedCard.characterX,
                charY = savedCard.characterY,
                charScale = savedCard.characterScale,
                customText = savedCard.customText,
                textStyle = savedCard.textStyle,
                textAlign = savedCard.textAlign,
                textX = savedCard.textX,
                textY = savedCard.textY,
                textScaleStep = savedCard.textScaleStep,
                showStats = savedCard.showStats,
                nicknameColor = savedCard.nicknameColor,
                taglineColor = savedCard.taglineColor,
                statsColor = savedCard.statsColor,
                initialized = true,
            )
            _editState.value = initialized
            initialized
        } else if (!edit.initialized) {
            val initialized = edit.copy(initialized = true)
            _editState.value = initialized
            initialized
        } else edit

        // 이번 달 누적 통계 — Home과 동일 포맷으로 Live Preview 카드에 노출.
        val stats = swimLogUseCase.getMonthStats(monthStart(), monthEnd())

        ProfileEditorUiState(
            activeTab = state.activeTab,
            bgItems = inventory.toUiList("bg", itemsMap, state.selectedBgInventoryId),
            charItems = inventory.toUiList("char", itemsMap, state.selectedCharInventoryId),
            frameItems = inventory.toUiList("frame", itemsMap, state.selectedFrameInventoryId),
            selectedBgInventoryId = state.selectedBgInventoryId,
            selectedCharInventoryId = state.selectedCharInventoryId,
            selectedFrameInventoryId = state.selectedFrameInventoryId,
            charX = state.charX,
            charY = state.charY,
            charScale = state.charScale,
            customText = state.customText,
            textStyle = state.textStyle,
            textAlign = state.textAlign,
            textX = state.textX,
            textY = state.textY,
            textScaleStep = state.textScaleStep,
            showStats = state.showStats,
            nicknameColor = state.nicknameColor,
            taglineColor = state.taglineColor,
            statsColor = state.statsColor,
            isSaving = state.isSaving,
            saveSuccess = state.saveSuccess,
            saveError = state.saveError,
            nickname = profile?.nickname ?: "",
            statsText = "${stats.totalDistanceMeters}m · ${stats.swimCount}회",
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileEditorUiState())

    /** 이번 달 1일 (yyyy-MM-dd). */
    private fun monthStart(): String = YearMonth.now().atDay(1).toString()

    /** 이번 달 말일 (yyyy-MM-dd). */
    private fun monthEnd(): String = YearMonth.now().atEndOfMonth().toString()

    init {
        viewModelScope.launch {
            appStateLoader.refreshInventory()
            appStateLoader.refreshGachaBoxes()
            appStateLoader.refreshProfileCard()
        }
    }

    fun setActiveTab(tab: EditorCategory) { _editState.value = _editState.value.copy(activeTab = tab) }

    /**
     * 카테고리별 장착 아이템을 선택한다.
     *
     * @param category 대상 슬롯 (배경/캐릭터/테두리). 텍스트 탭은 무시된다.
     * @param inventoryId 선택할 인벤토리 id. `null`이면 "선택안함" — 해당 슬롯을 비운다
     *   (배경/테두리만 해당, 캐릭터는 항상 하나가 장착됨).
     */
    fun selectItem(category: EditorCategory, inventoryId: Long?) {
        _editState.value = when (category) {
            EditorCategory.Background -> _editState.value.copy(selectedBgInventoryId = inventoryId)
            EditorCategory.Character -> _editState.value.copy(selectedCharInventoryId = inventoryId)
            EditorCategory.Frame -> _editState.value.copy(selectedFrameInventoryId = inventoryId)
            EditorCategory.Text -> _editState.value
        }
    }
    fun setCharX(v: Float) { _editState.value = _editState.value.copy(charX = v) }
    fun setCharY(v: Float) { _editState.value = _editState.value.copy(charY = v) }
    fun setCharScale(v: Float) { _editState.value = _editState.value.copy(charScale = v) }
    fun setCustomText(v: String) { _editState.value = _editState.value.copy(customText = v) }
    fun setTextStyle(v: String) { _editState.value = _editState.value.copy(textStyle = v) }

    /** 텍스트 블록 내부 줄 정렬 설정 ("LEFT" | "RIGHT"). */
    fun setTextAlign(v: String) { _editState.value = _editState.value.copy(textAlign = v) }

    /** 텍스트 블록 가로 위치 설정 (0~1로 강제). */
    fun setTextX(v: Float) { _editState.value = _editState.value.copy(textX = v.coerceIn(0f, 1f)) }

    /** 텍스트 블록 세로 중심 위치 설정 (0~1로 강제). */
    fun setTextY(v: Float) { _editState.value = _editState.value.copy(textY = v.coerceIn(0f, 1f)) }

    /** 텍스트 블록 크기 단계 설정 (1~5로 강제). */
    fun setTextScaleStep(v: Int) { _editState.value = _editState.value.copy(textScaleStep = v.coerceIn(1, 5)) }

    /** 기록 줄 표시 여부 설정. */
    fun setShowStats(v: Boolean) { _editState.value = _editState.value.copy(showStats = v) }

    /** 닉네임 색상 설정 ("#RRGGBB"). */
    fun setNicknameColor(v: String) { _editState.value = _editState.value.copy(nicknameColor = v) }

    /** 소개 줄 색상 설정 ("#RRGGBB"). */
    fun setTaglineColor(v: String) { _editState.value = _editState.value.copy(taglineColor = v) }

    /** 기록 줄 색상 설정 ("#RRGGBB"). */
    fun setStatsColor(v: String) { _editState.value = _editState.value.copy(statsColor = v) }
    fun clearSaveResult() { _editState.value = _editState.value.copy(saveSuccess = false, saveError = null) }

    /**
     * 편집 상태를 저장값으로 되돌린다(취소 시 미저장 변경 폐기).
     * initialized=false로 두면 다음 uiState 방출 때 savedCard 기준으로 재초기화된다.
     */
    fun resetToSaved() {
        _editState.value = _editState.value.copy(initialized = false)
    }

    fun save() {
        val s = _editState.value
        viewModelScope.launch {
            _editState.value = s.copy(isSaving = true, saveError = null)
            try {
                val card = ProfileCard(
                    userId = userSession.userId,
                    backgroundItemId = s.selectedBgInventoryId,
                    characterItemId = s.selectedCharInventoryId,
                    borderItemId = s.selectedFrameInventoryId,
                    characterX = s.charX.coerceIn(0f, 1f),
                    characterY = s.charY.coerceIn(0f, 1f),
                    characterScale = s.charScale.coerceIn(0.3f, 1f),
                    customText = s.customText,
                    textStyle = s.textStyle,
                    textAlign = s.textAlign,
                    textX = s.textX.coerceIn(0f, 1f),
                    textY = s.textY.coerceIn(0f, 1f),
                    textScaleStep = s.textScaleStep,
                    showStats = s.showStats,
                    nicknameColor = s.nicknameColor,
                    taglineColor = s.taglineColor,
                    statsColor = s.statsColor,
                )
                // 메모리 즉시 반영
                appStateLoader.applyProfileCardSaved(card)
                // 서버 PUT
                soodalApi.updateProfileCard(ServerProfileCard(
                    backgroundItemId = card.backgroundItemId,
                    characterItemId = card.characterItemId,
                    borderItemId = card.borderItemId,
                    characterX = card.characterX,
                    characterY = card.characterY,
                    characterScale = card.characterScale,
                    customText = card.customText.ifEmpty { null },
                    textStyle = card.textStyle,
                    textAlign = card.textAlign,
                    textX = card.textX,
                    textY = card.textY,
                    textScaleStep = card.textScaleStep,
                    showStats = card.showStats,
                    nicknameColor = card.nicknameColor,
                    taglineColor = card.taglineColor,
                    statsColor = card.statsColor,
                ))
                _editState.value = _editState.value.copy(isSaving = false, saveSuccess = true)
            } catch (e: Exception) {
                Timber.e(e, "ProfileCard 저장 실패")
                _editState.value = _editState.value.copy(
                    isSaving = false,
                    saveError = "저장에 실패했어요. 다시 시도해주세요.",
                )
            }
        }
    }

    private fun List<InventoryItem>.toUiList(
        category: String,
        itemsMap: Map<Long, com.soodalbbobgi.app.domain.model.Item>,
        selectedId: Long?,
    ): List<EditorItemUi> = this
        .filter { it.category == category }
        .mapNotNull { inv ->
            val meta = itemsMap[inv.itemId] ?: return@mapNotNull null
            EditorItemUi(
                inventoryId = inv.id,
                itemId = inv.itemId,
                name = meta.name,
                grade = inv.grade,
                imageAsset = meta.imageAsset ?: "",
                isSelected = inv.id == selectedId,
            )
        }
        .sortedByDescending { it.grade.ordinal }
}
