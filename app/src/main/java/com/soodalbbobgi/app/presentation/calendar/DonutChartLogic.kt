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

/**
 * 같은 쪽 리더선 라벨들의 y좌표를 위에서부터 차례로 최소 간격 이상 벌린다.
 * 입력은 정렬돼 있지 않아도 되며, 원래 순서를 유지한 채 보정값을 돌려준다.
 *
 * @param ys 라벨 y좌표 목록 (위쪽이 작은 값)
 * @param minGap 라벨 간 최소 세로 간격
 */
fun spreadLabelYs(ys: List<Float>, minGap: Float): List<Float> {
    if (ys.isEmpty()) return emptyList()
    val indexed = ys.withIndex().sortedBy { it.value }.toMutableList()
    for (i in 1 until indexed.size) {
        if (indexed[i].value - indexed[i - 1].value < minGap) {
            indexed[i] = IndexedValue(indexed[i].index, indexed[i - 1].value + minGap)
        }
    }
    val out = FloatArray(ys.size)
    indexed.forEach { (i, v) -> out[i] = v }
    return out.toList()
}
