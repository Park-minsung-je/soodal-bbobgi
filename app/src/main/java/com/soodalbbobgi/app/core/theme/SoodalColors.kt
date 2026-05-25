package com.soodalbbobgi.app.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class SoodalColors(
    val bgDeep: Color,
    val bgBase: Color,
    val surface1: Color,
    val surface2: Color,
    val surface3: Color,
    val glassBg: Color,
    val glassBorder: Color,
    val accentCyan: Color,
    val accentCyanSoft: Color,
    val accentPurple: Color,
    val accentPurpleSoft: Color,
    val accentGold: Color,
    val accentGoldSoft: Color,
    val success: Color,
    val warn: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val gradCyan: Brush,
    val gradGold: Brush,
    val gradPurple: Brush,
    val gradCard: Brush,
    val glowCyan: Color,
    val glowGold: Color,
    val glowPurple: Color,
    val tabbarBg: Color,
    val tabbarBorder: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val cardShadow: Color,
    val inputBg: Color,
    val inputBorder: Color,
    val inputPlaceholder: Color,
    val btnPrimaryText: Color,
    val btnGoldText: Color,
    val btnPurpleText: Color,
    val isDark: Boolean,
)

val LocalSoodalColors = staticCompositionLocalOf<SoodalColors> {
    error("No SoodalColors provided. Wrap your composable in SoodalTheme.")
}
