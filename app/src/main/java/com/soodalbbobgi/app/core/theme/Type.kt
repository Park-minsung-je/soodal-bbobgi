package com.soodalbbobgi.app.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.R

val PretendardFontFamily = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_bold, FontWeight.Bold),
    Font(R.font.pretendard_extrabold, FontWeight.ExtraBold),
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

// Spec Sheet v3.0 타입 스케일(2026.07): xl 22 / lg 18 / md 16 / body 15 / cap 12 / mini 11.
object SoodalTypography {
    val xl = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PretendardFontFamily, letterSpacing = (-0.01).sp, lineHeight = 25.3.sp)
    val lg = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PretendardFontFamily, letterSpacing = (-0.01).sp, lineHeight = 21.6.sp)
    val md = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PretendardFontFamily, lineHeight = 20.8.sp)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, fontFamily = PretendardFontFamily, lineHeight = 26.25.sp)
    val cap = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = PretendardFontFamily, lineHeight = 16.8.sp)
    val mini = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PretendardFontFamily, letterSpacing = 0.55.sp, lineHeight = 14.3.sp)
    val mono = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily, letterSpacing = 0.48.sp)
}
