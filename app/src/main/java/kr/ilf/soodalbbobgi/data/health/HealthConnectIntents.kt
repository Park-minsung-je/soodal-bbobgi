package kr.ilf.soodalbbobgi.data.health

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import timber.log.Timber

/**
 * Health Connect 설정 홈을 연다 — 사용자는 거기서 '앱 권한 > 수달 뽑기'로 들어가 권한을 조정한다.
 *
 * 앱별 권한 화면 액션(`android.health.connect.action.MANAGE_HEALTH_PERMISSIONS`)은 시스템 서명 권한이
 * 있어야 열 수 있어 일반 앱이 부르면 `SecurityException`으로 죽는다 — 그래서 쓰지 않는다.
 * 액션 값은 androidx가 SDK별로 고른다(14+: HEALTH_HOME_SETTINGS, 13 이하: HC 앱 설정).
 * 어떤 예외든 삼키고 false를 돌려주므로 호출자가 안내만 하면 된다.
 *
 * @param context 액티비티를 띄울 컨텍스트
 * @return 설정 화면이 열렸으면 true
 */
fun openHealthConnectSettings(context: Context): Boolean = try {
    context.startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
    true
} catch (e: Exception) {
    Timber.w(e, "Health Connect 설정 열기 실패")
    false
}
