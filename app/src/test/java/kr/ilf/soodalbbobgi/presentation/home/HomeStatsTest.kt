package kr.ilf.soodalbbobgi.presentation.home

import kr.ilf.soodalbbobgi.domain.model.SwimStats
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeStatsTest {
    @Test
    fun `monthDelta는 이번달에서 지난달을 뺀다`() {
        val d = monthDelta(SwimStats(18500, 12, 4200), SwimStats(15000, 9, 3500))
        assertEquals(3500, d.distanceDeltaM)
        assertEquals(3, d.countDelta)
        assertEquals(700, d.kcalDelta)
    }

    @Test
    fun `countDeltaPhrase는 양수 음수 0을 한국어로 표현한다`() {
        assertEquals("지난달보다 3회 더", countDeltaPhrase(3))
        assertEquals("지난달보다 2회 적게", countDeltaPhrase(-2))
        assertEquals("지난달과 같은 횟수로", countDeltaPhrase(0))
    }

    @Test
    fun `지난달 같은 기간의 끝은 지난달의 같은 일자다`() {
        assertEquals(
            java.time.LocalDate.parse("2026-06-15"),
            lastMonthSamePeriodEnd(java.time.LocalDate.parse("2026-07-15")),
        )
    }

    @Test
    fun `지난달이 더 짧으면 지난달 말일로 맞춘다`() {
        // 3/30 기준 — 2월은 28일까지
        assertEquals(
            java.time.LocalDate.parse("2026-02-28"),
            lastMonthSamePeriodEnd(java.time.LocalDate.parse("2026-03-30")),
        )
        // 윤년이면 2/29
        assertEquals(
            java.time.LocalDate.parse("2028-02-29"),
            lastMonthSamePeriodEnd(java.time.LocalDate.parse("2028-03-30")),
        )
    }

    @Test
    fun `월 말일이면 지난달 말일까지 비교한다`() {
        assertEquals(
            java.time.LocalDate.parse("2026-06-30"),
            lastMonthSamePeriodEnd(java.time.LocalDate.parse("2026-07-31")),
        )
    }

    @Test
    fun `연초에는 작년 12월과 비교한다`() {
        assertEquals(
            java.time.LocalDate.parse("2025-12-05"),
            lastMonthSamePeriodEnd(java.time.LocalDate.parse("2026-01-05")),
        )
    }
}
