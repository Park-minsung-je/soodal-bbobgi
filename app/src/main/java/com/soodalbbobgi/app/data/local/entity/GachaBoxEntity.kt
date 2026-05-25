package com.soodalbbobgi.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gacha_boxes")
data class GachaBoxEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val description: String,
    val category: String,
    val boxType: String = "FIXED",
    val isActive: Boolean = true,
    val iconAsset: String? = null,
    val updatedAt: Long,
)
