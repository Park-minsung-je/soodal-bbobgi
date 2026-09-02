package kr.ilf.soodalbbobgi.data.health

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.health.connect.client.HealthConnectClient
import timber.log.Timber

/**
 * Health Connect 화면을 여는 인텐트 후보 하나 — 액션 + (앱별 권한 화면용) 패키지 엑스트라.
 *
 * @param action 인텐트 액션 문자열
 * @param packageExtra [Intent.EXTRA_PACKAGE_NAME]에 실을 앱 패키지명 (앱별 권한 화면일 때만)
 */
data class HcScreenTarget(
    val action: String,
    val packageExtra: String? = null,
)

/** 플랫폼 상수 android.health.connect.HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS (API 34+). */
internal const val ACTION_MANAGE_HEALTH_PERMISSIONS = "android.health.connect.action.MANAGE_HEALTH_PERMISSIONS"

/**
 * 설정 "연결됨" 탭 시 시도할 HC 권한 화면 인텐트 후보를 우선순위대로 돌려준다.
 *
 * 14+는 앱별 권한 화면(MANAGE_HEALTH_PERMISSIONS + 패키지명) → HC 홈 순,
 * 13 이하는 앱별 권한 화면 액션이 공개돼 있지 않아 HC 앱 설정만 시도한다.
 * Android 객체를 만들지 않는 순수 함수라 JVM 유닛 테스트로 후보 순서를 고정한다.
 *
 * @param sdkInt Build.VERSION.SDK_INT
 * @param packageName 이 앱 패키지명 (Intent.EXTRA_PACKAGE_NAME)
 * @param settingsAction HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS (SDK별 값이 달라 주입)
 * @return 시도 순서대로 정렬된 후보 목록
 */
internal fun hcPermissionScreenTargets(
    sdkInt: Int,
    packageName: String,
    settingsAction: String,
): List<HcScreenTarget> =
    if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        listOf(
            HcScreenTarget(ACTION_MANAGE_HEALTH_PERMISSIONS, packageExtra = packageName),
            HcScreenTarget(settingsAction),
        )
    } else {
        listOf(HcScreenTarget(settingsAction))
    }

/**
 * HC 권한 화면(없으면 HC 설정)을 연다. 후보를 순서대로 시도하고 하나라도 열리면 true.
 * resolveActivity로 미리 거르지 않는다 — 14+ 권한 UI는 패키지 가시성 밖일 수 있어 try/catch가 안전하다.
 *
 * @param context 액티비티를 띄울 컨텍스트
 * @return 어느 후보든 열렸으면 true, 전부 실패하면 false (호출자가 안내)
 */
fun openHealthConnectPermissions(context: Context): Boolean {
    val targets = hcPermissionScreenTargets(
        sdkInt = Build.VERSION.SDK_INT,
        packageName = context.packageName,
        settingsAction = HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS,
    )
    for (target in targets) {
        val intent = Intent(target.action).apply {
            target.packageExtra?.let { putExtra(Intent.EXTRA_PACKAGE_NAME, it) }
        }
        try {
            context.startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "HC 화면 열기 실패 — 다음 후보로: ${target.action}")
        }
    }
    return false
}
