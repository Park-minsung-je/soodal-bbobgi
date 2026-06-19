package com.soodalbbobgi.app.presentation.profile

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.model.Item
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.time.YearMonth
import javax.inject.Inject

/** 갤러리 저장/공유 작업 상태 */
sealed interface SaveState {
    data object Idle : SaveState
    data object Saving : SaveState
    data object Success : SaveState
    data class Error(val message: String) : SaveState
}

/**
 * 전체보기 카드 렌더링에 필요한 상태 묶음.
 * [statsText]는 Home과 동일하게 이번 달 누적 거리/횟수를 "Nm · N회" 형식으로 담는다.
 */
data class FullscreenCardState(
    val nickname: String = "",
    val tagline: String = "수영을 사랑하는 수달",
    val statsText: String = "",
    val bgAsset: String? = null,
    val charAsset: String? = null,
    val frameAsset: String? = null,
    val charX: Float = 0.5f,
    val charY: Float = 0.5f,
    val charScale: Float = 1.0f,
    /** 텍스트 글꼴 스타일 ("REGULAR" | "BOLD" | "ITALIC"). */
    val textStyle: String = "REGULAR",
    /** 텍스트 블록 내부 줄 정렬 ("LEFT" | "RIGHT"). */
    val textAlign: String = "RIGHT",
    /** 텍스트 블록 가로 위치 (0~1). */
    val textX: Float = 0.95f,
    /** 텍스트 블록 세로 중심 위치 (0~1). */
    val textY: Float = 0.5f,
    /** 텍스트 블록 크기 단계 (1~5). */
    val textScaleStep: Int = 3,
    /** 기록 줄 표시 여부. */
    val showStats: Boolean = true,
    /** 닉네임 색상 ("#RRGGBB"). */
    val nicknameColor: String = "#FFFFFF",
    /** 소개 줄 색상 ("#RRGGBB"). */
    val taglineColor: String = "#FFFFFF",
    /** 기록 줄 색상 ("#RRGGBB"). */
    val statsColor: String = "#00F5FF",
    /** 텍스트 외곽선(테두리) 표시 여부. */
    val textOutline: Boolean = false,
)

/**
 * 프로필 카드 전체보기 화면의 카드 데이터와 저장/공유 상태를 관리한다.
 *
 * Home과 동일하게 [AppState]의 profile + profileCard + inventory + items를 결합해
 * [FullscreenCardState]를 노출한다. ProfileCard 슬롯이 서버에서 inventory.id로
 * 저장되는 점도 동일하게 인벤토리 한 단계 인다이렉션으로 해결한다.
 */
@HiltViewModel
class ProfileFullscreenViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appState: AppState,
    private val swimLogUseCase: SwimLogUseCase,
) : ViewModel() {

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState

    // 초기값을 null로 둔다 — 실제 데이터가 결합되기 전(null)에는 전체보기 오버레이가
    // 카드를 공개하지 않아, 에셋이 비어 보이는 흰 베이스 카드가 한 프레임 노출되는 걸 막는다.
    val cardState: StateFlow<FullscreenCardState?> = combine(
        appState.profile,
        appState.profileCard,
        appState.inventory,
        appState.items,
    ) { profile, card, inventory, items ->
        // combine 변환 람다는 suspend라 getMonthStats(suspend)를 직접 호출할 수 있다.
        // Home과 동일한 이번 달 통계 + 동일 포맷("Nm · N회")으로 카드 일관성 유지.
        val stats = swimLogUseCase.getMonthStats(monthStart(), monthEnd())
        FullscreenCardState(
            nickname = profile?.nickname ?: "",
            tagline = card?.customText?.takeIf { it.isNotBlank() } ?: "수영을 사랑하는 수달",
            statsText = "${stats.totalDistanceMeters}m · ${stats.swimCount}회",
            bgAsset = resolveCardAsset(card?.backgroundItemId, inventory, items),
            charAsset = resolveCardAsset(card?.characterItemId, inventory, items),
            frameAsset = resolveCardAsset(card?.borderItemId, inventory, items),
            charX = card?.characterX ?: 0.5f,
            charY = card?.characterY ?: 0.5f,
            charScale = card?.characterScale ?: 1.0f,
            textStyle = card?.textStyle ?: "REGULAR",
            textAlign = card?.textAlign ?: "RIGHT",
            textX = card?.textX ?: 0.95f,
            textY = card?.textY ?: 0.5f,
            textScaleStep = card?.textScaleStep ?: 3,
            showStats = card?.showStats ?: true,
            nicknameColor = card?.nicknameColor ?: "#FFFFFF",
            taglineColor = card?.taglineColor ?: "#FFFFFF",
            statsColor = card?.statsColor ?: "#00F5FF",
            textOutline = card?.textOutline ?: false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 저장된 ProfileCard 슬롯 값으로 표시할 이미지 에셋 경로를 해결한다.
     * 서버가 인벤토리 PK(`inv.id`)를 슬롯에 저장하는 관행 때문에 inventory를 한번 더
     * 조회한 뒤 itemId로 아이템 마스터에서 imageAsset을 가져온다.
     */
    private fun resolveCardAsset(
        inventoryId: Long?,
        inventory: List<InventoryItem>,
        items: Map<Long, Item>,
    ): String? {
        if (inventoryId == null) return null
        val inv = inventory.firstOrNull { it.id == inventoryId } ?: return null
        return items[inv.itemId]?.imageAsset
    }

    /** 상태를 초기화한다. Success/Error 토스트 표시 후 호출. */
    fun resetState() {
        _saveState.value = SaveState.Idle
    }

    /**
     * 카드 Bitmap을 갤러리에 PNG로 저장한다.
     * Android 10+ MediaStore API 사용, 별도 권한 불필요.
     *
     * @param bitmap 저장할 프로필 카드 Bitmap
     */
    fun saveToGallery(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            _saveState.value = SaveState.Saving
            try {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "soodal_card_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SoodalBbobgi")
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                )
                uri?.let {
                    context.contentResolver.openOutputStream(it)?.use { os ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
                    }
                } ?: throw IllegalStateException("MediaStore URI 생성 실패")
                _saveState.value = SaveState.Success
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "저장 실패")
            }
        }
    }

    /**
     * 카드 Bitmap을 임시 파일로 저장 후 Android ShareSheet를 실행한다.
     *
     * @param bitmap 공유할 프로필 카드 Bitmap
     */
    fun share(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "shared_cards").apply { mkdirs() }
                val file = File(cacheDir, "soodal_card.png")
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "프로필 카드 공유").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            } catch (e: Exception) {
                _saveState.value = SaveState.Error(e.message ?: "공유 실패")
            }
        }
    }

    /** 이번 달 1일 (YYYY-MM-DD). Home과 동일한 통계 구간 기준. */
    private fun monthStart(): String = YearMonth.now().atDay(1).toString()

    /** 이번 달 말일 (YYYY-MM-DD). Home과 동일한 통계 구간 기준. */
    private fun monthEnd(): String = YearMonth.now().atEndOfMonth().toString()
}
