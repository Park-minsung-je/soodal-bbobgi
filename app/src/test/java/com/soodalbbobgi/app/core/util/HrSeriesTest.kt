package com.soodalbbobgi.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 심박 시계열 직렬화 검증.
 * "오프셋초:bpm,..." 컴팩트 문자열로 저장하고 안전하게 복원한다.
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
    fun `휴식 임계는 바닥+29이고 바닥이 높으면 분류하지 않는다`() {
        // 바닥(하위 0.5%)=95 → 임계 124
        assertEquals(124, hrRestThreshold(List(300) { 95 } + List(600) { 150 }))
        // 바닥 125 > 110 — 휴식 없는 세션
        org.junit.Assert.assertNull(hrRestThreshold(List(600) { 125 }))
    }
}
