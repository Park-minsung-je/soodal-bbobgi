package com.soodalbbobgi.app.domain.model

enum class Grade(val pearlValue: Int) {
    N(pearlValue = 1),
    R(pearlValue = 3),
    SR(pearlValue = 10),
    SSR(pearlValue = 50);

    companion object {
        fun fromString(value: String): Grade = valueOf(value.uppercase())
    }
}
