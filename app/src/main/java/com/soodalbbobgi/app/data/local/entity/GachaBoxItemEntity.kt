package com.soodalbbobgi.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "gacha_box_items",
    indices = [Index(value = ["boxId"])],
    foreignKeys = [ForeignKey(
        entity = GachaBoxEntity::class,
        parentColumns = ["id"],
        childColumns = ["boxId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class GachaBoxItemEntity(
    @PrimaryKey
    val id: Long,
    val boxId: Long,
    val itemKey: String,
    val name: String,
    val grade: String,
    val weight: Int,
    val imageAsset: String,
    val description: String? = null,
)
