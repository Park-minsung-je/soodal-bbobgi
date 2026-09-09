package kr.ilf.soodalbbobgi.presentation.settings

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 닉네임 변경 규칙 안내 — 다이얼로그에 항상 보이는 문구. 서버는 90일로 계산한다. */
const val NICKNAME_COOLDOWN_HINT = "닉네임은 3개월에 한 번만 바꿀 수 있어요."

/** 닉네임 변경 확인 단계의 경고 — 강조된 닉네임 아래에 한 줄로 보여 준다. */
const val NICKNAME_CONFIRM_WARNING = "바꾼 뒤 3개월 동안은 다시 바꿀 수 없어요."

/**
 * 지금 닉네임을 바꿀 수 없는 상태인지.
 *
 * @param changeableAt 서버가 준 다음 변경 가능 시각(epoch ms). null이면 바로 가능
 * @param nowMillis 현재 시각(epoch ms)
 * @return 변경 가능 시각이 아직 오지 않았으면 true
 */
fun isNicknameCooldownActive(changeableAt: Long?, nowMillis: Long): Boolean =
    changeableAt != null && changeableAt > nowMillis

private val CHANGEABLE_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")

/**
 * 쿨다운 안내 문구 — "2026년 12월 1일부터 바꿀 수 있어요."
 *
 * @param changeableAt 다음 변경 가능 시각(epoch ms)
 * @param zone 표시 시간대 (기본 기기 시간대)
 * @return 날짜가 든 안내 문구
 */
fun nicknameCooldownMessage(changeableAt: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val date = Instant.ofEpochMilli(changeableAt).atZone(zone).toLocalDate()
    return "${date.format(CHANGEABLE_DATE_FORMAT)}부터 바꿀 수 있어요."
}

/**
 * 닉네임 변경 확인 문구 — 오타를 저장하기 전에 한 번 더 보여 준다.
 * 한글 끝글자의 받침에 따라 로/으로를 고르고(ㄹ 받침은 로), 한글이 아니면 "(으)로".
 *
 * @param name 바꾸려는 닉네임
 * @return 두 줄 문구 — 첫 줄 확인 질문, 둘째 줄 3개월 제한 경고
 */
fun nicknameConfirmMessage(name: String): String {
    val last = name.lastOrNull()
    val particle = when {
        last == null || last !in '가'..'힣' -> "(으)로"
        else -> {
            val jong = (last - '가') % 28 // 0 = 받침 없음, 8 = ㄹ
            if (jong == 0 || jong == 8) "로" else "으로"
        }
    }
    return "'$name'$particle 바꿀까요?\n바꾼 뒤 3개월 동안은 다시 바꿀 수 없어요."
}

