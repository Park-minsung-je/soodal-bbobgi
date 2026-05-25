package com.soodalbbobgi.app.presentation.gacha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GachaPhase { Idle, Spinning, Result }

data class GachaResultItem(
    val name: String,
    val grade: Grade,
    val kind: String,
    val isNew: Boolean,
    val pearlsEarned: Int,
)

data class BoxInfo(
    val name: String,
    val emoji: String,
    val desc: String,
)

data class GachaUiState(
    val shells: Int = 34,
    val phase: GachaPhase = GachaPhase.Idle,
    val selectedBoxIndex: Int = 0,
    val results: List<GachaResultItem> = emptyList(),
    val resultIndex: Int = 0,
    val pityRemaining: Int = 27,
    val boxes: List<BoxInfo> = listOf(
        BoxInfo("캐릭터 상자", "🦦", "캐릭터 아이템"),
        BoxInfo("배경 상자", "🎨", "배경 아이템"),
        BoxInfo("테두리 상자", "🖼️", "테두리 아이템"),
        BoxInfo("랜덤 상자", "🎁", "랜덤 아이템"),
    ),
)

@HiltViewModel
class GachaViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(GachaUiState())
    val uiState: StateFlow<GachaUiState> = _uiState

    private val demoNames = mapOf(
        "char" to listOf("진주 수달", "수달이", "황금 수달", "코랄 수달", "버블 수달", "바다 수달"),
        "bg" to listOf("오로라", "한밤", "산호초", "딥블루", "노을", "별빛"),
        "frame" to listOf("시안 라인", "골드 라인", "레인보우", "버블", "별자리", "크리스탈"),
        "random" to listOf("진주 수달", "오로라", "시안 라인", "수달이", "한밤", "골드 라인"),
    )

    private val kindForBox = listOf("char", "bg", "frame", "random")

    fun spin(count: Int) {
        val cost = if (count == 1) 1 else 9
        if (_uiState.value.shells < cost) return
        if (_uiState.value.phase != GachaPhase.Idle) return

        _uiState.value = _uiState.value.copy(
            phase = GachaPhase.Spinning,
            shells = _uiState.value.shells - cost,
        )

        viewModelScope.launch {
            delay(1500L)
            val results = (1..count).map { generateResult() }
            _uiState.value = _uiState.value.copy(
                phase = GachaPhase.Result,
                results = results,
                resultIndex = 0,
            )
        }
    }

    private fun generateResult(): GachaResultItem {
        val roll = Math.random()
        val grade = when {
            roll < 0.005 -> Grade.SSR
            roll < 0.100 -> Grade.SR
            roll < 0.500 -> Grade.R
            else -> Grade.N
        }
        val kind = kindForBox[_uiState.value.selectedBoxIndex]
        val names = demoNames[kind] ?: demoNames["random"]!!
        val name = names.random()
        val isNew = Math.random() > 0.4
        val pearls = if (isNew) 0 else grade.pearlValue
        return GachaResultItem(
            name = name,
            grade = grade,
            kind = kind,
            isNew = isNew,
            pearlsEarned = pearls,
        )
    }

    fun nextResult() {
        val state = _uiState.value
        if (state.resultIndex < state.results.size - 1) {
            _uiState.value = state.copy(resultIndex = state.resultIndex + 1)
        }
    }

    fun closeResults() {
        val state = _uiState.value
        val newPity = if (state.results.any { it.grade == Grade.SSR }) {
            30
        } else {
            (state.pityRemaining - state.results.size).coerceAtLeast(0)
        }
        _uiState.value = state.copy(
            phase = GachaPhase.Idle,
            results = emptyList(),
            resultIndex = 0,
            pityRemaining = newPity,
        )
    }

    fun selectBox(index: Int) {
        if (_uiState.value.phase == GachaPhase.Idle) {
            _uiState.value = _uiState.value.copy(selectedBoxIndex = index)
        }
    }
}
