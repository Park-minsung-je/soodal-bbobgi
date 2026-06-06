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
 * 영법 슬라이더 입력값을 기록된 거리 안으로 제한한다.
 * 수정은 입력된 기록(총 거리)의 재분배만 허용 — 다른 영법 합계를 뺀 잔여까지만 늘릴 수 있다.
 *
 * @param desired 사용자가 끌어온 값(m)
 * @param othersSum 혼영을 제외한 다른 영법들의 합(m)
 * @param distanceM 기록된 총 거리(m)
 */
fun clampStrokeMeters(desired: Int, othersSum: Int, distanceM: Int): Int =
    desired.coerceIn(0, (distanceM - othersSum).coerceAtLeast(0))
