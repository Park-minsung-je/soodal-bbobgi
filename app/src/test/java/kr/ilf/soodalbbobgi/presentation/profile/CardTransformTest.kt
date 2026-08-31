package kr.ilf.soodalbbobgi.presentation.profile

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CardTransformTest {

    // 카드 1472x704(가로형). 90도 회전 후 세로=1472, 가로=704 방향.
    // 세로형 화면 1080x2400 → 높이 제약(2400/1472) vs 폭 제약(1080/704) 중 작은 값.
    @Test
    fun `fullscreen scale fills by the tighter of height and width`() {
        val scale = fullscreenCardScale(cardW = 1472f, cardH = 704f, screenW = 1080f, screenH = 2400f)
        // byHeight = 2400/1472 = 1.630..., byWidth = 1080/704 = 1.534...
        assertThat(scale).isWithin(0.001f).of(1080f / 704f)
    }

    @Test
    fun `progress 0 keeps card at home position unscaled and unrotated`() {
        val t = fullscreenCardTransform(
            progress = 0f,
            homeCenterX = 300f, homeCenterY = 400f,
            overlayCenterX = 500f, overlayCenterY = 1200f,
            fullscreenScale = 1.5f,
        )
        assertThat(t.rotationZ).isEqualTo(0f)
        assertThat(t.scale).isEqualTo(1f)
        // 오버레이 카드를 홈 카드 중심으로 당긴다.
        assertThat(t.translationX).isEqualTo(300f - 500f)
        assertThat(t.translationY).isEqualTo(400f - 1200f)
    }

    @Test
    fun `progress 1 centers fully rotated and scaled card`() {
        val t = fullscreenCardTransform(
            progress = 1f,
            homeCenterX = 300f, homeCenterY = 400f,
            overlayCenterX = 500f, overlayCenterY = 1200f,
            fullscreenScale = 1.5f,
        )
        assertThat(t.rotationZ).isEqualTo(90f)
        assertThat(t.scale).isEqualTo(1.5f)
        // zoom=1에서 오버레이는 자기 자리(중앙)에 그대로 → 이동 0. (-0.0f 부호 아티팩트 허용)
        assertThat(t.translationX).isWithin(0.0001f).of(0f)
        assertThat(t.translationY).isWithin(0.0001f).of(0f)
    }

    @Test
    fun `progress half interpolates linearly`() {
        val t = fullscreenCardTransform(
            progress = 0.5f,
            homeCenterX = 0f, homeCenterY = 0f,
            overlayCenterX = 100f, overlayCenterY = 200f,
            fullscreenScale = 3f,
        )
        assertThat(t.rotationZ).isEqualTo(45f)
        assertThat(t.scale).isEqualTo(2f) // 1 + (3-1)*0.5
        assertThat(t.translationX).isEqualTo((0f - 100f) * 0.5f)
        assertThat(t.translationY).isEqualTo((0f - 200f) * 0.5f)
    }
}
