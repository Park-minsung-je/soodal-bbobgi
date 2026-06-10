package com.soodalbbobgi.app.presentation.settings

/** 닉네임 검증 실패 사유. */
enum class NicknameError { EMPTY, TOO_LONG, INVALID_CHAR }

private val NICKNAME_PATTERN = Regex("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]+$")

/**
 * 닉네임을 서버 규칙(1~10자, 한글/영문/숫자만)으로 검증한다.
 *
 * @return 통과하면 null, 실패하면 해당 [NicknameError]
 */
fun validateNickname(name: String): NicknameError? = when {
    name.isBlank() -> NicknameError.EMPTY
    name.length > 10 -> NicknameError.TOO_LONG
    !NICKNAME_PATTERN.matches(name) -> NicknameError.INVALID_CHAR
    else -> null
}
