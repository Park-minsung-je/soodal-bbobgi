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
)
