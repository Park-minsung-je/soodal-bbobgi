package com.soodalbbobgi.app.data.remote.api

import com.soodalbbobgi.app.data.remote.dto.ApiResponse
import com.soodalbbobgi.app.data.remote.dto.AuthData
import com.soodalbbobgi.app.data.remote.dto.GoogleAuthRequest
import com.soodalbbobgi.app.data.remote.dto.KakaoAuthRequest
import com.soodalbbobgi.app.data.remote.dto.RefreshRequest
import com.soodalbbobgi.app.data.remote.dto.TokenData
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 수달 뽑기 서버 REST API 인터페이스.
 * Base URL: https://soodal.bbobgi.ilf.kr/v1/
 */
interface SoodalApi {

    /** 카카오 액세스 토큰으로 로그인/회원가입 */
    @POST("auth/kakao")
    suspend fun authKakao(@Body request: KakaoAuthRequest): ApiResponse<AuthData>

    /** Google ID 토큰으로 로그인/회원가입 */
    @POST("auth/google")
    suspend fun authGoogle(@Body request: GoogleAuthRequest): ApiResponse<AuthData>

    /** 만료된 액세스 토큰을 리프레시 토큰으로 갱신 */
    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): ApiResponse<TokenData>
}
