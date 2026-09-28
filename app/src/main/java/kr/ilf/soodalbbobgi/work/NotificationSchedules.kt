package kr.ilf.soodalbbobgi.work

import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 알림 예약 복구의 단일 진입점 — 로그인 직후·앱 프로세스 시작·재부팅·업데이트 뒤에 부른다.
 *
 * 로그아웃은 예약을 걷지만 알림 설정은 남기므로(`LocalDataResetter.clearSession`), 되살릴 때는
 * **로그인 상태인지**를 먼저 본다. 아니면 로그아웃 상태에서 앱이 재시작될 때 알림이 다시 예약된다.
 */
@Singleton
class NotificationSchedules @Inject constructor(
    private val tokenStore: TokenStore,
    private val prefs: NotificationPrefs,
    private val reminderScheduler: ReminderScheduler,
    private val hcChangeCheckScheduler: HcChangeCheckScheduler,
) {

    /** 로그인된 상태에서 설정에 따라 리마인더 알람·새 기록 워커를 예약한다. 로그아웃 상태면 아무것도 하지 않는다. */
    fun restoreIfLoggedIn() {
        if (tokenStore.getAccessToken() == null) return
        reminderScheduler.rescheduleIfEnabled()
        if (prefs.newRecordEnabled) hcChangeCheckScheduler.schedule()
    }
}
