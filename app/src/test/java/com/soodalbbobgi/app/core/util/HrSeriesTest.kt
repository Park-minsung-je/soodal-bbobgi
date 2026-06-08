package com.soodalbbobgi.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 심박 시계열 직렬화 + 골짜기 기반 휴식 마스크 검증.
 * 직렬화는 "오프셋초:bpm,...|휴식시작-끝,..." 컴팩트 문자열로 저장하고 안전하게 복원한다.
 * 휴식 마스크는 절대 임계 없이 상대 하락/반등만으로 분류한다.
 */
class HrSeriesTest {

    @Test
    fun `인코딩과 디코딩은 왕복 보존된다`() {
        val points = listOf(0 to 98, 30 to 142, 60 to 155)
        assertEquals("0:98,30:142,60:155", encodeHrSeries(points))
        assertEquals(points, decodeHrSeries("0:98,30:142,60:155"))
    }

    @Test
    fun `빈 값과 잘못된 토큰은 안전하게 처리한다`() {
        assertTrue(decodeHrSeries(null).isEmpty())
        assertTrue(decodeHrSeries("").isEmpty())
        // 깨진 토큰은 건너뛰고 정상 토큰만 살린다
        assertEquals(listOf(10 to 120), decodeHrSeries("abc,10:120,5:"))
    }

    @Test
    fun `휴식 구간은 파이프 뒤에 같이 인코딩된다`() {
        val points = listOf(0 to 98, 30 to 142)
        assertEquals(
            "0:98,30:142|120-180,400-470",
            encodeHrSeries(points, listOf(120..180, 400..470)),
        )
        // 휴식 구간이 없으면 기존 포맷 그대로
        assertEquals("0:98,30:142", encodeHrSeries(points, emptyList()))
    }

    @Test
    fun `포인트 디코딩은 휴식 구간 부분을 무시한다`() {
        assertEquals(
            listOf(0 to 98, 30 to 142),
            decodeHrSeries("0:98,30:142|120-180,400-470"),
        )
    }

    @Test
    fun `휴식 구간 디코딩은 왕복 보존되고 없으면 빈 목록이다`() {
        assertEquals(
            listOf(120..180, 400..470),
            decodeHrRestRanges("0:98,30:142|120-180,400-470"),
        )
        // 구버전 포맷(휴식 구간 없음)과 빈 입력은 빈 목록
        assertTrue(decodeHrRestRanges("0:98,30:142").isEmpty())
        assertTrue(decodeHrRestRanges(null).isEmpty())
        // 깨진 구간 토큰은 건너뛴다
        assertEquals(listOf(5..10), decodeHrRestRanges("0:98|abc,5-10,20-"))
    }

    @Test
    fun `차트 휴식 구간은 봉우리에서 충분히 내려온 지점부터 시작한다`() {
        // 수영 정점(150)에서 바닥(90)까지 내려가는 골짜기 — 역확장이 정점까지 가면
        // 봉우리가 통째로 회색이 된다. 차트용 구간은 정점에서 깊이의 frac만큼
        // 내려온 지점부터 시작해 봉우리를 흰색으로 남긴다 (페이스 계산엔 영향 없음).
        val points = (0 until 600).map { it to swimBpm(it) } +
            (600 until 660).map { it to (150 - (it - 600)) } +  // 150→90 하강
            (660 until 780).map { it to (90 + it % 2) } +        // 바닥
            (780 until 840).map { it to (90 + (it - 780)) } +    // 90→149 상승
            (840 until 1440).map { it to swimBpm(it) }
        val full = hrRestRanges(points)
        val trimmed = chartRestRanges(points, entryTrimFrac = 0.33)
        assertEquals(1, trimmed.size)
        // 시작은 원본보다 뒤로 밀린다 (봉우리 트림)
        assertTrue("trim start ${trimmed[0].first} > ${full[0].first}", trimmed[0].first > full[0].first)
        // 끝은 동일 (반등부는 이미 마스크에서 처리됨)
        assertEquals(full[0].last, trimmed[0].last)
        // 트림된 시작 지점의 심박은 정점(150)보다 충분히 낮다 — 임계 150-(150-90)*0.33≈130 부근
        val startBpm = points.first { it.first == trimmed[0].first }.second
        assertTrue("startBpm=$startBpm", startBpm in 120..140)
    }

    @Test
    fun `차트 휴식 구간 트림은 frac 0이면 원본과 같다`() {
        val points = valleySession()
        assertEquals(hrRestRanges(points), chartRestRanges(points, entryTrimFrac = 0.0))
    }

    // ── 휴식 마스크 ──────────────────────────────────────────────

    /** 수영 구간용 합성 심박 — 주기 60초 ±12bpm 사인 출렁임 (실측처럼 평활 후에도 굴곡이 남는다). */
    private fun swimBpm(sec: Int, base: Int = 150): Int =
        (base + 12 * kotlin.math.sin(2 * Math.PI * sec / 60)).toInt()

