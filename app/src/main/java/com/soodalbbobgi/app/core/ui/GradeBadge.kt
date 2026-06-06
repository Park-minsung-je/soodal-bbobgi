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
    // borderless 원칙 — 등급 뱃지는 채움색만으로 구분한다.
    val (fg, bg) = when (grade) {
        Grade.SSR -> colors.accentGold to colors.accentGoldSoft
        Grade.SR -> colors.accentPurple to colors.accentPurpleSoft
        Grade.R -> colors.accentBlue to colors.accentBlueSoft
        Grade.N -> colors.textSecondary to colors.surface2
    }

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
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
