package com.soodalbbobgi.app.domain.model

enum class Grade(val pearlValue: Int) {
    N(pearlValue = 1),
    R(pearlValue = 2),
    SR(pearlValue = 4),
    SSR(pearlValue = 6);

    companion object {
        fun fromString(value: String): Grade = valueOf(value.uppercase())
    }
}
