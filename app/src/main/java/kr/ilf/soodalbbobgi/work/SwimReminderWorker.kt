package kr.ilf.soodalbbobgi.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kr.ilf.soodalbbobgi.core.notify.SoodalNotifier
import kr.ilf.soodalbbobgi.core.notify.nextReminderDelayMillis
import kr.ilf.soodalbbobgi.core.notify.shouldSendReminder
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.local.db.SwimLogDao
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 매일 설정 시간에 도는 수영 리마인더 워커.
 *
 * 오늘 수영 기록이 있으면 알림을 보내지 않는다 — 로컬 DB뿐 아니라 Health Connect도 확인해,
 * 수영은 했지만 아직 동기화 전인 날에 "기록이 없어요"라고 잘못 알리지 않게 한다
 * (그 시점엔 새 기록 알림이 함께 떠 서로 모순됐다).
 * 끝나면 다음 날 같은 시간으로 자기 재예약한다.
 */
@HiltWorker
class SwimReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: NotificationPrefs,
    private val swimLogDao: SwimLogDao,
    private val healthConnectManager: HealthConnectManager,
    private val notifier: SoodalNotifier,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.reminderEnabled) return Result.success()
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
        return Result.success()
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

/**
 * 수영 리마인더 예약 관리 — 설정 시간 기준 다음 발화 시각으로 1회성 작업을 건다.
 * (정확한 시각 발화가 필요해 주기 작업 대신 1회성 + 자기 재예약 체인을 쓴다)
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: NotificationPrefs,
) {

    /** 현재 설정(시/분) 기준 다음 발화 시각으로 예약한다. 기존 예약은 교체. */
    fun schedule() {
        val delay = nextReminderDelayMillis(
            System.currentTimeMillis(),
            prefs.reminderHour, prefs.reminderMinute,
            ZoneId.systemDefault(),
        )
        val request = OneTimeWorkRequestBuilder<SwimReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "swim_reminder"
    }
}
