package com.soodalbbobgi.app.domain.model

data class GachaResult(
    val item: GachaBoxItem,
    val wasNew: Boolean,
    val pearlsEarned: Int,
    val shellsSpent: Int,
)
