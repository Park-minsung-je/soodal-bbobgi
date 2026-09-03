package kr.ilf.soodalbbobgi.presentation.onboarding

import androidx.lifecycle.ViewModel
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.work.HcChangeCheckScheduler
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
    private val hcChangeCheckScheduler: HcChangeCheckScheduler,
    private val healthConnectManager: HealthConnectManager,
    private val appState: AppState,
) : ViewModel() {

    /** HC 필수 권한이 연결돼 있는지 — 수영 기록 알림은 HC 연동이 전제다. */
    suspend fun isHcConnected(): Boolean = healthConnectManager.hasAllPermissions()

    /**
     * "시작하기" 직전 호출 — 바로 이어지는 홈 진입에서 설정 안내 팝업(R30)을 한 번 건너뛰게 한다.
     * 온보딩이 방금 HC 연결과 알림을 물었으므로 홈에서 곧바로 또 묻지 않는다.
     */
    fun markOnboardingJustFinished() {
        appState.suppressSetupNudgeOnce = true
    }

    /** HC 백그라운드 읽기 권한이 이미 있는지 — 있으면 요청 화면을 띄우지 않는다. */
    suspend fun isBgReadGranted(): Boolean = healthConnectManager.isBackgroundReadGranted()

    private val _reminderEnabled = MutableStateFlow(notificationPrefs.reminderEnabled)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled

    private val _reminderTime = MutableStateFlow(notificationPrefs.reminderHour to notificationPrefs.reminderMinute)
    /** 리마인더 시각 (시, 분). */
    val reminderTime: StateFlow<Pair<Int, Int>> = _reminderTime

    private val _newRecordEnabled = MutableStateFlow(notificationPrefs.newRecordEnabled)
    /** 새 기록 알림 on/off — 백그라운드로 HC 변경을 확인해 알린다. */
    val newRecordEnabled: StateFlow<Boolean> = _newRecordEnabled

    /** 새 기록 알림 on/off — 켜면 주기적 HC 변경 확인을 예약, 끄면 취소. */
    fun setNewRecordEnabled(enabled: Boolean) {
        notificationPrefs.newRecordEnabled = enabled
        _newRecordEnabled.value = enabled
        if (enabled) hcChangeCheckScheduler.schedule() else hcChangeCheckScheduler.cancel()
    }

    /** 리마인더 on/off — 켜면 다음 발화 시각으로 예약, 끄면 취소. */
    fun setReminderEnabled(enabled: Boolean) {
        notificationPrefs.reminderEnabled = enabled
        _reminderEnabled.value = enabled
        if (enabled) reminderScheduler.schedule() else reminderScheduler.cancel()
    }

    init {
        // 온보딩은 항상 꺼진 상태에서 시작한다 — 백업 복원 등으로 이전 설정이 남아
        // 있어도 사용자가 이 화면에서 직접 켠 것만 유효하다.
        if (notificationPrefs.reminderEnabled) setReminderEnabled(false)
        if (notificationPrefs.newRecordEnabled) setNewRecordEnabled(false)
    }

    /** 리마인더 시각 변경 — 켜져 있으면 새 시각으로 재예약. */
    fun setReminderTime(hour: Int, minute: Int) {
        notificationPrefs.reminderHour = hour
        notificationPrefs.reminderMinute = minute
        _reminderTime.value = hour to minute
        if (notificationPrefs.reminderEnabled) reminderScheduler.schedule()
    }
}
