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

// 직관 모델 파라미터 기본값 (사용자 조정 가능성을 위해 상수로 분리)
/** 수영 수준 대비 이 값 이상 낮게 깔린 구간을 휴식으로 판정한다 (bpm). */
const val INTUITIVE_DROP_DEFAULT = 24.0

/** 이 초 미만의 짧은 휴식 골짜기는 무시한다 (초). */
const val INTUITIVE_MIN_REST_DEFAULT = 25

/**
 * 직관 모델 기반 휴식 구간과 휴식초를 반환한다.
 *
 * 수영 수준(양방향 ±[levelWindowSec] 범위 평활 심박의 최대)에 비해 [dropDelta] 이상 낮게
 * 깔린 구간을 휴식으로 잡는다. 봉우리(수준 근처)는 자동 제외된다.
 * 연속 휴식 구간 중 길이([dropDelta] 차 기준 오프셋 폭)가 [minRestSec] 미만인 것은 버린다.
 *
 * @param points (오프셋초, bpm) 목록 — 시간 오름차순
 * @param gapSec 공백 기준(초) — 이 값 초과 dt 또는 1 미만 dt는 세그먼트 경계
 * @param smoothSec 이동 평균 창(초) — 평활 심박 계산
 * @param levelWindowSec 수영 수준 산출 양방향 창(초) — 현재 점 기준 앞뒤 이 범위의 sm 최대
 * @param dropDelta 휴식 판정 하락 임계(bpm) — 수준 대비 이 값 이상 낮아야 휴식
 * @param minRestSec 최소 유효 휴식 구간 길이(초) — 이 값 미만 구간은 버림
 * @return 휴식 구간 목록(오프셋초 IntRange)과 휴식초의 쌍
 */
fun intuitiveRestRanges(
    points: List<Pair<Int, Int>>,
    gapSec: Int = 5,
    smoothSec: Int = 15,
    levelWindowSec: Int = 90,
    dropDelta: Double = INTUITIVE_DROP_DEFAULT,
    minRestSec: Int = INTUITIVE_MIN_REST_DEFAULT,
): Pair<List<IntRange>, Int> {
    val n = points.size
    if (n < 2) return Pair(emptyList(), 0)

    val sm = hrSmoothed(points, smoothSec)

    // 공백(dt > gapSec 또는 dt < 1)으로 세그먼트 분리
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

    val restMask = BooleanArray(n)
    for (seg in segs) {
        val s0 = seg.first
        val s1 = seg.last
        for (i in s0..s1) {
            // 양방향 ±levelWindowSec 트레일링 최대 = 수영 수준
            var level = sm[i]
            // 과거 방향
            var j = i
            while (j >= s0 && points[i].first - points[j].first <= levelWindowSec) {
                if (sm[j] > level) level = sm[j]
                j--
            }
            // 미래 방향
            j = i
            while (j <= s1 && points[j].first - points[i].first <= levelWindowSec) {
                if (sm[j] > level) level = sm[j]
                j++
            }
            // 수준 대비 drop 이상 낮아야 휴식
            if (level - sm[i] >= dropDelta) restMask[i] = true
        }
    }

    // 연속 휴식 구간 추출 + minRestSec 미만 구간 제거
    val ranges = mutableListOf<IntRange>()
    var start = -1
    for (i in 0 until n) {
        if (restMask[i] && start < 0) start = i
        if (!restMask[i] && start >= 0) {
            val rangeSpan = points[i - 1].first - points[start].first
            if (rangeSpan >= minRestSec) ranges.add(points[start].first..points[i - 1].first)
            start = -1
        }
    }
    if (start >= 0) {
        val rangeSpan = points[n - 1].first - points[start].first
        if (rangeSpan >= minRestSec) ranges.add(points[start].first..points[n - 1].first)
    }

    // 휴식초 = 유효 구간 내 dt(1부터 gapSec까지) 합
    val inRest = { off: Int -> ranges.any { off >= it.first && off < it.last } }
    var restSec = 0
    for (i in 0 until n - 1) {
        val dt = points[i + 1].first - points[i].first
        if (dt in 1..gapSec && inRest(points[i].first)) restSec += dt
    }

    return Pair(ranges, restSec)
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
