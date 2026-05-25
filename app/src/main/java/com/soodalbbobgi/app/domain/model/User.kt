package com.soodalbbobgi.app.domain.model

data class User(
    val id: String,
    val nickname: String,
    val shellBalance: Int,
    val pearlBalance: Int,
    val pityCounter: Int,
    val lastShellGrantDate: String?,
    val authProvider: String,
)
