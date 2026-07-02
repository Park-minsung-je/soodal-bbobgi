package com.soodalbbobgi.app.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.soodalbbobgi.app.core.ui.GlassBox
import com.soodalbbobgi.app.core.ui.GlassCornerSmall
import com.soodalbbobgi.app.core.ui.ProfileFrameCorner
import com.soodalbbobgi.app.core.ui.GlassCurrencyChip
import com.soodalbbobgi.app.core.ui.GlassSheen
import com.soodalbbobgi.app.core.ui.glass
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.AppOverlay
import com.soodalbbobgi.app.core.ui.ShellRewardPopup
import com.soodalbbobgi.app.core.ui.TabBarClearance
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.motion.Motion
import com.soodalbbobgi.app.presentation.calendar.StrokeEditSheet
import com.soodalbbobgi.app.presentation.calendar.SwimSessionData
import com.soodalbbobgi.app.presentation.common.MonthSummaryCard
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

    // 오늘 기록 영법 수정 시트 대상 세션 (null이면 닫힘).
    var editToday by remember { mutableStateOf<SwimSessionData?>(null) }

    // 수동 입력 시트 열림 여부.
    var manualOpen by remember { mutableStateOf(false) }

    // 홈 스크롤 상태 — 편집 시트가 열리면 최상단(카드 위치)으로 올린다.
    val homeScrollState = rememberScrollState()
    LaunchedEffect(editorOpen) {
        if (editorOpen) homeScrollState.animateScrollTo(0)
    }

    // 카드 아래 지점(dp) 계산: 위패딩16 + 헤더46 + 간격16 = 78, + 카드 높이, + 카드-시트 간격 8.
    val config = LocalConfiguration.current
    // 카드는 화면폭-32dp(좌우 16 여백)에서 카드 비율(2752×1536)로 높이가 정해진다.
    val cardHeightDp = (config.screenWidthDp - 32f) * 1536f / 2752f
    val sheetTopDp = 78f + cardHeightDp + 8f

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

    // 헤더가 고정인 화면 — 루트에서 상태바 인셋 처리 (콘텐츠는 헤더 경계에서 페이드).
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // ── 상단 고정 바 ─────────────────────────────────────
        // 통화 칩(조개·진주) + 조건부 연속 스트릭 · 우측 편집/설정 (디자인 v3.0 상단바).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.s4)
                .padding(top = spacing.s4)
                .height(46.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GlassCurrencyChip(SoodalIcons.Shell, state.shells.formatNumber(), colors.accentGold)
                GlassCurrencyChip(SoodalIcons.Pearl, state.pearls.formatNumber(), colors.accentPurple)
                // 연속 스트릭: 값이 있을 때만 등장(애니메이션과 함께).
                AnimatedVisibility(
                    visible = state.streak > 0,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 },
                ) {
                    TopStreakChip(state.streak)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 직접 기록(+) — 동기화(↻)는 상단 과밀 해소를 위해 캘린더 헤더로 이동.
                GlassIconButton(SoodalIcons.Plus, colors.accentBlue) { manualOpen = true }
                GlassIconButton(SoodalIcons.Edit, colors.accentBlue) { onEditorOpenChange(true) }
                GlassIconButton(SoodalIcons.Settings, colors.textSecondary, onNavigateToSettings)
            }
        }

        // 하단 스크롤: 프로필 카드 + 통화 + 오늘 + 최근 7일 + 이번 달
        Box(Modifier.weight(1f)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(homeScrollState)
                .padding(horizontal = spacing.s4),
        ) {
            // 상단바 ~ 프로필 카드 간격 (App Canvas ≈ 10px)
            Spacer(Modifier.height(10.dp))

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
                    showText = editorState.showText,
                    nicknamePill = editorState.nicknamePill,
                    taglinePill = editorState.taglinePill,
                    statsPill = editorState.statsPill,
                    nicknameColor = editorState.nicknameColor,
                    taglineColor = editorState.taglineColor,
                    statsColor = editorState.statsColor,
                    textOutline = editorState.textOutline,
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
                    showText = state.cardShowText,
                    nicknamePill = state.cardNicknamePill,
                    taglinePill = state.cardTaglinePill,
                    statsPill = state.cardStatsPill,
                    nicknameColor = state.cardNicknameColor,
                    taglineColor = state.cardTaglineColor,
                    statsColor = state.cardStatsColor,
                    textOutline = state.cardTextOutline,
                )
                cardBgAsset = state.cardBgAsset
                cardCharAsset = state.cardCharAsset
                cardFrameAsset = state.cardFrameAsset
            }

            // 프로필 카드 — 공용 GlassBox 프레임(sheen·코너·그림자 통일). 내부 카드가 실제 아트.
            GlassBox(
                modifier = Modifier.fillMaxWidth(),
                cornerDp = ProfileFrameCorner,
                contentPadding = 6.dp,
            ) {
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
                        .clip(RoundedCornerShape(ProfileFrameCorner - 6.dp))
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
            }

            // (통화·편집·연속은 상단 고정 바로 이동, 크게 보기는 카드 탭으로 대체됨)

                // ── 도감 컬렉션 (카테고리별 보유/전체) — 정보 표시 전용 ──
                Spacer(Modifier.height(14.dp))
                DexCollectionCard(
                    charOwned = state.dexCharOwned, charTotal = state.dexCharTotal,
                    bgOwned = state.dexBgOwned, bgTotal = state.dexBgTotal,
                    frameOwned = state.dexFrameOwned, frameTotal = state.dexFrameTotal,
                )

                // ── 오늘 — 기록이 생길 때만 애니메이션과 함께 나타난다 (빈 상태 카드 없음).
                // expandVertically(clip=false)라 카드 그림자가 전환 중에도 잘리지 않는다.
                AnimatedVisibility(
                    visible = state.todayHasRecord,
                    enter = fadeIn(tween(250)) +
                        expandVertically(spring(dampingRatio = 0.8f, stiffness = 380f), clip = false),
                ) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        TodayCard(
                            distanceM = state.todayDistanceM,
                            durationMin = state.todayDurationMin,
                            kcal = state.todayKcal,
                            avgHr = state.todayAvgHr,
                            onClick = { onNavigateToTab("calendar") },
                        )
                    }
                }

                // ── 최근 7일 활동 ─────────────────────────────────────
                Spacer(Modifier.height(14.dp))
                WeeklyActivityCard(state.weekly, trendPercent = state.weekly.trendPercent)

                // ── 이번 달 수영 (문장형) ─────────────────────────────
                Spacer(Modifier.height(14.dp))
                MonthSummaryCard(
                    monthLabel = "${java.time.YearMonth.now().monthValue}월",
                    subjectLabel = "이번 달",
                    distanceM = state.totalDistance,
                    sessions = state.swimSessions,
                    kcal = state.totalKcal,
                    lastMonthDistance = state.lastMonthDistance,
                    lastMonthSessions = state.lastMonthSessions,
                    topStroke = state.topStroke,
                )

                Spacer(Modifier.height(TabBarClearance))
        }

        // (헤더 경계 페이드 제거 — 통일 배경 위 고정 상단바/콘텐츠 경계 이음매 방지)
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

    // ── 조개 획득 팝업 (오버레이 레이어로 호이스팅 — 패널이 뒤 콘텐츠를 진짜 블러) ──
    if (shellReward > 0) {
        AppOverlay {
            ShellRewardPopup(
                shellCount = shellReward,
                distanceM = state.todayDistanceM.takeIf { it > 0 },
                durationMin = state.todayDurationMin.takeIf { it > 0 },
                onEditStrokes = state.todaySessions.lastOrNull()?.let { s ->
                    { viewModel.clearShellReward(); editToday = s }
                },
                onDismiss = { viewModel.clearShellReward() },
            )
        }
    }

    // ── 수동 입력 시트 (오버레이 레이어) ─────────────────────
    if (manualOpen) {
        AppOverlay {
            ManualEntrySheet(
                onDismiss = { manualOpen = false },
                onSubmit = { input ->
                    manualOpen = false
                    viewModel.onManualRegister(input)
                },
            )
        }
    }

    // ── 오늘 기록 영법 수정 시트 ─────────────────────────────
    editToday?.let { session ->
        AppOverlay {
            StrokeEditSheet(
                dateLabel = "오늘",
                data = session,
                onDismiss = { editToday = null },
                onSave = { free, breast, back, fly, kick, mixed ->
                    viewModel.saveStrokes(session.logId, free, breast, back, fly, kick, mixed)
                    editToday = null
                },
            )
        }
    }

    // -- 프로필 편집 시트 (오버레이 레이어 — 시트 표면이 뒤 홈 콘텐츠를 진짜 블러) --
    // 차단막과 시트를 한 오버레이로 묶어 차단막이 시트 '아래'에 깔리게 한다 (분리하면 터치를 먹는다).
    AppOverlay {
        if (editorOpen) {
            // 시트 밖 영역 터치 차단 (딤 없는 투명 차단막)
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            )
        }
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
                // 오버레이 레이어엔 상태바 패딩이 없으므로 여기서 직접 적용해 홈 좌표계와 맞춘다.
                .statusBarsPadding()
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
    }
    } // Box
}

