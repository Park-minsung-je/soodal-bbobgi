package com.soodalbbobgi.app.presentation.home

import com.soodalbbobgi.app.domain.model.SwimStats
import java.time.LocalDate
import java.time.YearMonth

/** 이번 달 − 지난 달. 양수=더 함, 음수=덜 함, 0=같음. */
data class MonthDelta(
    val distanceDeltaM: Int,
    val countDelta: Int,
    val kcalDelta: Int,
)

/** 이번 달 통계에서 지난 달 통계를 뺀 차이를 계산한다. */
fun monthDelta(current: SwimStats, last: SwimStats): MonthDelta =
    MonthDelta(
        distanceDeltaM = current.totalDistanceMeters - last.totalDistanceMeters,
        countDelta = current.swimCount - last.swimCount,
        kcalDelta = current.totalCalories - last.totalCalories,
    )

/** 지난 달 대비 횟수 변화 문구. */
fun countDeltaPhrase(countDelta: Int): String = when {
    countDelta > 0 -> "지난달보다 ${countDelta}회 더"
    countDelta < 0 -> "지난달보다 ${-countDelta}회 적게"
    else -> "지난달과 같은 횟수로"
}

/**
 * 진행 중인 이번 달과 공정하게 비교할 지난달 '같은 기간'의 끝 날짜를 구한다.
 *
 * 이번 달이 진행 중일 때 지난달 전체와 비교하면 항상 뒤처져 보이므로,
 * 오늘이 15일이면 지난달도 1일~15일까지의 페이스로 비교한다.
 * 지난달이 더 짧으면(예: 3/30 기준의 2월) 지난달 말일로 맞춘다.
 *
 * @param today 비교 기준 날짜 (보통 오늘)
 * @return 지난달 비교 구간의 마지막 날짜
 */
fun lastMonthSamePeriodEnd(today: LocalDate): LocalDate {
    val lastMonth = YearMonth.from(today).minusMonths(1)
    return lastMonth.atDay(minOf(today.dayOfMonth, lastMonth.lengthOfMonth()))
}
