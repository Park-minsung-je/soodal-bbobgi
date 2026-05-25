package com.soodalbbobgi.app.domain.model

data class ProfileCard(
    val userId: String,
    val backgroundItemId: Long? = null,
    val characterItemId: Long? = null,
    val borderItemId: Long? = null,
    val characterX: Float = 0.16f,
    val characterY: Float = 0.06f,
    val characterScale: Float = 0.70f,
    val customText: String = "",
    val textStyle: String = "REGULAR",
)