/** 상단바 연속 스트릭 칩 — 통화 칩과 동일한 글래스(sheen 포함) 스타일. 값>0일 때만 조건부 등장. */
@Composable
private fun TopStreakChip(streak: Int) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(GlassCornerSmall)
    Box(
        modifier = Modifier.height(38.dp).glass(colors, GlassCornerSmall, shape),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            SoodalIcon(icon = SoodalIcons.Fire, tint = colors.accentBlue, size = 15.dp)
            Text("${streak}일", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentBlue)
        }
        GlassSheen(shape)
    }
}

/** 상단바 글래스 아이콘 버튼 (편집/설정) — 공용 글래스(sheen 포함). */
@Composable
private fun GlassIconButton(icon: SoodalIcons, tint: Color, onClick: () -> Unit) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(GlassCornerSmall)
    Box(
        modifier = Modifier
            .size(38.dp)
            .glass(colors, GlassCornerSmall, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SoodalIcon(icon = icon, tint = tint, size = 18.dp)
        GlassSheen(shape)
    }
}

/** 도감 컬렉션 카드 — 카테고리별(캐릭터/배경/액자) 보유/전체 진행바. */
@Composable
private fun DexCollectionCard(
    charOwned: Int, charTotal: Int,
    bgOwned: Int, bgTotal: Int,
    frameOwned: Int, frameTotal: Int,
) {
    val colors = SoodalDesign.colors
    val totalOwned = charOwned + bgOwned + frameOwned
    val totalAll = charTotal + bgTotal + frameTotal
    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("도감 컬렉션", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$totalOwned", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentBlue)
                    Text(" / $totalAll", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textTertiary)
                }
            }
            Spacer(Modifier.height(12.dp))
            // 바 채움은 밝은 surface 색(App Canvas: --sky/--mint/--sun), 값 텍스트는 진한 ink 색.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DexBar(Modifier.weight(1f), "캐릭터", charOwned, charTotal, fill = Color(0xFF6FC0EC), ink = colors.accentBlue)
                DexBar(Modifier.weight(1f), "배경", bgOwned, bgTotal, fill = Color(0xFF5FD7AE), ink = colors.success)
                DexBar(Modifier.weight(1f), "액자", frameOwned, frameTotal, fill = Color(0xFFF6C95B), ink = colors.accentGold)
            }
        }
    }
}

