package kr.ilf.soodalbbobgi.data.remote.dto

/**
 * POST /auth/kakao 요청 바디.
 * 카카오 SDK에서 발급받은 액세스 토큰을 서버에 전달한다.
 */
data class KakaoAuthRequest(
    val accessToken: String,
)

/**
 * POST /auth/google 요청 바디.
 * Google Identity Library에서 발급받은 ID 토큰을 서버에 전달한다.
 */
data class GoogleAuthRequest(
    val idToken: String,
)

/**
 * POST /auth/refresh 요청 바디.
 */
data class RefreshRequest(
    val refreshToken: String,
)

/**
 * 서버 공통 응답 래퍼.
 *
 * @param T data 필드의 타입
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ApiError?,
)

/**
 * 서버 에러 정보.
 */
data class ApiError(
    val code: String,
    val message: String,
)

/**
 * 로그인/회원가입 성공 시 반환되는 인증 데이터.
 */
data class AuthData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val isNewUser: Boolean,
    val user: UserData,
)

/**
 * 서버에서 내려주는 사용자 기본 정보.
 */
data class UserData(
    val id: String,
    val nickname: String?,
    val shellBalance: Int,
    val pearlBalance: Int,
    val pityCounter: Int,
    val lastShellGrantDate: String? = null,
    val gender: String? = null,
    val ageRange: String? = null,
    val authProvider: String,
    val createdAt: Long,
)

/**
 * POST /auth/refresh 성공 시 반환되는 갱신된 토큰 데이터.
 */
data class TokenData(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
)
