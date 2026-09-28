package kr.ilf.soodalbbobgi.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 리마인더 알람 한 개를 거는 시스템 경계 — 예약 로직(`ReminderScheduler`)이 AlarmManager 없이 테스트되도록 분리했다.
 */
interface ReminderAlarm {
    /** 절대 시각(epoch ms)에 울리는 알람을 건다. 이미 걸린 알람은 교체된다. */
    fun set(triggerAtMillis: Long)
    fun cancel()
}

/**
 * AlarmManager 구현 — `setAndAllowWhileIdle`(RTC_WAKEUP)로 건다.
 *
 * WorkManager 지연 작업은 Doze·앱 대기 버킷에 따라 수십 분~수 시간 미뤄져 설정 시간과 동떨어진 시각에 알림이 왔다.
 * 정확 알람(`SCHEDULE_EXACT_ALARM`)은 사용자 동의와 Play 사유가 필요해 피하고, 권한 없이 쓸 수 있는
 * 유휴 허용 알람을 쓴다 — Doze 중에도 ±10분 안팎으로 울린다.
 */
@Singleton
class AlarmManagerReminderAlarm @Inject constructor(
    @ApplicationContext private val context: Context,
) : ReminderAlarm {

    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE,
        Intent(context, SwimReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    override fun set(triggerAtMillis: Long) {
        // 이전 버전이 남긴 WorkManager 1회성 예약은 워커 클래스가 없어 실패만 남기므로 함께 걷어낸다.
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent())
    }

    override fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(LEGACY_WORK_NAME)
        val pi = pendingIntent()
        alarmManager.cancel(pi)
        pi.cancel()
    }

    companion object {
        private const val REQUEST_CODE = 2001
        /** 알람 도입 전 `SwimReminderWorker`가 쓰던 WorkManager 고유 작업 이름. */
        private const val LEGACY_WORK_NAME = "swim_reminder"
    }
}

/** 리마인더 알람 바인딩 + 예약 시각 계산용 시계. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ReminderModule {

    @Binds
    abstract fun bindReminderAlarm(impl: AlarmManagerReminderAlarm): ReminderAlarm

    companion object {
        @Provides
        fun provideClock(): Clock = Clock.systemDefaultZone()
    }
}
