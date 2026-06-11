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

enum class ChipColor { Blue, Purple, Gold }

/**
 * 공용 칩 컴포저블.
 *
 * @param large 헤더 재화 표시처럼 강조가 필요할 때 — 아이콘/글자/패딩을 한 단계 키운다
 */
@Composable
fun SoodalChip(
    text: String,
    modifier: Modifier = Modifier,
    color: ChipColor = ChipColor.Blue,
    iconType: SoodalIcons? = null,
    label: String? = null,
    large: Boolean = false,
) {
    val colors = SoodalDesign.colors
    // borderless 원칙 — 칩은 채움색만으로 구분한다.
    val (bg, fg) = when (color) {
        ChipColor.Blue -> colors.accentBlueSoft to colors.accentBlue
        ChipColor.Purple -> colors.accentPurpleSoft to colors.accentPurple
        ChipColor.Gold -> colors.accentGoldSoft to colors.accentGold
    }
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(
                horizontal = if (large) 14.dp else 10.dp,
                vertical = if (large) 8.dp else 6.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(if (large) 7.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconType != null) {
            SoodalIcon(icon = iconType, tint = fg, size = if (large) 19.dp else 14.dp)
        }
        if (label != null) {
            Text(
                text = label,
                color = fg.copy(alpha = 0.7f),
                fontSize = if (large) 12.sp else 10.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(
            text = text,
            color = fg,
            fontSize = if (large) 16.sp else 12.sp,
            fontWeight = if (large) FontWeight.Bold else FontWeight.SemiBold,
        )
    }
}
