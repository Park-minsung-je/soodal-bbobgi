package com.soodalbbobgi.app.presentation.calendar

import java.time.YearMonth

/** 달력 한 칸. inMonth=false면 이전/다음 달에 속하는 흐린 칸. */
data class CalendarCell(val day: Int, val inMonth: Boolean)

/**
 * 일요일 시작 6×7(42칸) 달력 그리드를 만든다.
 * 앞쪽 빈칸은 이전 달 말일, 뒤쪽 빈칸은 다음 달 1일부터로 채운다.
 *
 * @param year 연도
 * @param month 월 (1~12)
 * @return 항상 42개의 셀
 */
fun buildMonthCells(year: Int, month: Int): List<CalendarCell> {
    val ym = YearMonth.of(year, month)
    // DayOfWeek는 MON=1..SUN=7 → %7 하면 일요일 시작 오프셋(앞 빈칸 개수)이 된다.
    val firstDow = ym.atDay(1).dayOfWeek.value % 7
    val daysInMonth = ym.lengthOfMonth()
    val daysInPrev = ym.minusMonths(1).lengthOfMonth()

    val cells = ArrayList<CalendarCell>(42)
    for (i in 0 until firstDow) {
        cells.add(CalendarCell(day = daysInPrev - firstDow + 1 + i, inMonth = false))
    }
    for (d in 1..daysInMonth) {
        cells.add(CalendarCell(day = d, inMonth = true))
    }
    var trailing = 1
    while (cells.size < 42) {
        cells.add(CalendarCell(day = trailing++, inMonth = false))
    }
    return cells
}

/**
 * 영법 비율(%) — 합계 기준 자동 환산. 합계가 0이면 0.
 *
 * @param meters 해당 영법 거리(m)
 * @param sumMeters 모든 영법 거리 합(m)
 */
fun strokePercent(meters: Int, sumMeters: Int): Int =
    if (sumMeters <= 0) 0 else Math.round(meters.toFloat() / sumMeters * 100f)

/**
 * 주간 거리 추세(%) — 지난주 대비 증감률. 지난주 기록이 0이면 비교 불가(null).
 *
 * @param thisWeekMeters 이번 주 누적 거리(m)
 * @param lastWeekMeters 지난주 누적 거리(m)
 */
fun weekTrendPercent(thisWeekMeters: Int, lastWeekMeters: Int): Int? =
    if (lastWeekMeters <= 0) null
    else Math.round((thisWeekMeters - lastWeekMeters).toFloat() / lastWeekMeters * 100f)
