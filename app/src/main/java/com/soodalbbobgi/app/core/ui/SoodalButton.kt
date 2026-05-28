package com.soodalbbobgi.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape

enum class ButtonStyle { Primary, Gold, Purple, Secondary, Ghost }

/**
 * 공용 버튼 컴포저블.
 *
 * @param heightOverride 스타일별 기본 높이(Primary/Gold/Purple 52dp, Secondary 44dp,
 *   Ghost 36dp)를 무시하고 강제로 적용할 높이. null이면 스타일 기본 사용.
 *   같은 행에 다른 스타일 버튼을 나란히 둘 때 시각적 높이를 맞추기 위해 사용.
 */
@Composable
fun SoodalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ButtonStyle = ButtonStyle.Primary,
    enabled: Boolean = true,
    heightOverride: Dp? = null,
) {
    val colors = SoodalDesign.colors
    val background = when (style) {
        ButtonStyle.Primary -> colors.gradCyan
        ButtonStyle.Gold -> colors.gradGold
        ButtonStyle.Purple -> colors.gradPurple
        else -> null
    }
    val textColor = when (style) {
        ButtonStyle.Primary -> colors.btnPrimaryText
        ButtonStyle.Gold -> colors.btnGoldText
        ButtonStyle.Purple -> colors.btnPurpleText
        ButtonStyle.Secondary -> colors.textPrimary
        ButtonStyle.Ghost -> colors.textSecondary
    }
    val glowColor = when (style) {
        ButtonStyle.Primary -> colors.glowCyan
        ButtonStyle.Gold -> colors.glowGold
        ButtonStyle.Purple -> colors.glowPurple
        else -> null
    }
    val height = heightOverride ?: when (style) {
        ButtonStyle.Ghost -> 36.dp
        ButtonStyle.Secondary -> 44.dp
        else -> 52.dp
    }
    val shape = SoodalShape.md

    val shadowModifier = if (glowColor != null && enabled) {
        Modifier.shadow(12.dp, shape, ambientColor = glowColor, spotColor = glowColor)
    } else Modifier

    val bgModifier = when {
        background != null -> Modifier.background(background, shape)
        style == ButtonStyle.Secondary -> Modifier
            .background(colors.glassBg, shape)
            .border(1.dp, colors.glassBorder, shape)
        else -> Modifier
    }

    Row(
        modifier = modifier
            .then(shadowModifier)
            .height(height)
            .clip(shape)
            .then(bgModifier)
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = when (style) {
                ButtonStyle.Ghost -> 12.dp
                ButtonStyle.Secondary -> 16.dp
                else -> 20.dp
            }),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else textColor.copy(alpha = 0.4f),
            fontSize = when (style) {
                ButtonStyle.Ghost -> 13.sp
                ButtonStyle.Secondary -> 14.sp
                else -> 15.sp
            },
            fontWeight = FontWeight.Bold,
        )
    }
}
