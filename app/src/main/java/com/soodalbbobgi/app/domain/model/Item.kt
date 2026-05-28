package com.soodalbbobgi.app.domain.model

/**
 * 아이템 마스터 카탈로그 (서버 items 테이블).
 * id가 inventory.itemId와 일치한다.
 */
data class Item(
    val id: Long,
    val itemKey: String,
    val name: String,
    val grade: Grade,
    val category: String,
    val imageAsset: String?,
    val isLimited: Boolean = false,
    val isDefault: Boolean = false,
)
