package com.soodalbbobgi.app.core.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape

/**
 * 그림자 + 라운드 배경의 공용 카드 컨테이너.
 *
 * @param contentPadding 카드 내부 패딩. 이미지가 주인공인 셀처럼 공간 활용이
 *   중요한 곳에서만 기본값(16dp)보다 줄여서 사용한다.
 */
@Composable
fun SoodalCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = SoodalDesign.colors
    Box(
        modifier = modifier
            .shadow(8.dp, SoodalShape.lg, ambientColor = colors.cardShadow.copy(alpha = 0.12f), spotColor = colors.cardShadow.copy(alpha = 0.08f))
            .shadow(2.dp, SoodalShape.lg, ambientColor = colors.cardShadow.copy(alpha = 0.06f), spotColor = colors.cardShadow.copy(alpha = 0.04f))
            .clip(SoodalShape.lg)
            .background(colors.cardBg)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = SoodalDesign.colors
    val glassBg = if (!colors.isDark && Build.VERSION.SDK_INT < 31) {
        colors.glassBg.copy(alpha = 0.80f)
    } else {
        colors.glassBg
    }

    Box(
        modifier = modifier
            .clip(SoodalShape.lg)
            .background(glassBg)
            .border(1.dp, colors.glassBorder, SoodalShape.lg)
            .padding(14.dp),
        content = content,
    )
}
