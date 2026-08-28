package com.soodalbbobgi.app.data.health

import com.soodalbbobgi.app.core.util.decodeHrRestRanges
import com.soodalbbobgi.app.core.util.decodeHrSeries
import com.soodalbbobgi.app.core.util.encodeHrSeries
import com.soodalbbobgi.app.domain.model.SwimLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 하루치 심박 집계 — 서버는 날짜당 1행이라 로컬의 여러 세션을 하나로 합쳐 보낸다.
 */
class DayHrAggregateTest {

    private fun log(
        startEpochSec: Long?,
        maxHr: Int? = null,
        minHr: Int? = null,
        avgHr: Int? = null,
        hrSeries: String? = null,
    ) = SwimLog(
        userId = "u1",
        date = "2026-08-28",
        startEpochSec = startEpochSec,
        distanceMeters = 1000,
        durationSeconds = 1800,
        calories = 300,
        source = "health_connect",
        maxHr = maxHr,
        minHr = minHr,
        avgHr = avgHr,
        hrSeries = hrSeries,
    )

    @Test
    fun `심박이 없으면 전부 null`() {
        val agg = aggregateDayHr(listOf(log(startEpochSec = 100)))
        assertNull(agg.maxHr)
        assertNull(agg.minHr)
        assertNull(agg.avgHr)
        assertNull(agg.hrSeries)
    }

    @Test
    fun `단일 세션은 값을 그대로 옮긴다`() {
        val series = encodeHrSeries(listOf(0 to 120, 60 to 140, 120 to 130))
        val agg = aggregateDayHr(listOf(log(startEpochSec = 1000, maxHr = 150, minHr = 110, hrSeries = series)))
        assertEquals(150, agg.maxHr)
        assertEquals(110, agg.minHr)
        assertEquals(listOf(0 to 120, 60 to 140, 120 to 130), decodeHrSeries(agg.hrSeries))
    }

    @Test
    fun `단일 세션의 평균은 시계열에서 계산한다`() {
        val series = encodeHrSeries(listOf(0 to 120, 60 to 140, 120 to 130))
        val agg = aggregateDayHr(listOf(log(startEpochSec = 1000, hrSeries = series)))
        assertEquals(130, agg.avgHr)
    }

    @Test
    fun `수동 입력의 평균 심박은 시계열이 없어도 살린다`() {
        val agg = aggregateDayHr(listOf(log(startEpochSec = null, avgHr = 135)))
        assertEquals(135, agg.avgHr)
    }

    @Test
    fun `여러 세션의 최대 최소는 전체에서 고른다`() {
        val agg = aggregateDayHr(
            listOf(
                log(startEpochSec = 1000, maxHr = 150, minHr = 110),
                log(startEpochSec = 5000, maxHr = 170, minHr = 95),
            )
        )
        assertEquals(170, agg.maxHr)
        assertEquals(95, agg.minHr)
    }

    @Test
    fun `여러 세션의 시계열은 하루 첫 세션 기준 오프셋으로 이어붙인다`() {
        val first = encodeHrSeries(listOf(0 to 120, 60 to 130))
        val second = encodeHrSeries(listOf(0 to 140, 60 to 150))
        val agg = aggregateDayHr(
            listOf(
                log(startEpochSec = 1000, hrSeries = first),
                log(startEpochSec = 1600, hrSeries = second),
            )
        )
        // 두 번째 세션은 첫 세션보다 600초 늦게 시작했으므로 600만큼 밀린다
        assertEquals(
            listOf(0 to 120, 60 to 130, 600 to 140, 660 to 150),
            decodeHrSeries(agg.hrSeries),
        )
    }

    @Test
    fun `세션 사이의 빈 시간은 휴식 구간으로 표시한다`() {
        val first = encodeHrSeries(listOf(0 to 120, 60 to 130))
        val second = encodeHrSeries(listOf(0 to 140))
        val agg = aggregateDayHr(
            listOf(
                log(startEpochSec = 1000, hrSeries = first),
                log(startEpochSec = 1600, hrSeries = second),
            )
        )
        // 첫 세션 마지막 포인트(60) ~ 두 번째 세션 첫 포인트(600) 사이가 휴식
        assertEquals(listOf(60..600), decodeHrRestRanges(agg.hrSeries))
    }

    @Test
    fun `세션별 휴식 구간도 함께 밀어서 보존한다`() {
        val first = encodeHrSeries(listOf(0 to 120, 60 to 130), listOf(10..20))
        val second = encodeHrSeries(listOf(0 to 140), listOf(0..5))
        val agg = aggregateDayHr(
            listOf(
                log(startEpochSec = 1000, hrSeries = first),
                log(startEpochSec = 1600, hrSeries = second),
            )
        )
        assertEquals(listOf(10..20, 60..600, 600..605), decodeHrRestRanges(agg.hrSeries))
    }

    @Test
    fun `여러 세션의 평균은 전체 포인트로 계산한다`() {
        val first = encodeHrSeries(listOf(0 to 100, 60 to 100))
        val second = encodeHrSeries(listOf(0 to 160, 60 to 160))
        val agg = aggregateDayHr(
            listOf(
                log(startEpochSec = 1000, hrSeries = first),
                log(startEpochSec = 1600, hrSeries = second),
            )
        )
        assertEquals(130, agg.avgHr)
    }

    @Test
    fun `시작 시각이 없는 행은 순서대로 이어붙인다`() {
        val first = encodeHrSeries(listOf(0 to 120, 60 to 130))
        val second = encodeHrSeries(listOf(0 to 140))
        val agg = aggregateDayHr(
            listOf(
                log(startEpochSec = null, hrSeries = first),
                log(startEpochSec = null, hrSeries = second),
            )
        )
        // 간격을 알 수 없으므로 앞 세션 끝에 바로 잇는다
        assertEquals(listOf(0 to 120, 60 to 130, 60 to 140), decodeHrSeries(agg.hrSeries))
    }
}
