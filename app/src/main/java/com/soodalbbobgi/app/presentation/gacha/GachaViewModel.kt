package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
)

data class GachaUiState(
    val shells: Int = 34,
    val phase: GachaPhase = GachaPhase.Idle,
    val offset: Float = 0f,
    val results: List<GachaResultItem> = emptyList(),
    val resultIndex: Int = 0,
    val pityRemaining: Int = 27,
)

val GACHA_BOXES = listOf(
    BoxInfo("char", SoodalIcons.Box, "캐릭터 상자", Color(0xFFFFD60A)),
    BoxInfo("bg", SoodalIcons.Wave, "배경 상자", Color(0xFF00F5FF)),
    BoxInfo("frame", SoodalIcons.Frame, "테두리 상자", Color(0xFFBF5AF2)),
    BoxInfo("mystery", SoodalIcons.Gift, "랜덤 상자", Color(0xFFE0B0FF)),
)

const val ITEM_WIDTH_WITH_GAP = 140f

/** idle 속도에서 서서히 가속하는 구간 비율 (0.25 = 전체의 25%) */
private const val SPIN_ACCEL_PHASE = 0.25f
/** 가속 구간에서 소화하는 거리 비율 (0.4 = 전체 거리의 40%) */
private const val SPIN_ACCEL_PROGRESS = 0.4f
/** 감속 강도 — 높을수록 끝에서 더 천천히 멈춤 (긴장감 조절) */
private const val SPIN_DECEL_POWER = 4
/** 스핀 총 시간 (ms) */
private const val SPIN_DURATION_MS = 4500L
/** 정지 후 결과 모달까지 대기 시간 (ms) */
private const val SPIN_PAUSE_MS = 800L

@HiltViewModel
class GachaViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(GachaUiState())
    val uiState: StateFlow<GachaUiState> = _uiState

    init {
        viewModelScope.launch {
            while (true) {
                delay(16)
                val current = _uiState.value
                if (current.phase == GachaPhase.Idle) {
                    _uiState.update { it.copy(offset = it.offset + 0.25f) }
                }
            }
        }
    }

    private val demoResults = listOf(
        GachaResultItem("진주 수달", Grade.SR, "char", true, 0),
        GachaResultItem("바다 수달", Grade.R, "char", false, 3),
        GachaResultItem("한밤", Grade.N, "bg", false, 1),
        GachaResultItem("황금 수달", Grade.SSR, "char", true, 0),
        GachaResultItem("시안 라인", Grade.R, "frame", true, 0),
        GachaResultItem("오로라", Grade.SR, "bg", true, 0),
        GachaResultItem("수달이", Grade.N, "char", false, 1),
        GachaResultItem("진주 액자", Grade.SR, "frame", true, 0),
        GachaResultItem("코랄", Grade.R, "char", true, 0),
        GachaResultItem("버블", Grade.R, "bg", true, 0),
    )
    private var resultCursor = 0

    /**
     * 뽑기를 실행한다. 결과 상자를 미리 결정한 뒤 룰렛을 자연스럽게 감속시켜 해당 상자에 정지시킨다.
     *
     * 비용은 단발 1개, 10연 9개(할인). 조개가 부족하거나 이미 회전 중이면 무시한다.
     * 내부적으로 결과가 먼저 정해지고, 룰렛 애니메이션은 연출일 뿐이다.
     *
     * @param count 뽑기 횟수 (1 = 단발, 10 = 10연)
     */
    fun spin(count: Int) {
        val cost = if (count >= 10) 9 else count
        val s = _uiState.value
        if (s.shells < cost || s.phase != GachaPhase.Idle) return

        _uiState.value = s.copy(phase = GachaPhase.Spinning, shells = s.shells - cost)

        viewModelScope.launch {
            val startOffset = _uiState.value.offset
            val resultBoxIndex = (0 until GACHA_BOXES.size).random()

            // 현재 위치에서 6~8바퀴 + 결과 상자까지의 잔여 슬롯으로 목표 offset 산출
            val currentSlot = (startOffset / ITEM_WIDTH_WITH_GAP).toInt()
            val extraSlots = (6 + (0..2).random()) * GACHA_BOXES.size
            var targetSlot = currentSlot + extraSlots
            while (((targetSlot % GACHA_BOXES.size) + GACHA_BOXES.size) % GACHA_BOXES.size != resultBoxIndex) {
                targetSlot++
            }
            val targetOffset = targetSlot * ITEM_WIDTH_WITH_GAP
            val totalDistance = targetOffset - startOffset

            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= SPIN_DURATION_MS) break
                val k = (elapsed.toFloat() / SPIN_DURATION_MS).coerceIn(0f, 1f)

                // 커스텀 이징: 서서히 가속 → 피크 → 끝에서 쫄깃하게 감속
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

                _uiState.update { it.copy(offset = startOffset + totalDistance * eased) }
                delay(16)
            }

            _uiState.update { it.copy(offset = targetOffset) }
            delay(SPIN_PAUSE_MS)

            val batch = (0 until count).map {
                val r = demoResults[resultCursor % demoResults.size]
                resultCursor++
                r
            }
            _uiState.update {
                it.copy(phase = GachaPhase.Result, results = batch, resultIndex = 0)
            }
        }
    }

    fun nextResult() {
        val s = _uiState.value
        if (s.resultIndex < s.results.size - 1) {
            _uiState.value = s.copy(resultIndex = s.resultIndex + 1)
        }
    }

    fun skipToLastResult() {
        val s = _uiState.value
        _uiState.value = s.copy(resultIndex = s.results.size - 1)
    }

    fun closeResults() {
        val s = _uiState.value
        val newPity = if (s.results.any { it.grade == Grade.SSR }) 27
        else (s.pityRemaining - s.results.size).coerceAtLeast(0)
        _uiState.value = s.copy(
            phase = GachaPhase.Idle,
            results = emptyList(),
            resultIndex = 0,
            pityRemaining = newPity,
        )
    }
}
