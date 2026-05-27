package com.soodalbbobgi.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import javax.inject.Inject

data class StrokeBreakdown(
    val freestyle: Float = 0f,
    val backstroke: Float = 0f,
    val breaststroke: Float = 0f,
    val butterfly: Float = 0f,
)

data class SwimDayData(
    val distanceM: Int,
    val durationMin: Int,
    val kcal: Int,
    val strokes: StrokeBreakdown,
    val shellReward: Int,
)

data class CalendarUiState(
    val year: Int = YearMonth.now().year,
    val month: Int = YearMonth.now().monthValue,
    val selectedDay: Int? = null,
    val swimData: Map<Int, SwimDayData> = emptyMap(),
)

/**
 * 캘린더 화면 ViewModel.
 * 선택된 월의 수영 기록을 Room DB에서 관찰하여 UI 상태를 구성한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val userSession: UserSession,
    private val swimLogUseCase: SwimLogUseCase,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDay = MutableStateFlow<Int?>(null)

    val uiState: StateFlow<CalendarUiState> = combine(
        _yearMonth,
        _selectedDay,
        _yearMonth.flatMapLatest { ym ->
            val start = "${ym.year}-${"%02d".format(ym.monthValue)}-01"
            val end = "${ym.year}-${"%02d".format(ym.monthValue)}-${"%02d".format(ym.lengthOfMonth())}"
            swimLogUseCase.getLogsByDateRange(start, end)
        },
    ) { ym, selected, logs ->
        val swimMap = logs.associate { log ->
            val day = log.date.substringAfterLast("-").toInt()
            day to SwimDayData(
                distanceM = log.distanceMeters,
                durationMin = log.durationSeconds / 60,
                kcal = log.calories,
                strokes = StrokeBreakdown(
                    freestyle = log.strokeFreeStyle / 100f,
                    breaststroke = log.strokeBreast / 100f,
                    backstroke = log.strokeBack / 100f,
                    butterfly = log.strokeFly / 100f,
                ),
                shellReward = log.shellsEarned,
            )
        }
        CalendarUiState(
            year = ym.year,
            month = ym.monthValue,
            selectedDay = selected,
            swimData = swimMap,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun previousMonth() {
        _yearMonth.update { it.minusMonths(1) }
        _selectedDay.value = null
    }

    fun nextMonth() {
        _yearMonth.update { it.plusMonths(1) }
        _selectedDay.value = null
    }
}