@Composable
private fun DexBar(modifier: Modifier, label: String, owned: Int, total: Int, fill: Color, ink: Color) {
    val colors = SoodalDesign.colors
    val frac = if (total > 0) owned.toFloat() / total else 0f
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Text("$owned/$total", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = ink)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF273848).copy(alpha = 0.10f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(frac.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(fill),
            )
        }
    }
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
    // borderless 원칙 — 칩은 채움색만으로 구분한다.
    val (bg, fg) = when (color) {
        ChipColor.Gold -> colors.accentGoldSoft to colors.accentGold
        ChipColor.Purple -> colors.accentPurpleSoft to colors.accentPurple
        ChipColor.Blue -> colors.accentBlueSoft to colors.accentBlue
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
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
 * 오늘 수영 카드 — 거리/칼로리/평균 심박/시간 요약. 탭하면 캘린더로 이동한다.
 * 기록이 없는 날은 호출부에서 카드 자체를 띄우지 않는다.
 */
@Composable
private fun TodayCard(
    distanceM: Int,
    durationMin: Int,
    kcal: Int,
    avgHr: Int?,
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    SoodalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TodayMetric(Modifier.weight(1f), "거리", distanceM.formatNumber(), "m", colors.accentBlue)
            TodayMetric(Modifier.weight(1f), "칼로리", kcal.formatNumber(), "kcal", colors.success)
            TodayMetric(Modifier.weight(1f), "평균 심박", avgHr?.toString() ?: "—", if (avgHr != null) "bpm" else "", Color(0xFFF43F5E))
            TodayMetric(Modifier.weight(1f), "시간", "$durationMin", "분", colors.textPrimary)
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
