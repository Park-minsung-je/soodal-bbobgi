package kr.ilf.soodalbbobgi.presentation.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 월간 영법 도넛 차트 지오메트리 검증.
 * 각도는 12시 기준 시계방향(도), 라벨은 중점각 기준 좌/우로 나뉘고
 * 같은 쪽 라벨은 최소 간격으로 세로 충돌을 푼다.
 */
class DonutChartLogicTest {

    @Test
    fun `세그먼트는 비율대로 각도를 나눈다`() {
        val segs = donutSegments(listOf("자유형" to 500, "평영" to 500))
        assertEquals(2, segs.size)
        assertEquals(0f, segs[0].startAngle, 0.01f)
        assertEquals(180f, segs[0].sweepAngle, 0.01f)
        assertEquals(90f, segs[0].midAngle, 0.01f)
        assertEquals(180f, segs[1].startAngle, 0.01f)
        assertEquals(270f, segs[1].midAngle, 0.01f)
        assertEquals(50, segs[0].pct)
        assertEquals(50, segs[1].pct)
    }

    @Test
    fun `0m 영법은 제외된다`() {
        val segs = donutSegments(listOf("자유형" to 300, "평영" to 0, "배영" to 100))
        assertEquals(listOf("자유형", "배영"), segs.map { it.label })
        assertEquals(75, segs[0].pct)
        assertEquals(25, segs[1].pct)
    }

    @Test
    fun `중점각이 0~180도면 오른쪽 라벨이다`() {
        val segs = donutSegments(listOf("자유형" to 500, "평영" to 500))
        assertTrue(segs[0].onRight)   // mid 90° → 오른쪽
        assertFalse(segs[1].onRight)  // mid 270° → 왼쪽
    }

    @Test
    fun `빈 목록이면 빈 결과`() {
        assertTrue(donutSegments(emptyList()).isEmpty())
    }
}
