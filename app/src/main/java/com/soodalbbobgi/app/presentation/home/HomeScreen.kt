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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.SoodalTabBar
import com.soodalbbobgi.app.presentation.profile.CardLayers
import com.soodalbbobgi.app.presentation.profile.ProfileCardComposite

private fun Int.formatNumber(): String = String.format("%,d", this)

@Composable
fun HomeScreen(
    onNavigateToTab: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfileFullscreen: () -> Unit,
    onNavigateToProfileEditor: () -> Unit,
    onSyncClick: () -> Unit = {},
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = state.nickname,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                        )
                        SoodalIcon(icon = SoodalIcons.Otter, size = 20.dp)
                    }
                }
                Row(
                    modifier = Modifier
                        .clip(SoodalShape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateToSettings,
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Settings, tint = colors.textSecondary, size = 18.dp)
                    Text("설정", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                }
            }

            Spacer(Modifier.height(spacing.s4))

            // ── Profile Card ────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp, SoodalShape.lg)
                    .clip(SoodalShape.lg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToProfileFullscreen,
                    ),
            ) {
                ProfileCardComposite(
                    layers = CardLayers(),
                    modifier = Modifier.fillMaxWidth(),
                )
                // Edit button overlay (top-right)
                SoodalButton(
                    text = "편집",
                    onClick = onNavigateToProfileEditor,
                    style = ButtonStyle.Ghost,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }

            Text(
                text = "전체화면 보기",
                fontSize = 12.sp,
                color = colors.textTertiary,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToProfileFullscreen,
                    )
                    .padding(top = 6.dp, bottom = 2.dp),
            )

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
                            iconType = SoodalIcons.Shell,
                            label = "조개",
                        )
                        SoodalChip(
                            text = state.pearls.formatNumber(),
                            color = ChipColor.Purple,
                            iconType = SoodalIcons.Pearl,
                            label = "진주",
                        )
                    }
                    SoodalButton(
                        text = "뽑기 →",
                        onClick = { onNavigateToTab("gacha") },
                        style = ButtonStyle.Secondary,
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
                            onClick = {
                                viewModel.onSync()
                                onSyncClick()
                            },
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                    ) {
                        SoodalIcon(
                            icon = if (state.syncing) SoodalIcons.Sync else SoodalIcons.Warn,
                            tint = colors.warn,
                            size = 18.dp,
                        )
                        Text(
                            text = if (state.syncing) "동기화 중이에요…"
                            else "오늘 수영 기록이 없어요. Health Connect를 동기화해보세요.",
                            fontSize = 13.sp,
                            color = colors.warn.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f),
                            lineHeight = 18.sp,
                        )
                        Text("동기화", fontSize = 12.sp, color = colors.warn, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(spacing.s3))
            }

            // ── Month Stats ─────────────────────────────────────
            Text(
                text = "이번 달 수영",
                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                color = colors.textSecondary, letterSpacing = 0.6.sp,
            )
            Spacer(Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                StatCard(modifier = Modifier.weight(1f), label = "누적거리",
                    value = state.totalDistance.formatNumber(), unit = "m",
                    valueColor = colors.accentCyan,
                    onClick = { onNavigateToTab("calendar") })
                StatCard(modifier = Modifier.weight(1f), label = "수영 횟수",
                    value = "${state.swimSessions}", unit = "회",
                    valueColor = colors.textPrimary,
                    onClick = { onNavigateToTab("calendar") })
                StatCard(modifier = Modifier.weight(1f), label = "칼로리",
                    value = state.totalKcal.formatNumber(), unit = "kcal",
                    valueColor = colors.success,
                    onClick = { onNavigateToTab("calendar") })
            }

            Spacer(Modifier.height(spacing.s5))

            // ── Recent Items ────────────────────────────────────
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("최근 획득 아이템", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    color = colors.textSecondary, letterSpacing = 0.6.sp)
                Text("최근 7일", fontSize = 11.sp, color = colors.textTertiary)
            }
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
    label: String,
    value: String,
    unit: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val colors = SoodalDesign.colors
    SoodalCard(modifier = modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null, onClick = onClick,
    )) {
        Column {
            Text(label, fontSize = 10.sp, color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
                Spacer(Modifier.width(3.dp))
                Text(unit, fontSize = 10.sp, color = colors.textSecondary)
            }
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
        val (icon, bgColor) = when (item.kind) {
            "char" -> SoodalIcons.Otter to colors.accentGoldSoft
            "frame" -> SoodalIcons.Frame to colors.accentCyanSoft
            "bg" -> SoodalIcons.Aurora to colors.accentPurpleSoft
            else -> SoodalIcons.Gift to colors.surface2
        }
        val iconTint = when (item.kind) {
            "char" -> colors.accentGold
            "frame" -> colors.accentCyan
            "bg" -> colors.accentPurple
            else -> colors.textTertiary
        }
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(SoodalShape.md)
                .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            SoodalIcon(icon = icon, tint = iconTint, size = 28.dp)
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
