package com.soodalbbobgi.app.presentation.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private fun Int.formatNumber(): String = String.format("%,d", this)

@Composable
fun HomeScreen(
    onNavigateToTab: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfileFullscreen: () -> Unit,
    onNavigateToProfileEditor: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

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
            // ── Header ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "안녕하세요,",
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                    )
                    Text(
                        text = "${state.nickname} 🦦",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToSettings,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "⚙️", fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(spacing.s4))

            // ── Profile Card Placeholder ────────────────────────
            SoodalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToProfileFullscreen,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Avatar placeholder
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(SoodalShape.md)
                            .background(colors.surface3),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "🦦", fontSize = 28.sp)
                    }
                    Spacer(Modifier.width(spacing.s3))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.nickname,
                            style = SoodalDesign.typography.md,
                            color = colors.textPrimary,
                        )
                        Text(
                            text = "프로필 카드 보기",
                            style = SoodalDesign.typography.cap,
                            color = colors.textTertiary,
                        )
                    }
                    SoodalButton(
                        text = "편집",
                        onClick = onNavigateToProfileEditor,
                        style = ButtonStyle.Ghost,
                    )
                }
            }

            Spacer(Modifier.height(spacing.s3))

            // ── Currency Row ────────────────────────────────────
            GlassPanel(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                        SoodalChip(
                            text = state.shells.formatNumber(),
                            color = ChipColor.Gold,
                            icon = "🐚",
                        )
                        SoodalChip(
                            text = state.pearls.formatNumber(),
                            color = ChipColor.Purple,
                            icon = "💎",
                        )
                    }
                    SoodalButton(
                        text = "뽑기",
                        onClick = { onNavigateToTab("gacha") },
                        style = ButtonStyle.Primary,
                    )
                }
            }

            Spacer(Modifier.height(spacing.s3))

            // ── No-record Banner (conditional) ──────────────────
            if (!state.todayHasRecord) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoodalShape.md)
                        .background(colors.warn.copy(alpha = 0.12f))
                        .border(1.dp, colors.warn.copy(alpha = 0.3f), SoodalShape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.onSync() },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    ) {
                        Text(text = "⚠️", fontSize = 16.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "오늘 수영 기록이 없어요",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.warn,
                            )
                            Text(
                                text = "탭하여 Health Connect에서 동기화",
                                fontSize = 11.sp,
                                color = colors.warn.copy(alpha = 0.7f),
                            )
                        }
                        if (state.syncing) {
                            Text(
                                text = "동기화 중…",
                                fontSize = 11.sp,
                                color = colors.textTertiary,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(spacing.s3))
            }

            // ── Month Stats ─────────────────────────────────────
            Text(
                text = "이번 달 기록",
                style = SoodalDesign.typography.md,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🏊",
                    label = "거리",
                    value = "${(state.totalDistance / 1000f).let { String.format("%.1f", it) }}km",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "📅",
                    label = "수영 횟수",
                    value = "${state.swimSessions}회",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = "🔥",
                    label = "소모 칼로리",
                    value = "${state.totalKcal.formatNumber()}kcal",
                )
            }

            Spacer(Modifier.height(spacing.s5))

            // ── Recent Items ────────────────────────────────────
            Text(
                text = "최근 획득 아이템",
                style = SoodalDesign.typography.md,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(spacing.s2))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                state.recentItems.forEach { item ->
                    RecentItemCard(item = item)
                }
            }

            Spacer(Modifier.height(spacing.s4))
        }

        // ── Tab Bar ─────────────────────────────────────────
        SoodalTabBar(activeTab = "home", onTabSelected = onNavigateToTab)
    }
}

@Composable
private fun StatCard(
    icon: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    SoodalCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = icon, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accentCyan,
                textAlign = TextAlign.Center,
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RecentItemCard(item: RecentItem) {
    val colors = SoodalDesign.colors
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(SoodalShape.md)
                .background(colors.surface2),
            contentAlignment = Alignment.Center,
        ) {
            val emoji = when (item.kind) {
                "char" -> "🦦"
                "frame" -> "🖼️"
                "bg" -> "🎨"
                else -> "❓"
            }
            Text(text = emoji, fontSize = 28.sp)
        }
        Spacer(Modifier.height(4.dp))
        GradeBadge(grade = item.grade)
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.name,
            fontSize = 11.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
