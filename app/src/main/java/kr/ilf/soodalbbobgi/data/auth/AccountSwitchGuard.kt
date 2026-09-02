package kr.ilf.soodalbbobgi.data.auth

import kr.ilf.soodalbbobgi.data.local.LocalDataResetter
import javax.inject.Inject
import javax.inject.Singleton

/** 로그인한 계정이 로컬 데이터의 마지막 소유자와 다르면 로컬을 초기화한다(에셋 제외). */
@Singleton
class AccountSwitchGuard @Inject constructor(
    private val accountPrefs: AccountPrefs,
    private val resetter: LocalDataResetter,
) {
    /**
     * 토큰 저장·상태 로드 **전에** 호출해야 한다 — 초기화가 토큰과 AppState도 지운다.
     *
     * @param userId 서버가 돌려준 사용자 ID (`AuthData.user.id`)
     * @return 로컬을 초기화했으면 true
     */
    suspend fun ensureLocalOwnedBy(userId: String): Boolean {
        val last = accountPrefs.lastLocalUserId
        val switched = last != null && last != userId
        if (switched) resetter.clearAll(keepAssets = true)
        accountPrefs.lastLocalUserId = userId
        return switched
    }
}
