package com.soodalbbobgi.app.domain.model

/**
 * 자주 변동되는 사용자 재화/뽑기 상태.
 * 가챠/구매/조개 지급 후 서버 응답값으로 통째로 교체된다.
 */
data class Currency(
    val shellBalance: Int = 0,
    val pearlBalance: Int = 0,
    val pityCounter: Int = 0,
    val lastShellGrantDate: String? = null,
)
