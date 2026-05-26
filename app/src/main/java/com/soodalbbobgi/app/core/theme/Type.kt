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

object SoodalTypography {
    val xl = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PretendardFontFamily, letterSpacing = (-0.01).sp, lineHeight = 33.6.sp)
    val lg = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, fontFamily = PretendardFontFamily, letterSpacing = (-0.01).sp, lineHeight = 27.5.sp)
    val md = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = PretendardFontFamily, lineHeight = 23.4.sp)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, fontFamily = PretendardFontFamily, lineHeight = 23.25.sp)
    val cap = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, fontFamily = PretendardFontFamily, lineHeight = 16.8.sp)
    val mini = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = PretendardFontFamily, letterSpacing = 0.6.sp)
    val mono = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily, letterSpacing = 0.48.sp)
}
