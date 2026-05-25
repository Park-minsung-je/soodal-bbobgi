package com.soodalbbobgi.app.domain.model

data class InventoryItem(
    val id: Long = 0,
    val userId: String,
    val boxItemId: Long,
    val grade: Grade,
    val category: String,
    val isEquippedAs: String = "NONE",
    val acquiredAt: Long,
)
