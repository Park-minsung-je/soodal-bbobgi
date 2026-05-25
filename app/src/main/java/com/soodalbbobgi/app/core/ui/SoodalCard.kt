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
import androidx.compose.ui.unit.dp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape

@Composable
fun SoodalCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = SoodalDesign.colors
    Box(
        modifier = modifier
            .shadow(2.dp, SoodalShape.lg, ambientColor = colors.cardShadow, spotColor = colors.cardShadow)
            .clip(SoodalShape.lg)
            .background(colors.cardBg)
            .border(1.dp, colors.cardBorder, SoodalShape.lg)
            .padding(16.dp),
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
