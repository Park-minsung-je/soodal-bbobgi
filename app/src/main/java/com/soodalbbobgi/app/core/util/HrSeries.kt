package com.soodalbbobgi.app.core.util

/**
 * 심박 시계열(오프셋초, bpm)을 "0:98,30:142,..." 컴팩트 문자열로 직렬화한다.
 * swim_logs.hrSeries 컬럼 저장용.
 */
fun encodeHrSeries(points: List<Pair<Int, Int>>): String =
    points.joinToString(",") { "${it.first}:${it.second}" }

/**
 * 휴식 분류 임계값(bpm) — 세션 심박의 하위 0.5% 바닥 + [restBand].
 * 바닥이 [floorCap]보다 높으면 휴식 없는 세션으로 보고 null(분류 안 함).
 * 동기화의 실운동시간 계산과 차트의 휴식 표시가 같은 규칙을 쓰도록 여기에 단일화한다.
 */
fun hrRestThreshold(bpms: List<Int>, restBand: Int = 29, floorCap: Int = 110): Int? {
    if (bpms.isEmpty()) return null
    val sorted = bpms.sorted()
    val floor = sorted[(sorted.size * 5 / 1000).coerceAtMost(sorted.size - 1)]
    return if (floor <= floorCap) floor + restBand else null
}

/**
 * 직렬화된 심박 시계열을 복원한다. 깨진 토큰은 건너뛴다.
 */
fun decodeHrSeries(raw: String?): List<Pair<Int, Int>> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.split(',').mapNotNull { token ->
        val parts = token.split(':')
        if (parts.size != 2) return@mapNotNull null
        val offset = parts[0].toIntOrNull() ?: return@mapNotNull null
        val bpm = parts[1].toIntOrNull() ?: return@mapNotNull null
        offset to bpm
    }
}
