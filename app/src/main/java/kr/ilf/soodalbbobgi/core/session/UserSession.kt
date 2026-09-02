package kr.ilf.soodalbbobgi.core.session

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 로그인된 사용자의 ID를 제공한다.
 * Debug 빌드에서는 고정 mock ID, Release에서는 OAuth 인증 후 설정.
 */
@Singleton
class UserSession @Inject constructor() {
    var userId: String = DEFAULT_USER_ID
        private set

    /**
     * OAuth 인증 성공 후 실제 사용자 ID로 교체한다.
     *
     * @param id 서버에서 발급받은 사용자 ID
     */
    fun setAuthenticatedUser(id: String) {
        userId = id
    }

    /** 로그아웃/탈퇴 시 사용자 ID를 초기값으로 되돌린다. */
    fun clear() {
        userId = DEFAULT_USER_ID
    }

    companion object {
        /** 로그인 전·로그아웃 후의 자리표시 ID. */
        private const val DEFAULT_USER_ID = "debug_user"
    }
}
