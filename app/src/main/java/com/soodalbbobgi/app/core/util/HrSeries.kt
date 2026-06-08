package com.soodalbbobgi.app.core.util

/**
 * 심박 시계열(오프셋초, bpm)을 "0:98,30:142,..." 컴팩트 문자열로 직렬화한다.
 * 휴식 구간이 있으면 파이프 뒤에 "시작-끝,..." (오프셋초)으로 덧붙인다 —
 * 차트가 동기화 때 원본 해상도로 계산된 분류를 그대로 쓰게 하기 위함.
 * swim_logs.hrSeries 컬럼 저장용.
 *
 * @param restRanges 휴식 구간 목록 (오프셋초, 양끝 포함)
 */
fun encodeHrSeries(points: List<Pair<Int, Int>>, restRanges: List<IntRange> = emptyList()): String {
    val pts = points.joinToString(",") { "${it.first}:${it.second}" }
    return if (restRanges.isEmpty()) pts
    else pts + "|" + restRanges.joinToString(",") { "${it.first}-${it.last}" }
}

/**
 * 심박 골짜기 기반 휴식 마스크 — 동기화의 실운동시간 계산과 차트 표시가 공유하는 단일 분류.
 * 절대 임계값 없이 상대 변화만 본다 (빡센 날 쉴 때 심박이 140대여도 잡히고, 느린 수영의
 * 완만한 표류는 기준선이 따라가서 오탐하지 않는다):
 *  - 휴식 시작: 최근 수영 수준([levelWindowSec] 트레일링 최대, [smoothSec] 평활) 대비
 *    [dropDelta] 이상 뚝 떨어질 때 — 하락 시작점(꼭대기)까지 거꾸로 확장
 *  - 휴식 끝: 진행 최저점 대비 [riseDelta] 반등으로 출발을 확인하면,
 *    마지막 연속 상승이 시작된 순간까지 되돌린다 (상승 시작 = 수영 시작)
 *  - 샘플 공백 > [gapSec] = 일시정지 경계 (그 너머와는 비교하지 않음)
 *
 * 파라미터는 2026-05-20/31·06-04/05 네 세션을 삼성헬스 실측 페이스와 대조해 적합한 값.
 *
 * @param points (오프셋초, bpm) 목록 — 시간 오름차순
 * @return points와 같은 길이의 휴식 여부 마스크
 */
fun hrRestMask(
    points: List<Pair<Int, Int>>,
    gapSec: Int = 5,
    smoothSec: Int = 15,
    levelWindowSec: Int = 90,
    dropDelta: Double = 36.0,
    riseDelta: Double = 22.0,
): BooleanArray {
    val n = points.size
    val rest = BooleanArray(n)
    if (n < 2) return rest

    val sm = hrSmoothed(points, smoothSec)

    // 일시정지 공백으로 세그먼트 분리
    val segs = mutableListOf<IntRange>()
    var segStart = 0
    for (i in 1 until n) {
        val dt = points[i].first - points[i - 1].first
        if (dt > gapSec || dt < 1) {
            if (i - segStart >= 2) segs.add(segStart until i)
            segStart = i
        }
    }
    if (n - segStart >= 2) segs.add(segStart until n)

    for (seg in segs) {
        var resting = false
        var restMin = 0.0
        val deque = ArrayDeque<Int>() // 트레일링 최대용 단조 데크 (인덱스)
        for (i in seg) {
            if (!resting) {
                while (deque.isNotEmpty() && sm[deque.last()] <= sm[i]) deque.removeLast()
                deque.addLast(i)
                while (deque.isNotEmpty() && points[i].first - points[deque.first()].first > levelWindowSec) deque.removeFirst()
                if (sm[deque.first()] - sm[i] >= dropDelta) {
                    resting = true
                    restMin = sm[i]
                    // 하락 시작점(꼭대기)까지 역확장
                    var j = i
                    while (j - 1 >= seg.first && !rest[j - 1] && sm[j - 1] >= sm[j] - 0.5) j--
                    for (k in j..i) rest[k] = true
                }
            } else {
                if (sm[i] < restMin) restMin = sm[i]
                if (sm[i] >= restMin + riseDelta) {
                    // 출발 확인 — 마지막 연속 상승의 시작까지 휴식을 되돌린다 (상승 = 수영)
                    var k = i
                    while (k - 1 >= seg.first && sm[k - 1] < sm[k]) k--
                    for (q in k..i) rest[q] = false
                    resting = false
                    deque.clear()
                    deque.addLast(i) // 수준 재학습
                } else {
                    rest[i] = true
                }
            }
        }
    }
    return rest
}

