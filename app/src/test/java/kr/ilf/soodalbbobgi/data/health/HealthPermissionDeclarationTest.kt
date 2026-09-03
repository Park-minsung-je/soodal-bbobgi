package kr.ilf.soodalbbobgi.data.health

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * `themes.xml`의 `health_permissions` 배열, `AndroidManifest.xml`의 `uses-permission`,
 * 런타임 요청 셋(`HealthConnectManager.requestPermissions` + `BG_READ_PERMISSION`)이
 * 서로 어긋나지 않는지 확인한다.
 *
 * 세 곳 중 하나만 권한을 바꾸고 나머지를 깜빡하면 여기서 걸린다 — 기대값은 의도적으로
 * 하드코딩해서, 권한을 바꾸려면 이 테스트도 사람이 직접 고쳐야 하게 만든다.
 */
class HealthPermissionDeclarationTest {

    /** Gradle은 모듈 디렉터리(app/)를 작업 디렉터리로 유닛 테스트를 돌리지만, 다른 작업 디렉터리에서 돌리는 경우를 대비해 폴백한다. */
    private fun resolveFile(relativeFromModule: String): File {
        val fromModuleRoot = File(relativeFromModule)
        if (fromModuleRoot.exists()) return fromModuleRoot
        return File("app/$relativeFromModule")
    }

    private fun readItemsInArray(xml: String, arrayName: String): List<String> {
        val arrayRegex = Regex(
            """<array\s+name="$arrayName">(.*?)</array>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        val block = arrayRegex.find(xml)?.groupValues?.get(1)
            ?: error("<array name=\"$arrayName\"> 블록을 찾지 못했다")
        return Regex("""<item>(.*?)</item>""").findAll(block).map { it.groupValues[1] }.toList()
    }

    private fun readManifestHealthPermissions(xml: String): List<String> {
        val nameRegex = Regex("""<uses-permission\s+android:name="(android\.permission\.health\.[A-Z_]+)"""")
        return nameRegex.findAll(xml).map { it.groupValues[1] }.toList()
    }

    @Test
    fun `health_permissions 배열은 앱이 요청하는 여섯 권한과 1대1이다`() {
        val themesXml = resolveFile("src/main/res/values/themes.xml").readText()
        val items = readItemsInArray(themesXml, "health_permissions")

        val expected = listOf(
            "androidx.health.permission.ExerciseSession.READ",
            "androidx.health.permission.Distance.READ",
            "androidx.health.permission.HeartRate.READ",
            "androidx.health.permission.TotalCaloriesBurned.READ",
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND",
            "android.permission.health.READ_HEALTH_DATA_HISTORY",
        )

        assertThat(items).containsExactlyElementsIn(expected).inOrder()
    }

    @Test
    fun `매니페스트의 health 권한 선언은 정확히 여섯 개이며 READ_SPEED를 포함하지 않는다`() {
        val manifestXml = resolveFile("src/main/AndroidManifest.xml").readText()
        val permissions = readManifestHealthPermissions(manifestXml)

        val expected = setOf(
            "android.permission.health.READ_EXERCISE",
            "android.permission.health.READ_DISTANCE",
            "android.permission.health.READ_HEART_RATE",
            "android.permission.health.READ_TOTAL_CALORIES_BURNED",
            "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND",
            "android.permission.health.READ_HEALTH_DATA_HISTORY",
        )

        assertThat(permissions.toSet()).isEqualTo(expected)
        assertThat(permissions).doesNotContain("android.permission.health.READ_SPEED")
    }

    @Test
    fun `런타임 요청 셋은 정확히 여섯 개다`() {
        val runtimeRequestSet = HealthConnectManager.requestPermissions + HealthConnectManager.BG_READ_PERMISSION

        assertThat(runtimeRequestSet).hasSize(6)
    }
}
