package com.soodalbbobgi.app.core.notify

/**
 * 리마인더("오늘 아직 수영 기록이 없어요")를 보낼지 판단한다.
 *
 * 로컬 기록만 보면, 수영은 했지만 아직 앱을 열지 않아 동기화가 안 된 날에도
 * "기록이 없다"고 알리게 된다 — 같은 시점에 새 기록 알림이 함께 뜨면 서로 모순된다.
 * 그래서 Health Connect에 오늘 수영이 있는지도 함께 본다.
 *
 * @param hasLocalRecordToday 오늘 날짜의 로컬 swim_log 존재 여부
 * @param hasHealthRecordToday Health Connect의 오늘 수영 세션 존재 여부.
 *   권한 없음/조회 실패로 알 수 없으면 null (이때는 로컬 기준으로 판단)
 * @return 리마인더를 보내야 하면 true
 */
fun shouldSendReminder(hasLocalRecordToday: Boolean, hasHealthRecordToday: Boolean?): Boolean =
    !hasLocalRecordToday && hasHealthRecordToday != true

/**
 * 새 기록 알림("기록이 있어요, 열어서 조개 받으세요")을 보낼지 판단한다.
 *
 * 오늘 기록이 이미 로컬에 등록됐다면 조개 지급까지 끝난 상태다. 이후 Health Connect가
 * 같은 세션을 수정(심박·거리 보정 등)해 변경 이벤트가 다시 발생해도 알리지 않는다.
 *
 * @param hasChanges Health Connect 변경 토큰 기준 새 수영 세션 변경 존재 여부
 * @param hasLocalRecordToday 오늘 날짜의 로컬 swim_log 존재 여부
 * @param alreadyNotified 현재 변경 토큰 상태로 이미 알림을 보냈는지
 * @return 새 기록 알림을 보내야 하면 true
 */
fun shouldSendNewRecordNotice(
    hasChanges: Boolean,
    hasLocalRecordToday: Boolean,
    alreadyNotified: Boolean,
): Boolean = hasChanges && !hasLocalRecordToday && !alreadyNotified