/**
 * 심박 포인트의 시간 창 이동 평균 — 휴식 마스크와 강도 적분이 공유한다.
 * 샘플 간격이 1초든 다운샘플이든 동일하게 동작한다.
 *
 * @param points (오프셋초, bpm) 목록 — 시간 오름차순
 * @return points와 같은 길이의 평활 bpm 배열
 */
fun hrSmoothed(points: List<Pair<Int, Int>>, smoothSec: Int = 15): DoubleArray {
    val n = points.size
    val sm = DoubleArray(n)
    if (n == 0) return sm
    val half = smoothSec / 2
    var lo = 0
    var hi = -1
    var sum = 0.0
    for (i in 0 until n) {
        while (hi + 1 < n && points[hi + 1].first <= points[i].first + half) {
            hi++
            sum += points[hi].second
        }
        while (points[lo].first < points[i].first - half) {
            sum -= points[lo].second
            lo++
        }
        sm[i] = sum / (hi - lo + 1)
    }
    return sm
}

/**
 * [hrRestMask]의 휴식 마스크를 (시작오프셋..끝오프셋) 연속 구간 목록으로 접는다.
 * 동기화 시 원본 샘플로 계산해 [encodeHrSeries]에 함께 저장한다.
 */
fun hrRestRanges(
    points: List<Pair<Int, Int>>,
    gapSec: Int = 5,
    smoothSec: Int = 15,
    levelWindowSec: Int = 90,
    dropDelta: Double = 36.0,
    riseDelta: Double = 22.0,
): List<IntRange> {
    val rest = hrRestMask(points, gapSec, smoothSec, levelWindowSec, dropDelta, riseDelta)
    val ranges = mutableListOf<IntRange>()
    var start = -1
    for (i in points.indices) {
        if (rest[i] && start < 0) start = i
        if (!rest[i] && start >= 0) {
            ranges.add(points[start].first..points[i - 1].first)
            start = -1
        }
    }
    if (start >= 0) ranges.add(points[start].first..points.last().first)
    return ranges
}

/**
 * 목표 휴식총량에 맞는 drop 임계를 이진탐색해 휴식 구간과 실제 휴식초를 반환한다.
 * 차트 회색 구간과 페이스 계산이 동일한 마스크를 공유하도록 단일 소스를 보장한다.
 *
 * drop이 낮을수록 더 많은 골짜기를 잡아 휴식초가 늘어난다. 이진탐색(8.0~160.0, 24회)으로
 * restSec이 [targetRestSec] 이상이 되는 최대 drop을 찾는다.
 *
 * @param points (오프셋초, bpm) 목록 — 시간 오름차순
 * @param targetRestSec 목표 휴식총량(초) — 보정식이 산출한 값
 * @return 확정된 휴식 구간 목록(오프셋초 IntRange)과 실제 휴식초의 쌍
 */
