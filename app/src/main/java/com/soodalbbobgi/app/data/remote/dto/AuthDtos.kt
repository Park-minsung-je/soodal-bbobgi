package com.soodalbbobgi.app.data.remote.dto

data class OAuthRequest(
    val token: String,
    val provider: String,
)

data class AuthResponse(
    val userId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val nickname: String?,
    val isNewUser: Boolean,
)

data class RefreshRequest(
    val refreshToken: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
