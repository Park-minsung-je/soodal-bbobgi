package com.soodalbbobgi.app.data.health

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * 겹침 제거 합산 검증 — HC 거리/칼로리 레코드의 이중 기록 방어.
 *
 * 같은 수영이 총계 레코드 + 랩별 레코드로 함께 저장되거나(같은 앱),
 * 워치/폰 두 소스가 각각 기록하면 단순 합산은 값이 두 배가 된다.
 * 시간 구간이 겹치는 레코드는 한 벌만 합산해야 한다.
 */
class SumNonOverlappingTest {

    private fun t(sec: Long): Instant = Instant.ofEpochSecond(sec)

    @Test
    fun `겹치지 않는 랩 레코드는 전부 합산한다`() {
        val laps = listOf(
            Triple(t(0), t(300), 500.0),
            Triple(t(300), t(600), 500.0),
            Triple(t(660), t(960), 400.0),
        )
        assertEquals(1400.0, sumNonOverlapping(laps), 0.0)
    }

    @Test
    fun `총계 레코드와 랩 레코드가 공존하면 한 벌만 합산한다`() {
        // 세션 전체를 덮는 총계(1900) + 랩별 레코드(합 1900) — 단순 합산이면 3800이 된다
        val records = listOf(
            Triple(t(0), t(3600), 1900.0),
            Triple(t(0), t(1200), 700.0),
            Triple(t(1200), t(2400), 700.0),
            Triple(t(2400), t(3600), 500.0),
        )
        assertEquals(1900.0, sumNonOverlapping(records), 0.0)
    }

    @Test
    fun `두 소스가 같은 구간을 각각 기록하면 한 벌만 합산한다`() {
        // 워치와 폰이 같은 랩들을 이중 기록한 경우
        val records = listOf(
            Triple(t(0), t(300), 500.0),
            Triple(t(0), t(300), 500.0),
            Triple(t(300), t(600), 500.0),
            Triple(t(300), t(600), 500.0),
        )
        assertEquals(1000.0, sumNonOverlapping(records), 0.0)
    }

    @Test
    fun `미세하게 겹치는 연속 랩은 노이즈로 보고 전부 합산한다`() {
        // 워치가 랩 경계를 1초 겹치게 쓰는 노이즈 — 이중 기록으로 오판하면 안 된다
        val laps = listOf(
            Triple(t(0), t(301), 500.0),
            Triple(t(300), t(601), 500.0),
            Triple(t(600), t(900), 500.0),
        )
        assertEquals(1500.0, sumNonOverlapping(laps), 0.0)
    }

    @Test
    fun `빈 목록은 0을 반환한다`() {
        assertEquals(0.0, sumNonOverlapping(emptyList()), 0.0)
    }

    @Test
    fun `레코드가 하나면 그 값을 그대로 반환한다`() {
        assertEquals(1200.0, sumNonOverlapping(listOf(Triple(t(0), t(3600), 1200.0))), 0.0)
    }
}
