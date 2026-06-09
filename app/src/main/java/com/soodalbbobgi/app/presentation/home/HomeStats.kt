package com.soodalbbobgi.app.presentation.home

import com.soodalbbobgi.app.domain.model.SwimStats

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
