package com.soodalbbobgi.app.core.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.soodalbbobgi.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 앱 알림 발송 담당 — 채널 보장 + 리마인더/새 기록 알림 빌드.
 * 알림 권한이 없으면 조용히 무시한다 (워커가 죽지 않게).
 */
@Singleton
class SoodalNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private fun ensureChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDER, "수영 리마인더", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "설정한 시간에 수영을 잊지 않게 알려줘요" },
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_NEW_RECORD, "새 수영 기록", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "새 수영 기록이 감지되면 알려줘요" },
        )
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAppIntent(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        return PendingIntent.getActivity(
            context, 0, launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun notify(id: Int, channel: String, title: String, body: String) {
        if (!canNotify()) {
            Timber.w("알림 권한 없음 — $title 발송 생략")
            return
        }
        ensureChannels()
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_otter)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Timber.w(e, "알림 발송 실패")
        }
    }

    /** 매일 리마인더 — 오늘 아직 수영 기록이 없을 때. */
    fun showSwimReminder() {
        notify(
            id = ID_REMINDER, channel = CHANNEL_REMINDER,
            title = "오늘 수영 어때요? 🦦",
            body = "수달이가 기다리고 있어요. 수영하고 조개 받아가세요!",
        )
    }

    /** 백그라운드에서 새 수영 기록(HC 변경)이 감지됐을 때. */
    fun showNewSwimRecord() {
        notify(
            id = ID_NEW_RECORD, channel = CHANNEL_NEW_RECORD,
            title = "새 수영 기록 발견! 🐚",
            body = "수달 뽑기를 열면 기록이 등록되고 조개를 받을 수 있어요.",
        )
    }

    companion object {
        private const val CHANNEL_REMINDER = "swim_reminder"
        private const val CHANNEL_NEW_RECORD = "new_swim_record"
        private const val ID_REMINDER = 1001
        private const val ID_NEW_RECORD = 1002
    }
}
