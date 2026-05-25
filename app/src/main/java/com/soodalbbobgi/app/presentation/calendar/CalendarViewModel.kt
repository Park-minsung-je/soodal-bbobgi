package com.soodalbbobgi.app.presentation.calendar

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.YearMonth
import javax.inject.Inject

data class StrokeBreakdown(
    val freestyle: Float = 0f,
    val backstroke: Float = 0f,
    val breaststroke: Float = 0f,
    val butterfly: Float = 0f,
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
    val selectedDay: Int? = null,
    val swimData: Map<Int, SwimDayData> = emptyMap(),
)

@HiltViewModel
class CalendarViewModel @Inject constructor() : ViewModel() {

    private val demoSwimData: Map<Int, SwimDayData> = mapOf(
        1 to SwimDayData(1, 1200, 45, 320, StrokeBreakdown(0.6f, 0.2f, 0.15f, 0.05f), 2),
        3 to SwimDayData(3, 800, 30, 210, StrokeBreakdown(0.5f, 0.3f, 0.2f, 0f), 1),
        5 to SwimDayData(5, 1500, 55, 400, StrokeBreakdown(0.7f, 0.1f, 0.1f, 0.1f), 3),
        6 to SwimDayData(6, 600, 25, 160, StrokeBreakdown(0.4f, 0.4f, 0.2f, 0f), 1),
        8 to SwimDayData(8, 1000, 40, 270, StrokeBreakdown(0.5f, 0.2f, 0.2f, 0.1f), 2),
        10 to SwimDayData(10, 1800, 65, 480, StrokeBreakdown(0.6f, 0.15f, 0.15f, 0.1f), 3),
        12 to SwimDayData(12, 900, 35, 240, StrokeBreakdown(0.55f, 0.25f, 0.2f, 0f), 1),
        14 to SwimDayData(14, 1100, 42, 290, StrokeBreakdown(0.5f, 0.3f, 0.15f, 0.05f), 2),
        16 to SwimDayData(16, 1400, 50, 370, StrokeBreakdown(0.65f, 0.15f, 0.1f, 0.1f), 2),
        18 to SwimDayData(18, 700, 28, 190, StrokeBreakdown(0.4f, 0.3f, 0.3f, 0f), 1),
        19 to SwimDayData(19, 1300, 48, 350, StrokeBreakdown(0.6f, 0.2f, 0.1f, 0.1f), 2),
        21 to SwimDayData(21, 2000, 70, 530, StrokeBreakdown(0.5f, 0.2f, 0.2f, 0.1f), 3),
        22 to SwimDayData(22, 850, 32, 225, StrokeBreakdown(0.45f, 0.35f, 0.2f, 0f), 1),
        24 to SwimDayData(24, 1600, 58, 420, StrokeBreakdown(0.55f, 0.2f, 0.15f, 0.1f), 3),
        25 to SwimDayData(25, 950, 36, 250, StrokeBreakdown(0.5f, 0.25f, 0.25f, 0f), 1),
    )

    private val _uiState = MutableStateFlow(
        CalendarUiState(swimData = demoSwimData),
    )
    val uiState: StateFlow<CalendarUiState> = _uiState

    fun selectDay(day: Int) {
        val current = _uiState.value.selectedDay
        _uiState.value = _uiState.value.copy(
            selectedDay = if (current == day) null else day,
        )
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
