package com.soodalbbobgi.app.core.util

/**
 * 심박 시계열(오프셋초, bpm)을 "0:98,30:142,..." 컴팩트 문자열로 직렬화한다.
 * swim_logs.hrSeries 컬럼 저장용.
 */
fun encodeHrSeries(points: List<Pair<Int, Int>>): String =
    points.joinToString(",") { "${it.first}:${it.second}" }

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
