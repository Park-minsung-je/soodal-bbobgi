package kr.ilf.soodalbbobgi.core.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent

/**
 * 이 컨텍스트가 속한 Activity. Compose의 LocalContext는 래퍼일 수 있어 baseContext를 따라 올라간다.
 *
 * @return 감싸고 있는 Activity, 없으면 null
 */
fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * 앱을 스플래시부터 다시 시작한다 — 런처 액티비티를 새 태스크(NEW_TASK|CLEAR_TASK)로 띄우고 현재 액티비티를 끝낸다.
 * 이전 Activity의 ViewModelStore(내비 항목별 ViewModel 포함)와 저장 상태가 전부 폐기된다.
 * Hilt 싱글톤은 프로세스와 함께 남으므로 호출 전에 LocalDataResetter로 비워 둬야 한다.
 */
fun Context.restartApp() {
    val component = packageManager.getLaunchIntentForPackage(packageName)?.component ?: return
    startActivity(Intent.makeRestartActivityTask(component))
    findActivity()?.finish()
}
