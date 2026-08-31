package kr.ilf.soodalbbobgi.core.notify

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderTimeTest {

    private val zone = ZoneId.of("Asia/Seoul")

    private fun at(hour: Int, minute: Int): Long =
        ZonedDateTime.of(2026, 6, 12, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `오늘 목표 시각 이전이면 오늘까지의 지연`() {
        // 지금 14:00, 목표 21:00 → 7시간
        val delay = nextReminderDelayMillis(at(14, 0), 21, 0, zone)
        assertThat(delay).isEqualTo(7 * 60 * 60 * 1000L)
    }

    @Test
    fun `목표 시각을 지났으면 다음 날로 넘어간다`() {
        // 지금 22:00, 목표 21:00 → 23시간
        val delay = nextReminderDelayMillis(at(22, 0), 21, 0, zone)
        assertThat(delay).isEqualTo(23 * 60 * 60 * 1000L)
    }

    @Test
    fun `정확히 목표 시각이면 다음 날로 예약한다`() {
        val delay = nextReminderDelayMillis(at(21, 0), 21, 0, zone)
        assertThat(delay).isEqualTo(24 * 60 * 60 * 1000L)
    }

    @Test
    fun `분 단위 목표도 처리한다`() {
        // 지금 20:50, 목표 21:30 → 40분
        val delay = nextReminderDelayMillis(at(20, 50), 21, 30, zone)
        assertThat(delay).isEqualTo(40 * 60 * 1000L)
    }
}
