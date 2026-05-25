package com.soodalbbobgi.app.domain.model

data class GachaHistory(
    val id: Long = 0,
    val userId: String,
    val timestamp: Long,
    val boxId: Long,
    val itemId: Long,
    val grade: Grade,
    val wasNew: Boolean,
    val pearlsReceived: Int,
    val shellsSpent: Int,
    val pityCountAtPull: Int,
)
