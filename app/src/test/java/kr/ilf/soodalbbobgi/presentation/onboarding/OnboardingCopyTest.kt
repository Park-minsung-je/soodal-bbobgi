package kr.ilf.soodalbbobgi.presentation.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 온보딩 안내 문구 규칙 — 권한이 필요한 항목은 마지막 줄에 권한을 명시하고,
 * HC 과거 데이터 권한은 공식 라벨을 쓰며, 말투는 해요체로 통일한다.
 */
class OnboardingCopyTest {

    private val twoLinePermissionCopies = listOf(
        OnboardingCopy.HISTORY_GUIDE,
        OnboardingCopy.REMINDER,
        OnboardingCopy.NEW_RECORD,
    )

    @Test
    fun `권한 안내는 둘째 줄에 권한을 명시한다`() {
        twoLinePermissionCopies.forEach { copy ->
            val lines = copy.split("\n")
            assertThat(lines).hasSize(2)
            assertThat(lines[1]).contains("권한이 필요해요")
        }
    }

    @Test
    fun `HC 필수 카드는 읽기 권한 다섯 가지를 전부 말한다`() {
        listOf("운동", "거리", "심박수", "속도", "칼로리").forEach {
            assertThat(OnboardingCopy.HC_REQUIRED).contains(it)
        }
        assertThat(OnboardingCopy.HC_REQUIRED).contains("읽기 권한이 필요해요")
    }

    @Test
    fun `지난 기록 안내는 HC 공식 권한 라벨을 그대로 쓴다`() {
        assertThat(OnboardingCopy.HISTORY_GUIDE).contains("'모든 기간의 데이터에 액세스'")
        assertThat(OnboardingCopy.HISTORY_PERMISSION_MISSING).contains("'모든 기간의 데이터에 액세스'")
    }

    @Test
    fun `지난 기록 정책은 조개 미지급과 소요시간을 함께 말한다`() {
        assertThat(OnboardingCopy.HISTORY_POLICY).contains("오늘 수영한 기록에만")
        assertThat(OnboardingCopy.HISTORY_POLICY).contains("새벽 2시")
        assertThat(OnboardingCopy.HISTORY_POLICY).contains("캘린더에만")
        assertThat(OnboardingCopy.HISTORY_POLICY).contains("시간이 더 걸려요")
    }

    @Test
    fun `기록 알림은 알림 권한과 HC 백그라운드 권한을 둘 다 말한다`() {
        assertThat(OnboardingCopy.NEW_RECORD).contains("Android 알림 권한")
        assertThat(OnboardingCopy.NEW_RECORD).contains("Health Connect 백그라운드 읽기 권한")
    }

    @Test
    fun `말투는 해요체로 통일하고 동의라는 말을 쓰지 않는다`() {
        OnboardingCopy.ALL.forEach { copy ->
            assertThat(copy).doesNotContain("동의")
            assertThat(copy).doesNotContain("습니다")
            assertThat(copy).doesNotContain("달력")
            assertThat(copy.trimEnd()).endsWith("요.")
        }
    }
}
