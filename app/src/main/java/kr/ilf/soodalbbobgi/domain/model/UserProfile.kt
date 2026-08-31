package kr.ilf.soodalbbobgi.domain.model

/**
 * 거의 변하지 않는 사용자 식별 + 프로필 정보.
 * 앱 시작 시 1회 로드, 닉네임/성별/연령대 수정 시에만 갱신.
 */
data class UserProfile(
    val id: String,
    val nickname: String?,
    val gender: String?,
    val ageRange: String?,
    val authProvider: String,
)
