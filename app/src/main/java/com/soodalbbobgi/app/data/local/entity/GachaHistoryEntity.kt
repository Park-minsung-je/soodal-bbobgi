package com.soodalbbobgi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gacha_history",
    indices = [Index(value = ["timestamp"])]
)
data class GachaHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val timestamp: Long,
    val boxId: Long,
    val itemId: Long,
    val grade: String,
    val wasNew: Boolean,
    val pearlsReceived: Int = 0,
    val shellsSpent: Int = 1,
    val pityCountAtPull: Int,
    val synced: Boolean = false,
)
