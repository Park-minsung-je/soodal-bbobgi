package com.soodalbbobgi.app.domain.model

data class SwimLog(
    val id: Long = 0,
    val userId: String,
    val date: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
    val strokeFreestyleM: Int = 0,
    val strokeBreastM: Int = 0,
    val strokeBackM: Int = 0,
    val strokeFlyM: Int = 0,
    val strokeMixedM: Int = 0,
    val strokeKickM: Int = 0,
    val source: String,
    val shellsEarned: Int = 0,
    val hcRecordId: String? = null,
    /** 세션 중 최대/최소 심박(bpm). 심박 기록이 없으면 null. */
    val maxHr: Int? = null,
    val minHr: Int? = null,
    /** 실제 운동 시간(초) — HC 세그먼트/랩 기반. 없으면 null (경과 시간으로 폴백). */
    val activeSeconds: Int? = null,
)
