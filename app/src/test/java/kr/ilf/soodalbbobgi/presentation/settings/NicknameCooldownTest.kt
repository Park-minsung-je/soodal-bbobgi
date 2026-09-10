package kr.ilf.soodalbbobgi.presentation.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** 닉네임 쿨다운 판정과 안내 문구 — 서버가 준 epoch ms를 기기 시각·시간대로 해석한다. */
class NicknameCooldownTest {

    @Test
    fun `changeableAt이 null이면 쿨다운 아님`() {
        assertThat(isNicknameCooldownActive(null, 1_000L)).isFalse()
    }

    @Test
    fun `미래면 쿨다운`() {
        assertThat(isNicknameCooldownActive(2_000L, 1_000L)).isTrue()
    }

    @Test
    fun `같거나 지났으면 쿨다운 아님`() {
        assertThat(isNicknameCooldownActive(1_000L, 1_000L)).isFalse()
        assertThat(isNicknameCooldownActive(999L, 1_000L)).isFalse()
    }

    @Test
    fun `안내 문구는 표시 시간대의 날짜로 적는다`() {
        val seoul = ZoneId.of("Asia/Seoul")
        val at = ZonedDateTime.of(2026, 12, 1, 0, 30, 0, 0, seoul).toInstant().toEpochMilli()
        assertThat(nicknameCooldownMessage(at, seoul)).isEqualTo("2026년 12월 1일부터 바꿀 수 있어요.")
        assertThat(nicknameCooldownMessage(at, ZoneId.of("UTC"))).isEqualTo("2026년 11월 30일부터 바꿀 수 있어요.")
    }
}
