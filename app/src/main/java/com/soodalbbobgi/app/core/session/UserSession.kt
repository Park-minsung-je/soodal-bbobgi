package com.soodalbbobgi.app.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 현재 로그인된 사용자의 ID + 세션 단위로 전달할 일회성 상태를 제공한다.
 * Debug 빌드에서는 고정 mock ID, Release에서는 OAuth 인증 후 설정.
 */
@Singleton
class UserSession @Inject constructor() {
    var userId: String = "debug_user"
        private set

    private val _pendingShellReward = MutableStateFlow(0)
    /** 스플래시 동기화 중 지급된 조개 수. Home이 읽어 팝업 표시 후 [consumePendingShellReward]로 초기화. */
    val pendingShellReward: StateFlow<Int> = _pendingShellReward

    /**
     * OAuth 인증 성공 후 실제 사용자 ID로 교체한다.
     *
     * @param id 서버에서 발급받은 사용자 ID
     */
    fun setAuthenticatedUser(id: String) {
        userId = id
    }

    /** 스플래시에서 지급된 조개 수를 설정한다 (Home 팝업 트리거용). */
    fun setPendingShellReward(amount: Int) {
        _pendingShellReward.value = amount
    }

    /** Home에서 팝업 표시 후 호출. 누적 보상을 0으로 리셋. */
    fun consumePendingShellReward(): Int {
        val v = _pendingShellReward.value
        _pendingShellReward.value = 0
        return v
    }
}
