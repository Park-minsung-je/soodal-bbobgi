package com.soodalbbobgi.app.presentation.calendar

/**
 * 도넛 차트 세그먼트 — 각도는 12시 기준 시계방향(도).
 *
 * @property pct 전체 대비 비율(반올림 %)
 * @property midAngle 세그먼트 중점각 — 리더선 시작점 계산용
 * @property onRight 중점각이 0~180도(시계 오른쪽 절반)면 true — 라벨을 오른쪽에 단다
 */
data class DonutSegment(
    val label: String,
    val meters: Int,
    val pct: Int,
    val startAngle: Float,
    val sweepAngle: Float,
    val midAngle: Float,
    val onRight: Boolean,
)

/**
 * (영법, 거리m) 목록을 도넛 세그먼트로 나눈다. 0m 항목은 제외.
 *
 * @param entries 표시 순서대로의 (라벨, 거리m) 목록
 */
fun donutSegments(entries: List<Pair<String, Int>>): List<DonutSegment> {
    val valid = entries.filter { it.second > 0 }
    val total = valid.sumOf { it.second }
    if (total <= 0) return emptyList()

    var angle = 0f
    return valid.map { (label, meters) ->
        val sweep = meters.toFloat() / total * 360f
        val start = angle
        angle += sweep
        val mid = start + sweep / 2f
        DonutSegment(
            label = label,
            meters = meters,
            pct = Math.round(meters.toFloat() / total * 100f),
            startAngle = start,
            sweepAngle = sweep,
            midAngle = mid,
            onRight = mid % 360f < 180f,
        )
    }
}

