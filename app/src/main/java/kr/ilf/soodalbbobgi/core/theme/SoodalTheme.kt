package kr.ilf.soodalbbobgi.core.theme

import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle

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
    // 밀도 오버라이드/스케일링 없음 — 기기 네이티브 dp/sp 그대로 쓴다(표준 Android 동작).
    // 색상 팔레트와 기본 폰트만 주입한다.
    CompositionLocalProvider(
        LocalSoodalColors provides soodalColors(theme),
    ) {
        ProvideTextStyle(
            value = TextStyle(fontFamily = PretendardFontFamily),
            content = content,
        )
    }
}

object SoodalDesign {
    val colors: SoodalColors
        @Composable get() = LocalSoodalColors.current
    val typography = SoodalTypography
    val shape = SoodalShape
    val spacing = SoodalSpacing
}
