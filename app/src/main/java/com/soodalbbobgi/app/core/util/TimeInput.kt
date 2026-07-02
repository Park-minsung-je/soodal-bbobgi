package com.soodalbbobgi.app.core.util

import java.time.Duration
import java.time.LocalTime

/**
 * 시각 숫자 입력("930", "1430" 등)을 [LocalTime]으로 해석한다.
 * 1~2자리는 시(분=0), 3~4자리는 앞자리가 시·뒤 2자리가 분.
 *
 * @return 유효하지 않으면 null (빈 문자열, 숫자 아님, 시/분 범위 밖)
 */
fun parseTimeDigits(text: String): LocalTime? {
    if (text.isEmpty() || text.length > 4 || !text.all { it.isDigit() }) return null
    val n = text.toInt()
    val hour: Int
    val minute: Int
    if (text.length <= 2) {
        hour = n
        minute = 0
    } else {
        hour = n / 100
        minute = n % 100
    }
    return if (hour in 0..23 && minute in 0..59) LocalTime.of(hour, minute) else null
}

/**
 * 시작~종료 시각의 경과 분을 구한다. 같은 날 안에서만 유효하다.
 *
 * @return 종료가 시작보다 늦지 않으면 null
 */
fun minutesBetween(start: LocalTime, end: LocalTime): Int? {
    val diff = Duration.between(start, end).toMinutes().toInt()
    return if (diff > 0) diff else null
}
