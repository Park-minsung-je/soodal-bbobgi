package kr.ilf.soodalbbobgi.work

import kr.ilf.soodalbbobgi.core.notify.SoodalNotifier
import kr.ilf.soodalbbobgi.core.notify.shouldSendReminder
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.local.db.SwimLogDao
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 리마인더 알람이 울렸을 때 하는 일 — 오늘 수영 기록이 없으면 알림을 띄우고 다음 날 같은 시간으로 재예약한다.
 *
 * 로컬 DB뿐 아니라 Health Connect도 확인해, 수영은 했지만 아직 동기화 전인 날에 "기록이 없어요"라고
 * 잘못 알리지 않게 한다 (그 시점엔 새 기록 알림이 함께 떠 서로 모순됐다).
 * WorkManager 워커가 아니라 알람 리시버에서 직접 돈다 — Doze 중 알람이 준 짧은 실행 창 안에 끝내기 위해서다.
 */
@Singleton
class SwimReminderTask @Inject constructor(
    private val prefs: NotificationPrefs,
    private val swimLogDao: SwimLogDao,
    private val healthConnectManager: HealthConnectManager,
    private val notifier: SoodalNotifier,
    private val scheduler: ReminderScheduler,
) {

    suspend fun run() {
        if (!prefs.reminderEnabled) return
        try {
            val today = LocalDate.now()
            val hasLocal = swimLogDao.getByDateOnce(today.toString()).isNotEmpty()
            if (shouldSendReminder(hasLocal, hasHealthRecordToday(today))) {
                notifier.showSwimReminder()
            }
        } catch (e: Exception) {
            Timber.w(e, "수영 리마인더 처리 실패")
        } finally {
            // 성공/실패와 무관하게 다음 날 같은 시간으로 재예약
            if (prefs.reminderEnabled) scheduler.schedule()
        }
    }

    /**
     * Health Connect에 오늘 수영 세션이 있는지 확인한다.
     *
     * @param today 오늘 날짜
     * @return 있으면 true, 없으면 false, 권한 없음/조회 실패로 알 수 없으면 null
     */
    private suspend fun hasHealthRecordToday(today: LocalDate): Boolean? = try {
        val zone = ZoneId.systemDefault()
        val start = today.atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        healthConnectManager.readSwimSessions(start, end).isNotEmpty()
    } catch (e: Exception) {
        // 백그라운드 읽기 권한 미허용 등 — 판단 불가로 두고 로컬 기준으로 폴백한다
        Timber.w(e, "리마인더용 HC 오늘 기록 확인 실패")
        null
    }
}
