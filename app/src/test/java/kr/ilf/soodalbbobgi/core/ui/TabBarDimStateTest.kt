package kr.ilf.soodalbbobgi.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TabBarDimStateTest {

    @Test
    fun `처음엔 dim이 없다`() {
        val state = TabBarDimState()
        assertThat(state.alpha).isEqualTo(0f)
        assertThat(state.blocking).isFalse()
    }

    @Test
    fun `요청한 알파가 그대로 반영된다`() {
        // 팝업 스크림과 같은 박자로 움직이도록 탭바 dim은 자체 애니메이션 없이 전달값을 쓴다.
        val state = TabBarDimState()
        val key = Any()
        state.set(key, 0.4f)
        assertThat(state.alpha).isEqualTo(0.4f)
        assertThat(state.blocking).isTrue()
    }

    @Test
    fun `겹친 팝업 중 가장 진한 알파를 쓴다`() {
        val state = TabBarDimState()
        state.set("a", 0.3f)
        state.set("b", 0.9f)
        assertThat(state.alpha).isEqualTo(0.9f)
    }

    @Test
    fun `팝업 하나가 닫혀도 남은 요청이 dim을 유지한다`() {
        val state = TabBarDimState()
        state.set("a", 1f)
        state.set("b", 1f)
        state.remove("a")
        assertThat(state.alpha).isEqualTo(1f)
        assertThat(state.blocking).isTrue()
    }

    @Test
    fun `모든 요청이 사라지면 dim도 사라진다`() {
        val state = TabBarDimState()
        state.set("a", 1f)
        state.remove("a")
        assertThat(state.alpha).isEqualTo(0f)
        assertThat(state.blocking).isFalse()
    }

    @Test
    fun `알파는 0과 1 사이로 잘린다`() {
        val state = TabBarDimState()
        state.set("a", 1.7f)
        assertThat(state.alpha).isEqualTo(1f)
        state.set("a", -0.5f)
        assertThat(state.alpha).isEqualTo(0f)
    }
}
