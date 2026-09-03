package kr.ilf.soodalbbobgi.data.auth

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * 카카오 OAuth 로그인을 수행한다.
 * 카카오톡 설치 시 카카오톡 로그인, 미설치 시 웹 로그인으로 자동 전환.
 */
@Singleton
class KakaoAuthManager @Inject constructor() {
    /**
     * 카카오 로그인을 실행하고 액세스 토큰을 반환한다.
     * Activity Context가 필요하므로 호출 시 현재 Activity를 전달해야 한다.
     *
     * @param activity 현재 Activity (카카오 SDK가 로그인 화면을 띄우기 위해 필요)
     * @return 카카오 액세스 토큰
     */
    suspend fun signIn(activity: android.app.Activity): Result<String> = suspendCoroutine { cont ->
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                cont.resume(Result.failure(error))
            } else if (token != null) {
                cont.resume(Result.success(token.accessToken))
            } else {
                cont.resume(Result.failure(IllegalStateException("No token received")))
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(activity)) {
            UserApiClient.instance.loginWithKakaoTalk(activity, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(activity, callback = callback)
        }
    }

    /**
     * 카카오 SDK가 기기에 저장한 토큰을 지운다 (탈퇴·로그아웃 후 정리).
     * 서버가 이미 연결을 끊어 토큰이 폐기된 경우 API 호출은 실패하지만 SDK는 저장 토큰을 무조건 삭제한다.
     *
     * @return 카카오 로그아웃 API 결과 — 실패여도 저장 토큰은 지워져 있으므로 호출자는 진행해도 된다
     */
    suspend fun signOutLocally(): Result<Unit> = suspendCoroutine { cont ->
        UserApiClient.instance.logout { error ->
            cont.resume(if (error == null) Result.success(Unit) else Result.failure(error))
        }
    }
}
