package kr.ilf.soodalbbobgi.presentation.home

import androidx.compose.ui.zIndex
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.theme.SoodalShape
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.ChipColor
import kr.ilf.soodalbbobgi.core.ui.GlassBox
import kr.ilf.soodalbbobgi.core.ui.GlassCornerSmall
import kr.ilf.soodalbbobgi.core.ui.GlassInfoGroup
import kr.ilf.soodalbbobgi.core.ui.GlassInfoSegment
import kr.ilf.soodalbbobgi.core.ui.ProfileFrameCorner
import kr.ilf.soodalbbobgi.core.ui.GlassSheen
import kr.ilf.soodalbbobgi.core.ui.glass
import kr.ilf.soodalbbobgi.core.ui.SoodalCard
import kr.ilf.soodalbbobgi.core.ui.AppOverlay
import kr.ilf.soodalbbobgi.core.ui.ShellRewardKind
import kr.ilf.soodalbbobgi.core.ui.ShellRewardPopup
import kr.ilf.soodalbbobgi.core.ui.TabBarClearance
import kr.ilf.soodalbbobgi.core.ui.topFadeEdge
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.core.ui.motion.Motion
import kr.ilf.soodalbbobgi.presentation.calendar.StrokeEditSheet
import kr.ilf.soodalbbobgi.presentation.calendar.SwimSessionData
import kr.ilf.soodalbbobgi.presentation.common.MonthSummaryCard
import kr.ilf.soodalbbobgi.presentation.common.WeeklyActivityCard
import kr.ilf.soodalbbobgi.presentation.profile.CardLayers
import kr.ilf.soodalbbobgi.presentation.profile.EditorCardPreview
import kr.ilf.soodalbbobgi.presentation.profile.EditorSheet
import kr.ilf.soodalbbobgi.presentation.profile.ProfileCardBounds
import kr.ilf.soodalbbobgi.presentation.profile.ProfileCardComposite
import kr.ilf.soodalbbobgi.presentation.profile.ProfileEditorViewModel
import kotlinx.coroutines.delay

/**
 * 상단바 ~ 프로필 카드 간격. 아래 카드들끼리의 간격(14dp)과 같은 값으로 맞춘다.
 * 경계 페이드도 같은 길이를 써서, 페이드가 끝나는 지점에서 곧바로 카드가 시작한다.
 */
private val HomeTopGap = 14.dp

private fun Int.formatNumber(): String = String.format("%,d", this)

