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

    // 텍스트용 고채도 변형 — 파스텔 막대색은 글자로 쓰면 연해서 한 단계 진한 색을 쓴다.
    // (그래프/색칩은 파스텔 유지, %·영법명 같은 텍스트만 이 변형 사용)
    val FreeText = Color(0xFF38BDF8)
    val BreastText = Color(0xFFA78BFA)
    val BackText = Color(0xFF34BD7C)
    val FlyText = Color(0xFFFB7185)
    val KickText = Color(0xFF7C8DA3)
    val MedleyText = Color(0xFFEAB308)
}
