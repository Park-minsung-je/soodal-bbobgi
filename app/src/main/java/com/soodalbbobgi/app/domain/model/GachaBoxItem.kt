package com.soodalbbobgi.app.domain.model

data class GachaBoxItem(
    val id: Long,
    val boxId: Long,
    val itemKey: String,
    val name: String,
    val grade: Grade,
    val weight: Int,
    val imageAsset: String,
)
