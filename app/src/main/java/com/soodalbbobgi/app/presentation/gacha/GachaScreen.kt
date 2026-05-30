package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import kotlin.math.roundToInt

@Composable
fun GachaScreen(
    viewModel: GachaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = SoodalDesign.colors

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().background(colors.bgDeep)) {
            Column(
                Modifier.weight(1f).padding(top = 12.dp),
            ) {
                // -- Header --
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SoodalIcon(icon = SoodalIcons.Gacha, size = 22.dp)
                        Text("뽑기", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                    }
                    SoodalChip(state.shells.toString(), color = ChipColor.Gold, iconType = SoodalIcons.Shell, label = "조개")
                }

                Spacer(Modifier.weight(1f))

                // -- Label --
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SoodalIcon(icon = SoodalIcons.Gacha, tint = colors.textSecondary, size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "어떤 상자가 나올까?",
                        fontSize = 13.sp, color = colors.textSecondary,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // -- Roulette Track (infinite loop, center = biggest) --
                val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()
                val itemW = 128f
                val slotW = ITEM_WIDTH_WITH_GAP
                val centerX = screenWidth / 2f
                val visibleSlots = (centerX / slotW).toInt() + 2

                Box(Modifier.fillMaxWidth().height(170.dp).clipToBounds()) {
                    for (di in -visibleSlots..visibleSlots) {
                        val i = ((state.offset / slotW).roundToInt()) + di
                        val boxIndex = ((i % GACHA_BOXES.size) + GACHA_BOXES.size) % GACHA_BOXES.size
                        val box = GACHA_BOXES[boxIndex]
                        val x = centerX + (i * slotW - state.offset) - itemW / 2f
                        val fracDist = kotlin.math.abs(i * slotW - state.offset) / slotW
                        val itemScale = (1.12f - fracDist * 0.12f).coerceIn(0.82f, 1.12f)

                        Box(Modifier.offset(x = x.dp, y = 10.dp)) {
                            GachaBoxCard(box = box, itemScale = itemScale)
                        }
                    }

                    // Left edge fade
                    Box(
                        Modifier.align(Alignment.CenterStart).width(60.dp).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(colors.bgDeep, Color.Transparent)))
                    )
                    // Right edge fade
                    Box(
                        Modifier.align(Alignment.CenterEnd).width(60.dp).fillMaxHeight()
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, colors.bgDeep)))
                    )
                }

                Spacer(Modifier.weight(1.5f))

                // -- Spin Buttons --
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpinButton(
                        topText = "단발 뽑기", bottomText = "1개", shellIcon = true,
                        bgColors = listOf(Color(0xFFFFC845), Color(0xFFF09000)), textColor = colors.btnGoldText, glowColor = colors.glowGold,
                        enabled = state.phase == GachaPhase.Idle && state.shells >= 1,
                        onClick = { viewModel.spin(1) },
                        modifier = Modifier.weight(1f),
                    )
                    SpinButton(
                        topText = "10연 뽑기", bottomText = "10개", shellIcon = true,
                        bgColors = listOf(Color(0xFFB57BFF), Color(0xFF6E3BD8)), textColor = colors.btnPurpleText, glowColor = colors.glowPurple,
                        enabled = state.phase == GachaPhase.Idle && state.shells >= 10,
                        onClick = { viewModel.spin(10) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(14.dp))

                // -- Footer --
                Text(
                    "※ 확률 정보는 상점 → FAQ에서 확인할 수 있어요",
                    fontSize = 11.sp, color = colors.textTertiary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(24.dp))
            }
        }

        // -- Result Modal (공용 오버레이: 인덱스/전체보기 내부 관리) --
        if (state.phase == GachaPhase.Result && state.results.isNotEmpty()) {
            GachaResultOverlay(
                results = state.results,
                onClose = { viewModel.closeResults() },
            )
        }
    }
}

@Composable
private fun GachaBoxCard(box: BoxInfo, itemScale: Float = 1f) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .size(128.dp, 140.dp)
            .scale(itemScale)
            .clip(shape)
            .drawBehind {
                drawRect(Brush.linearGradient(
                    listOf(box.color.copy(alpha = 0.13f), box.color.copy(alpha = 0.03f)),
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                ))
            }
            .border(1.5.dp, box.color.copy(alpha = 0.4f), shape),
        contentAlignment = Alignment.Center,
    ) {
        SoodalIcon(icon = box.icon, tint = box.color, size = 48.dp)
    }
}

@Composable
private fun SpinButton(
    topText: String, bottomText: String,
    bgColors: List<Color>, textColor: Color, glowColor: Color,
    enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    shellIcon: Boolean = false,
) {
    val shape = SoodalShape.md
    Column(
        modifier = modifier
            .height(60.dp)
            .shadow(if (enabled) 12.dp else 0.dp, shape, ambientColor = glowColor, spotColor = glowColor)
            .clip(shape)
            .drawBehind {
                drawRect(Brush.linearGradient(bgColors, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(size.width, size.height)))
                if (!enabled) drawRect(Color.Black.copy(alpha = 0.3f))
            }
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(topText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (enabled) textColor else textColor.copy(alpha = 0.4f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            if (shellIcon) {
                SoodalIcon(icon = SoodalIcons.Shell, tint = if (enabled) textColor.copy(alpha = 0.7f) else textColor.copy(alpha = 0.3f), size = 11.dp)
            }
            Text(bottomText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) textColor.copy(alpha = 0.7f) else textColor.copy(alpha = 0.3f))
        }
    }
}

