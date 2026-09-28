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

/**
 * 앱을 완전히 닫는다 — 태스크를 지우고 프로세스를 끝낸다. 다음 실행은 아이콘에서 스플래시부터.
 *
 * 탈퇴용. 탈퇴 때 회수한 Health Connect 권한은 시스템이 **앱이 포그라운드를 떠난 뒤** 프로세스를 죽이며 적용한다
 * (`revokeSelfPermissionsOnKill`: 포그라운드에 있는 동안은 실행되지 않는다). 재시작하면 새 태스크가 곧바로
 * 포그라운드에 올라 회수가 계속 미뤄지고, 새 계정 온보딩이 권한을 아직 가진 것으로 본다.
 */
fun Context.closeApp() {
    findActivity()?.finishAndRemoveTask()
    Runtime.getRuntime().exit(0)
}
