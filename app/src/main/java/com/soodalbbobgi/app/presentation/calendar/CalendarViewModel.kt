package com.soodalbbobgi.app.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.core.util.decodeHrRestRanges
import com.soodalbbobgi.app.core.util.decodeHrSeries
import com.soodalbbobgi.app.data.health.HcSwimSyncer
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.UpdateStrokesRequest
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import com.soodalbbobgi.app.presentation.common.WeeklyActivity
import com.soodalbbobgi.app.presentation.common.buildWeeklyActivity
import com.soodalbbobgi.app.presentation.home.ManualEntryInput
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
 * 한 세션(한 번의 수영)의 표시 데이터.
 *
 * freeM~kickM은 영법별 원본 거리(m) — 막대 그래프·상세·수정 시트가 모두 이 값을 쓴다.
 * maxHr·minHr은 최대·최소 심박(bpm)이며 심박 기록이 없으면 null이라 심박 행이 표시되지 않는다.
 */
data class SwimSessionData(
    /** swim_logs 행 id — 영법 수정 저장 대상. */
    val logId: Long,
    /** 세션 시작 시각(epoch 초) — 같은 날 여러 세션의 시간대 라벨용. 서버산이면 null. */
    val startEpochSec: Long? = null,
    val distanceM: Int,
    val durationMin: Int,
    /** 총 경과 시간(초) — 운동시간이 없을 때 페이스 폴백용. */
    val durationSec: Int,
    /** 실제 운동 시간(초) — 심박 추정 기반. 없으면 null. */
    val activeSec: Int? = null,
    val kcal: Int,
    val freeM: Int = 0,
    val breastM: Int = 0,
    val backM: Int = 0,
    val flyM: Int = 0,
    val mixedM: Int = 0,
    val kickM: Int = 0,
    val maxHr: Int? = null,
    val minHr: Int? = null,
    /** 차트용 심박 시계열 (오프셋초, bpm). 없으면 빈 목록. */
    val hrSeries: List<Pair<Int, Int>> = emptyList(),
    /** 휴식으로 계산된 구간 (오프셋초 범위) — 동기화 때 원본 해상도로 분류된 값. */
    val hrRestRanges: List<IntRange> = emptyList(),
)

/**
 * 하루치 수영 데이터 — 하루 여러 세션 전제.
 * 달력 셀·헤더·월 통계는 합계 필드를 쓰고, 상세 카드는 [sessions]를 세션별로 보여준다.
 */
