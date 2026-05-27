package com.soodalbbobgi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val nickname: String,
    val shellBalance: Int = 0,
    val pearlBalance: Int = 0,
    val pityCounter: Int = 0,
    val lastShellGrantDate: String? = null,
    val gender: String? = null,
    val ageRange: String? = null,
    val authProvider: String,
    val createdAt: Long,
    val updatedAt: Long,
    val synced: Boolean = false,
)
