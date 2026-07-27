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
    // 테두리(액자)는 보류 — 탭에서 제외 (장착 데이터는 보존, 추후 부활 가능).
    // 텍스트 요소는 각각 자기 탭을 가진다 — 표시/위치/크기/칩/색/글꼴 전부 요소별 독립.
    Nickname("nickname", "닉네임"),
    Tagline("tagline", "한마디"),
    Stats("stats", "기록"),
}

/** 카드 텍스트 요소 식별자 — 탭과 1:1로 대응한다. */
enum class TextElement(val label: String) {
    Nickname("닉네임"),
    Tagline("한마디"),
    Stats("기록"),
}

/** 텍스트 요소 탭 → 요소 식별자. 배경/캐릭터 탭은 null. */
fun EditorCategory.textElementOrNull(): TextElement? = when (this) {
    EditorCategory.Nickname -> TextElement.Nickname
    EditorCategory.Tagline -> TextElement.Tagline
    EditorCategory.Stats -> TextElement.Stats
    else -> null
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

/** 텍스트 요소 하나의 편집 상태 — 표시/중심 위치(0~1)/크기 단계/알약/색/글꼴/외곽선. */
data class TextElementState(
    val show: Boolean = true,
    val x: Float = 0.83f,
    val y: Float = 0.40f,
    val scaleStep: Int = 3,
    val pill: String = "WHITE",
    val color: String = "#FFFFFF",
    val bold: Boolean = false,
    val italic: Boolean = false,
    val outline: Boolean = false,
) {
    /** 굵게/기울임 토글 → 저장용 textStyle 문자열. */
    val style: String get() = combineTextStyle(bold, italic)
}

data class ProfileEditorUiState(
    val activeTab: EditorCategory = EditorCategory.Background,
    val bgItems: List<EditorItemUi> = emptyList(),
    val charItems: List<EditorItemUi> = emptyList(),
    val selectedBgInventoryId: Long? = null,
    val selectedCharInventoryId: Long? = null,
    val charX: Float = 0.5f,
    val charY: Float = 0.5f,
    val charScale: Float = 1.0f,
    val charShadow: Boolean = true,
    val customText: String = "",
    val textStyle: String = "REGULAR",
    /** 요소별 편집 상태 — 닉네임/한마디/기록. */
    val nicknameEl: TextElementState = TextElementState(),
    val taglineEl: TextElementState = TextElementState(y = 0.57f, pill = "NONE"),
    val statsEl: TextElementState = TextElementState(x = 0.16f, y = 0.90f, pill = "BLUR", color = "#00F5FF"),
    /** 텍스트 외곽선(테두리) 표시 여부. */
    val textOutline: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val saveError: String? = null,
    /** Live Preview 카드 닉네임 — 현재 로그인 사용자의 닉네임. 미로드 상태에선 빈 문자열. */
    val nickname: String = "",
    /** [customText]가 비었을 때 카드에 표시할 기본 한 줄 소개 (Home과 동일 문구). */
    val taglineFallback: String = "수영을 사랑하는 수달",
    /** Live Preview 카드 통계 문구 — 이번 달 누적 거리·횟수 (Home과 동일 포맷). */
    val statsText: String = "",
) {
    fun element(el: TextElement): TextElementState = when (el) {
        TextElement.Nickname -> nicknameEl
        TextElement.Tagline -> taglineEl
        TextElement.Stats -> statsEl
    }
}

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
        /** 테두리 장착 — 편집 UI에선 보류지만 저장값 보존을 위해 유지한다. */
        val selectedFrameInventoryId: Long? = null,
        val charX: Float = 0.5f,
        val charY: Float = 0.5f,
        val charScale: Float = 1.0f,
        val charShadow: Boolean = true,
        val customText: String = "",
        val textStyle: String = "REGULAR",
        // 구 블록 필드 — UI에선 안 쓰고 저장 호환용으로만 보존.
        val textAlign: String = "RIGHT",
        val textX: Float = 0.95f,
        val textY: Float = 0.5f,
        val textScaleStep: Int = 3,
        val nicknameEl: TextElementState = TextElementState(),
        val taglineEl: TextElementState = TextElementState(y = 0.57f, pill = "NONE"),
        val statsEl: TextElementState = TextElementState(x = 0.16f, y = 0.90f, pill = "BLUR", color = "#00F5FF"),
        val textOutline: Boolean = false,
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
                charShadow = savedCard.characterShadow,
                customText = savedCard.customText,
                textStyle = savedCard.textStyle,
                textAlign = savedCard.textAlign,
                textX = savedCard.textX,
                textY = savedCard.textY,
                textScaleStep = savedCard.textScaleStep,
                nicknameEl = TextElementState(
                    show = savedCard.showNickname, x = savedCard.nicknameX, y = savedCard.nicknameY,
                    scaleStep = savedCard.nicknameScaleStep, pill = savedCard.nicknamePill, color = savedCard.nicknameColor,
                    bold = textStyleHasBold(savedCard.nicknameStyle), italic = textStyleHasItalic(savedCard.nicknameStyle),
                    outline = savedCard.nicknameOutline,
                ),
                taglineEl = TextElementState(
                    show = savedCard.showTagline, x = savedCard.taglineX, y = savedCard.taglineY,
                    scaleStep = savedCard.taglineScaleStep, pill = savedCard.taglinePill, color = savedCard.taglineColor,
                    bold = textStyleHasBold(savedCard.taglineStyle), italic = textStyleHasItalic(savedCard.taglineStyle),
                    outline = savedCard.taglineOutline,
                ),
                statsEl = TextElementState(
                    show = savedCard.showStats, x = savedCard.statsX, y = savedCard.statsY,
                    scaleStep = savedCard.statsScaleStep, pill = savedCard.statsPill, color = savedCard.statsColor,
                    bold = textStyleHasBold(savedCard.statsStyle), italic = textStyleHasItalic(savedCard.statsStyle),
                    outline = savedCard.statsOutline,
                ),
                textOutline = savedCard.textOutline,
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
            selectedBgInventoryId = state.selectedBgInventoryId,
            selectedCharInventoryId = state.selectedCharInventoryId,
            charX = state.charX,
            charY = state.charY,
            charScale = state.charScale,
            charShadow = state.charShadow,
            customText = state.customText,
            textStyle = state.textStyle,
            nicknameEl = state.nicknameEl,
            taglineEl = state.taglineEl,
            statsEl = state.statsEl,
            textOutline = state.textOutline,
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
            else -> _editState.value // 텍스트 요소 탭엔 장착 아이템이 없다
        }
    }
    fun setCharX(v: Float) { _editState.value = _editState.value.copy(charX = v) }
    fun setCharY(v: Float) { _editState.value = _editState.value.copy(charY = v) }
    fun setCharScale(v: Float) { _editState.value = _editState.value.copy(charScale = v) }
    fun setCharShadow(v: Boolean) { _editState.value = _editState.value.copy(charShadow = v) }
    fun setCustomText(v: String) { _editState.value = _editState.value.copy(customText = v) }
    fun setTextStyle(v: String) { _editState.value = _editState.value.copy(textStyle = v) }

    /** 대상 요소의 편집 상태를 변환 함수로 갱신한다 — 요소별 커스텀의 단일 진입점. */
    private fun updateElement(el: TextElement, transform: (TextElementState) -> TextElementState) {
        val s = _editState.value
        _editState.value = when (el) {
            TextElement.Nickname -> s.copy(nicknameEl = transform(s.nicknameEl))
            TextElement.Tagline -> s.copy(taglineEl = transform(s.taglineEl))
            TextElement.Stats -> s.copy(statsEl = transform(s.statsEl))
        }
    }

    fun setElementShow(el: TextElement, v: Boolean) = updateElement(el) { it.copy(show = v) }
    fun setElementX(el: TextElement, v: Float) = updateElement(el) { it.copy(x = v.coerceIn(0f, 1f)) }
    fun setElementY(el: TextElement, v: Float) = updateElement(el) { it.copy(y = v.coerceIn(0f, 1f)) }
    fun setElementScaleStep(el: TextElement, v: Int) = updateElement(el) { it.copy(scaleStep = v.coerceIn(1, 5)) }
    fun setElementPill(el: TextElement, v: String) = updateElement(el) { it.copy(pill = v) }
    fun setElementColor(el: TextElement, v: String) = updateElement(el) { it.copy(color = v) }
    fun setElementBold(el: TextElement, v: Boolean) = updateElement(el) { it.copy(bold = v) }
    fun setElementItalic(el: TextElement, v: Boolean) = updateElement(el) { it.copy(italic = v) }
    fun setElementOutline(el: TextElement, v: Boolean) = updateElement(el) { it.copy(outline = v) }
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
                    characterShadow = s.charShadow,
                    customText = s.customText,
                    // 구 전역 필드는 닉네임 값으로 채워 구버전 렌더 호환을 유지한다
                    textStyle = s.nicknameEl.style,
                    textAlign = s.textAlign,
                    textX = s.textX.coerceIn(0f, 1f),
                    textY = s.textY.coerceIn(0f, 1f),
                    textScaleStep = s.textScaleStep,
                    showText = s.nicknameEl.show || s.taglineEl.show,
                    showNickname = s.nicknameEl.show,
                    nicknameX = s.nicknameEl.x, nicknameY = s.nicknameEl.y,
                    nicknameScaleStep = s.nicknameEl.scaleStep,
                    showTagline = s.taglineEl.show,
                    taglineX = s.taglineEl.x, taglineY = s.taglineEl.y,
                    taglineScaleStep = s.taglineEl.scaleStep,
                    showStats = s.statsEl.show,
                    statsX = s.statsEl.x, statsY = s.statsEl.y,
                    statsScaleStep = s.statsEl.scaleStep,
                    nicknamePill = s.nicknameEl.pill,
                    taglinePill = s.taglineEl.pill,
                    statsPill = s.statsEl.pill,
                    nicknameColor = s.nicknameEl.color,
                    taglineColor = s.taglineEl.color,
                    statsColor = s.statsEl.color,
                    nicknameStyle = s.nicknameEl.style,
                    taglineStyle = s.taglineEl.style,
                    statsStyle = s.statsEl.style,
                    nicknameOutline = s.nicknameEl.outline,
                    taglineOutline = s.taglineEl.outline,
                    statsOutline = s.statsEl.outline,
                    textOutline = s.nicknameEl.outline || s.taglineEl.outline || s.statsEl.outline,
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
                    characterShadow = card.characterShadow,
                    customText = card.customText.ifEmpty { null },
                    textStyle = card.textStyle,
                    textAlign = card.textAlign,
                    textX = card.textX,
                    textY = card.textY,
                    textScaleStep = card.textScaleStep,
                    showStats = card.showStats,
                    showText = card.showText,
                    showNickname = card.showNickname,
                    nicknameX = card.nicknameX, nicknameY = card.nicknameY,
                    nicknameScaleStep = card.nicknameScaleStep,
                    showTagline = card.showTagline,
                    taglineX = card.taglineX, taglineY = card.taglineY,
                    taglineScaleStep = card.taglineScaleStep,
                    statsX = card.statsX, statsY = card.statsY,
                    statsScaleStep = card.statsScaleStep,
                    nicknamePill = card.nicknamePill,
                    taglinePill = card.taglinePill,
                    statsPill = card.statsPill,
                    nicknameColor = card.nicknameColor,
                    taglineColor = card.taglineColor,
                    statsColor = card.statsColor,
                    nicknameStyle = card.nicknameStyle,
                    taglineStyle = card.taglineStyle,
                    statsStyle = card.statsStyle,
                    nicknameOutline = card.nicknameOutline,
                    taglineOutline = card.taglineOutline,
                    statsOutline = card.statsOutline,
                    textOutline = card.textOutline,
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
