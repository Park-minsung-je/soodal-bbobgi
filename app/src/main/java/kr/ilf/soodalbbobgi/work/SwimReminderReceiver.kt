package kr.ilf.soodalbbobgi.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kr.ilf.soodalbbobgi.core.di.ApplicationScope
import timber.log.Timber
import javax.inject.Inject

/**
 * 리마인더 알람 리시버 — `AlarmManagerReminderAlarm`이 건 알람이 울리면 [SwimReminderTask]를 바로 돌린다.
 * `goAsync`로 리시버 수명을 잠깐 늘려 HC 조회·DB 조회를 끝낸다 (리시버 허용 시간 안에 끝나는 가벼운 작업).
 */
@AndroidEntryPoint
class SwimReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var task: SwimReminderTask
    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        scope.launch {
            try {
                task.run()
            } catch (e: Exception) {
                Timber.w(e, "리마인더 알람 처리 실패")
            } finally {
                pending.finish()
            }
        }
    }
}

/**
 * 재부팅·앱 업데이트 뒤 리마인더 알람 복구 — AlarmManager 알람은 이 두 경우에 지워진다.
 * 리마인더가 꺼져 있으면 아무것도 하지 않는다.
 */
@AndroidEntryPoint
class ReminderBootReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> scheduler.rescheduleIfEnabled()
        }
    }
}
