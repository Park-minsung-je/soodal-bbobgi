package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.GachaPullRequest
import com.soodalbbobgi.app.domain.model.GachaBoxWithDrops
import com.soodalbbobgi.app.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GachaPhase { Idle, Spinning, Result }

data class BoxInfo(
    val id: String,
    val icon: SoodalIcons,
    val label: String,
    val color: Color,
)

data class GachaResultItem(
    val name: String,
    val grade: Grade,
    val kind: String,
    val isNew: Boolean,
    val pearlsEarned: Int,
    val imageAsset: String? = null,
)

data class GachaUiState(
    val shells: Int = 0,
    val phase: GachaPhase = GachaPhase.Idle,
    val offset: Float = 0f,
    val results: List<GachaResultItem> = emptyList(),
    val pityRemaining: Int = 90,
)

val GACHA_BOXES = listOf(
    BoxInfo("char", SoodalIcons.Box, "캐릭터 상자", Color(0xFFFFD60A)),
    BoxInfo("bg", SoodalIcons.Wave, "배경 상자", Color(0xFF00F5FF)),
    BoxInfo("frame", SoodalIcons.Frame, "테두리 상자", Color(0xFFBF5AF2)),
    BoxInfo("mystery", SoodalIcons.Gift, "랜덤 상자", Color(0xFFE0B0FF)),
)

const val ITEM_WIDTH_WITH_GAP = 140f

private const val SPIN_ACCEL_PHASE = 0.25f
private const val SPIN_ACCEL_PROGRESS = 0.4f
private const val SPIN_DECEL_POWER = 4
private const val SPIN_DURATION_MS = 4500L
private const val SPIN_PAUSE_MS = 800L

/**
 * 뽑기 화면 ViewModel.
 *
 * - currency/pity는 [AppState.currency]에서 관찰
 * - 박스 목록은 [AppState.gachaBoxes]에서 관찰 (진입 시 [AppStateLoader.refreshGachaBoxes]로 새로고침)
 * - 뽑기 pull 결과는 [AppStateLoader.applyGachaResults]로 메모리에 즉시 반영
 */
