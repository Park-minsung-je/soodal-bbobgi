package com.soodalbbobgi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "swim_logs",
    indices = [Index(value = ["date"], unique = true)]
)
data class SwimLogEntity(
    @PrimaryKey(autoGenerate = true)
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
    val synced: Boolean = false,
    val createdAt: Long,
)
