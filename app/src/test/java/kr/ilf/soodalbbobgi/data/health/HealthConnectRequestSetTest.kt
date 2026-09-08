package kr.ilf.soodalbbobgi.data.health

import androidx.health.connect.client.permission.HealthPermission
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 온보딩·설정이 Health Connect에 요청하는 권한 셋을 검증한다.
 * 백그라운드 읽기와 모든 기간은 선택 권한이라 토글에 따라 붙고, 설정 재연결은 전부 요청한다.
 */
class HealthConnectRequestSetTest {

    private val history = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    private val background = HealthConnectManager.BG_READ_PERMISSION

    @Test
    fun `둘 다 끄면 읽기 4종만 요청한다`() {
        val set = HealthConnectManager.requestPermissionsFor(includeHistory = false, includeBackground = false)

        assertThat(set).hasSize(4)
        assertThat(set).containsNoneOf(history, background)
    }

    @Test
    fun `백그라운드 토글만 켜면 백그라운드 읽기가 추가된다`() {
        val set = HealthConnectManager.requestPermissionsFor(includeHistory = false, includeBackground = true)

        assertThat(set).hasSize(5)
        assertThat(set).contains(background)
        assertThat(set).doesNotContain(history)
    }

    @Test
    fun `지난 기록 토글만 켜면 모든 기간 권한이 추가된다`() {
        val set = HealthConnectManager.requestPermissionsFor(includeHistory = true, includeBackground = false)

        assertThat(set).hasSize(5)
        assertThat(set).contains(history)
        assertThat(set).doesNotContain(background)
    }

    @Test
    fun `설정 재연결 셋은 선택 권한 둘을 모두 포함한다`() {
        assertThat(HealthConnectManager.requestPermissions).hasSize(6)
        assertThat(HealthConnectManager.requestPermissions).containsAtLeast(history, background)
    }
}
