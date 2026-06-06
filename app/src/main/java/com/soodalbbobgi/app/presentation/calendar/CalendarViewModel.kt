package com.soodalbbobgi.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.UpdateStrokesRequest
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import com.soodalbbobgi.app.presentation.common.WeeklyActivity
import com.soodalbbobgi.app.presentation.common.buildWeeklyActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * 하루치 수영 데이터.
 *
 * freeM~kickM은 영법별 원본 거리(m) — 막대 그래프·상세·수정 시트가 모두 이 값을 쓴다.
 * maxHr·minHr은 최대·최소 심박(bpm)이며 Health Connect 심박 연동 전까지는 null이라 심박 행이 표시되지 않는다.
 */
data class SwimDayData(
    val distanceM: Int,
    val durationMin: Int,
    /** 총 시간(초) — 평균 페이스 계산용 (분 단위는 반올림 오차가 큼). */
    val durationSec: Int,
    val kcal: Int,
    val shellReward: Int,
    val freeM: Int = 0,
    val breastM: Int = 0,
    val backM: Int = 0,
    val flyM: Int = 0,
    val mixedM: Int = 0,
    val kickM: Int = 0,
    val maxHr: Int? = null,
    val minHr: Int? = null,
)

data class CalendarUiState(
    val year: Int = YearMonth.now().year,
    val month: Int = YearMonth.now().monthValue,
    val selectedDay: Int? = null,
    val swimData: Map<Int, SwimDayData> = emptyMap(),
)

/**
 * 캘린더 화면 ViewModel.
 * 선택된 월의 수영 기록과 최근 7일 활동을 Room DB에서 관찰하여 UI 상태를 구성한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val userSession: UserSession,
    private val swimLogUseCase: SwimLogUseCase,
    private val soodalApi: SoodalApi,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    // 진입 시 오늘 날짜를 선택해 "선택한 날" 카드가 비어 보이지 않게 한다.
    private val _selectedDay = MutableStateFlow<Int?>(LocalDate.now().dayOfMonth)

    /** 월과 그 달의 로그를 한 묶음으로 — 월만 먼저 바뀌어 이전 달 데이터가 잠깐 보이는 깜빡임을 막는다. */
    private data class MonthLogs(val ym: YearMonth, val logs: List<SwimLog>)

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedDay,
        _yearMonth.flatMapLatest { ym ->
            val start = "${ym.year}-${"%02d".format(ym.monthValue)}-01"
            val end = "${ym.year}-${"%02d".format(ym.monthValue)}-${"%02d".format(ym.lengthOfMonth())}"
            swimLogUseCase.getLogsByDateRange(start, end).map { logs -> MonthLogs(ym, logs) }
        },
    ) { selected, month ->
        val swimMap = month.logs.associate { log ->
            val day = log.date.substringAfterLast("-").toInt()
            day to log.toDayData()
        }
        CalendarUiState(
            year = month.ym.year,
            month = month.ym.monthValue,
            selectedDay = selected,
            swimData = swimMap,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CalendarUiState())

    // 최근 14일을 조회해 최근 7일 막대 + 지난주(그 이전 7일) 대비 추세를 만든다.
    val weeklyActivity: StateFlow<WeeklyActivity> = run {
        val today = LocalDate.now()
        val rangeStart = today.minusDays(13)
        swimLogUseCase.getLogsByDateRange(rangeStart.toString(), today.toString())
            .map { logs -> buildWeeklyActivity(logs, today) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeeklyActivity())
    }

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

    /** 수정 시트에서 보정한 영법별 거리(m)를 로컬과 서버에 저장한다. */
    fun saveStrokes(day: Int, free: Int, breast: Int, back: Int, fly: Int, kick: Int, mixed: Int) {
        val ym = _yearMonth.value
        val date = "%04d-%02d-%02d".format(ym.year, ym.monthValue, day)
        viewModelScope.launch {
            swimLogUseCase.updateStrokes(date, free, breast, back, fly, mixed, kick)
            // 서버에도 반영 — 로컬 DB가 초기화돼도 분배가 보존되게. 실패해도 로컬 저장은 유지된다.
            try {
                soodalApi.updateSwimLogStrokes(
                    date,
                    UpdateStrokesRequest(
                        strokeFreestyleM = free, strokeBreastM = breast, strokeBackM = back,
                        strokeFlyM = fly, strokeMixedM = mixed, strokeKickM = kick,
                    ),
                )
            } catch (e: Exception) {
                Timber.w(e, "영법 수정 서버 반영 실패 — 로컬에만 저장됨: $date")
            }
        }
    }
}

/** 도메인 로그 → 하루치 표시 데이터. */
private fun SwimLog.toDayData(): SwimDayData = SwimDayData(
    distanceM = distanceMeters,
    durationMin = durationSeconds / 60,
    durationSec = durationSeconds,
    kcal = calories,
    shellReward = shellsEarned,
    freeM = strokeFreestyleM,
    breastM = strokeBreastM,
    backM = strokeBackM,
    flyM = strokeFlyM,
    mixedM = strokeMixedM,
    kickM = strokeKickM,
    maxHr = maxHr,
    minHr = minHr,
)

