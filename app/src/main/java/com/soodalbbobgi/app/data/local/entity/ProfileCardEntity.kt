package com.soodalbbobgi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile_cards")
data class ProfileCardEntity(
    @PrimaryKey
    val userId: String,
    val backgroundItemId: Long? = null,
    val characterItemId: Long? = null,
    val borderItemId: Long? = null,
    val characterX: Float = 0.16f,
    val characterY: Float = 0.06f,
    val characterScale: Float = 0.70f,
    val customText: String = "",
    val textStyle: String = "REGULAR",
    val lastEditedAt: Long,
)
