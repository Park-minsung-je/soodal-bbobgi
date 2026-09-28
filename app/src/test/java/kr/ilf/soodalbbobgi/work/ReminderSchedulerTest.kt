package kr.ilf.soodalbbobgi.work

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 리마인더 예약 — 설정 시각을 AlarmManager 절대 시각(RTC)으로 건다.
 * WorkManager 지연 작업은 Doze·대기 버킷에 따라 수십 분~수 시간 미뤄져 "설정한 시간에 안 온다"는 문제가 있었다.
 */
class ReminderSchedulerTest {

    private val zone = ZoneId.of("Asia/Seoul")
    private fun at(hour: Int, minute: Int): Instant =
        ZonedDateTime.of(2026, 9, 28, hour, minute, 0, 0, zone).toInstant()

    /** 알람을 실제로 걸지 않고 마지막 요청만 기록하는 대역. */
    private class FakeAlarm : ReminderAlarm {
        var triggerAt: Long? = null
        var setCount = 0
        var cancelled = false
        override fun set(triggerAtMillis: Long) { triggerAt = triggerAtMillis; setCount++ }
        override fun cancel() { cancelled = true; triggerAt = null }
    }

    private fun prefs(enabled: Boolean = true, hour: Int = 21, minute: Int = 0): NotificationPrefs =
        mockk {
            every { reminderEnabled } returns enabled
            every { reminderHour } returns hour
            every { reminderMinute } returns minute
        }

    private fun scheduler(prefs: NotificationPrefs, alarm: FakeAlarm, now: Instant) =
        ReminderScheduler(prefs, alarm, Clock.fixed(now, zone))

    @Test
    fun `설정 시각이 아직 안 지났으면 오늘 그 시각으로 알람을 건다`() {
        val alarm = FakeAlarm()
        scheduler(prefs(hour = 21), alarm, now = at(14, 0)).schedule()
        assertThat(alarm.triggerAt).isEqualTo(at(21, 0).toEpochMilli())
    }

    @Test
    fun `설정 시각을 지났으면 다음 날 같은 시각으로 건다`() {
        val alarm = FakeAlarm()
        scheduler(prefs(hour = 21, minute = 30), alarm, now = at(22, 0)).schedule()
        val expected = ZonedDateTime.of(2026, 9, 29, 21, 30, 0, 0, zone).toInstant().toEpochMilli()
        assertThat(alarm.triggerAt).isEqualTo(expected)
    }

    @Test
    fun `취소하면 알람을 지운다`() {
        val alarm = FakeAlarm()
        val s = scheduler(prefs(), alarm, now = at(14, 0))
        s.schedule()
        s.cancel()
        assertThat(alarm.cancelled).isTrue()
        assertThat(alarm.triggerAt).isNull()
    }

    @Test
    fun `재부팅·업데이트 뒤 재예약은 리마인더가 켜져 있을 때만 건다`() {
        val on = FakeAlarm()
        scheduler(prefs(enabled = true), on, now = at(14, 0)).rescheduleIfEnabled()
        assertThat(on.setCount).isEqualTo(1)

        val off = FakeAlarm()
        scheduler(prefs(enabled = false), off, now = at(14, 0)).rescheduleIfEnabled()
        assertThat(off.setCount).isEqualTo(0)
        assertThat(off.cancelled).isFalse()
    }
}
