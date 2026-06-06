package com.soodalbbobgi.app.presentation.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 캘린더 순수 로직 검증.
 * 일요일 시작 그리드와 영법 비율 환산을 다룬다.
 */
class CalendarLogicTest {

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
}
