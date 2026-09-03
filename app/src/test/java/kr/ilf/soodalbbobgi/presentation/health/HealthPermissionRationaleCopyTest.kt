package kr.ilf.soodalbbobgi.presentation.health

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import org.junit.Test

/**
 * 권한 사용 근거 화면의 문구가 앱이 실제로 요청하는 Health Connect 권한을 빠짐없이 설명하는지 확인한다.
 * 권한을 새로 추가하면서 설명을 빠뜨리면 여기서 걸린다.
 */
class HealthPermissionRationaleCopyTest {

    @Test
    fun `앱이 요청하는 모든 HC 권한에 설명 항목이 있다`() {
        val requested = HealthConnectManager.requestPermissions + HealthConnectManager.BG_READ_PERMISSION
        val described = HealthPermissionRationaleCopy.dataItems.map { it.permission }.toSet()

        assertThat(described).containsAtLeastElementsIn(requested)
    }

    @Test
    fun `설명 항목은 요청하지 않는 권한을 나열하지 않는다`() {
        val requested = HealthConnectManager.requestPermissions + HealthConnectManager.BG_READ_PERMISSION
        val described = HealthPermissionRationaleCopy.dataItems.map { it.permission }

        assertThat(described).containsNoDuplicates()
        assertThat(requested).containsAtLeastElementsIn(described)
    }

    @Test
    fun `모든 항목에 제목과 설명이 채워져 있다`() {
        HealthPermissionRationaleCopy.dataItems.forEach {
            assertThat(it.title).isNotEmpty()
            assertThat(it.description).isNotEmpty()
        }
        assertThat(HealthPermissionRationaleCopy.purposes).isNotEmpty()
        assertThat(HealthPermissionRationaleCopy.storage).isNotEmpty()
        assertThat(HealthPermissionRationaleCopy.revoke).isNotEmpty()
    }
}