data class SwimDayData(
    /** 그 날의 세션 목록 — 시작 시각 순. */
    val sessions: List<SwimSessionData>,
    val distanceM: Int,
    val kcal: Int,
    val shellReward: Int,
    val freeM: Int = 0,
    val breastM: Int = 0,
    val backM: Int = 0,
    val flyM: Int = 0,
    val mixedM: Int = 0,
    val kickM: Int = 0,
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
    private val hcSwimSyncer: HcSwimSyncer,
    private val appStateLoader: AppStateLoader,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())
    // 진입 시 오늘 날짜를 선택해 "선택한 날" 카드가 비어 보이지 않게 한다.
    private val _selectedDay = MutableStateFlow<Int?>(LocalDate.now().dayOfMonth)

    /** 수동 등록으로 지급된 조개 수 — 0보다 크면 보상 팝업 표시. */
    private val _shellReward = MutableStateFlow(0)
    val shellReward: StateFlow<Int> = _shellReward

    /** 수동 등록 실패 안내 문구 — null이면 표시 없음. */
    private val _registerError = MutableStateFlow<String?>(null)
    val registerError: StateFlow<String?> = _registerError

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
        // 하루 여러 세션 가능 — 날짜별로 묶어 합계 + 세션 목록을 만든다
        val swimMap = month.logs
            .groupBy { it.date.substringAfterLast("-").toInt() }
            .mapValues { (_, logs) -> logs.toDayData() }
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

    /**
     * 선택한 날짜에 수동 입력 기록을 등록한다 — 과거 날짜 보충 기록용 (미래는 UI에서 차단).
     * 등록 후 서버 보고까지 이어지며, 그 날 첫 기록이면 조개가 지급돼 보상 팝업이 뜬다.
     */
    fun registerManual(day: Int, input: ManualEntryInput) {
        val ym = _yearMonth.value
        val date = LocalDate.of(ym.year, ym.monthValue, day)
        if (date.isAfter(LocalDate.now())) return // 미래 날짜 방어
        viewModelScope.launch {
            _shellReward.value = 0
            try {
                val earned = hcSwimSyncer.registerManual(
                    distanceMeters = input.distanceM, durationMin = input.durationMin,
                    calories = input.calories, maxHr = input.maxHr, minHr = input.minHr,
                    date = date, startTime = input.startTime,
                    strokeFreeM = input.freeM, strokeBreastM = input.breastM,
                    strokeBackM = input.backM, strokeFlyM = input.flyM, strokeKickM = input.kickM,
                )
                appStateLoader.refreshCurrency()
                _shellReward.value = earned
            } catch (e: Exception) {
                Timber.e(e, "캘린더 수동 기록 등록 실패: $date")
                _registerError.value = "기록 등록에 실패했어요. 다시 시도해주세요."
            }
        }
    }

    fun clearShellReward() { _shellReward.value = 0 }

    fun clearRegisterError() { _registerError.value = null }

    /** 수정 시트에서 보정한 세션의 영법별 거리(m)를 로컬과 서버에 저장한다. */
    fun saveStrokes(day: Int, logId: Long, free: Int, breast: Int, back: Int, fly: Int, kick: Int, mixed: Int) {
        val ym = _yearMonth.value
        val date = "%04d-%02d-%02d".format(ym.year, ym.monthValue, day)
        viewModelScope.launch {
            swimLogUseCase.updateStrokes(logId, free, breast, back, fly, mixed, kick)
            // 서버에도 반영 — 로컬 DB가 초기화돼도 분배가 보존되게. 서버는 일 단위라
            // 그 날 모든 세션의 합계를 보낸다. 실패해도 로컬 저장은 유지된다.
            try {
                val dayLogs = swimLogUseCase.getLogsForDate(date)
                soodalApi.updateSwimLogStrokes(
                    date,
                    UpdateStrokesRequest(
                        strokeFreestyleM = dayLogs.sumOf { it.strokeFreestyleM },
                        strokeBreastM = dayLogs.sumOf { it.strokeBreastM },
                        strokeBackM = dayLogs.sumOf { it.strokeBackM },
                        strokeFlyM = dayLogs.sumOf { it.strokeFlyM },
                        strokeMixedM = dayLogs.sumOf { it.strokeMixedM },
                        strokeKickM = dayLogs.sumOf { it.strokeKickM },
                    ),
                )
            } catch (e: Exception) {
                Timber.w(e, "영법 수정 서버 반영 실패 — 로컬에만 저장됨: $date")
            }
        }
    }
}

/** 도메인 로그 → 세션 표시 데이터. */
private fun SwimLog.toSessionData(): SwimSessionData = SwimSessionData(
    logId = id,
    startEpochSec = startEpochSec,
    distanceM = distanceMeters,
    durationMin = durationSeconds / 60,
    durationSec = durationSeconds,
    activeSec = activeSeconds,
    kcal = calories,
    freeM = strokeFreestyleM,
    breastM = strokeBreastM,
    backM = strokeBackM,
    flyM = strokeFlyM,
    mixedM = strokeMixedM,
    kickM = strokeKickM,
    maxHr = maxHr,
    minHr = minHr,
    hrSeries = decodeHrSeries(hrSeries),
    hrRestRanges = decodeHrRestRanges(hrSeries),
)

/** 홈/공용에서 SwimLog를 세션 표시 데이터로 변환한다 (영법 수정 시트 입력용). */
fun SwimLog.toHomeSessionData(): SwimSessionData = toSessionData()

/** 같은 날짜의 로그 목록 → 하루치 표시 데이터 (합계 + 세션 목록). */
private fun List<SwimLog>.toDayData(): SwimDayData = SwimDayData(
    sessions = sortedWith(compareBy(nullsLast()) { it.startEpochSec }).map { it.toSessionData() },
    distanceM = sumOf { it.distanceMeters },
    kcal = sumOf { it.calories },
    shellReward = sumOf { it.shellsEarned },
    freeM = sumOf { it.strokeFreestyleM },
    breastM = sumOf { it.strokeBreastM },
    backM = sumOf { it.strokeBackM },
    flyM = sumOf { it.strokeFlyM },
    mixedM = sumOf { it.strokeMixedM },
    kickM = sumOf { it.strokeKickM },
)

