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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.GlassPanel
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalChip
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
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 12.dp),
            ) {
                // -- Header --
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🎰 뽑기", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                    SoodalChip(state.shells.toString(), color = ChipColor.Gold, icon = "🐚")
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

                Spacer(Modifier.height(16.dp))

                // -- Label --
                Text(
                    "🎰 어떤 상자가 나올까?",
                    fontSize = 13.sp, color = colors.textSecondary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                // -- Roulette Track --
                val screenWidth = LocalConfiguration.current.screenWidthDp.toFloat()
                val loopedBoxes = remember { buildList { repeat(8) { addAll(GACHA_BOXES) } } }

                Box(Modifier.fillMaxWidth().height(160.dp)) {
                    // Track items
                    val trackOffset = state.offset
                    val startX = screenWidth / 2f - 60f

                    Row(
                        Modifier.offset(x = (startX - trackOffset).dp, y = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        loopedBoxes.forEachIndexed { i, box ->
                            val isFocused = i == state.focusedBoxIndex
                            GachaBoxCard(box = box, focused = isFocused)
                        }
                    }

                    // Center marker
                    Box(
                        Modifier.align(Alignment.TopCenter).width(2.dp)
                            .padding(vertical = 8.dp).fillMaxHeight()
                            .shadow(6.dp, RoundedCornerShape(2.dp), ambientColor = colors.accentCyan, spotColor = colors.accentCyan)
                            .background(colors.accentCyan, RoundedCornerShape(2.dp))
                    )

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

                Spacer(Modifier.height(20.dp))

                // -- Spin Buttons (two-line) --
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SpinButton(
                        topText = "단발 뽑기", bottomText = "🐚 1개",
                        background = colors.gradGold, textColor = colors.btnGoldText, glowColor = colors.glowGold,
                        enabled = state.phase == GachaPhase.Idle && state.shells >= 1,
                        onClick = { viewModel.spin(1) },
                        modifier = Modifier.weight(1f),
                    )
                    SpinButton(
                        topText = "10연 뽑기", bottomText = "🐚 9개",
                        background = colors.gradPurple, textColor = colors.btnPurpleText, glowColor = colors.glowPurple,
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
private fun GachaBoxCard(box: BoxInfo, focused: Boolean) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .size(120.dp, 130.dp)
            .scale(if (focused) 1.06f else 1f)
            .then(
                if (focused) Modifier.shadow(14.dp, shape, ambientColor = box.color.copy(alpha = 0.5f), spotColor = box.color.copy(alpha = 0.5f))
                else Modifier
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(box.color.copy(alpha = 0.13f), box.color.copy(alpha = 0.03f)),
                )
            )
            .border(1.5.dp, box.color.copy(alpha = 0.4f), shape),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(box.emoji, fontSize = 32.sp)
        Spacer(Modifier.height(8.dp))
        Text(box.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = box.color, letterSpacing = (-0.05).sp)
    }
}

@Composable
private fun SpinButton(
    topText: String, bottomText: String,
    background: Brush, textColor: Color, glowColor: Color,
    enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val shape = SoodalShape.md
    Column(
        modifier = modifier
            .height(52.dp)
            .shadow(if (enabled) 12.dp else 0.dp, shape, ambientColor = glowColor, spotColor = glowColor)
            .clip(shape)
            .background(background)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .then(if (!enabled) Modifier.drawBehind { drawRect(Color.Black.copy(alpha = 0.3f)) } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(topText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (enabled) textColor else textColor.copy(alpha = 0.4f))
        Text(bottomText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) textColor.copy(alpha = 0.7f) else textColor.copy(alpha = 0.3f))
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
    val itemEmoji = when (item.kind) {
        "char" -> "🦦"
        "bg" -> "🎨"
        "frame" -> "🖼️"
        else -> "🎁"
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
                    .background(Brush.linearGradient(listOf(Color(0xFF1A2235), Color(0xFF131A2C))))
                    .border(1.5.dp, gradeColor, RoundedCornerShape(24.dp))
                    .padding(28.dp, 24.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // Counter badge (multi-pull only)
                    if (state.results.size > 1) {
                        Text(
                            "${state.resultIndex + 1} / ${state.results.size}",
                            fontSize = 11.sp, color = colors.textSecondary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.align(Alignment.End)
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }

                    // Box chip
                    SoodalChip("📦 $boxLabel 선택됨", color = ChipColor.Cyan)

                    Spacer(Modifier.height(16.dp))

                    // Grade badge
                    GradeBadge(item.grade)

                    Spacer(Modifier.height(20.dp))

                    // Item visual
                    Box(
                        Modifier.size(80.dp).scale(bounceScale)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.radialGradient(listOf(gradeGlow, Color.Transparent))
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(itemEmoji, fontSize = 40.sp)
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
                        Text(
                            "이미 보유 → 🔮 진주 ${item.pearlsEarned}개로 교환!",
                            fontSize = 13.sp, color = colors.textSecondary,
                        )
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
                SoodalButton("전체 결과 보기 ⏭", onClick = onSkipToLast, style = ButtonStyle.Ghost)
            }
        }
    }
}
