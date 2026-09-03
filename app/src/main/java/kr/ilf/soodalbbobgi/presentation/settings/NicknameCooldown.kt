package kr.ilf.soodalbbobgi.presentation.settings

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 닉네임 변경 규칙 안내 — 다이얼로그에 항상 보이는 문구. 서버는 90일로 계산한다. */
const val NICKNAME_COOLDOWN_HINT = "닉네임은 3개월에 한 번만 바꿀 수 있어요."

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
