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
    val strokeFreeStyle: Int = 0,
    val strokeBreast: Int = 0,
    val strokeBack: Int = 0,
    val strokeFly: Int = 0,
    val source: String,
    val shellsEarned: Int = 0,
    val synced: Boolean = false,
    val createdAt: Long,
)