@Composable
fun HomeScreen(
    onNavigateToTab: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToCollection: () -> Unit,
    onOpenFullscreen: () -> Unit,
    hideCard: Boolean,
    editorOpen: Boolean,
    onEditorOpenChange: (Boolean) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val shellReward by viewModel.shellReward.collectAsState()
    val shellRewardKind by viewModel.shellRewardKind.collectAsState()
    val hcSyncing by viewModel.hcSyncing.collectAsState()
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

    // 카드 아래 지점(dp) 계산: 위패딩16 + 헤더38 = 54, + 카드 위 간격, + 카드 높이, + 카드-시트 간격 14.
    val config = LocalConfiguration.current
    // 카드는 화면폭-32dp(좌우 16 여백)에서 카드 비율(2752×1536)로 높이가 정해진다.
    val cardHeightDp = (config.screenWidthDp - 32f) * 1536f / 2752f
    val sheetTopDp = 54f + HomeTopGap.value + cardHeightDp + 14f

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
                .height(38.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 재화·스트릭을 한 패널로 묶는다 — 작은 유리 조각이 여러 개 흩어지는 대신 하나로.
            GlassInfoGroup {
                GlassInfoSegment(SoodalIcons.Shell, state.shells.formatNumber(), colors.accentGold)
                GlassInfoSegment(SoodalIcons.Pearl, state.pearls.formatNumber(), colors.accentPurple)
                // 연속 스트릭: 값이 있을 때만 등장 — 패널 폭이 가로로 늘고 줄어든다.
                AnimatedVisibility(
                    visible = state.streak > 0,
                    enter = fadeIn() + expandHorizontally(clip = false),
                    exit = fadeOut() + shrinkHorizontally(clip = false),
                ) {
                    GlassInfoSegment(SoodalIcons.Fire, "${state.streak}일", colors.accentBlue)
                }
            }
            // 액션 버튼은 묶지 않는다 — 서로 다른 동작이라 각자 눌리는 낱개 버튼으로 보여야 한다.
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
        // 최초 HC 가져오기 진행 표시 — 온보딩이 시작한 동기화가 끝날 때까지 상단 중앙에 뜬다.
        androidx.compose.animation.AnimatedVisibility(
            visible = hcSyncing,
            modifier = Modifier.align(Alignment.TopCenter).zIndex(2f).padding(top = 6.dp),
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut(),
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.surface1)
                    .border(1.dp, colors.glassBorder, RoundedCornerShape(999.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = colors.accentBlue,
                )
                Text("수영 기록 가져오는 중…", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 설정 화면 목록과 같은 마스크. 길이는 카드 간격에 맞춘다 — 여백보다 길면
                // 정지 상태에서 카드 윗변이 마스크에 걸린다.
                .topFadeEdge(HomeTopGap)
                .verticalScroll(homeScrollState)
                .padding(horizontal = spacing.s4),
        ) {
            // 상단바 ~ 프로필 카드 간격 — 경계 페이드가 이 안에서 끝난다.
            Spacer(Modifier.height(HomeTopGap))

            // ── Profile Card ────────────────────────────────────
            // 합성 카드(저장값)는 항상 아래에 깔아두고, 편집 중에는 GPU 프리뷰(편집값)를 위에
            // 겹친다. 컴포저블을 통째로 교체하면 새로 마운트된 쪽이 준비될 때까지 빈 프레임이
            // 보이므로(깜빡임), 프리뷰는 시트가 닫혀도 합성이 최신 저장값을 그려낼 때까지 유지한다.
            // (테두리 레이어는 보류 — frame 에셋을 전달하지 않는다)
            val editLayers = CardLayers(
                nickname = editorState.nickname,
                tagline = editorState.customText.ifEmpty { editorState.taglineFallback },
                stats = editorState.statsText,
                charX = editorState.charX,
                charY = editorState.charY,
                charScale = editorState.charScale,
                showShadow = editorState.charShadow,
                nicknameStyle = editorState.nicknameEl.style,
                taglineStyle = editorState.taglineEl.style,
                statsStyle = editorState.statsEl.style,
                nicknameOutline = editorState.nicknameEl.outline,
                taglineOutline = editorState.taglineEl.outline,
                statsOutline = editorState.statsEl.outline,
                nicknameOutlineColor = editorState.nicknameEl.outlineColor,
                taglineOutlineColor = editorState.taglineEl.outlineColor,
                statsOutlineColor = editorState.statsEl.outlineColor,
                showNickname = editorState.nicknameEl.show,
                nicknameX = editorState.nicknameEl.x, nicknameY = editorState.nicknameEl.y,
                nicknameScaleStep = editorState.nicknameEl.scaleStep,
                showTagline = editorState.taglineEl.show,
                taglineX = editorState.taglineEl.x, taglineY = editorState.taglineEl.y,
                taglineScaleStep = editorState.taglineEl.scaleStep,
                showStats = editorState.statsEl.show,
                statsX = editorState.statsEl.x, statsY = editorState.statsEl.y,
                statsScaleStep = editorState.statsEl.scaleStep,
                nicknamePill = editorState.nicknameEl.pill,
                taglinePill = editorState.taglineEl.pill,
                statsPill = editorState.statsEl.pill,
                nicknameColor = editorState.nicknameEl.color,
                taglineColor = editorState.taglineEl.color,
                statsColor = editorState.statsEl.color,
            )
            val editBgAsset = editorState.bgItems.firstOrNull { it.isSelected }?.imageAsset
            val editCharAsset = editorState.charItems.firstOrNull { it.isSelected }?.imageAsset

            val saved = state.card
            val savedLayers = CardLayers(
                nickname = state.cardNickname,
                tagline = state.cardTagline,
                stats = state.cardStats,
                charX = state.cardCharX,
                charY = state.cardCharY,
                charScale = state.cardCharScale,
                showShadow = saved?.characterShadow ?: true,
                nicknameStyle = saved?.nicknameStyle ?: "REGULAR",
                taglineStyle = saved?.taglineStyle ?: "REGULAR",
                statsStyle = saved?.statsStyle ?: "REGULAR",
                nicknameOutline = saved?.nicknameOutline ?: false,
                taglineOutline = saved?.taglineOutline ?: false,
                statsOutline = saved?.statsOutline ?: false,
                nicknameOutlineColor = saved?.nicknameOutlineColor,
                taglineOutlineColor = saved?.taglineOutlineColor,
                statsOutlineColor = saved?.statsOutlineColor,
                showNickname = saved?.showNickname ?: true,
                nicknameX = saved?.nicknameX ?: 0.83f, nicknameY = saved?.nicknameY ?: 0.40f,
                nicknameScaleStep = saved?.nicknameScaleStep ?: 3,
                showTagline = saved?.showTagline ?: true,
                taglineX = saved?.taglineX ?: 0.83f, taglineY = saved?.taglineY ?: 0.57f,
                taglineScaleStep = saved?.taglineScaleStep ?: 3,
                showStats = saved?.showStats ?: true,
                statsX = saved?.statsX ?: 0.16f, statsY = saved?.statsY ?: 0.90f,
                statsScaleStep = saved?.statsScaleStep ?: 3,
                nicknamePill = saved?.nicknamePill ?: "WHITE",
                taglinePill = saved?.taglinePill ?: "NONE",
                statsPill = saved?.statsPill ?: "BLUR",
                nicknameColor = saved?.nicknameColor ?: "#FFFFFF",
                taglineColor = saved?.taglineColor ?: "#FFFFFF",
                statsColor = saved?.statsColor ?: "#00F5FF",
            )

            // 합성 카드가 '현재 저장값'을 실제로 그리고 있는지 — 프리뷰를 내릴 타이밍의 근거.
            var compositeUpToDate by remember { mutableStateOf(false) }
            // 편집 프리뷰 표시 여부: 열리면 즉시 켜고, 닫힌 뒤에는 합성이 프리뷰와 같은 값을
            // 그려낼 때까지 유지한다 — 저장 직후 저장값 전파가 늦어도 옛 카드가 비치지 않는다.
            var showEditorPreview by remember { mutableStateOf(false) }
            val layersMatch = savedLayers == editLayers &&
                state.cardBgAsset == editBgAsset && state.cardCharAsset == editCharAsset
            LaunchedEffect(editorOpen, compositeUpToDate, layersMatch) {
                when {
                    editorOpen -> showEditorPreview = true
                    !showEditorPreview -> {}
                    compositeUpToDate && layersMatch -> showEditorPreview = false
                    else -> {
                        // 저장값 파생 텍스트가 프리뷰와 끝내 일치하지 않는 예외 상황에서도
                        // 프리뷰가 눌러앉지 않도록, 유예 후에는 합성 준비만 확인하고 내린다.
                        delay(600)
                        if (compositeUpToDate) showEditorPreview = false
                    }
                }
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
                        // 카드는 아트웍이라 누름 스크림을 얹지 않는다 — 탭하면 전체보기 전환 자체가 피드백.
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = !editorOpen,
                            onClick = onOpenFullscreen,
                        ),
                ) {
                    // 합성 카드(저장값) — 항상 마운트해 둔다. produceState가 직전 비트맵을
                    // 유지하므로 저장값이 바뀌어도 빈 프레임 없이 새 카드로 이어진다.
                    ProfileCardComposite(
                        layers = savedLayers,
                        bgAsset = state.cardBgAsset,
                        charAsset = state.cardCharAsset,
                        modifier = Modifier.fillMaxWidth(),
                        onUpToDateChange = { compositeUpToDate = it },
                    )
                    if (showEditorPreview) {
                        // 편집 중엔 비트맵 재합성 없이 GPU 레이어(오버레이)로 그린다 — 슬라이더가
                        // 프레임 단위로 따라온다. (BLUR 칩만 프로스트 폴백, 저장하면 진짜 블러)
                        EditorCardPreview(
                            layers = editLayers,
                            bgAsset = editBgAsset,
                            charAsset = editCharAsset,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            // (통화·편집·연속은 상단 고정 바로 이동, 크게 보기는 카드 탭으로 대체됨)

                // ── 내 컬렉션 (카테고리별 보유/전체) — 탭하면 컬렉션 화면으로 ──
                Spacer(Modifier.height(14.dp))
                DexCollectionCard(
                    charOwned = state.dexCharOwned, charTotal = state.dexCharTotal,
                    bgOwned = state.dexBgOwned, bgTotal = state.dexBgTotal,
                    frameOwned = state.dexFrameOwned, frameTotal = state.dexFrameTotal,
                    onClick = onNavigateToCollection,
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
        kr.ilf.soodalbbobgi.core.ui.SyncLoadingOverlay("수영 기록 동기화 중이에요...")
    }

    // ── 동기화 에러 표시 ─────────────────────────────────────
    if (syncError != null) {
        LaunchedEffect(syncError) {
            android.widget.Toast.makeText(context, syncError, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearSyncError()
        }
    }

    // ── 조개 획득 팝업 (오버레이 레이어로 호이스팅 — 패널이 뒤 콘텐츠를 진짜 블러) ──
    // 영법 수정 시트가 열려 있는 동안은 숨긴다 — 시트 뒤에 팝업이 비쳐 보일 이유가 없다.
    if (shellReward > 0 && editToday == null) {
        AppOverlay {
            ShellRewardPopup(
                shellCount = shellReward,
                kind = shellRewardKind,
                distanceM = state.todayDistanceM.takeIf { it > 0 },
                durationMin = state.todayDurationMin.takeIf { it > 0 },
                // 영법을 아직 안 채운 오늘 세션이 있을 때만 유도한다 — 이미 채웠으면
                // 더 받을 게 없고, 영법 보너스로 뜬 팝업에서 또 권하면 무한 루프처럼 보인다.
                onEditStrokes = state.todaySessions
                    .lastOrNull { it.strokesUnset }
                    ?.takeIf { shellRewardKind == ShellRewardKind.SwimRecord }
                    ?.let { s -> { viewModel.clearShellReward(); editToday = s } },
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

/** 상단바 글래스 아이콘 버튼 (직접 기록/편집/설정) — 공용 글래스(sheen 포함). */
@Composable
private fun GlassIconButton(icon: SoodalIcons, tint: Color, onClick: () -> Unit) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(GlassCornerSmall)
    Box(
        modifier = Modifier
            .size(38.dp)
            .glass(colors, GlassCornerSmall, shape)
            .pressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        SoodalIcon(icon = icon, tint = tint, size = 18.dp)
        GlassSheen(shape)
    }
}

/** 내 컬렉션 카드 — 카테고리별(캐릭터/배경/액자) 보유/전체 진행바. 탭하면 컬렉션 화면으로. */
@Composable
private fun DexCollectionCard(
    charOwned: Int, charTotal: Int,
    bgOwned: Int, bgTotal: Int,
    frameOwned: Int, frameTotal: Int,
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val totalOwned = charOwned + bgOwned + frameOwned
    val totalAll = charTotal + bgTotal + frameTotal
    SoodalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("내 컬렉션", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$totalOwned", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentBlue)
                    Text(" / $totalAll", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textTertiary)
                    Text(" ›", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textTertiary)
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
    SoodalCard(modifier = modifier, onClick = onClick) {
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
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
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
