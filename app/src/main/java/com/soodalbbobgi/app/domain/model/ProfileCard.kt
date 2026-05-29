package com.soodalbbobgi.app.domain.model

data class ProfileCard(
    val userId: String,
    val backgroundItemId: Long? = null,
    val characterItemId: Long? = null,
    val borderItemId: Long? = null,
    val characterX: Float = 0.5f,
    val characterY: Float = 0.5f,
    val characterScale: Float = 1.0f,
    val customText: String = "",
    val textStyle: String = "REGULAR",
)
