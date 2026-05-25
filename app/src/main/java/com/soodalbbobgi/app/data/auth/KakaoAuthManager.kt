package com.soodalbbobgi.app.data.auth

import android.content.Context
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class KakaoAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun signIn(): Result<String> = suspendCoroutine { cont ->
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                cont.resume(Result.failure(error))
            } else if (token != null) {
                cont.resume(Result.success(token.accessToken))
            } else {
                cont.resume(Result.failure(IllegalStateException("No token received")))
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context, callback = callback)
        } else {
            UserApiClient.instance.loginWithKakaoAccount(context, callback = callback)
        }
    }
}
