package kr.ilf.soodalbbobgi.core.notify

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 알림 발송 조건 검증 — 두 알림이 서로 모순되거나 이미 등록된 기록에 다시 뜨지 않아야 한다.
 *
 * 리마인더("아직 기록 없어요")와 새 기록 알림("기록이 있어요")은 동시에 참일 수 없다.
 */
class NotificationDecisionTest {

    // ── 리마인더 ────────────────────────────────────────────

    @Test
    fun `오늘 기록이 없으면 리마인더를 보낸다`() {
        assertThat(shouldSendReminder(hasLocalRecordToday = false, hasHealthRecordToday = false)).isTrue()
    }

    @Test
    fun `오늘 로컬에 기록이 있으면 리마인더를 보내지 않는다`() {
        assertThat(shouldSendReminder(hasLocalRecordToday = true, hasHealthRecordToday = false)).isFalse()
    }

    @Test
    fun `동기화 전이라도 헬스커넥트에 오늘 수영이 있으면 리마인더를 보내지 않는다`() {
        // 로컬은 비어 있어도 이미 수영한 날 — "아직 기록이 없어요"는 거짓이 된다
        assertThat(shouldSendReminder(hasLocalRecordToday = false, hasHealthRecordToday = true)).isFalse()
    }

    @Test
    fun `헬스커넥트 확인 불가(null)면 로컬 기준으로 판단한다`() {
        // 백그라운드 읽기 권한이 없거나 조회 실패 — 기존 동작(로컬만 확인)으로 폴백
        assertThat(shouldSendReminder(hasLocalRecordToday = false, hasHealthRecordToday = null)).isTrue()
        assertThat(shouldSendReminder(hasLocalRecordToday = true, hasHealthRecordToday = null)).isFalse()
    }

    // ── 새 기록 알림 ────────────────────────────────────────

    @Test
    fun `변경이 있고 오늘 기록이 아직 없으면 새 기록 알림을 보낸다`() {
        assertThat(
            shouldSendNewRecordNotice(hasChanges = true, hasLocalRecordToday = false, alreadyNotified = false),
        ).isTrue()
    }

    @Test
    fun `오늘 기록이 이미 등록됐으면 새 기록 알림을 보내지 않는다`() {
        // 동기화 후 HC가 세션을 수정해 변경 이벤트가 또 생겨도, 이미 등록·지급된 날은 알리지 않는다
        assertThat(
            shouldSendNewRecordNotice(hasChanges = true, hasLocalRecordToday = true, alreadyNotified = false),
        ).isFalse()
    }

    @Test
    fun `변경이 없으면 새 기록 알림을 보내지 않는다`() {
        assertThat(
            shouldSendNewRecordNotice(hasChanges = false, hasLocalRecordToday = false, alreadyNotified = false),
        ).isFalse()
    }

    @Test
    fun `같은 토큰 상태로 이미 알렸으면 다시 보내지 않는다`() {
        assertThat(
            shouldSendNewRecordNotice(hasChanges = true, hasLocalRecordToday = false, alreadyNotified = true),
        ).isFalse()
    }

    // ── 두 알림의 상호 배타성 ───────────────────────────────

    @Test
    fun `어떤 상황에서도 두 알림이 동시에 발송되지 않는다`() {
        val flags = listOf(true, false)
        for (local in flags) {
            for (hc in flags) {
                for (notified in flags) {
                    val reminder = shouldSendReminder(local, hc)
                    // 새 기록 알림은 HC에 변경이 있을 때만 후보가 된다
                    val newRecord = shouldSendNewRecordNotice(
                        hasChanges = hc, hasLocalRecordToday = local, alreadyNotified = notified,
                    )
                    assertThat(reminder && newRecord).isFalse()
                }
            }
        }
    }
}
