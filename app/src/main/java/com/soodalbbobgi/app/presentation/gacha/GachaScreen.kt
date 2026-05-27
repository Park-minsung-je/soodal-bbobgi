package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.GlassPanel
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.SoodalTabBar
import com.soodalbbobgi.app.domain.model.Grade
import kotlin.math.roundToInt

@Composable
fun GachaScreen(
    onNavigateToTab: (String) -> Unit,
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

                Spacer(Modifier.height(20.dp))

                // -- Pity Bar --
                GlassPanel(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("SSR 천장까지", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.4.sp)
                            Text("${state.pityRemaining}회 남음", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentGold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.surface2.copy(alpha = 0.3f))) {
                            val progress = 1f - (state.pityRemaining / 90f)
                            Box(Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).clip(RoundedCornerShape(2.dp)).background(colors.gradGold))
                        }
                    }
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
                        topText = "10연 뽑기", bottomText = "9개", shellIcon = true,
                        bgColors = listOf(Color(0xFFB57BFF), Color(0xFF6E3BD8)), textColor = colors.btnPurpleText, glowColor = colors.glowPurple,
                        enabled = state.phase == GachaPhase.Idle && state.shells >= 9,
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

            SoodalTabBar(activeTab = "gacha", onTabSelected = onNavigateToTab)
        }

        // -- Result Modal --
        if (state.phase == GachaPhase.Result && state.results.isNotEmpty()) {
            GachaResultOverlay(
                state = state,
                onNext = { viewModel.nextResult() },
                onSkipToLast = { viewModel.skipToLastResult() },
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

@Composable
private fun GachaResultOverlay(
    state: GachaUiState,
    onNext: () -> Unit,
    onSkipToLast: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val item = state.results[state.resultIndex]
    val isLast = state.resultIndex == state.results.size - 1

    val gradeColor = when (item.grade) {
        Grade.SSR -> colors.accentGold
        Grade.SR -> colors.accentPurple
        Grade.R -> colors.accentCyan
        Grade.N -> colors.textSecondary
    }
    val gradeGlow = when (item.grade) {
        Grade.SSR -> colors.accentGold.copy(alpha = 0.6f)
        Grade.SR -> colors.accentPurple.copy(alpha = 0.6f)
        Grade.R -> colors.accentCyan.copy(alpha = 0.5f)
        Grade.N -> Color.White.copy(alpha = 0.1f)
    }

    val kindLabel = when (item.kind) {
        "char" -> "캐릭터"
        "bg" -> "배경"
        "frame" -> "테두리"
        else -> "아이템"
    }
    val boxLabel = GACHA_BOXES.find { it.id == item.kind }?.label ?: "상자"
    val itemIcon = when (item.kind) {
        "char" -> SoodalIcons.Otter
        "bg" -> SoodalIcons.Aurora
        "frame" -> SoodalIcons.Frame
        else -> SoodalIcons.Gift
    }

    // Bounce animation
    val bounceScale by animateFloatAsState(
        targetValue = 1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "bounce",
    )

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Card
            Box(
                Modifier.padding(horizontal = 28.dp).fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = gradeGlow, spotColor = gradeGlow)
                    .clip(RoundedCornerShape(24.dp))
                    .drawBehind {
                        drawRect(Brush.linearGradient(
                            listOf(Color(0xFF1A2235), Color(0xFF131A2C)),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                        ))
                    }
                    .border(1.5.dp, gradeColor, RoundedCornerShape(24.dp))
                    .padding(28.dp, 24.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Counter badge (multi-pull only)
                    if (state.results.size > 1) {
                        Text(
                            "${state.resultIndex + 1} / ${state.results.size}",
                            fontSize = 11.sp, color = colors.textSecondary,
                            fontFamily = JetBrainsMonoFamily,
                            modifier = Modifier.align(Alignment.End)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    // Box chip
                    SoodalChip("$boxLabel 선택됨", color = ChipColor.Cyan, iconType = SoodalIcons.Box)

                    Spacer(Modifier.height(16.dp))

                    // Grade badge
                    GradeBadge(item.grade)

                    Spacer(Modifier.height(20.dp))

                    // Item visual
                    Box(
                        Modifier.size(80.dp).scale(bounceScale)
                            .clip(RoundedCornerShape(24.dp))
                            .background(gradeGlow.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        SoodalIcon(icon = itemIcon, tint = gradeColor, size = 40.dp)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Item name
                    Text(item.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)

                    Spacer(Modifier.height(6.dp))

                    // Status
                    if (item.isNew) {
                        Text(
                            "$kindLabel — 새로 획득!",
                            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = gradeColor,
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("이미 보유 →", fontSize = 13.sp, color = colors.textSecondary)
                            SoodalIcon(icon = SoodalIcons.Pearl, tint = colors.accentPurple, size = 14.dp)
                            Text("진주 ${item.pearlsEarned}개로 교환!", fontSize = 13.sp, color = colors.textSecondary)
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Buttons
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!isLast && state.results.size > 1) {
                            SoodalButton("다음 →", onClick = onNext, style = ButtonStyle.Primary, modifier = Modifier.weight(1f))
                        } else {
                            SoodalButton("계속 뽑기", onClick = onClose, style = ButtonStyle.Secondary, modifier = Modifier.weight(1f))
                            if (item.kind == "char") {
                                SoodalButton("프로필 적용", onClick = onClose, style = ButtonStyle.Primary, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Skip button (not last)
            if (!isLast && state.results.size > 1) {
                Spacer(Modifier.height(14.dp))
                SoodalButton("전체 결과 보기", onClick = onSkipToLast, style = ButtonStyle.Ghost)
            }
        }
    }
}
