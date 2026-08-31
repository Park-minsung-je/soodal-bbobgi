package kr.ilf.soodalbbobgi.core.notify

import java.time.Instant
import java.time.ZoneId

/**
 * 다음 리마인더 발화 시각까지의 지연(ms)을 계산한다.
 * 오늘의 목표 시각이 아직 안 지났으면 오늘, 지났거나 정확히 그 시각이면 다음 날로 예약한다.
 *
 * @param nowMillis 현재 시각 (epoch ms)
 * @param hour 목표 시 (0~23)
 * @param minute 목표 분 (0~59)
 * @param zone 사용자 시간대
 */
fun nextReminderDelayMillis(nowMillis: Long, hour: Int, minute: Int, zone: ZoneId): Long {
    val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
    var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
    if (!target.isAfter(now)) target = target.plusDays(1)
    return target.toInstant().toEpochMilli() - nowMillis
}
