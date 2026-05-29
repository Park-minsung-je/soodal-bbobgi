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

data class ProfileEditorUiState(
    val activeTab: EditorCategory = EditorCategory.Background,
    val bgItems: List<EditorItemUi> = emptyList(),
    val charItems: List<EditorItemUi> = emptyList(),
    val frameItems: List<EditorItemUi> = emptyList(),
    val selectedBgInventoryId: Long? = null,
    val selectedCharInventoryId: Long? = null,
    val selectedFrameInventoryId: Long? = null,
    val charX: Float = 0.16f,
    val charY: Float = 0.06f,
    val charScale: Float = 0.70f,
    val customText: String = "",
    val textStyle: String = "REGULAR",
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
        val charX: Float = 0.16f,
        val charY: Float = 0.06f,
        val charScale: Float = 0.70f,
        val customText: String = "",
        val textStyle: String = "REGULAR",
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
    fun selectItem(category: EditorCategory, inventoryId: Long) {
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
    fun clearSaveResult() { _editState.value = _editState.value.copy(saveSuccess = false, saveError = null) }

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