@HiltViewModel
class GachaViewModel @Inject constructor(
    private val userSession: UserSession,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val soodalApi: SoodalApi,
) : ViewModel() {

    private val _localState = MutableStateFlow(LocalGachaState())

    data class LocalGachaState(
        val phase: GachaPhase = GachaPhase.Idle,
        val offset: Float = 0f,
        val results: List<GachaResultItem> = emptyList(),
    )

    val uiState: StateFlow<GachaUiState> = combine(
        appState.currency, _localState,
    ) { currency, local ->
        GachaUiState(
            shells = currency.shellBalance,
            phase = local.phase,
            offset = local.offset,
            results = local.results,
            pityRemaining = 90 - currency.pityCounter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GachaUiState())

    val boxes: StateFlow<List<BoxInfo>> = appState.gachaBoxes.map { list ->
        list.map { box ->
            BoxInfo(
                id = box.id.toString(),
                icon = iconFor(box.category),
                label = box.name,
                color = colorFor(box.category),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GACHA_BOXES)

    init {
        // 화면 진입 시 박스 목록 + 인벤토리 + 잔액 새로고침
        viewModelScope.launch {
            appStateLoader.refreshGachaBoxes()
            appStateLoader.refreshInventory()
            appStateLoader.refreshCurrency()
        }
        // Idle 룰렛 자동 회전 (코루틴 cancel 시 종료되도록 isActive 체크)
        viewModelScope.launch {
            while (isActive) {
                delay(16)
                if (_localState.value.phase == GachaPhase.Idle) {
                    _localState.update { it.copy(offset = it.offset + 0.25f) }
                }
            }
        }
    }

    /** 룰렛을 자연스럽게 감속해 정지시킨 뒤, 매 뽑기마다 랜덤 박스로 서버 뽑기 실행. */
    fun spin(count: Int) {
        // 할인 없음: 단발 1, 10연 10 (1회당 조개 1개)
        val cost = count
        val s = uiState.value
        if (s.shells < cost || s.phase != GachaPhase.Idle) return

        _localState.update { it.copy(phase = GachaPhase.Spinning) }

        viewModelScope.launch {
            val startOffset = _localState.value.offset
            val activeBoxes: List<GachaBoxWithDrops> = appState.gachaBoxes.value
            if (activeBoxes.isEmpty()) {
                _localState.update { it.copy(phase = GachaPhase.Idle) }
                return@launch
            }
            val selectedBox = activeBoxes.random()
            val resultBoxIndex = activeBoxes.indexOf(selectedBox)

            // 애니메이션과 병행: 서버에서 가챠 실행 (mixed=true → 매 뽑기마다 랜덤 박스)
            val pullDeferred = async(Dispatchers.IO) {
                soodalApi.gachaPull(GachaPullRequest(count = count, mixed = true))
            }

            val boxCount = activeBoxes.size.coerceAtLeast(1)
            val currentSlot = (startOffset / ITEM_WIDTH_WITH_GAP).toInt()
            val extraSlots = (6 + (0..2).random()) * boxCount
            var targetSlot = currentSlot + extraSlots
            while (((targetSlot % boxCount) + boxCount) % boxCount != resultBoxIndex) {
                targetSlot++
            }
            val targetOffset = targetSlot * ITEM_WIDTH_WITH_GAP
            val totalDistance = targetOffset - startOffset

            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= SPIN_DURATION_MS) break
                val k = (elapsed.toFloat() / SPIN_DURATION_MS).coerceIn(0f, 1f)
                val eased = if (k < SPIN_ACCEL_PHASE) {
                    val t = k / SPIN_ACCEL_PHASE
                    SPIN_ACCEL_PROGRESS * t * t
                } else {
                    val t = (k - SPIN_ACCEL_PHASE) / (1f - SPIN_ACCEL_PHASE)
                    val oneMinusT = 1f - t
                    var decel = oneMinusT
                    repeat(SPIN_DECEL_POWER - 1) { decel *= oneMinusT }
                    SPIN_ACCEL_PROGRESS + (1f - SPIN_ACCEL_PROGRESS) * (1f - decel)
                }
                _localState.update { it.copy(offset = startOffset + totalDistance * eased) }
                delay(16)
            }
            _localState.update { it.copy(offset = targetOffset) }
            delay(SPIN_PAUSE_MS)

            val response = pullDeferred.await()
            if (response.success && response.data != null) {
                val batch = response.data.results.map { r ->
                    GachaResultItem(
                        name = r.item.name,
                        grade = Grade.fromString(r.item.grade),
                        // 혼합 뽑기: 아이템별 실제 출처 박스 카테고리 사용
                        kind = r.item.category ?: selectedBox.category,
                        isNew = r.wasNew,
                        pearlsEarned = r.pearlsEarned,
                        imageAsset = r.item.imageAsset,
                    )
                }
                // 서버 응답으로 메모리 즉시 반영 (currency + 신규 인벤토리)
                appStateLoader.applyGachaResults(response.data.results, response.data.currency)

                _localState.update {
                    it.copy(phase = GachaPhase.Result, results = batch)
                }
            } else {
                _localState.update { it.copy(phase = GachaPhase.Idle) }
            }
        }
    }

    fun closeResults() {
        _localState.update { it.copy(phase = GachaPhase.Idle, results = emptyList()) }
    }

    private fun iconFor(category: String): SoodalIcons = when (category) {
        "char" -> SoodalIcons.Box
        "bg" -> SoodalIcons.Wave
        "frame" -> SoodalIcons.Frame
        else -> SoodalIcons.Gift
    }

    private fun colorFor(category: String): Color = when (category) {
        "char" -> Color(0xFFFFD60A)
        "bg" -> Color(0xFF00F5FF)
        "frame" -> Color(0xFFBF5AF2)
        else -> Color(0xFFE0B0FF)
    }
}
