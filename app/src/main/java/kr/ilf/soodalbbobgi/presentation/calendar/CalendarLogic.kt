package kr.ilf.soodalbbobgi.presentation.calendar

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
 * 평균 페이스 — 100m당 소요 시간(초). 거리나 시간이 없으면 계산 불가(null).
 *
 * @param distanceM 총 거리(m)
 * @param durationSec 총 시간(초)
 */
fun paceSecPer100m(distanceM: Int, durationSec: Int): Int? =
    if (distanceM <= 0 || durationSec <= 0) null
    else Math.round(durationSec * 100f / distanceM)

/** 페이스(초)를 분'초" 형식으로 표기한다. 예: 126 → 2'06". */
fun formatPace(sec: Int): String = "${sec / 60}'${"%02d".format(sec % 60)}\""

/**
 * 영법 그리드 표시 정책 — 거리 상위 [count]개만 보여준다.
 * 동률이면 입력 순서를 유지한다 (호출자가 자유형>평영>배영>접영>혼영>킥판 우선순위로 전달).
 * 기록이 적어도 항상 [count]개를 채운다 (0m 영법도 우선순위 순으로 포함).
 *
 * @param entriesInPriorityOrder (영법, 거리m) — 우선순위 순서로 정렬된 입력
 */
fun <T> topStrokes(entriesInPriorityOrder: List<Pair<T, Int>>, count: Int = 4): List<Pair<T, Int>> =
    entriesInPriorityOrder.sortedByDescending { it.second }.take(count)

/**
 * 그래프 세그먼트 정렬 — 많이 한 영법이 앞(왼쪽)/위에 오도록 거리 내림차순 (2026-06-12 결정).
 * 동률은 입력 순서를 유지한다 (호출자가 우선순위 순으로 전달).
 *
 * @param entries (세그먼트, 거리m) — 우선순위 순서로 정렬된 입력
 */
fun <T> sortedStrokeSegments(entries: List<Pair<T, Int>>): List<Pair<T, Int>> =
    entries.sortedByDescending { it.second }

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

/**
 * 영법 거리를 step 배수로 한 단계 올리거나 내린다.
 * 직접 입력 등으로 배수가 아닌 값이면 가장 가까운 다음/이전 배수로 스냅한다.
 * 하한은 0 — 상한은 호출자가 [clampStrokeMeters]로 잔여 거리에 맞춰 제한한다.
 *
 * @param current 현재 값(m)
 * @param step 조절 단위(m) — 수영장 길이 기본 25
 * @param up true면 증가, false면 감소
 */
fun stepStrokeMeters(current: Int, step: Int, up: Boolean): Int {
    val stepped = if (up) {
        (current / step + 1) * step
    } else {
        if (current % step == 0) current - step else (current / step) * step
    }
    return stepped.coerceAtLeast(0)
}
