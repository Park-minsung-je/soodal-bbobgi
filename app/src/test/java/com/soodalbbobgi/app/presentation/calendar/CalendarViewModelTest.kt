package com.soodalbbobgi.app.presentation.calendar

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * 캘린더 월 전환 시 표시 월과 swimData가 항상 같은 달인지(원자성) 검증.
 * 월만 먼저 바뀌고 이전 달 데이터가 잠깐 보이는 깜빡임 회귀 방지용.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var session: UserSession
    private lateinit var swimLogUseCase: SwimLogUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        session = UserSession().apply { setAuthenticatedUser("u1") }
        swimLogUseCase = mockk(relaxed = true)
        // 달마다 다른 데이터: 10일자 기록의 거리(m) = 조회한 달의 월 값
        every { swimLogUseCase.getLogsByDateRange(any(), any()) } answers {
            val start = firstArg<String>() // "yyyy-MM-dd"
            val month = start.substring(5, 7).toInt()
            flowOf(
                listOf(
                    SwimLog(
                        userId = "u1",
                        date = start.substring(0, 8) + "10",
                        distanceMeters = month,
                        durationSeconds = 60,
                        calories = 10,
                        source = "test",
                    ),
                ),
            )
        }
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `월 전환 중에도 표시 월과 데이터의 달이 항상 일치한다`() = runTest(testDispatcher) {
        val vm = CalendarViewModel(session, swimLogUseCase)
        val emissions = mutableListOf<CalendarUiState>()
        backgroundScope.launch(testDispatcher) { vm.uiState.collect { emissions.add(it) } }
        advanceUntilIdle()

        vm.previousMonth()
        advanceUntilIdle()
        vm.nextMonth()
        vm.nextMonth()
        advanceUntilIdle()

        // 데이터가 있는 모든 emission에서 10일 기록의 거리(=조회한 달)가 표시 월과 같아야 한다.
        val withData = emissions.filter { it.swimData.isNotEmpty() }
        assertThat(withData).isNotEmpty()
        withData.forEach { s ->
            assertThat(s.swimData.getValue(10).distanceM).isEqualTo(s.month)
        }
    }
}
