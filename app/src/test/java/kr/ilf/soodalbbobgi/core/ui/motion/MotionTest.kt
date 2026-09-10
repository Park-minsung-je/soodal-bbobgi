package kr.ilf.soodalbbobgi.core.ui.motion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MotionTest {

    @Test
    fun `누름 스크림 페이드는 이전 180ms보다 길고 300ms를 넘지 않는다`() {
        // R2: "조금 더 길게" — 300 초과는 탭 직후 바뀌는 상태 위에 스크림이 남는다.
        assertThat(Motion.DUR_PRESS_FADE).isGreaterThan(180)
        assertThat(Motion.DUR_PRESS_FADE).isAtMost(300)
    }

    @Test
    fun `누름 스크림 페이드는 화면 단순 페이드보다 길지 않다`() {
        // 손끝 피드백이 화면 전환보다 오래 남으면 앱 전체가 느리게 느껴진다.
        assertThat(Motion.DUR_PRESS_FADE).isAtMost(Motion.DUR_FADE)
    }
}
