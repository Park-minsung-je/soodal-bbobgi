package com.soodalbbobgi.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.soodalbbobgi.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Credential Manager 래퍼.
 *
 * `setServerClientId`에 Web Client ID를 넘기면 발급되는 idToken의 `aud` 클레임이
 * 그 값으로 박힌다. 서버는 audience 비교로 토큰이 우리 백엔드 대상임을 검증한다.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val credentialManager = CredentialManager.create(context)

    /**
     * Google 계정 선택 UI를 띄우고 idToken을 받는다.
     *
     * @param activityContext 현재 Activity Context (시스템 UI 호출 필요)
     * @return 성공 시 idToken 문자열, 실패 시 예외
     */
    suspend fun signIn(activityContext: Context): Result<String> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
            Result.success(googleIdToken.idToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
