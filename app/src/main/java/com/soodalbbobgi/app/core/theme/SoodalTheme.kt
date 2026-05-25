package com.soodalbbobgi.app.core.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

enum class SoodalThemeType {
    Light,
    Neon,
    Calm,
    Pastel,
}

fun soodalColors(theme: SoodalThemeType): SoodalColors = when (theme) {
    SoodalThemeType.Light -> LightColors
    SoodalThemeType.Neon -> NeonColors
    SoodalThemeType.Calm -> CalmColors
    SoodalThemeType.Pastel -> PastelColors
}

@Composable
fun SoodalTheme(
    theme: SoodalThemeType = SoodalThemeType.Light,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSoodalColors provides soodalColors(theme),
        content = content,
    )
}

object SoodalDesign {
    val colors: SoodalColors
        @Composable get() = LocalSoodalColors.current
    val typography = SoodalTypography
    val shape = SoodalShape
    val spacing = SoodalSpacing
}
