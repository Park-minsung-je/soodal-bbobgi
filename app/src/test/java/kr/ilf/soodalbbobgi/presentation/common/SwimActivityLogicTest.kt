package kr.ilf.soodalbbobgi.presentation.common

import kr.ilf.soodalbbobgi.domain.model.SwimLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * 공용 수영 활동 로직 검증.
 * 주간 활동 빌드, 지난주 대비 추세, 연속 수영 일수(스트릭)를 다룬다.
 */
class SwimActivityLogicTest {

    private fun log(date: String, distance: Int, free: Int = 0, mixed: Int = 0) = SwimLog(
        userId = "u", date = date, distanceMeters = distance,
        durationSeconds = 1800, calories = 200,
        strokeFreestyleM = free, strokeMixedM = mixed,
        source = "test",
    )

    @Test
    fun `주간 추세는 지난주 대비 증감 퍼센트다`() {
        assertEquals(12, weekTrendPercent(1120, 1000))
        assertEquals(-20, weekTrendPercent(800, 1000))
        assertNull(weekTrendPercent(500, 0)) // 지난주 기록 없음 → 비교 불가
    }

    @Test
    fun `주간 활동은 최근 7일 막대와 지난주 합계 추세를 만든다`() {
        val today = LocalDate.of(2026, 6, 6)
        val logs = listOf(
            log(date = "2026-06-06", distance = 1000, free = 600, mixed = 400), // 오늘
            log(date = "2026-06-03", distance = 500, mixed = 500),              // 이번 주
            log(date = "2026-05-26", distance = 1500, mixed = 1500),            // 지난주
        )
        val weekly = buildWeeklyActivity(logs, today)

        assertEquals(7, weekly.days.size)
        assertEquals(1500, weekly.totalMeters) // 1000 + 500
        assertEquals(2, weekly.activeDays)
        assertTrue(weekly.days.last().isToday)
        assertEquals("토", weekly.days.last().label) // 2026-06-06은 토요일
        assertEquals(0, weekly.trendPercent) // (1500-1500)/1500 = 0%
    }

    @Test
    fun `같은 날 여러 세션은 막대에 합산된다`() {
        val today = LocalDate.of(2026, 6, 6)
        val logs = listOf(
            log(date = "2026-06-06", distance = 500, free = 500),
            log(date = "2026-06-06", distance = 700, mixed = 700),
        )
        val weekly = buildWeeklyActivity(logs, today)

        assertEquals(1200, weekly.days.last().distanceM)
        assertEquals(1200, weekly.totalMeters)
        assertEquals(1, weekly.activeDays)
        // 영법 분포도 합산: free=500, mixed=700
        assertEquals(500, weekly.days.last().strokeMeters[0])
        assertEquals(700, weekly.days.last().strokeMeters[5])
    }

    @Test
    fun `스트릭은 오늘부터 거꾸로 연속된 일수를 센다`() {
        val today = LocalDate.of(2026, 6, 6)
        val dates = setOf(
            LocalDate.of(2026, 6, 6),
            LocalDate.of(2026, 6, 5),
            LocalDate.of(2026, 6, 4),
            LocalDate.of(2026, 6, 2), // 6/3 공백 → 끊김
        )
        assertEquals(3, swimStreak(dates, today))
    }

    @Test
    fun `오늘 기록이 없으면 어제부터 센다 (하루 유예)`() {
        val today = LocalDate.of(2026, 6, 6)
        val dates = setOf(
            LocalDate.of(2026, 6, 5),
            LocalDate.of(2026, 6, 4),
        )
        assertEquals(2, swimStreak(dates, today))
    }

    @Test
    fun `오늘도 어제도 기록이 없으면 스트릭은 0이다`() {
        val today = LocalDate.of(2026, 6, 6)
        val dates = setOf(LocalDate.of(2026, 6, 3))
        assertEquals(0, swimStreak(dates, today))
    }
}
