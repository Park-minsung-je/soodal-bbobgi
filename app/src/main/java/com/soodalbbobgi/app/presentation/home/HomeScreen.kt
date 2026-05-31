package com.soodalbbobgi.app.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
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
import com.soodalbbobgi.app.core.ui.ShellRewardPopup
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.motion.Motion
import com.soodalbbobgi.app.presentation.profile.CardLayers
import com.soodalbbobgi.app.presentation.profile.EditorSheet
import com.soodalbbobgi.app.presentation.profile.ProfileCardBounds
import com.soodalbbobgi.app.presentation.profile.ProfileCardComposite
import com.soodalbbobgi.app.presentation.profile.ProfileEditorViewModel

private fun Int.formatNumber(): String = String.format("%,d", this)

@Composable
fun HomeScreen(
    onNavigateToTab: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfileFullscreen: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val shellReward by viewModel.shellReward.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing
    val context = LocalContext.current

    val editorVm: ProfileEditorViewModel = hiltViewModel()
    val editorState by editorVm.uiState.collectAsState()
    var editorOpen by rememberSaveable { mutableStateOf(false) }

    // 시스템 뒤로가기 → 시트 닫기(취소): 미저장 변경 폐기.
    BackHandler(enabled = editorOpen) {
        editorVm.resetToSaved()
        editorOpen = false
    }

    // [적용] 저장 성공 시 시트 닫기.
    LaunchedEffect(editorState.saveSuccess) {
        if (editorState.saveSuccess) {
            android.widget.Toast.makeText(context, "프로필 카드가 저장됐어요", android.widget.Toast.LENGTH_SHORT).show()
            editorVm.clearSaveResult()
            editorOpen = false
        }
    }

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
            // ── Header ──────────────────────────────────────────
            // 프로필편집 화면 헤더와 같은 고정 높이(52dp) — 두 화면의 카드 세로 위치를 일치시켜
            // 홈↔편집 전환 시 카드가 제자리에 머무는 것처럼 보이게 한다.
            Row(
                modifier = Modifier.fillMaxWidth().height(52.dp),
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
            // 편집 중이면 편집값(미저장)으로, 아니면 저장값으로 카드를 그린다. 카드 위치는 고정.
            val cardLayers: CardLayers
            val cardBgAsset: String?
            val cardCharAsset: String?
            val cardFrameAsset: String?
            if (editorOpen) {
                cardLayers = CardLayers(
                    nickname = editorState.nickname,
                    tagline = editorState.customText.ifEmpty { editorState.taglineFallback },
                    stats = editorState.statsText,
                    charX = editorState.charX,
                    charY = editorState.charY,
                    charScale = editorState.charScale,
                    textStyle = editorState.textStyle,
                    textAlign = editorState.textAlign,
                    textX = editorState.textX,
                    textY = editorState.textY,
                    textScaleStep = editorState.textScaleStep,
                    showStats = editorState.showStats,
                    nicknameColor = editorState.nicknameColor,
                    taglineColor = editorState.taglineColor,
                    statsColor = editorState.statsColor,
                )
                cardBgAsset = editorState.bgItems.firstOrNull { it.isSelected }?.imageAsset
                cardCharAsset = editorState.charItems.firstOrNull { it.isSelected }?.imageAsset
                cardFrameAsset = editorState.frameItems.firstOrNull { it.isSelected }?.imageAsset
            } else {
                cardLayers = CardLayers(
                    nickname = state.cardNickname,
                    tagline = state.cardTagline,
                    stats = state.cardStats,
                    charX = state.cardCharX,
                    charY = state.cardCharY,
                    charScale = state.cardCharScale,
                    textStyle = state.cardTextStyle,
                    textAlign = state.cardTextAlign,
                    textX = state.cardTextX,
                    textY = state.cardTextY,
                    textScaleStep = state.cardTextScaleStep,
                    showStats = state.cardShowStats,
                    nicknameColor = state.cardNicknameColor,
                    taglineColor = state.cardTaglineColor,
                    statsColor = state.cardStatsColor,
                )
                cardBgAsset = state.cardBgAsset
                cardCharAsset = state.cardCharAsset
                cardFrameAsset = state.cardFrameAsset
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 전체보기 진입 시 카드가 이 자리에서 떠오르도록 화면상 중심을 기록한다.
                    .onGloballyPositioned { ProfileCardBounds.homeCardCenter = it.boundsInWindow().center }
                    .shadow(8.dp, RectangleShape)
                    .clip(RectangleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onNavigateToProfileFullscreen,
                    ),
            ) {
                ProfileCardComposite(
                    layers = cardLayers,
                    bgAsset = cardBgAsset,
                    charAsset = cardCharAsset,
                    frameAsset = cardFrameAsset,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (!editorOpen) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "전체화면 보기",
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onNavigateToProfileFullscreen,
                            )
                            .padding(vertical = 4.dp),
                    )
                    Text(
                        text = "프로필 편집",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentCyan,
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { editorOpen = true },
                            )
                            .padding(vertical = 4.dp),
                    )
                }

                Spacer(Modifier.height(spacing.s3))

                // ── Currency Row ────────────────────────────────────
                SoodalCard(modifier = Modifier.fillMaxWidth()) {
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
        }
    }

    // ── 동기화 로딩 오버레이 ────────────────────────────────
    if (state.syncing) {
        com.soodalbbobgi.app.core.ui.SyncLoadingOverlay("수영 기록 동기화 중이에요...")
    }

    // ── 동기화 에러 표시 ─────────────────────────────────────
    if (syncError != null) {
        LaunchedEffect(syncError) {
            android.widget.Toast.makeText(context, syncError, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearSyncError()
        }
    }

    // ── 조개 획득 팝업 ──────────────────────────────────────
    if (shellReward > 0) {
        ShellRewardPopup(
            shellCount = shellReward,
            distance = "오늘의 수영 기록",
            onDismiss = { viewModel.clearShellReward() },
        )
    }

    // -- 프로필 편집 시트 (하단 오버레이) --
    AnimatedVisibility(
        visible = editorOpen,
        enter = slideInVertically(
            animationSpec = tween(Motion.DUR_EDITOR, easing = Motion.easeEmphasized),
        ) { it } + fadeIn(),
        exit = slideOutVertically(
            animationSpec = tween(Motion.DUR_EDITOR, easing = Motion.easeEmphasized),
        ) { it } + fadeOut(),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        EditorSheet(
            state = editorState,
            vm = editorVm,
            onApply = { editorVm.save() },
            onPreview = onNavigateToProfileFullscreen,
            onDismiss = {
                editorVm.resetToSaved()
                editorOpen = false
            },
            modifier = Modifier.fillMaxHeight(0.62f),
        )
    }
    } // Box
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
            if (!item.imageAsset.isNullOrBlank()) {
                com.soodalbbobgi.app.core.ui.AssetImage(
                    imageAsset = item.imageAsset,
                    contentDescription = item.name,
                    modifier = Modifier.size(56.dp),
                )
            } else {
                SoodalIcon(icon = icon, tint = iconTint, size = 28.dp)
            }
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
