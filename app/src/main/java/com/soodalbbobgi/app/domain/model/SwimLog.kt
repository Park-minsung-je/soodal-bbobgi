package com.soodalbbobgi.app.domain.model

data class SwimLog(
    val id: Long = 0,
    val userId: String,
    val date: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
    val strokeFreeStyle: Int = 0,
    val strokeBreast: Int = 0,
    val strokeBack: Int = 0,
    val strokeFly: Int = 0,
    val source: String,
    val shellsEarned: Int = 0,
)
