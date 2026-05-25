package com.soodalbbobgi.app.data.remote.api

import com.soodalbbobgi.app.data.remote.dto.AuthResponse
import com.soodalbbobgi.app.data.remote.dto.OAuthRequest
import com.soodalbbobgi.app.data.remote.dto.RefreshRequest
import com.soodalbbobgi.app.data.remote.dto.TokenResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SoodalApi {
    @POST("auth/oauth/google")
    suspend fun authGoogle(@Body request: OAuthRequest): Response<AuthResponse>

    @POST("auth/oauth/kakao")
    suspend fun authKakao(@Body request: OAuthRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshRequest): Response<TokenResponse>
}
