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

    // ── intuitiveRestRanges ───────────────────────────────────────

    /**
     * 깊은 골짜기(40초) 하나 + 짧은 골짜기(10초) 하나 합성 세션.
     * 수영(높은 평평) → 깊은 골짜기 40초 → 수영 → 짧은 골짜기 10초 → 수영 (1초 간격).
     * drop=24, minRest=25로 호출하면 깊은 골짜기만 잡히고 짧은 것은 버려진다.
     */
    private fun twoValleySession(): List<Pair<Int, Int>> {
        val swim = { sec: Int -> (150 + 12 * kotlin.math.sin(2 * Math.PI * sec / 60)).toInt() }
        // 0..299: 수영
        val seg1 = (0 until 300).map { it to swim(it) }
        // 300..339: 깊은 골짜기 (40초, 바닥 ~110bpm — 수영 수준 150bpm과 40bpm 차이)
        val deepDown = (300 until 320).map { it to (150 - (it - 300) * 2) }   // 150→110
        val deepFlat = (320 until 340).map { it to (110 + it % 2) }
        // 340..359: 반등 (110→150)
        val deepUp = (340 until 360).map { it to (110 + (it - 340) * 2) }
        // 360..659: 수영
        val seg2 = (360 until 660).map { it to swim(it) }
        // 660..669: 짧은 골짜기 (10초, 바닥 ~120bpm — 수준 대비 30bpm↓)
        val shortDown = (660 until 665).map { it to (150 - (it - 660) * 6) }  // 150→120
        val shortFlat = (665 until 670).map { it to (120 + it % 2) }
        // 670..679: 반등 (120→150)
        val shortUp = (670 until 680).map { it to (120 + (it - 670) * 3) }
        // 680..979: 수영
        val seg3 = (680 until 980).map { it to swim(it) }
        return seg1 + deepDown + deepFlat + deepUp + seg2 + shortDown + shortFlat + shortUp + seg3
    }

    @Test
    fun `intuitiveRestRanges는 깊은 골짜기만 잡고 짧은 골짜기는 버린다`() {
        // drop=24, minRest=25 → 깊은 40초 골짜기는 잡히고, 10초 짧은 골짜기는 버려진다
        val points = twoValleySession()
        val (ranges, restSec) = intuitiveRestRanges(points, dropDelta = 24.0, minRestSec = 25)
        assertEquals("구간 수 = 1 (짧은 골짜기 제거)", 1, ranges.size)
        assertTrue("restSec=$restSec > 0", restSec > 0)
        // 깊은 골짜기 중심부(320..339)가 휴식 구간 안에 포함돼야 한다
        assertTrue("range[0].first <= 320", ranges[0].first <= 320)
        assertTrue("range[0].last >= 339", ranges[0].last >= 339)
    }

    @Test
    fun `intuitiveRestRanges는 봉우리와 평평한 수영 구간은 휴식으로 잡지 않는다`() {
        // 수영 중간만으로 구성된 세션: 골짜기가 없으면 구간 없음을 확인 (봉우리 자동 제외)
        // 깊은 골짜기가 있는 세션에서도 수영 중간 오프셋(150, 500, 800)은 어떤 구간에도 속하지 않아야 한다
        val points = twoValleySession()
        val (ranges, _) = intuitiveRestRanges(points, dropDelta = 24.0, minRestSec = 25)
        // 구간이 1개이고 수영 중간 오프셋이 그 구간 밖이어야 한다
        assertEquals(1, ranges.size)
        val r = ranges[0]
        // sec=150 (첫 수영 구간 중간) — 깊은 골짜기 이전이므로 휴식 구간 밖
        assertTrue("sec=150은 구간 밖: ${r.first}~${r.last}", 150 < r.first || 150 > r.last)
        // sec=500 (두 번째 수영 구간) — 깊은 골짜기와 짧은 골짜기 사이
        assertTrue("sec=500은 구간 밖: ${r.first}~${r.last}", 500 < r.first || 500 > r.last)
        // sec=800 (세 번째 수영 구간)
        assertTrue("sec=800은 구간 밖: ${r.first}~${r.last}", 800 < r.first || 800 > r.last)
    }

    @Test
    fun `intuitiveRestRanges는 minRest가 작을수록 구간 수가 많거나 같다`() {
        // 단조성: minRestSec 작을수록 짧은 골짜기도 잡혀 구간 수 증가
        val points = twoValleySession()
        val (rangesSmall, _) = intuitiveRestRanges(points, dropDelta = 24.0, minRestSec = 5)
        val (rangesLarge, _) = intuitiveRestRanges(points, dropDelta = 24.0, minRestSec = 25)
        assertTrue(
            "minRest=5 구간 수(${rangesSmall.size}) >= minRest=25 구간 수(${rangesLarge.size})",
            rangesSmall.size >= rangesLarge.size,
        )
    }

    @Test
    fun `intuitiveRestRanges는 연속 수영 세션에서 휴식이 없다`() {
        // 골짜기 없음 → 모든 점에서 level ≈ sm → level-sm < drop → 휴식 구간 없음
        val points = (0 until 600).map { it to (150 + 12 * kotlin.math.sin(2 * Math.PI * it / 60)).toInt() }
        val (ranges, restSec) = intuitiveRestRanges(points, dropDelta = 24.0, minRestSec = 25)
        assertTrue("연속 수영에서 휴식 구간 없음: ranges=${ranges.size}", ranges.isEmpty())
        assertEquals("restSec=0", 0, restSec)
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
