package com.soodalbbobgi.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ServerProfileCard
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.model.ProfileCard
import com.soodalbbobgi.app.domain.repository.GachaRepository
import com.soodalbbobgi.app.domain.repository.InventoryRepository
import com.soodalbbobgi.app.domain.usecase.ProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
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

/** 카테고리 탭 키 */
enum class EditorCategory(val key: String, val label: String) {
    Background("bg", "배경"),
    Character("char", "캐릭터"),
    Frame("frame", "테두리"),
    Text("text", "텍스트"),
}

/** 프로필 에디터 화면의 UI 상태 */
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
)

/**
 * 프로필 에디터 ViewModel.
 *
 * 사용자의 보유 아이템과 현재 프로필 카드 설정을 Room에서 관찰하고,
 * 사용자의 편집 내용을 로컬 + 서버에 저장한다.
 */
@HiltViewModel
class ProfileEditorViewModel @Inject constructor(
    private val userSession: UserSession,
    private val profileUseCase: ProfileUseCase,
    private val inventoryRepository: InventoryRepository,
    private val gachaRepository: GachaRepository,
    private val soodalApi: SoodalApi,
) : ViewModel() {

    private val userId get() = userSession.userId

    /** 사용자가 편집 중인 로컬 상태 */
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
        /** Room에서 받은 초기 ProfileCard로 한 번만 초기화됐는지 */
        val initialized: Boolean = false,
    )

    /** 인벤토리 + items 마스터 캐시를 결합해 카테고리별 UI 리스트 생성 */
    private val itemsByCategoryFlow = inventoryRepository.getAll(userId).map { inventory ->
        val itemIds = inventory.map { it.itemId }.distinct()
        val itemsMap = gachaRepository.getBoxItemsByIds(itemIds).associateBy { it.id }
        inventory to itemsMap
    }

    val uiState: StateFlow<ProfileEditorUiState> = combine(
        itemsByCategoryFlow,
        profileUseCase.getProfileCard(userId),
        _editState,
    ) { (inventory, itemsMap), savedCard, edit ->
        // 최초 1회: Room의 ProfileCard로 편집 상태 초기화
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
            // 카드가 없으면 기본값으로 초기화 완료 처리
            val initialized = edit.copy(initialized = true)
            _editState.value = initialized
            initialized
        } else edit

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
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileEditorUiState())

    init {
        refreshFromServer()
    }

    /** 서버에서 최신 ProfileCard를 받아 Room을 갱신한다. */
    private fun refreshFromServer() {
        viewModelScope.launch {
            try {
                val res = soodalApi.getProfileCard()
                if (res.success && res.data != null) {
                    val s = res.data
                    profileUseCase.saveProfileCard(ProfileCard(
                        userId = userId,
                        backgroundItemId = s.backgroundItemId,
                        characterItemId = s.characterItemId,
                        borderItemId = s.borderItemId,
                        characterX = s.characterX,
                        characterY = s.characterY,
                        characterScale = s.characterScale,
                        customText = s.customText ?: "",
                        textStyle = s.textStyle,
                    ))
                }
            } catch (e: Exception) {
                Timber.w(e, "ProfileCard 서버 조회 실패 (오프라인일 수 있음)")
            }
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

    fun setCharX(value: Float) { _editState.value = _editState.value.copy(charX = value) }
    fun setCharY(value: Float) { _editState.value = _editState.value.copy(charY = value) }
    fun setCharScale(value: Float) { _editState.value = _editState.value.copy(charScale = value) }
    fun setCustomText(value: String) { _editState.value = _editState.value.copy(customText = value) }
    fun setTextStyle(value: String) { _editState.value = _editState.value.copy(textStyle = value) }

    fun clearSaveResult() {
        _editState.value = _editState.value.copy(saveSuccess = false, saveError = null)
    }

    /** 편집 내용을 로컬 Room + 서버에 저장한다. */
    fun save() {
        val s = _editState.value
        viewModelScope.launch {
            _editState.value = s.copy(isSaving = true, saveError = null)
            try {
                val card = ProfileCard(
                    userId = userId,
                    backgroundItemId = s.selectedBgInventoryId,
                    characterItemId = s.selectedCharInventoryId,
                    borderItemId = s.selectedFrameInventoryId,
                    characterX = s.charX,
                    characterY = s.charY,
                    characterScale = s.charScale,
                    customText = s.customText,
                    textStyle = s.textStyle,
                )
                profileUseCase.saveProfileCard(card)

                // 서버에도 저장 (실패해도 로컬은 저장됨)
                try {
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
                } catch (e: Exception) {
                    Timber.w(e, "ProfileCard 서버 저장 실패 (로컬 저장은 완료)")
                }

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
        itemsMap: Map<Long, com.soodalbbobgi.app.domain.model.GachaBoxItem>,
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
                imageAsset = meta.imageAsset,
                isSelected = inv.id == selectedId,
            )
        }
        .sortedByDescending { it.grade.ordinal }
}
