package kr.ilf.soodalbbobgi.core.update

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 인앱 업데이트 방식 결정 — 서버 최소 버전 아래면 즉시, 그 외 새 버전은 유연, 새 버전 없으면 없음. */
class UpdatePolicyTest {

    @Test
    fun `새 버전이 없으면 아무것도 띄우지 않는다`() {
        assertThat(decideUpdateMode(updateAvailable = false, currentVersionCode = 3, minVersionCode = 10, immediateAllowed = true, flexibleAllowed = true))
            .isEqualTo(UpdateMode.NONE)
    }

    @Test
    fun `최소 버전보다 낮으면 즉시 방식으로 강제한다`() {
        assertThat(decideUpdateMode(true, currentVersionCode = 5, minVersionCode = 6, immediateAllowed = true, flexibleAllowed = true))
            .isEqualTo(UpdateMode.IMMEDIATE)
    }

    @Test
    fun `최소 버전 이상이면 유연 방식이다`() {
        assertThat(decideUpdateMode(true, currentVersionCode = 6, minVersionCode = 6, immediateAllowed = true, flexibleAllowed = true))
            .isEqualTo(UpdateMode.FLEXIBLE)
    }

    @Test
    fun `서버 값을 못 받았으면 강제하지 않고 유연 방식이다`() {
        assertThat(decideUpdateMode(true, currentVersionCode = 1, minVersionCode = null, immediateAllowed = true, flexibleAllowed = true))
            .isEqualTo(UpdateMode.FLEXIBLE)
    }

    @Test
    fun `Play가 허용하는 방식으로 내려간다`() {
        // 강제 대상인데 즉시가 막혀 있으면 유연으로
        assertThat(decideUpdateMode(true, 5, 6, immediateAllowed = false, flexibleAllowed = true)).isEqualTo(UpdateMode.FLEXIBLE)
        // 유연이 막혀 있으면 즉시로
        assertThat(decideUpdateMode(true, 6, 6, immediateAllowed = true, flexibleAllowed = false)).isEqualTo(UpdateMode.IMMEDIATE)
        // 둘 다 막혀 있으면 없음
        assertThat(decideUpdateMode(true, 5, 6, immediateAllowed = false, flexibleAllowed = false)).isEqualTo(UpdateMode.NONE)
    }
}
