package com.soodalbbobgi.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.domain.model.Grade

@Composable
fun GradeBadge(
    grade: Grade,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    val (fg, borderColor, bg) = when (grade) {
        Grade.SSR -> Triple(colors.accentGold, colors.accentGold.copy(alpha = 0.45f), colors.accentGoldSoft)
        Grade.SR -> Triple(colors.accentPurple, colors.accentPurple.copy(alpha = 0.45f), colors.accentPurpleSoft)
        Grade.R -> Triple(colors.accentBlue, colors.accentBlue.copy(alpha = 0.45f), colors.accentBlueSoft)
        Grade.N -> Triple(colors.textSecondary, colors.textTertiary, colors.surface2)
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (grade) {
            Grade.SSR -> SoodalIcon(icon = SoodalIcons.Star, tint = fg, size = 10.dp)
            Grade.SR -> SoodalIcon(icon = SoodalIcons.Sparkle, tint = fg, size = 10.dp)
            else -> {}
        }
        Text(
            text = grade.name,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}
