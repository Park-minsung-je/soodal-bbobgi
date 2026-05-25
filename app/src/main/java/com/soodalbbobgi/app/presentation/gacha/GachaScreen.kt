package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalTabBar

@Composable
fun GachaScreen(
    onNavigateToTab: (String) -> Unit,
    viewModel: GachaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.bgDeep),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.s4, vertical = spacing.s4),
            ) {
                // -- Header --
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "🎰 뽑기",
                        style = SoodalDesign.typography.lg,
                        color = colors.textPrimary,
                    )
                    SoodalChip(
                        text = state.shells.toString(),
                        color = ChipColor.Gold,
                        icon = "🐚",
                    )
                }

                Spacer(Modifier.height(spacing.s4))

                // -- Pity Bar --
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SSR 천장까지 ${state.pityRemaining}회 남음",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accentGold,
                        )
                        Spacer(Modifier.height(spacing.s2))
                        val progress = 1f - (state.pityRemaining / 30f)
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            animationSpec = tween(400),
                            label = "pity",
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(SoodalShape.sm)
                                .background(colors.surface2),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                    .height(8.dp)
                                    .clip(SoodalShape.sm)
                                    .background(colors.accentGold),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(spacing.s5))

                // -- Box Selector --
                Text(
                    text = "상자 선택",
                    style = SoodalDesign.typography.md,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(spacing.s3))

                Box(contentAlignment = Alignment.Center) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                    ) {
                        state.boxes.forEachIndexed { index, box ->
                            BoxCard(
                                box = box,
                                isSelected = index == state.selectedBoxIndex,
                                onClick = { viewModel.selectBox(index) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(spacing.s5))

                // -- Spin Buttons --
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                ) {
                    SoodalButton(
                        text = "단발 뽑기 🐚1개",
                        onClick = { viewModel.spin(1) },
                        style = ButtonStyle.Gold,
                        enabled = state.phase == GachaPhase.Idle && state.shells >= 1,
                        modifier = Modifier.weight(1f),
                    )
                    SoodalButton(
                        text = "10연 뽑기 🐚9개",
                        onClick = { viewModel.spin(10) },
                        style = ButtonStyle.Purple,
                        enabled = state.phase == GachaPhase.Idle && state.shells >= 9,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Spinning indicator
                if (state.phase == GachaPhase.Spinning) {
                    Spacer(Modifier.height(spacing.s5))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "🎰 뽑는 중...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentCyan,
                        )
                    }
                }

                Spacer(Modifier.height(spacing.s5))
            }

            // -- Tab Bar --
            SoodalTabBar(activeTab = "gacha", onTabSelected = onNavigateToTab)
        }

        // -- Result Modal Overlay --
        if (state.phase == GachaPhase.Result && state.results.isNotEmpty()) {
            ResultOverlay(
                state = state,
                onNext = { viewModel.nextResult() },
                onClose = { viewModel.closeResults() },
            )
        }
    }
}

@Composable
private fun BoxCard(
    box: BoxInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val borderColor = if (isSelected) colors.accentCyan else colors.cardBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        modifier = Modifier
            .width(120.dp)
            .height(130.dp)
            .clip(SoodalShape.lg)
            .background(colors.cardBg)
            .border(borderWidth, borderColor, SoodalShape.lg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = box.emoji, fontSize = 36.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = box.name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = box.desc,
            fontSize = 10.sp,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ResultOverlay(
    state: GachaUiState,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing
    val item = state.results[state.resultIndex]
    val isLast = state.resultIndex == state.results.size - 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        SoodalCard(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Counter
                Text(
                    text = "${state.resultIndex + 1} / ${state.results.size}",
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                )

                Spacer(Modifier.height(spacing.s4))

                // Item emoji placeholder
                val emoji = when (item.kind) {
                    "char" -> "🦦"
                    "bg" -> "🎨"
                    "frame" -> "🖼️"
                    else -> "🎁"
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(SoodalShape.lg)
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = emoji, fontSize = 40.sp)
                }

                Spacer(Modifier.height(spacing.s3))

                GradeBadge(grade = item.grade)

                Spacer(Modifier.height(spacing.s2))

                Text(
                    text = item.name,
                    style = SoodalDesign.typography.md,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(spacing.s2))

                if (item.isNew) {
                    Text(
                        text = "✨ 신규!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentCyan,
                    )
                } else {
                    Text(
                        text = "→ 진주 ${item.pearlsEarned}개",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentPurple,
                    )
                }

                Spacer(Modifier.height(spacing.s5))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                ) {
                    if (!isLast) {
                        SoodalButton(
                            text = "다음",
                            onClick = onNext,
                            style = ButtonStyle.Primary,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    SoodalButton(
                        text = if (isLast) "확인" else "닫기",
                        onClick = onClose,
                        style = if (isLast) ButtonStyle.Primary else ButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
