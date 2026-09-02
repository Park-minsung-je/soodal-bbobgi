package kr.ilf.soodalbbobgi.data.health

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 설정 "연결됨" 탭이 SDK별로 어느 HC 화면을 어떤 순서로 시도하는지 검증.
 * 14+는 앱별 권한 화면이 첫 후보여야 하고, 13 이하는 HC 앱 설정만 시도한다.
 */
class HealthConnectIntentsTest {

    private val pkg = "kr.ilf.soodalbbobgi"

    @Test
    fun `Android 14 이상은 앱별 권한 화면을 먼저, HC 홈을 폴백으로 시도한다`() {
        val targets = hcPermissionScreenTargets(
            sdkInt = 34, packageName = pkg,
            settingsAction = "android.health.connect.action.HEALTH_HOME_SETTINGS",
        )
        assertThat(targets.map { it.action }).containsExactly(
            ACTION_MANAGE_HEALTH_PERMISSIONS,
            "android.health.connect.action.HEALTH_HOME_SETTINGS",
        ).inOrder()
        assertThat(targets.first().packageExtra).isEqualTo(pkg)
        assertThat(targets[1].packageExtra).isNull()
    }

    @Test
    fun `Android 13 이하는 HC 앱 설정만 시도한다`() {
        val targets = hcPermissionScreenTargets(
            sdkInt = 33, packageName = pkg,
            settingsAction = "androidx.health.ACTION_HEALTH_CONNECT_SETTINGS",
        )
        assertThat(targets.map { it.action }).containsExactly("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
        assertThat(targets.first().packageExtra).isNull()
    }
}
