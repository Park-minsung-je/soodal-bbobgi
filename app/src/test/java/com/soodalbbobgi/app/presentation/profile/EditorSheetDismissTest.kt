package com.soodalbbobgi.app.presentation.profile

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EditorSheetDismissTest {

    private val distance = 100f
    private val velocity = 1000f

    @Test
    fun `dismisses when dragged past the distance threshold`() {
        assertThat(shouldDismissSheet(150f, 0f, distance, velocity)).isTrue()
    }

    @Test
    fun `dismisses on a fast downward flick even when distance is short`() {
        assertThat(shouldDismissSheet(20f, 1500f, distance, velocity)).isTrue()
    }

    @Test
    fun `stays open when both distance and velocity are below threshold`() {
        assertThat(shouldDismissSheet(40f, 300f, distance, velocity)).isFalse()
    }

    @Test
    fun `an upward flick never dismisses`() {
        // 위로 튕김(음수 속도) + 거리 미달이면 닫지 않는다.
        assertThat(shouldDismissSheet(40f, -2000f, distance, velocity)).isFalse()
    }

    @Test
    fun `exactly at threshold does not dismiss`() {
        // 임계값은 초과(>)일 때만 닫는다 — 경계값은 유지.
        assertThat(shouldDismissSheet(distance, velocity, distance, velocity)).isFalse()
    }
}
