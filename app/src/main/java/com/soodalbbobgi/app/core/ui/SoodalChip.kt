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

@Composable
fun SoodalChip(
    text: String,
    modifier: Modifier = Modifier,
    color: ChipColor = ChipColor.Blue,
    iconType: SoodalIcons? = null,
    label: String? = null,
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
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconType != null) {
            SoodalIcon(icon = iconType, tint = fg, size = 14.dp)
        }
        if (label != null) {
            Text(text = label, color = fg.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Text(text = text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
