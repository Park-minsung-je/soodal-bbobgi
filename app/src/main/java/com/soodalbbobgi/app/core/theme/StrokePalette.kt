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
    val Medley = Color(0xFFF6C95B)

    /** 주간 차트 스택·수정 시트 순서: 자유형/평영/배영/접영/킥판/혼영. */
    val ordered = listOf(Free, Breast, Back, Fly, Kick, Medley)

    // 텍스트용 잉크 변형 — 디자인 v3 가독성 개정(--st-*-ink): 파스텔 스와치와 쌍을 이루는
    // 진한 잉크색. 글래스 위 %·거리·영법명 텍스트는 반드시 이 변형을 쓴다.
    val FreeText = Color(0xFF2477AE)
    val BreastText = Color(0xFF6E55C9)
    val BackText = Color(0xFF1E9E78)
    val FlyText = Color(0xFFD9614A)
    val KickText = Color(0xFF566579)
    val MedleyText = Color(0xFFC8910F)
}
