package kr.ilf.soodalbbobgi.data.auth

import kr.ilf.soodalbbobgi.core.di.BaseUrl
import kr.ilf.soodalbbobgi.data.remote.dto.RefreshRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 모든 API 요청에 Bearer 토큰을 자동 첨부하고, 401 응답 시 토큰 갱신을 시도한다.
 *
 * 토큰 삭제는 서버가 리프레시를 **명시적으로 거부**했을 때만 한다.
 * 네트워크 장애/서버 다운(5xx)으로 갱신에 실패한 경우엔 토큰을 보존한다 —
 * 일시 장애로 멀쩡한 세션이 로그아웃되는 것을 막기 위함.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStore: TokenStore,
    @BaseUrl private val baseUrl: String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.getAccessToken()
        val originalRequest = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(originalRequest)

        // 401 수신 시 리프레시 토큰으로 재발급 시도
        if (response.code == 401) {
            val refreshToken = tokenStore.getRefreshToken() ?: return response

            return when (val outcome = tryRefresh(refreshToken)) {
                is RefreshOutcome.Success -> {
                    response.close()
                    val retryRequest = chain.request().newBuilder()
                        .header("Authorization", "Bearer ${outcome.accessToken}")
                        .build()
                    chain.proceed(retryRequest)
                }
                RefreshOutcome.Rejected -> {
                    // 서버가 리프레시 토큰을 거부 — 진짜 세션 만료이므로 삭제
                    tokenStore.clearTokens()
                    response
                }
                RefreshOutcome.Unreachable -> {
                    // 일시 장애 — 토큰을 보존하고 원본 401을 그대로 반환 (다음 기회에 재시도)
                    response
                }
            }
        }

        return response
    }

    /** 토큰 갱신 시도 결과. */
    private sealed interface RefreshOutcome {
        /** 갱신 성공 — 새 액세스 토큰. */
        data class Success(val accessToken: String) : RefreshOutcome

        /** 서버가 명시적으로 거부 (4xx / 응답 envelope 실패) — 토큰 삭제 대상. */
        data object Rejected : RefreshOutcome

        /** 서버에 닿지 못함 (네트워크 오류/5xx) — 토큰 보존. */
        data object Unreachable : RefreshOutcome
    }

    /**
     * 리프레시 토큰으로 액세스 토큰 갱신을 시도한다.
     * 순환 참조 방지를 위해 AuthInterceptor 없는 별도 Retrofit 인스턴스를 생성한다.
     *
     * @param refreshToken 저장된 리프레시 토큰
     * @return 갱신 결과 — 성공/거부/장애를 구분해 호출자가 토큰 삭제 여부를 결정한다
     */
    private fun tryRefresh(refreshToken: String): RefreshOutcome {
        return try {
            runBlocking {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(kr.ilf.soodalbbobgi.data.remote.api.SoodalApi::class.java)
                val response = api.refreshToken(RefreshRequest(refreshToken))

                if (response.success && response.data != null) {
                    val data = response.data
                    tokenStore.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)
                    RefreshOutcome.Success(data.accessToken)
                } else {
                    Timber.w("토큰 갱신 거부: ${response.error?.code}")
                    RefreshOutcome.Rejected
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "토큰 갱신 중 예외 발생")
            when (classifyServerFailure(e)) {
                ServerFailure.REJECTED -> RefreshOutcome.Rejected
                ServerFailure.UNREACHABLE -> RefreshOutcome.Unreachable
            }
        }
    }
}
