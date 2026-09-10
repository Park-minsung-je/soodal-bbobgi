package kr.ilf.soodalbbobgi.presentation.common

import kr.ilf.soodalbbobgi.domain.model.SwimLog
import java.time.DayOfWeek
import java.time.LocalDate

/** 주간 활동 막대 하나. strokeMeters 순서: [자유형, 평영, 배영, 접영, 킥판, 혼영]. */
data class WeekdayBar(
    val label: String,
    val distanceM: Int,
    val isToday: Boolean,
    val strokeMeters: List<Int>,
)

/** 최근 7일 활동 + 지난주 대비 추세. */
data class WeeklyActivity(
    val days: List<WeekdayBar> = emptyList(),
    val totalMeters: Int = 0,
    val activeDays: Int = 0,
    val trendPercent: Int? = null,
)

/**
 * 주간 거리 추세(%) — 지난주 대비 증감률. 지난주 기록이 0이면 비교 불가(null).
 *
 * @param thisWeekMeters 이번 주 누적 거리(m)
 * @param lastWeekMeters 지난주 누적 거리(m)
 */
fun weekTrendPercent(thisWeekMeters: Int, lastWeekMeters: Int): Int? =
    if (lastWeekMeters <= 0) null
    else Math.round((thisWeekMeters - lastWeekMeters).toFloat() / lastWeekMeters * 100f)

private val WEEKDAY_LABELS = mapOf(
    DayOfWeek.SUNDAY to "일", DayOfWeek.MONDAY to "월", DayOfWeek.TUESDAY to "화",
    DayOfWeek.WEDNESDAY to "수", DayOfWeek.THURSDAY to "목", DayOfWeek.FRIDAY to "금",
    DayOfWeek.SATURDAY to "토",
)

/** 최근 14일 로그에서 최근 7일 막대 + 지난주(그 이전 7일) 대비 추세를 만든다. 같은 날 여러 세션은 합산. */
fun buildWeeklyActivity(logs: List<SwimLog>, today: LocalDate): WeeklyActivity {
    val byDate = logs.groupBy { it.date }
    val days = (0..6).map { i ->
        val d = today.minusDays((6 - i).toLong())
        val dayLogs = byDate[d.toString()].orEmpty()
        WeekdayBar(
            label = WEEKDAY_LABELS.getValue(d.dayOfWeek),
            distanceM = dayLogs.sumOf { it.distanceMeters },
            isToday = d == today,
            strokeMeters = if (dayLogs.isNotEmpty()) {
                listOf(
                    dayLogs.sumOf { it.strokeFreestyleM },
                    dayLogs.sumOf { it.strokeBreastM },
                    dayLogs.sumOf { it.strokeBackM },
                    dayLogs.sumOf { it.strokeFlyM },
                    dayLogs.sumOf { it.strokeKickM },
                    dayLogs.sumOf { it.strokeMixedM },
                )
            } else {
                emptyList()
            },
        )
    }
    val thisWeek = days.sumOf { it.distanceM }
    // 지난주 = 최근 7일 막대 이전(today-13 ~ today-7)
    val weekCutoff = today.minusDays(6).toString()
    val lastWeek = logs.filter { it.date < weekCutoff }.sumOf { it.distanceMeters }
    return WeeklyActivity(
        days = days,
        totalMeters = thisWeek,
        activeDays = days.count { it.distanceM > 0 },
        trendPercent = weekTrendPercent(thisWeek, lastWeek),
    )
}

/**
 * 연속 수영 일수(스트릭). 오늘 기록이 있으면 오늘부터, 없으면 어제부터 거꾸로 센다.
 * (오늘 아직 수영을 안 했다고 스트릭이 끊겨 보이지 않게 하루 유예)
 */
fun swimStreak(swimDates: Set<LocalDate>, today: LocalDate): Int {
    var d = if (today in swimDates) today else today.minusDays(1)
    var streak = 0
    while (d in swimDates) {
        streak++
        d = d.minusDays(1)
    }
    return streak
}
