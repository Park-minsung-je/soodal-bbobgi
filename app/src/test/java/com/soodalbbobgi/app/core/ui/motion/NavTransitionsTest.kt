package com.soodalbbobgi.app.core.ui.motion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavTransitionsTest {

    @Test
    fun `tab index maps known tab routes`() {
        assertThat(tabIndexOf("home")).isEqualTo(0)
        assertThat(tabIndexOf("calendar")).isEqualTo(1)
        assertThat(tabIndexOf("gacha")).isEqualTo(2)
        assertThat(tabIndexOf("shop")).isEqualTo(3)
    }

    @Test
    fun `tab index is null for non-tab routes`() {
        assertThat(tabIndexOf("settings")).isNull()
        assertThat(tabIndexOf("profile_editor")).isNull()
        assertThat(tabIndexOf(null)).isNull()
    }

    @Test
    fun `between two tabs forward when target index larger`() {
        assertThat(transitionFor("home", "shop")).isEqualTo(TransitionKind.TAB_FORWARD)
        assertThat(transitionFor("calendar", "gacha")).isEqualTo(TransitionKind.TAB_FORWARD)
    }

    @Test
    fun `between two tabs backward when target index smaller`() {
        assertThat(transitionFor("shop", "home")).isEqualTo(TransitionKind.TAB_BACKWARD)
        assertThat(transitionFor("gacha", "calendar")).isEqualTo(TransitionKind.TAB_BACKWARD)
    }

    @Test
    fun `settings uses push`() {
        assertThat(transitionFor("home", "settings")).isEqualTo(TransitionKind.PUSH)
        assertThat(transitionFor("onboarding_nickname", "onboarding_permission"))
            .isEqualTo(TransitionKind.PUSH)
    }

    @Test
    fun `null routes fall back to push`() {
        assertThat(transitionFor(null, "home")).isEqualTo(TransitionKind.PUSH)
    }

    @Test
    fun `leaving splash uses fade`() {
        assertThat(transitionFor("splash", "home")).isEqualTo(TransitionKind.FADE)
        assertThat(transitionFor("splash", "auth")).isEqualTo(TransitionKind.FADE)
        assertThat(transitionFor("splash", "onboarding_nickname")).isEqualTo(TransitionKind.FADE)
    }

    @Test
    fun `entering home from auth or onboarding uses fade`() {
        assertThat(transitionFor("auth", "home")).isEqualTo(TransitionKind.FADE)
        assertThat(transitionFor("onboarding_permission", "home")).isEqualTo(TransitionKind.FADE)
    }

    @Test
    fun `onboarding step progression still uses push`() {
        assertThat(transitionFor("onboarding_nickname", "onboarding_permission"))
            .isEqualTo(TransitionKind.PUSH)
    }
}
