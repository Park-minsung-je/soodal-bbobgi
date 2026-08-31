package kr.ilf.soodalbbobgi.presentation.onboarding

import androidx.lifecycle.ViewModel
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/**
 * 온보딩 알림 단계 — 수영 리마인더 on/off와 시각을 정한다.
 * 토글/시각은 입력 즉시 [NotificationPrefs]에 영속화하고, 켜져 있으면 다음 발화로 예약한다.
 */
@HiltViewModel
class OnboardingNotificationViewModel @Inject constructor(
    private val notificationPrefs: NotificationPrefs,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _reminderEnabled = MutableStateFlow(notificationPrefs.reminderEnabled)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled

    private val _reminderTime = MutableStateFlow(notificationPrefs.reminderHour to notificationPrefs.reminderMinute)
    /** 리마인더 시각 (시, 분). */
    val reminderTime: StateFlow<Pair<Int, Int>> = _reminderTime

    /** 리마인더 on/off — 켜면 다음 발화 시각으로 예약, 끄면 취소. */
    fun setReminderEnabled(enabled: Boolean) {
        notificationPrefs.reminderEnabled = enabled
        _reminderEnabled.value = enabled
        if (enabled) reminderScheduler.schedule() else reminderScheduler.cancel()
    }

    /** 리마인더 시각 변경 — 켜져 있으면 새 시각으로 재예약. */
    fun setReminderTime(hour: Int, minute: Int) {
        notificationPrefs.reminderHour = hour
        notificationPrefs.reminderMinute = minute
        _reminderTime.value = hour to minute
        if (notificationPrefs.reminderEnabled) reminderScheduler.schedule()
    }
}
