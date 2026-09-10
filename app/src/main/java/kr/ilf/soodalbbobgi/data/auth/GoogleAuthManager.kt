package kr.ilf.soodalbbobgi.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kr.ilf.soodalbbobgi.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Credential Manager 래퍼.
 *
 * 생성자에 Web Client ID를 넘기면 발급되는 idToken의 `aud` 클레임이
 * 그 값으로 박힌다. 서버는 audience 비교로 토큰이 우리 백엔드 대상임을 검증한다.
 *
 * 옵션 선택 — [GetSignInWithGoogleOption]을 사용한다.
 * Android 14+에서 `GetGoogleIdOption` + 다중 Google 계정 환경 + GMS 24.40 미만 조합에서는
 * `CredentialSelectorActivity` 시작 시 `TransactionTooLargeException`이 터져 로그인 시트가
 * 뜨지 않는 알려진 버그가 있다. `GetSignInWithGoogleOption`은 이 버그의 영향을 받지 않는다.
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
            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val googleIdToken = GoogleIdTokenCredential.createFrom(result.credential.data)
            Result.success(googleIdToken.idToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 자격증명 세션을 지워 다음 로그인 때 계정 선택 UI가 다시 뜨게 한다 (탈퇴·로그아웃 후 정리).
     * 구글 쪽 동의 철회(revoke)는 아니다 — ID 토큰만 쓰는 구조라 폐기할 토큰이 없다.
     *
     * @return 초기화 결과 — 실패해도 호출자는 진행해도 된다
     */
    suspend fun clearCredentialState(): Result<Unit> = runCatching {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
    }
}
