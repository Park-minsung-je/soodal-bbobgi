package com.soodalbbobgi.app.presentation.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 캘린더 순수 로직 검증.
 * 일요일 시작 그리드와 영법 비율 환산을 다룬다.
 */
class CalendarLogicTest {

    @Test
    fun `영법 그리드는 거리 상위 4개만 보여준다`() {
        // 입력은 우선순위 순서(자평배접혼킥)로 들어온다
        val entries = listOf("자유형" to 100, "평영" to 500, "배영" to 200, "접영" to 50, "혼영" to 300, "킥판" to 400)
        assertEquals(listOf("평영", "킥판", "혼영", "배영"), topStrokes(entries).map { it.first })
    }

    @Test
    fun `동률이면 자평배접혼킥 우선순위를 따른다`() {
        val entries = listOf("자유형" to 100, "평영" to 300, "배영" to 300, "접영" to 100, "혼영" to 300, "킥판" to 100)
        // 300 동률: 평>배>혼, 100 동률에서 남은 1자리: 자유형
        assertEquals(listOf("평영", "배영", "혼영", "자유형"), topStrokes(entries).map { it.first })
    }

    @Test
    fun `기록이 적어도 항상 4개를 채운다`() {
        val entries = listOf("자유형" to 500, "평영" to 0, "배영" to 0, "접영" to 0, "혼영" to 0, "킥판" to 0)
        assertEquals(listOf("자유형", "평영", "배영", "접영"), topStrokes(entries).map { it.first })
    }

    @Test
    fun `5월 2026 그리드는 금요일 자리에서 1일이 시작된다`() {
        val cells = buildMonthCells(2026, 5)
        assertEquals(42, cells.size)
        // 2026-05-01은 금요일 → 일요일 시작 그리드에서 인덱스 5
        assertEquals(CalendarCell(1, true), cells[5])
        // 앞 5칸은 4월(30일) 말일들
        assertEquals(CalendarCell(26, false), cells[0])
        assertEquals(CalendarCell(30, false), cells[4])
        assertEquals(31, cells.count { it.inMonth })
    }

    @Test
    fun `일요일에 시작하는 달은 앞 빈칸이 없다`() {
        // 2026-02-01은 일요일
        val cells = buildMonthCells(2026, 2)
        assertEquals(CalendarCell(1, true), cells[0])
        assertEquals(28, cells.count { it.inMonth })
    }

    @Test
    fun `그리드 뒤쪽은 다음 달 1일부터 채운다`() {
        val cells = buildMonthCells(2026, 5) // 앞 5칸 + 31일 = 36칸 → 트레일링 6칸
        val trailing = cells.drop(36)
        assertEquals(6, trailing.size)
        assertEquals(CalendarCell(1, false), trailing.first())
        assertTrue(trailing.all { !it.inMonth })
    }

    @Test
    fun `영법 비율은 합계 기준으로 환산된다`() {
        assertEquals(50, strokePercent(500, 1000))
        assertEquals(0, strokePercent(0, 1000))
        assertEquals(0, strokePercent(100, 0)) // 합계 0 → 0
    }

    @Test
    fun `평균 페이스는 100m당 소요 시간이다`() {
        assertEquals(126, paceSecPer100m(1500, 1890)) // 1890초 / 15 = 126초
        assertEquals(120, paceSecPer100m(1000, 1200))
        assertNull(paceSecPer100m(0, 1890)) // 거리 없음 → 계산 불가
        assertNull(paceSecPer100m(1500, 0)) // 시간 없음 → 계산 불가
    }

    @Test
    fun `페이스는 분'초 형식으로 표기한다`() {
        assertEquals("2'06\"", formatPace(126))
        assertEquals("0'59\"", formatPace(59))
        assertEquals("10'00\"", formatPace(600))
    }

    @Test
    fun `영법 수정은 기록된 거리 안에서만 허용된다`() {
        assertEquals(700, clampStrokeMeters(900, 800, 1500)) // 잔여 700까지만
        assertEquals(900, clampStrokeMeters(900, 200, 1500)) // 잔여 내면 그대로
        assertEquals(0, clampStrokeMeters(-50, 0, 1500)) // 음수 방지
        assertEquals(0, clampStrokeMeters(100, 1500, 1500)) // 잔여 없음 → 0
        assertEquals(0, clampStrokeMeters(100, 1600, 1500)) // 이미 초과돼도 음수 한도 없음
    }
}
