package com.soodalbbobgi.app.data.notify

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 설정 영속화 — 토글 2종과 리마인더 시간, 마지막으로 알림 보낸 HC 변경 토큰.
 */
@Singleton
class NotificationPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("notify", Context.MODE_PRIVATE)

    /** 수영 리마인더 on/off (기본 off — 켜는 순간 알림 권한을 요청한다). */
    var reminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, 21)
        set(value) = prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()

    /** 새 수영 기록(조개) 알림 on/off. */
    var newRecordEnabled: Boolean
        get() = prefs.getBoolean(KEY_NEW_RECORD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_NEW_RECORD_ENABLED, value).apply()

    /**
     * 마지막으로 "새 기록" 알림을 보낸 시점의 HC 변경 토큰.
     * 같은 토큰 상태에서 반복 알림을 막는다 — 앱이 열려 동기화하면 토큰이 바뀌어 다시 알릴 수 있게 된다.
     */
    var notifiedChangeToken: String?
        get() = prefs.getString(KEY_NOTIFIED_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_NOTIFIED_TOKEN, value).apply()

    companion object {
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"
        private const val KEY_NEW_RECORD_ENABLED = "new_record_enabled"
        private const val KEY_NOTIFIED_TOKEN = "notified_change_token"
    }
}
