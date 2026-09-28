package kr.ilf.soodalbbobgi.work

import kr.ilf.soodalbbobgi.core.notify.nextReminderTriggerMillis
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 수영 리마인더 예약 관리 — 설정 시각(시/분) 기준 다음 발화 시각으로 알람 한 개를 건다.
 * 발화 후 재예약은 `SwimReminderTask`가 하고, 재부팅·앱 업데이트·프로세스 시작 시엔 [rescheduleIfEnabled]로 되살린다
 * (AlarmManager 알람은 WorkManager와 달리 재부팅·업데이트 때 지워진다).
 */
@Singleton
class ReminderScheduler @Inject constructor(
    private val prefs: NotificationPrefs,
    private val alarm: ReminderAlarm,
    private val clock: Clock,
) {

    /** 현재 설정(시/분) 기준 다음 발화 시각으로 예약한다. 기존 예약은 교체. */
    fun schedule() {
        val triggerAt = nextReminderTriggerMillis(
            clock.millis(),
            prefs.reminderHour, prefs.reminderMinute,
            clock.zone,
        )
        alarm.set(triggerAt)
    }

    fun cancel() {
        alarm.cancel()
    }

    /** 리마인더가 켜져 있으면 다시 예약한다 — 재부팅·업데이트 뒤 지워진 알람 복구용. 꺼져 있으면 아무것도 하지 않는다. */
    fun rescheduleIfEnabled() {
        if (prefs.reminderEnabled) schedule()
    }
}
