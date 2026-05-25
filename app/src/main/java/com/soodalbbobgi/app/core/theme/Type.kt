package com.soodalbbobgi.app.core.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object SoodalTypography {
    val xl = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).sp, lineHeight = 33.6.sp)
    val lg = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).sp, lineHeight = 27.5.sp)
    val md = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 23.4.sp)
    val body = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 23.25.sp)
    val cap = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.8.sp)
    val mini = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp)
    val mono = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, letterSpacing = 0.48.sp)
}
