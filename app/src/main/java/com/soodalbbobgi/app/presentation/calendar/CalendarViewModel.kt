package com.soodalbbobgi.app.presentation.calendar

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth
import javax.inject.Inject

data class StrokeBreakdown(
    val freestyle: Float = 0f,   // free
    val backstroke: Float = 0f,  // back
    val breaststroke: Float = 0f, // breast
    val butterfly: Float = 0f,   // fly
)

data class SwimDayData(
    val day: Int,
    val distanceM: Int,
    val durationMin: Int,
    val kcal: Int,
    val strokes: StrokeBreakdown,
    val shellReward: Int,
)

data class CalendarUiState(
    val year: Int = 2026,
    val month: Int = 5,
    val selectedDay: Int? = 22,
    val swimData: Map<Int, SwimDayData> = emptyMap(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor() : ViewModel() {

    // Demo swim data matching the JSX design prototype
    // Stroke ratios are in 0..1 range (e.g., 0.7 = 70%)
    private val demoSwimData: Map<Int, SwimDayData> = mapOf(
        2 to SwimDayData(2, 800, 25, 180, StrokeBreakdown(0.70f, 0f, 0.30f, 0f), 2),
        4 to SwimDayData(4, 1000, 30, 220, StrokeBreakdown(0.50f, 0.20f, 0.30f, 0f), 2),
        7 to SwimDayData(7, 1500, 40, 310, StrokeBreakdown(0.60f, 0.20f, 0.20f, 0f), 3),
        9 to SwimDayData(9, 600, 20, 140, StrokeBreakdown(1.00f, 0f, 0f, 0f), 1),
        10 to SwimDayData(10, 2000, 55, 420, StrokeBreakdown(0.40f, 0.20f, 0.30f, 0.10f), 4),
        13 to SwimDayData(13, 1200, 35, 260, StrokeBreakdown(0.50f, 0.10f, 0.40f, 0f), 2),
        15 to SwimDayData(15, 900, 28, 195, StrokeBreakdown(0.30f, 0f, 0.70f, 0f), 2),
        16 to SwimDayData(16, 1800, 48, 380, StrokeBreakdown(0.55f, 0.15f, 0.25f, 0.05f), 3),
        18 to SwimDayData(18, 2000, 55, 410, StrokeBreakdown(0.50f, 0.15f, 0.25f, 0.10f), 4),
        20 to SwimDayData(20, 1200, 35, 280, StrokeBreakdown(0.65f, 0.15f, 0.20f, 0f), 2),
        22 to SwimDayData(22, 1500, 42, 320, StrokeBreakdown(0.45f, 0.20f, 0.30f, 0.05f), 3),
        24 to SwimDayData(24, 800, 22, 180, StrokeBreakdown(0f, 0.20f, 0.80f, 0f), 2),
        25 to SwimDayData(25, 1100, 32, 240, StrokeBreakdown(0.55f, 0.20f, 0.25f, 0f), 2),
        27 to SwimDayData(27, 1600, 45, 340, StrokeBreakdown(0.50f, 0.20f, 0.20f, 0.10f), 3),
        29 to SwimDayData(29, 1300, 38, 270, StrokeBreakdown(0.60f, 0.10f, 0.30f, 0f), 2),
    )

    private val _uiState = MutableStateFlow(
        CalendarUiState(swimData = demoSwimData),
    )
    val uiState: StateFlow<CalendarUiState> = _uiState

    fun selectDay(day: Int) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    fun previousMonth() {
        val ym = YearMonth.of(_uiState.value.year, _uiState.value.month).minusMonths(1)
        _uiState.value = _uiState.value.copy(
            year = ym.year,
            month = ym.monthValue,
            selectedDay = null,
            swimData = if (ym.year == 2026 && ym.monthValue == 5) demoSwimData else emptyMap(),
        )
    }

    fun nextMonth() {
        val ym = YearMonth.of(_uiState.value.year, _uiState.value.month).plusMonths(1)
        _uiState.value = _uiState.value.copy(
            year = ym.year,
            month = ym.monthValue,
            selectedDay = null,
            swimData = if (ym.year == 2026 && ym.monthValue == 5) demoSwimData else emptyMap(),
        )
    }
}