    /** 수영 600초 → 하강 60초 → 바닥 120초 → 상승 60초 → 수영 600초 (1초 간격). */
    private fun valleySession(): List<Pair<Int, Int>> =
        (0 until 600).map { it to swimBpm(it) } +
            (600 until 660).map { it to (150 - (it - 600)) } +
            (660 until 780).map { it to (90 + it % 2) } +
            (780 until 840).map { it to (90 + (it - 780)) } +
            (840 until 1440).map { it to swimBpm(it) }

    @Test
    fun `휴식은 하락 시작부터 반등 시작까지 잡힌다`() {
        val points = valleySession()
        val rest = hrRestMask(points)
        // 수영 중간은 휴식이 아니다
        assertFalse("offset 300", rest[points.indexOfFirst { it.first == 300 }])
        assertFalse("offset 1100", rest[points.indexOfFirst { it.first == 1100 }])
        // 바닥 한가운데는 휴식이다
        assertTrue("offset 700", rest[points.indexOfFirst { it.first == 700 }])
        // 휴식 총량: 하락 시작(~596)~상승 시작(~772) — 실제 골짜기(600~780)와 거의 일치
        val count = rest.count { it }
        assertTrue("rest=$count", count in 150..210)
    }

    @Test
    fun `절대 수준과 무관하게 상대 하락만으로 잡는다`() {
        // 고강도 날: 수영 ~180bpm, 휴식 바닥이 140bpm(절대값으로는 높음)이어도 잡혀야 한다
        val points = (0 until 600).map { it to swimBpm(it, base = 180) } +
            (600 until 640).map { it to (180 - (it - 600)) } +
            (640 until 760).map { it to (140 + it % 2) } +
            (760 until 800).map { it to (140 + (it - 760)) } +
            (800 until 1400).map { it to swimBpm(it, base = 180) }
        val rest = hrRestMask(points)
        assertTrue("offset 700", rest[points.indexOfFirst { it.first == 700 }])
        assertFalse("offset 300", rest[points.indexOfFirst { it.first == 300 }])
        assertFalse("offset 1100", rest[points.indexOfFirst { it.first == 1100 }])
    }

    @Test
    fun `완만한 표류는 휴식으로 오탐하지 않는다`() {
        // 1,200초 동안 30bpm 완만 하강(느린 수영 피로 회복) — 기준선이 따라가서 오탐 없음
        val points = (0 until 1200).map { it to (swimBpm(it) - it / 40) }
        val rest = hrRestMask(points)
        assertTrue(rest.none { it })
    }

    @Test
    fun `반등 없이 끝나는 벽 휴식은 끝까지 휴식이다`() {
        val points = (0 until 600).map { it to swimBpm(it) } +
            (600 until 660).map { it to (150 - (it - 600)) } +
            (660 until 900).map { it to (90 + it % 2) }
        val rest = hrRestMask(points)
        assertTrue("offset 700", rest[points.indexOfFirst { it.first == 700 }])
        assertTrue("offset 850", rest[points.indexOfFirst { it.first == 850 }])
        // 하락 시작(~550)부터 세션 끝(899)까지
        val count = rest.count { it }
        assertTrue("rest=$count", count in 300..370)
    }

    @Test
    fun `일시정지 공백 너머는 수준을 재학습한다`() {
        // 공백(>5초) 이후 저심박 세그먼트는 비교 대상이 없으므로 휴식이 아니다 (수영으로 본다)
        val points = (0 until 600).map { it to swimBpm(it) } +
            (900 until 1200).map { it to (95 + it % 2) }
        val rest = hrRestMask(points)
        assertTrue(rest.none { it })
    }

    @Test
    fun `휴식 구간 목록은 마스크의 연속 구간과 일치한다`() {
        val points = valleySession()
        val rest = hrRestMask(points)
        val ranges = hrRestRanges(points)
        // 마스크에서 직접 접은 연속 구간과 동일해야 한다
        val expected = mutableListOf<IntRange>()
        var start = -1
        for (i in points.indices) {
            if (rest[i] && start < 0) start = i
            if (!rest[i] && start >= 0) {
                expected.add(points[start].first..points[i - 1].first)
                start = -1
            }
        }
        if (start >= 0) expected.add(points[start].first..points.last().first)
        assertEquals(expected, ranges)
        // 골짜기 하나 = 구간 하나, 경계는 하락 시작(마지막 꼭대기 ~550)과 상승 시작(~775) 부근
        // (휴식 끝 = 마지막 연속 상승의 시작 — 상승 구간은 수영)
        assertEquals(1, ranges.size)
        assertTrue("start=${ranges[0].first}", ranges[0].first in 540..645)
        assertTrue("end=${ranges[0].last}", ranges[0].last in 755..800)
    }
}
