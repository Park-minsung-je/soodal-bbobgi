package com.soodalbbobgi.app.core.theme

import androidx.compose.ui.graphics.Color

/**
 * 영법 파스텔 팔레트 — 디자인 확정 고정값 (라이트/다크 테마와 무관).
 */
object StrokePalette {
    val Free = Color(0xFF7DD3FC)
    val Breast = Color(0xFFC4B5FD)
    val Back = Color(0xFF5CD69B)
    val Fly = Color(0xFFFDA4AF)
    val Kick = Color(0xFF94A3B8)
    val Medley = Color(0xFFFCD34D)

    /** 주간 차트 스택·수정 시트 순서: 자유형/평영/배영/접영/킥판/혼영. */
    val ordered = listOf(Free, Breast, Back, Fly, Kick, Medley)
}
