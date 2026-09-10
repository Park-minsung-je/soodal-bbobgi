package kr.ilf.soodalbbobgi.presentation.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NicknameValidationTest {

    @Test
    fun `정상 닉네임은 null 반환`() {
        assertThat(validateNickname("수달이")).isNull()
        assertThat(validateNickname("Soodal")).isNull()
        assertThat(validateNickname("수달123")).isNull()
        assertThat(validateNickname("ㅋㅋ")).isNull()
        assertThat(validateNickname("a")).isNull()
        assertThat(validateNickname("열글자닉네임입니다요")).isNull() // 정확히 10자
    }

    @Test
    fun `빈 문자열과 공백만은 EMPTY`() {
        assertThat(validateNickname("")).isEqualTo(NicknameError.EMPTY)
        assertThat(validateNickname("   ")).isEqualTo(NicknameError.EMPTY)
    }

    @Test
    fun `10자 초과는 TOO_LONG`() {
        assertThat(validateNickname("열글자가넘는닉네임이다")).isEqualTo(NicknameError.TOO_LONG) // 11자
    }

    @Test
    fun `특수문자와 공백 포함은 INVALID_CHAR`() {
        assertThat(validateNickname("수달!")).isEqualTo(NicknameError.INVALID_CHAR)
        assertThat(validateNickname("수 달")).isEqualTo(NicknameError.INVALID_CHAR)
        assertThat(validateNickname("otter_1")).isEqualTo(NicknameError.INVALID_CHAR)
        assertThat(validateNickname("수달🦦")).isEqualTo(NicknameError.INVALID_CHAR)
    }
}
