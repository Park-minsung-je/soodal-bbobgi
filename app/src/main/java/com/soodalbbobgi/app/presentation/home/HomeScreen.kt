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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.GlassPanel
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.ShellRewardPopup
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.motion.Motion
import com.soodalbbobgi.app.presentation.common.SectionLabel
import com.soodalbbobgi.app.presentation.common.TrendBadge
import com.soodalbbobgi.app.presentation.common.WeeklyActivityCard
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
    onOpenFullscreen: () -> Unit,
    hideCard: Boolean,
    editorOpen: Boolean,
    onEditorOpenChange: (Boolean) -> Unit,
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

    // 카드 아래 지점(dp) 계산: 위패딩16 + 헤더52 + 간격16 = 84, + 카드 높이, + 카드-시트 간격 8.
    val config = LocalConfiguration.current
    val cardHeightDp = (config.screenWidthDp - 32f) * 704f / 1472f
    val sheetTopDp = 84f + cardHeightDp + 8f

    // 시스템 뒤로가기 → 시트 닫기(취소): 미저장 변경 폐기.
    BackHandler(enabled = editorOpen) {
        editorVm.resetToSaved()
        onEditorOpenChange(false)
    }

    // [적용] 저장 성공 시 시트 닫기.
    LaunchedEffect(editorState.saveSuccess) {
        if (editorState.saveSuccess) {
            android.widget.Toast.makeText(context, "프로필 카드가 저장됐어요", android.widget.Toast.LENGTH_SHORT).show()
            editorVm.clearSaveResult()
            onEditorOpenChange(false)
        }
    }

    // 저장 실패 시 에러 토스트 (시트는 유지해 재시도 가능).
    LaunchedEffect(editorState.saveError) {
        editorState.saveError?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            editorVm.clearSaveResult()
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
                    Text(
                        text = "${state.nickname}님",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                    )
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
                    // 전체보기 오버레이가 이 카드의 자리·크기에서 시작하도록 화면상 위치/크기를 기록.
                    .onGloballyPositioned {
                        val b = it.boundsInWindow()
                        ProfileCardBounds.homeCardCenter = b.center
                        ProfileCardBounds.homeCardSize = b.size
                    }
                    // 오버레이 카드가 같은 자리를 덮을 준비가 된 뒤에만 홈 카드를 숨긴다(교체 순간 빈 프레임 방지).
                    .graphicsLayer { alpha = if (hideCard) 0f else 1f }
                    .shadow(8.dp, RectangleShape)
                    .clip(RectangleShape)
                    // 편집 중에는 카드 탭으로 전체화면(저장값) 진입을 막는다 — 미저장 편집값과 어긋나기 때문.
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = !editorOpen,
                        onClick = onOpenFullscreen,
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

            Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "전체화면 보기 ↗",
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                enabled = !editorOpen,
                                onClick = onOpenFullscreen,
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                    // 프로필 편집: 글래스 배경+테두리에 edit 아이콘을 둔 칩 버튼 (디자인 시안 기준).
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.glassBg)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onEditorOpenChange(true) },
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SoodalIcon(icon = SoodalIcons.Edit, tint = colors.accentBlue, size = 14.dp)
                        Text("프로필 편집", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accentBlue)
                    }
                }

                Spacer(Modifier.height(spacing.s3))

                // ── Currency Row ────────────────────────────────────
                // 동일 너비 3칩: 조개/진주/연속 (뽑기 진입은 하단 탭바 — 디자인 확정).
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CurrencyChip(
                            iconType = SoodalIcons.Shell,
                            label = "조개",
                            value = state.shells.formatNumber(),
                            color = ChipColor.Gold,
                            modifier = Modifier.weight(1f),
                        )
                        CurrencyChip(
                            iconType = SoodalIcons.Pearl,
                            label = "진주",
                            value = state.pearls.formatNumber(),
                            color = ChipColor.Purple,
                            modifier = Modifier.weight(1f),
                        )
                        CurrencyChip(
                            iconType = SoodalIcons.Fire,
                            label = "연속",
                            value = "${state.streak}일",
                            color = ChipColor.Blue,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                // ── 오늘 ────────────────────────────────────────────
                SectionLabel(
                    text = "오늘",
                    action = if (state.todayHasRecord) {
                        { Text("기록 완료 ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accentBlue) }
                    } else {
                        null
                    },
                )
                Spacer(Modifier.height(12.dp))
                TodayCard(
                    hasRecord = state.todayHasRecord,
                    distanceM = state.todayDistanceM,
                    durationMin = state.todayDurationMin,
                    kcal = state.todayKcal,
                    syncing = state.syncing,
                    onSync = { viewModel.onSync() },
                )

                // ── 이번 주 활동 ─────────────────────────────────────
                SectionLabel(
                    text = "이번 주 활동",
                    action = { TrendBadge(state.weekly.trendPercent) },
                )
                Spacer(Modifier.height(12.dp))
                WeeklyActivityCard(state.weekly, onTap = { onNavigateToTab("calendar") })

                // ── 이번 달 수영 ─────────────────────────────────────
                SectionLabel(text = "이번 달 수영")
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    StatCard(modifier = Modifier.weight(1f), label = "누적거리",
                        value = state.totalDistance.formatNumber(), unit = "m",
                        valueColor = colors.accentBlue,
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

                Spacer(Modifier.height(spacing.s4))
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
        modifier = Modifier
            .fillMaxSize()
            .padding(top = sheetTopDp.dp),
    ) {
        EditorSheet(
            state = editorState,
            vm = editorVm,
            isOpen = editorOpen,
            onApply = { editorVm.save() },
            onDismiss = {
                editorVm.resetToSaved()
                onEditorOpenChange(false)
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
    } // Box
}

/**
 * 홈 통화 칩 — 아이콘 + (라벨/값 세로). 등급색 soft 배경과 0.35 테두리 (디자인 시안 기준).
 */
@Composable
private fun CurrencyChip(
    iconType: SoodalIcons,
    label: String,
    value: String,
    color: ChipColor,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    val (bg, border, fg) = when (color) {
        ChipColor.Gold -> Triple(colors.accentGoldSoft, colors.accentGold.copy(alpha = 0.35f), colors.accentGold)
        ChipColor.Purple -> Triple(colors.accentPurpleSoft, colors.accentPurple.copy(alpha = 0.35f), colors.accentPurple)
        ChipColor.Blue -> Triple(colors.accentBlueSoft, colors.accentBlue.copy(alpha = 0.35f), colors.accentBlue)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoodalIcon(icon = iconType, tint = fg, size = 20.dp)
        Column(horizontalAlignment = Alignment.Start) {
            Text(text = label, fontSize = 10.sp, color = fg.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            Text(text = value, fontSize = 18.sp, color = fg, fontWeight = FontWeight.ExtraBold)
        }
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

/**
 * 오늘 수영 카드 — 기록이 있으면 거리/시간/칼로리 요약, 없으면 차분한 빈 상태 + 동기화 버튼.
 * (수동 입력은 v1 비활성 — DECISIONS 참조)
 */
@Composable
private fun TodayCard(
    hasRecord: Boolean,
    distanceM: Int,
    durationMin: Int,
    kcal: Int,
    syncing: Boolean,
    onSync: () -> Unit,
) {
    val colors = SoodalDesign.colors
    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        if (hasRecord) {
            Row(modifier = Modifier.fillMaxWidth()) {
                TodayMetric(Modifier.weight(1f), "거리", distanceM.formatNumber(), "m", colors.accentBlue)
                TodayMetric(Modifier.weight(1f), "시간", "$durationMin", "분", colors.textPrimary)
                TodayMetric(Modifier.weight(1f), "칼로리", kcal.formatNumber(), "kcal", colors.success)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SoodalIcon(
                    icon = if (syncing) SoodalIcons.Sync else SoodalIcons.Wave,
                    tint = if (syncing) colors.accentBlue else colors.textTertiary,
                    size = 22.dp,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (syncing) "동기화 중이에요…" else "아직 오늘 기록이 없어요",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Health Connect에서 동기화해보세요",
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                    )
                }
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentBlue.copy(alpha = 0.10f))
                        .border(1.dp, colors.accentBlue.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !syncing,
                            onClick = onSync,
                        )
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Sync, tint = colors.accentBlue, size = 15.dp)
                    Text("동기화", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.accentBlue)
                }
            }
        }
    }
}

@Composable
private fun TodayMetric(modifier: Modifier, label: String, value: String, unit: String, valueColor: Color) {
    val colors = SoodalDesign.colors
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            Text(unit, fontSize = 10.sp, color = colors.textSecondary, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
        }
    }
}
