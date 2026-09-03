package kr.ilf.soodalbbobgi.presentation.onboarding

import kr.ilf.soodalbbobgi.data.auth.AccountPrefs
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.work.HcChangeCheckScheduler
import kr.ilf.soodalbbobgi.work.ReminderScheduler
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

/**
 * OnboardingNotificationViewModel — 온보딩 마지막 단계("시작하기")의 단위 테스트.
 */
class OnboardingNotificationViewModelTest {

    private val notificationPrefs: NotificationPrefs = mockk(relaxed = true)
    private val reminderScheduler: ReminderScheduler = mockk(relaxed = true)
    private val hcChangeCheckScheduler: HcChangeCheckScheduler = mockk(relaxed = true)
    private val healthConnectManager: HealthConnectManager = mockk(relaxed = true)
    private val accountPrefs: AccountPrefs = mockk(relaxed = true)

    private fun vm() = OnboardingNotificationViewModel(
        notificationPrefs = notificationPrefs,
        reminderScheduler = reminderScheduler,
        hcChangeCheckScheduler = hcChangeCheckScheduler,
        healthConnectManager = healthConnectManager,
        accountPrefs = accountPrefs,
    )

    @Test
    fun `시작하기는 온보딩 완료를 기록한다`() {
        vm().completeOnboarding()

        // 이 표시가 있어야 스플래시·로그인 분기가 HC 권한이 없어도 권한 화면으로 되돌리지 않는다 (R19).
        verify(exactly = 1) { accountPrefs.onboardingCompleted = true }
    }
}
