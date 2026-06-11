package com.soodalbbobgi.app.core.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TabBarDimStateTest {

    @Test
    fun `not dimmed initially`() {
        assertThat(TabBarDimState().dimmed).isFalse()
    }

    @Test
    fun `dimmed after acquire`() {
        val state = TabBarDimState()
        state.acquire()
        assertThat(state.dimmed).isTrue()
    }

    @Test
    fun `stays dimmed while any nested request is alive`() {
        // 팝업 위에 팝업이 겹친 경우 — 하나만 닫혀도 dim은 유지된다.
        val state = TabBarDimState()
        state.acquire()
        state.acquire()
        state.release()
        assertThat(state.dimmed).isTrue()
    }

    @Test
    fun `undimmed when all requests are released`() {
        val state = TabBarDimState()
        state.acquire()
        state.acquire()
        state.release()
        state.release()
        assertThat(state.dimmed).isFalse()
    }

    @Test
    fun `extra release does not push the counter below zero`() {
        // 여분의 release 후에도 다음 acquire가 정상적으로 dim을 켜야 한다.
        val state = TabBarDimState()
        state.release()
        state.acquire()
        assertThat(state.dimmed).isTrue()
    }
}