fun adaptiveRestRanges(
    points: List<Pair<Int, Int>>,
    targetRestSec: Int,
    gapSec: Int = 5,
    smoothSec: Int = 15,
    levelWindowSec: Int = 90,
    riseDelta: Double = 22.0,
): Pair<List<IntRange>, Int> {
    val n = points.size
    if (n < 2) return Pair(emptyList(), 0)

    val sm = hrSmoothed(points, smoothSec)

    // 세그먼트 경계 목록 — 공백(>gapSec 또는 <1초) 기준
    val segs = mutableListOf<IntRange>()
    var segStart = 0
    for (i in 1 until n) {
        val dt = points[i].first - points[i - 1].first
        if (dt > gapSec || dt < 1) {
            if (i - segStart >= 2) segs.add(segStart until i)
            segStart = i
        }
    }
    if (n - segStart >= 2) segs.add(segStart until n)

    /** drop 임계로 마스크를 만들고 restSec(비공백 구간 중 rest=true인 dt 합)을 계산한다. */
    fun restSecForDrop(drop: Double): Pair<BooleanArray, Int> {
        val rest = BooleanArray(n)
        for (seg in segs) {
            var resting = false
            var restMin = 0.0
            val deque = ArrayDeque<Int>()
            for (i in seg) {
                if (!resting) {
                    while (deque.isNotEmpty() && sm[deque.last()] <= sm[i]) deque.removeLast()
                    deque.addLast(i)
                    while (deque.isNotEmpty() && points[i].first - points[deque.first()].first > levelWindowSec) deque.removeFirst()
                    if (sm[deque.first()] - sm[i] >= drop) {
                        resting = true
                        restMin = sm[i]
                        var j = i
                        while (j - 1 >= seg.first && !rest[j - 1] && sm[j - 1] >= sm[j] - 0.5) j--
                        for (k in j..i) rest[k] = true
                    }
                } else {
                    if (sm[i] < restMin) restMin = sm[i]
                    if (sm[i] >= restMin + riseDelta) {
                        var k = i
                        while (k - 1 >= seg.first && sm[k - 1] < sm[k]) k--
                        for (q in k..i) rest[q] = false
                        resting = false
                        deque.clear()
                        deque.addLast(i)
                    } else {
                        rest[i] = true
                    }
                }
            }
        }
        var restSec = 0
        for (i in 0 until n - 1) {
            val dt = points[i + 1].first - points[i].first
            if (dt in 1..gapSec && rest[i]) restSec += dt
        }
        return Pair(rest, restSec)
    }

    // 이진탐색: restSec >= targetRestSec이 되는 최대 drop을 찾는다
    var lo = 8.0
    var hi = 160.0
    repeat(24) {
        val mid = (lo + hi) / 2.0
        val (_, sec) = restSecForDrop(mid)
        if (sec >= targetRestSec) lo = mid else hi = mid
    }

    // 확정된 lo로 최종 마스크와 구간 목록 생성
    val (finalMask, finalRestSec) = restSecForDrop(lo)
    val ranges = mutableListOf<IntRange>()
    var start = -1
    for (i in points.indices) {
        if (finalMask[i] && start < 0) start = i
        if (!finalMask[i] && start >= 0) {
            ranges.add(points[start].first..points[i - 1].first)
            start = -1
        }
    }
    if (start >= 0) ranges.add(points[start].first..points.last().first)

    return Pair(ranges, finalRestSec)
}

/**
 * 직렬화된 심박 시계열을 복원한다. 깨진 토큰은 건너뛰고, 휴식 구간 부분('|' 뒤)은 무시한다.
 */
fun decodeHrSeries(raw: String?): List<Pair<Int, Int>> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.substringBefore('|').split(',').mapNotNull { token ->
        val parts = token.split(':')
        if (parts.size != 2) return@mapNotNull null
        val offset = parts[0].toIntOrNull() ?: return@mapNotNull null
        val bpm = parts[1].toIntOrNull() ?: return@mapNotNull null
        offset to bpm
    }
}

/**
 * 직렬화된 심박 시계열에서 휴식 구간 목록을 복원한다.
 * 구간 없이 저장된 구버전 포맷이나 깨진 토큰은 빈 목록/건너뛰기로 안전하게 처리한다.
 */
fun decodeHrRestRanges(raw: String?): List<IntRange> {
    val part = raw?.substringAfter('|', "") ?: return emptyList()
    if (part.isBlank()) return emptyList()
    return part.split(',').mapNotNull { token ->
        val bounds = token.split('-')
        if (bounds.size != 2) return@mapNotNull null
        val start = bounds[0].toIntOrNull() ?: return@mapNotNull null
        val end = bounds[1].toIntOrNull() ?: return@mapNotNull null
        if (end < start) return@mapNotNull null
        start..end
    }
}
