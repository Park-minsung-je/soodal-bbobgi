package com.soodalbbobgi.app.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.soodalbbobgi.app.core.notify.SoodalNotifier
import com.soodalbbobgi.app.data.health.HcSyncPreferences
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.notify.NotificationPrefs
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 백그라운드 새 수영 기록 감지 워커.
 *
 * 저장된 HC 변경 토큰으로 **변경 존재 여부만** 확인하고, 있으면 "앱을 열어 조개를 받으세요"
 * 알림을 띄운다. 실제 동기화/조개 지급은 앱을 열었을 때 기존 흐름이 처리한다 —
 * 그래서 토큰은 절대 소비하지 않으며, 같은 토큰 상태에는 한 번만 알린다.
 */
@HiltWorker
class HcChangeCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: NotificationPrefs,
    private val hcSyncPreferences: HcSyncPreferences,
    private val healthConnectManager: HealthConnectManager,
    private val notifier: SoodalNotifier,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!prefs.newRecordEnabled) return Result.success()
        val token = hcSyncPreferences.getChangesToken() ?: return Result.success()
        if (prefs.notifiedChangeToken == token) return Result.success() // 이 상태는 이미 알림

        try {
            val hasChanges = healthConnectManager.hasSwimChanges(token)
            if (hasChanges == true) {
                notifier.showNewSwimRecord()
                prefs.notifiedChangeToken = token
            }
        } catch (e: SecurityException) {
            // HC 백그라운드 읽기 권한 미허용 — 조용히 스킵 (포그라운드 동기화는 정상)
            Timber.w(e, "HC 백그라운드 읽기 권한 없음")
        } catch (e: Exception) {
            Timber.w(e, "백그라운드 HC 변경 확인 실패")
        }
        return Result.success()
    }
}

/** 새 기록 감지 주기 작업 예약 관리 (30분 간격). */
@Singleton
class HcChangeCheckScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    // 변경 토큰으로 '변경 유무'만 확인하는 가벼운 작업이라 짧은 주기에도 배터리 부담이 작다.
    // 수영 직후 되도록 빨리 알리려 30분으로 둔다.
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<HcChangeCheckWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        private const val WORK_NAME = "hc_change_check"
    }
}
