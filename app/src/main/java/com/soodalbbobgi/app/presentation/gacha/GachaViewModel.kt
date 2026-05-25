package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.roundToInt

enum class GachaPhase { Idle, Spinning, Result }

data class BoxInfo(
    val id: String,
    val emoji: String,
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
    val focusedBoxIndex: Int = 0,
    val results: List<GachaResultItem> = emptyList(),
    val resultIndex: Int = 0,
    val pityRemaining: Int = 27,
    val boxes: List<BoxInfo> = GACHA_BOXES,
)

val GACHA_BOXES = listOf(
    BoxInfo("char", "📦", "캐릭터 상자", Color(0xFFFFD60A)),
    BoxInfo("bg", "🌊", "배경 상자", Color(0xFF00F5FF)),
    BoxInfo("frame", "🖼️", "테두리 상자", Color(0xFFBF5AF2)),
    BoxInfo("mystery", "🎁", "랜덤 상자", Color(0xFFE0B0FF)),
)

const val ITEM_WIDTH_WITH_GAP = 132f

@HiltViewModel
class GachaViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(GachaUiState())
    val uiState: StateFlow<GachaUiState> = _uiState

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

    fun spin(count: Int) {
        val cost = if (count >= 10) 9 else count
        val s = _uiState.value
        if (s.shells < cost || s.phase != GachaPhase.Idle) return

        _uiState.value = s.copy(phase = GachaPhase.Spinning, shells = s.shells - cost)

        viewModelScope.launch {
            val initialOffset = s.offset
            val targetBoxes = (24 + (0..3).random())
            val targetOffset = initialOffset + targetBoxes * ITEM_WIDTH_WITH_GAP
            val duration = 2400L
            val startTime = System.currentTimeMillis()

            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val k = (elapsed.toFloat() / duration).coerceAtMost(1f)
                val ease = 1f - (1f - k).pow(3)
                val current = initialOffset + (targetOffset - initialOffset) * ease
                _uiState.value = _uiState.value.copy(
                    offset = current,
                    focusedBoxIndex = (current / ITEM_WIDTH_WITH_GAP).roundToInt(),
                )
                if (k >= 1f) break
                delay(16)
            }

            delay(600)

            val batch = (0 until count).map {
                val r = demoResults[resultCursor % demoResults.size]
                resultCursor++
                r
            }
            _uiState.value = _uiState.value.copy(
                phase = GachaPhase.Result,
                results = batch,
                resultIndex = 0,
            )
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
