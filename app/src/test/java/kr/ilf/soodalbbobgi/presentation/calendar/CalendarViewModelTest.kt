package kr.ilf.soodalbbobgi.presentation.calendar

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.domain.model.SwimLog
import kr.ilf.soodalbbobgi.domain.usecase.SwimLogUseCase
import io.mockk.coEvery
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
        val vm = CalendarViewModel(session, swimLogUseCase, mockk(relaxed = true), mockk(relaxed = true))
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

    // ── 수동 동기화 실패 — 토스트 대신 동기화 카드가 실패 안내로 바뀐다 ──

    private fun vmWith(syncer: HcSwimSyncer) =
        CalendarViewModel(session, swimLogUseCase, syncer, mockk(relaxed = true))

    @Test
    fun `동기화가 실패하면 로딩 표시가 끝난 뒤 실패 안내로 바뀐다`() = runTest(testDispatcher) {
        val syncer = mockk<HcSwimSyncer>(relaxed = true)
        coEvery { syncer.sync() } throws java.io.IOException("HC 변경 토큰 발급 실패")
        val vm = vmWith(syncer)

        vm.onSync()

        // 로딩 카드가 최소 표시 시간만큼 떠 있는 동안에는 실패 안내가 겹쳐 뜨지 않는다
        assertThat(vm.syncing.value).isTrue()
        assertThat(vm.syncFailed.value).isFalse()

        advanceUntilIdle()

        assertThat(vm.syncing.value).isFalse()
        assertThat(vm.syncFailed.value).isTrue()
    }

    @Test
    fun `동기화 실패는 토스트 문구를 올리지 않는다`() = runTest(testDispatcher) {
        val syncer = mockk<HcSwimSyncer>(relaxed = true)
        coEvery { syncer.sync() } throws java.io.IOException("HC 변경 토큰 발급 실패")
        val vm = vmWith(syncer)

        vm.onSync()
        advanceUntilIdle()

        assertThat(vm.registerError.value).isNull()
    }

    @Test
    fun `동기화가 성공하면 실패 안내가 뜨지 않는다`() = runTest(testDispatcher) {
        val syncer = mockk<HcSwimSyncer>(relaxed = true)
        coEvery { syncer.sync() } returns 0
        val vm = vmWith(syncer)

        vm.onSync()
        advanceUntilIdle()

        assertThat(vm.syncing.value).isFalse()
        assertThat(vm.syncFailed.value).isFalse()
    }

    @Test
    fun `실패 안내를 닫으면 내려간다`() = runTest(testDispatcher) {
        val syncer = mockk<HcSwimSyncer>(relaxed = true)
        coEvery { syncer.sync() } throws java.io.IOException("HC 변경 토큰 발급 실패")
        val vm = vmWith(syncer)
        vm.onSync()
        advanceUntilIdle()

        vm.dismissSyncFailure()

        assertThat(vm.syncFailed.value).isFalse()
    }

    @Test
    fun `다시 동기화를 시작하면 이전 실패 안내는 내려간다`() = runTest(testDispatcher) {
        val syncer = mockk<HcSwimSyncer>(relaxed = true)
        coEvery { syncer.sync() } throws java.io.IOException("HC 변경 토큰 발급 실패")
        val vm = vmWith(syncer)
        vm.onSync()
        advanceUntilIdle()
        assertThat(vm.syncFailed.value).isTrue()

        vm.onSync()

        // 로딩 카드와 실패 카드가 같은 자리라 둘이 동시에 보이면 안 된다
        assertThat(vm.syncing.value).isTrue()
        assertThat(vm.syncFailed.value).isFalse()
    }
}
