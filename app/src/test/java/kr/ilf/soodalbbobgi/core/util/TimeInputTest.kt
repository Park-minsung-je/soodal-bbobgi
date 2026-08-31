package kr.ilf.soodalbbobgi.core.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalTime

/**
 * 수동 입력 시트의 시각 숫자 입력("930", "1430") 해석 규칙 검증.
 */
class TimeInputTest {

    @Test
    fun `4자리는 HHMM으로 해석한다`() {
        assertThat(parseTimeDigits("1430")).isEqualTo(LocalTime.of(14, 30))
        assertThat(parseTimeDigits("0930")).isEqualTo(LocalTime.of(9, 30))
        assertThat(parseTimeDigits("2359")).isEqualTo(LocalTime.of(23, 59))
    }

    @Test
    fun `3자리는 HMM으로 해석한다`() {
        assertThat(parseTimeDigits("930")).isEqualTo(LocalTime.of(9, 30))
        assertThat(parseTimeDigits("105")).isEqualTo(LocalTime.of(1, 5))
    }

    @Test
    fun `1~2자리는 시로 해석하고 분은 0이다`() {
        assertThat(parseTimeDigits("9")).isEqualTo(LocalTime.of(9, 0))
        assertThat(parseTimeDigits("14")).isEqualTo(LocalTime.of(14, 0))
        assertThat(parseTimeDigits("0")).isEqualTo(LocalTime.of(0, 0))
    }

    @Test
    fun `범위를 벗어난 시나 분은 null이다`() {
        assertThat(parseTimeDigits("24")).isNull()
        assertThat(parseTimeDigits("2400")).isNull()
        assertThat(parseTimeDigits("1260")).isNull()
        assertThat(parseTimeDigits("999")).isNull()
    }

    @Test
    fun `빈 문자열과 5자리 이상, 숫자 아닌 입력은 null이다`() {
        assertThat(parseTimeDigits("")).isNull()
        assertThat(parseTimeDigits("12345")).isNull()
        assertThat(parseTimeDigits("9:30")).isNull()
    }

    @Test
    fun `경과 분은 종료가 시작보다 늦을 때만 계산된다`() {
        assertThat(minutesBetween(LocalTime.of(9, 0), LocalTime.of(10, 30))).isEqualTo(90)
        assertThat(minutesBetween(LocalTime.of(10, 0), LocalTime.of(10, 0))).isNull()
        assertThat(minutesBetween(LocalTime.of(11, 0), LocalTime.of(10, 0))).isNull()
    }
}
