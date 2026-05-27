package com.soodalbbobgi.app.data.auth

import com.soodalbbobgi.app.core.di.BaseUrl
import com.soodalbbobgi.app.data.remote.dto.RefreshRequest
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
 * 갱신 실패 시 토큰을 초기화하여 로그아웃 상태로 만든다.
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

            val newToken = tryRefresh(refreshToken)
            return if (newToken != null) {
                response.close()
                val retryRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                chain.proceed(retryRequest)
            } else {
                // 갱신 실패 — 토큰 삭제 후 원본 401 응답 반환
                tokenStore.clearTokens()
                response
            }
        }

        return response
    }

    /**
     * 리프레시 토큰으로 액세스 토큰 갱신을 시도한다.
     * 순환 참조 방지를 위해 AuthInterceptor 없는 별도 Retrofit 인스턴스를 생성한다.
     * 성공하면 새 액세스 토큰을 TokenStore에 저장하고 반환, 실패하면 null 반환.
     *
     * @param refreshToken 저장된 리프레시 토큰
     * @return 새 액세스 토큰 또는 null
     */
    private fun tryRefresh(refreshToken: String): String? {
        return try {
            runBlocking {
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val api = retrofit.create(com.soodalbbobgi.app.data.remote.api.SoodalApi::class.java)
                val response = api.refreshToken(RefreshRequest(refreshToken))

                if (response.success && response.data != null) {
                    val data = response.data
                    tokenStore.saveTokens(data.accessToken, data.refreshToken, data.expiresIn)
                    data.accessToken
                } else {
                    Timber.w("토큰 갱신 실패: ${response.error?.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "토큰 갱신 중 예외 발생")
            null
        }
    }
}
