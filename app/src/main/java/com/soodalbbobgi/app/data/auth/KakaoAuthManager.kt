package com.soodalbbobgi.app.data.auth

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
}
