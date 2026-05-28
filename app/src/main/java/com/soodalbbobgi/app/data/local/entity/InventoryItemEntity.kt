package com.soodalbbobgi.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    indices = [Index(value = ["itemId"])]
)
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val itemId: Long,
    val grade: String,
    val category: String,
    val isEquippedAs: String = "NONE",
    val acquiredAt: Long,
    val synced: Boolean = false,
)
