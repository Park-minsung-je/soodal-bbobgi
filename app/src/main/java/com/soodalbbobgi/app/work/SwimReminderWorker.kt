package com.soodalbbobgi.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.soodalbbobgi.app.core.notify.SoodalNotifier
import com.soodalbbobgi.app.core.notify.nextReminderDelayMillis
import com.soodalbbobgi.app.data.local.db.SwimLogDao
import com.soodalbbobgi.app.data.notify.NotificationPrefs
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
 * 오늘 수영 기록이 이미 있으면 알림을 보내지 않고, 끝나면 다음 날로 자기 재예약한다.
 */
@HiltWorker
class SwimReminderWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: NotificationPrefs,
    private val swimLogDao: SwimLogDao,
    private val notifier: SoodalNotifier,
    private val scheduler: ReminderScheduler,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.reminderEnabled) return Result.success()
        try {
            val today = LocalDate.now().toString()
            if (swimLogDao.getByDateOnce(today).isEmpty()) {
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
