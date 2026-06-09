package com.soodalbbobgi.app.presentation.home

import com.soodalbbobgi.app.domain.model.SwimStats
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
}
