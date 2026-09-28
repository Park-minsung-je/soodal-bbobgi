package kr.ilf.soodalbbobgi.work

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kr.ilf.soodalbbobgi.core.notify.SoodalNotifier
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.local.db.SwimLogDao
import kr.ilf.soodalbbobgi.data.local.entity.SwimLogEntity
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import org.junit.Test

/**
 * 알람이 울렸을 때 실제로 하는 일 — 오늘 기록이 없으면 리마인더를 띄우고, 어떤 경우든 다음 날로 재예약한다.
 * 이전엔 WorkManager 워커 안에 있어 단위 테스트가 없었다.
 */
class SwimReminderTaskTest {

    private val prefs: NotificationPrefs = mockk { every { reminderEnabled } returns true }
    private val dao: SwimLogDao = mockk()
    private val hc: HealthConnectManager = mockk()
    private val notifier: SoodalNotifier = mockk(relaxed = true)
    private val scheduler: ReminderScheduler = mockk(relaxed = true)

    private fun task() = SwimReminderTask(prefs, dao, hc, notifier, scheduler)

    private fun noLocalToday() { coEvery { dao.getByDateOnce(any()) } returns emptyList() }
    private fun localToday() { coEvery { dao.getByDateOnce(any()) } returns listOf(mockk<SwimLogEntity>()) }
    private fun hcEmpty() { coEvery { hc.readSwimSessions(any(), any()) } returns emptyList() }

    @Test
    fun `리마인더가 꺼져 있으면 아무것도 하지 않는다`() = runTest {
        every { prefs.reminderEnabled } returns false
        task().run()
        verify(exactly = 0) { notifier.showSwimReminder() }
        verify(exactly = 0) { scheduler.schedule() }
    }

    @Test
    fun `오늘 기록이 로컬에도 HC에도 없으면 리마인더를 보내고 재예약한다`() = runTest {
        noLocalToday(); hcEmpty()
        task().run()
        verify(exactly = 1) { notifier.showSwimReminder() }
        verify(exactly = 1) { scheduler.schedule() }
    }

    @Test
    fun `오늘 로컬 기록이 있으면 보내지 않지만 재예약은 한다`() = runTest {
        localToday(); hcEmpty()
        task().run()
        verify(exactly = 0) { notifier.showSwimReminder() }
        verify(exactly = 1) { scheduler.schedule() }
    }

    @Test
    fun `HC에 오늘 세션이 있으면 동기화 전이어도 보내지 않는다`() = runTest {
        noLocalToday()
        coEvery { hc.readSwimSessions(any(), any()) } returns listOf(mockk())
        task().run()
        verify(exactly = 0) { notifier.showSwimReminder() }
    }

    @Test
    fun `HC 조회가 실패하면 로컬 기준으로 판단하고 재예약도 한다`() = runTest {
        noLocalToday()
        coEvery { hc.readSwimSessions(any(), any()) } throws SecurityException("no bg permission")
        task().run()
        verify(exactly = 1) { notifier.showSwimReminder() }
        verify(exactly = 1) { scheduler.schedule() }
    }

    @Test
    fun `로컬 조회가 실패해도 다음 날 재예약은 빠지지 않는다`() = runTest {
        coEvery { dao.getByDateOnce(any()) } throws IllegalStateException("db closed")
        task().run()
        verify(exactly = 0) { notifier.showSwimReminder() }
        verify(exactly = 1) { scheduler.schedule() }
    }
}
