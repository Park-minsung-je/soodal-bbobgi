package kr.ilf.soodalbbobgi.data.auth

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 데이터(Room·prefs)를 마지막으로 사용한 계정 ID를 영속한다.
 *
 * [TokenStore]와 분리한 이유: 토큰은 세션 만료(401 거부)로도 지워지는데, 그때마다
 * "로컬 소유자"를 잊으면 같은 계정 재로그인과 다른 계정 로그인을 구분할 수 없다.
 * 비밀값이 아니므로 일반 SharedPreferences를 쓴다.
 */
@Singleton
class AccountPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("account", Context.MODE_PRIVATE)

    /** 로컬 데이터를 마지막으로 쓴 서버 사용자 ID. 첫 로그인 전이거나 탈퇴 후면 null. */
    var lastLocalUserId: String?
        get() = prefs.getString(KEY_LAST_LOCAL_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_LAST_LOCAL_USER_ID, value).apply()

    /** 소유자 기록을 지운다 — 탈퇴·계정 전환 초기화용. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_LAST_LOCAL_USER_ID = "last_local_user_id"
    }
}
