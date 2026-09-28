package kr.ilf.soodalbbobgi.work

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import org.junit.Test

/**
 * 알림 예약 복구 — 로그인된 상태에서만, 설정에 따라 리마인더 알람·새 기록 워커를 되살린다.
 * 로그아웃 뒤 앱이 재시작(프로세스 시작)되거나 재부팅돼도 로그아웃 상태에선 알림이 다시 예약되면 안 된다(R45).
 */
class NotificationSchedulesTest {

    private val tokenStore: TokenStore = mockk()
    private val prefs: NotificationPrefs = mockk()
    private val reminder: ReminderScheduler = mockk(relaxed = true)
    private val hcCheck: HcChangeCheckScheduler = mockk(relaxed = true)

    private fun schedules(loggedIn: Boolean, newRecordOn: Boolean): NotificationSchedules {
        every { tokenStore.getAccessToken() } returns if (loggedIn) "at" else null
        every { prefs.newRecordEnabled } returns newRecordOn
        return NotificationSchedules(tokenStore, prefs, reminder, hcCheck)
    }

    @Test
    fun `로그아웃 상태면 아무것도 예약하지 않는다`() {
        schedules(loggedIn = false, newRecordOn = true).restoreIfLoggedIn()
        verify(exactly = 0) { reminder.rescheduleIfEnabled(); reminder.schedule(); hcCheck.schedule() }
    }

    @Test
    fun `로그인 상태면 리마인더는 설정에 맡기고 새 기록 워커는 켜져 있을 때만 건다`() {
        schedules(loggedIn = true, newRecordOn = true).restoreIfLoggedIn()
        verify(exactly = 1) { reminder.rescheduleIfEnabled() }
        verify(exactly = 1) { hcCheck.schedule() }
    }

    @Test
    fun `새 기록 알림이 꺼져 있으면 워커를 걸지 않는다`() {
        schedules(loggedIn = true, newRecordOn = false).restoreIfLoggedIn()
        verify(exactly = 1) { reminder.rescheduleIfEnabled() }
        verify(exactly = 0) { hcCheck.schedule() }
    }
}
