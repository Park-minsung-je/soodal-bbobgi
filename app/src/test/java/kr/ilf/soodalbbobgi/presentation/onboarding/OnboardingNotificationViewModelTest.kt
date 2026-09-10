package kr.ilf.soodalbbobgi.presentation.onboarding

import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.work.HcChangeCheckScheduler
import kr.ilf.soodalbbobgi.work.ReminderScheduler
import org.junit.Before
import org.junit.Test

/**
 * 온보딩 알림 단계 ViewModel 테스트 — "시작하기"가 홈의 설정 안내 팝업(R30)을 한 번 억제하는지 본다.
 */
class OnboardingNotificationViewModelTest {

    private lateinit var notificationPrefs: NotificationPrefs
    private lateinit var appState: AppState

    @Before
    fun setup() {
        notificationPrefs = mockk(relaxed = true)
        every { notificationPrefs.reminderEnabled } returns false
        every { notificationPrefs.newRecordEnabled } returns false
        appState = AppState()
    }

    private fun vm() = OnboardingNotificationViewModel(
        notificationPrefs = notificationPrefs,
        reminderScheduler = mockk<ReminderScheduler>(relaxed = true),
        hcChangeCheckScheduler = mockk<HcChangeCheckScheduler>(relaxed = true),
        healthConnectManager = mockk<HealthConnectManager>(relaxed = true),
        appState = appState,
    )

    @Test
    fun `시작하기는 홈의 설정 안내 팝업을 한 번 건너뛰도록 표시한다`() {
        assertThat(appState.suppressSetupNudgeOnce).isFalse()

        vm().markOnboardingJustFinished()

        // 온보딩이 방금 같은 것을 물었으므로 바로 이어지는 홈 진입에서는 안내를 띄우지 않는다
        assertThat(appState.suppressSetupNudgeOnce).isTrue()
    }
}
