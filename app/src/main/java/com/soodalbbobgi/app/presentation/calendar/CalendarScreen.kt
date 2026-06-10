package com.soodalbbobgi.app.presentation.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.TabBarClearance
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.util.averageHr
import com.soodalbbobgi.app.core.theme.StrokePalette
import com.soodalbbobgi.app.presentation.common.WeeklyActivityCard
import com.soodalbbobgi.app.presentation.common.strokeTextColorOf
import java.time.LocalDate
import java.time.YearMonth

// 주말 요일 색 — 흐릿하다는 피드백으로 디자인 원안(#FF9B9B/#9BC4FF)보다 한 단계 진하게.
private val SundayColor = Color(0xFFFF7A7A)
private val SaturdayColor = Color(0xFF6FA8FF)

// 범례 — 막대 그래프에 나오는 6개 영법 전부.
private val LEGEND_STROKES = listOf(
    "자유형" to StrokePalette.Free,
    "평영" to StrokePalette.Breast,
    "배영" to StrokePalette.Back,
    "접영" to StrokePalette.Fly,
    "킥판" to StrokePalette.Kick,
    "혼영" to StrokePalette.Medley,
)

// 막대 그래프 세그먼트 순서 — 디자인 확정: 혼영/자유형/평영/배영/접영/킥판.
private val BAR_COLORS = listOf(
    StrokePalette.Medley, StrokePalette.Free, StrokePalette.Breast,
    StrokePalette.Back, StrokePalette.Fly, StrokePalette.Kick,
)

/** 막대 그래프 순서(BAR_COLORS와 동일)로 영법 거리(m)를 늘어놓는다. */
private fun barMeters(d: SwimDayData) = listOf(d.mixedM, d.freeM, d.breastM, d.backM, d.flyM, d.kickM)

/** 세션 단위 막대 그래프용 — 순서는 [barMeters]와 동일. */
private fun barMeters(s: SwimSessionData) = listOf(s.mixedM, s.freeM, s.breastM, s.backM, s.flyM, s.kickM)

private val monthNames = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월")

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val weekly by viewModel.weeklyActivity.collectAsState()
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    // 영법 비율 수정 시트 — 어느 날의 어느 세션을 수정 중인지.
    var editTarget by remember { mutableStateOf<Pair<Int, SwimSessionData>?>(null) }
    // 심박 그래프 펼침 상태 — 세션/날짜 간 공유(한 번 열면 다른 기록으로 넘어가도 유지).
    var hrChartExpanded by remember { mutableStateOf(false) }
    // 선택한 날이 바뀌면 열린 수정 시트는 닫는다.
    LaunchedEffect(state.selectedDay) { editTarget = null }

    // ── 접히는 달력 (collapsing header + nested scroll) ─────────────
    // 달력은 상단 고정이고 아래 콘텐츠만 스크롤된다. 스크롤 오프셋을 달력이 1:1로 받아
    // 셀이 실시간으로 얇아지며(0=정사각 셀 → 1=날짜+그래프 간단 셀), 손을 떼면
    // 절반 임계 기준으로 어느 한쪽 크기로 스냅한다. 맨 위로 돌아오면 다시 펼쳐진다.
    val density = LocalDensity.current
    val contentScroll = rememberScrollState()
    val compactCellPx = with(density) { 32.dp.toPx() }
    // 셀 풀 높이(=셀 너비): (화면폭 - 좌우 패딩 - 셀 간격 4dp×6) / 7
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val fullCellPx = with(density) { ((screenWidthDp - spacing.s4 * 2 - 4.dp * 6) / 7f).toPx() }
    val weeks = remember(state.year, state.month) {
        buildMonthCells(state.year, state.month).chunked(7).count { w -> w.any { it.inMonth } }
    }
    val maxCollapsePx = ((fullCellPx - compactCellPx) * weeks).coerceAtLeast(1f)
    var collapsePx by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(maxCollapsePx) { collapsePx = collapsePx.coerceIn(0f, maxCollapsePx) }
    val collapseFraction = (collapsePx / maxCollapsePx).coerceIn(0f, 1f)
    val cellHeight = with(density) { (fullCellPx - (maxCollapsePx / weeks) * collapseFraction).toDp() }

    val nestedConnection = remember(maxCollapsePx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 가속도(플링) 미적용 정책 — 손가락 드래그만 달력 접힘에 반영한다.
                // 플링 델타가 들어오면 스냅 직후 헤더가 다시 움직이며 흔들리는 원인이 된다.
                if (source != NestedScrollSource.Drag) return Offset.Zero
                val dy = available.y
                // 위로 스크롤: 콘텐츠보다 달력을 먼저 접는다
                if (dy < 0 && collapsePx < maxCollapsePx) {
                    val consumed = minOf(maxCollapsePx - collapsePx, -dy)
                    collapsePx += consumed
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.Drag) return Offset.Zero
                // 아래로 스크롤: 콘텐츠가 맨 위에 닿아 다 못 쓴 잔여 델타로 달력을 펼친다.
                // 같은 드래그 안에서 "콘텐츠 끝까지 → 이어서 달력 펼침"이 끊기지 않는다.
                val dy = available.y
                if (dy > 0 && collapsePx > 0f) {
                    val used = minOf(collapsePx, dy)
                    collapsePx -= used
                    return Offset(0f, used)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // 임계는 무조건 거리(절반) 기준 — 속도는 보지 않고 한쪽 크기로 스냅
                if (collapsePx > 0f && collapsePx < maxCollapsePx) {
                    val target = if (collapsePx > maxCollapsePx / 2f) maxCollapsePx else 0f
                    val expanding = target == 0f
                    animate(collapsePx, target, animationSpec = tween(200)) { v, _ -> collapsePx = v }
                    // 펼침 스냅이면 남은 플링을 먹어 콘텐츠가 더 튀지 않게 한다
                    if (expanding) return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bgDeep)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // 헤더·달력이 고정인 화면 — 루트에서 상태바 인셋 처리.
                .statusBarsPadding()
                .nestedScroll(nestedConnection),
        ) {
            // ── 고정: 헤더 + 요일 + 달력 (스크롤 오프셋에 따라 접힘) ──
            // 달력↔범례 사이 12dp를 반씩 분담: 고정부 하단 6dp + 범례 위 6dp(스크롤 쪽).
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4)
                    .padding(top = spacing.s4, bottom = 6.dp),
            ) {
            // ── 헤더: 제목 + 이번 달 인라인 통계 ──────────────
            CalendarHeader(
                year = state.year,
                month = state.month,
                swimData = state.swimData,
            )

            Spacer(Modifier.height(18.dp))

            // ── 요일 헤더 (일요일 시작) ─────────────────────
            DayHeaderRow()

            Spacer(Modifier.height(6.dp))

            // ── 그리드 (좌우 스와이프로 월 이동, 방향에 맞춰 슬라이드) ─────
            val currentYm = YearMonth.of(state.year, state.month)
            // 나가는 달의 그리드가 자기 달 데이터로 그려지도록 월별 데이터를 보관한다.
            val gridData = remember { HashMap<YearMonth, Map<Int, SwimDayData>>() }
            gridData[currentYm] = state.swimData
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {
                        var dragTotal = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragTotal = 0f },
                            onDragEnd = {
                                val threshold = 56.dp.toPx()
                                if (dragTotal > threshold) viewModel.previousMonth()
                                else if (dragTotal < -threshold) viewModel.nextMonth()
                            },
                        ) { _, dragAmount -> dragTotal += dragAmount }
                    },
            ) {
                AnimatedContent(
                    targetState = currentYm,
                    transitionSpec = {
                        // 다음 달은 오른쪽에서, 이전 달은 왼쪽에서 밀려 들어온다.
                        val forward = targetState > initialState
                        (slideInHorizontally { w -> if (forward) w else -w } + fadeIn()) togetherWith
                            (slideOutHorizontally { w -> if (forward) -w else w } + fadeOut())
                    },
                    label = "calendarMonth",
                ) { ym ->
                    CalendarGrid(
                        year = ym.year,
                        month = ym.monthValue,
                        selectedDay = if (ym == currentYm) state.selectedDay else null,
                        swimData = gridData[ym] ?: emptyMap(),
                        cellHeight = cellHeight,
                        collapseFraction = collapseFraction,
                        onSelect = viewModel::selectDay,
                    )
                }
            }
            }

            // ── 스크롤: 범례 + 선택한 날 + 주간 + 월간 ─────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(contentScroll)
                    .padding(horizontal = spacing.s4),
            ) {
            // ── 영법 범례 (위 12dp 중 나머지 절반) ──────────────────
            Spacer(Modifier.height(6.dp))
            StrokeLegend()

            // ── 선택한 날 상세 (소제목 없이 카드 자체로 구분) ──────
            Spacer(Modifier.height(14.dp))
            DayDetailCard(
                year = state.year,
                month = state.month,
                day = state.selectedDay,
                data = state.selectedDay?.let { state.swimData[it] },
                onEdit = { session -> state.selectedDay?.let { editTarget = it to session } },
                chartExpanded = hrChartExpanded,
                onToggleChart = { hrChartExpanded = !hrChartExpanded },
            )

            // ── 최근 7일 활동 (홈과 동일 — 트렌드 내장 자체 라벨 카드) ──
            Spacer(Modifier.height(14.dp))
            WeeklyActivityCard(weekly, trendPercent = weekly.trendPercent)

            // ── 선택한 달 영법별 기록 (도넛 차트) ──────────────────
            Spacer(Modifier.height(14.dp))
            val isCurrentMonth = YearMonth.of(state.year, state.month) == YearMonth.now()
            MonthStrokeDonutCard(
                monthLabel = "${state.month}월",
                subjectLabel = if (isCurrentMonth) "이번 달" else "${state.month}월",
                strokeMeters = listOf(
                    Triple("자유형", state.swimData.values.sumOf { it.freeM }, StrokePalette.Free),
                    Triple("평영", state.swimData.values.sumOf { it.breastM }, StrokePalette.Breast),
                    Triple("배영", state.swimData.values.sumOf { it.backM }, StrokePalette.Back),
                    Triple("접영", state.swimData.values.sumOf { it.flyM }, StrokePalette.Fly),
                    Triple("킥판", state.swimData.values.sumOf { it.kickM }, StrokePalette.Kick),
                    Triple("혼영", state.swimData.values.sumOf { it.mixedM }, StrokePalette.Medley),
                ),
                totalDistanceM = state.swimData.values.sumOf { it.distanceM },
                sessions = state.swimData.values.sumOf { it.sessions.size },
            )

            Spacer(Modifier.height(TabBarClearance))
            }
        }

        // ── 기록 수정 바텀시트 (세션 단위) ──────────────────
        val target = editTarget
        if (target != null) {
            val (day, session) = target
            StrokeEditSheet(
                dateLabel = "${state.year}년 ${monthNames[state.month - 1]} ${day}일",
                data = session,
                onDismiss = { editTarget = null },
                onSave = { free, breast, back, fly, kick, mixed ->
                    viewModel.saveStrokes(day, session.logId, free, breast, back, fly, kick, mixed)
                    editTarget = null
                },
            )
        }
    }
}

// ── 헤더 ─────────────────────────────────────────────────────────
@Composable
private fun CalendarHeader(year: Int, month: Int, swimData: Map<Int, SwimDayData>) {
    val colors = SoodalDesign.colors
    val swimDays = swimData.size
    val totalShells = swimData.values.sumOf { it.shellReward }
    val totalKm = swimData.values.sumOf { it.distanceM } / 1000f

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${year}년 ${monthNames[month - 1]}",
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = colors.textPrimary,
            letterSpacing = (-0.01).sp,
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            HeaderStat(label = "수영", value = "$swimDays", unit = "일", color = colors.accentBlue, first = true)
            HeaderStat(label = "조개", value = "$totalShells", unit = "개", color = colors.accentGold)
            HeaderStat(label = "거리", value = String.format("%.1f", totalKm), unit = "km", color = colors.accentPurple)
        }
    }
}

@Composable
private fun HeaderStat(label: String, value: String, unit: String, color: Color, first: Boolean = false) {
    val colors = SoodalDesign.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!first) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .width(1.dp)
                    .height(12.dp)
                    .background(colors.glassBorder),
            )
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(label, fontSize = 9.sp, color = colors.textTertiary, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 13.sp, color = color, fontWeight = FontWeight.ExtraBold)
            Text(unit, fontSize = 8.5.sp, color = colors.textTertiary)
        }
    }
}

// ── 요일 헤더 ────────────────────────────────────────────────────
@Composable
private fun DayHeaderRow() {
    val colors = SoodalDesign.colors
    val labels = listOf("일", "월", "화", "수", "목", "금", "토")
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, label ->
            val tint = when (i) {
                0 -> SundayColor
                6 -> SaturdayColor
                else -> colors.textTertiary
            }
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

// ── 그리드 ───────────────────────────────────────────────────────
@Composable
private fun CalendarGrid(
    year: Int,
    month: Int,
    selectedDay: Int?,
    swimData: Map<Int, SwimDayData>,
    cellHeight: Dp,
    collapseFraction: Float,
    onSelect: (Int) -> Unit,
) {
    val cells = remember(year, month) { buildMonthCells(year, month) }
    val today = LocalDate.now()
    val isCurrentMonth = year == today.year && month == today.monthValue

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // 모든 셀이 당월 밖인 마지막 주는 건너뛴다.
        for (week in 0 until 6) {
            val weekCells = cells.subList(week * 7, week * 7 + 7)
            if (weekCells.all { !it.inMonth }) continue
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                weekCells.forEachIndexed { dow, cell ->
                    Box(modifier = Modifier.weight(1f)) {
                        DayCell(
                            day = cell.day,
                            inMonth = cell.inMonth,
                            data = if (cell.inMonth) swimData[cell.day] else null,
                            isSelected = cell.inMonth && cell.day == selectedDay,
                            isToday = cell.inMonth && isCurrentMonth && cell.day == today.dayOfMonth,
                            dow = dow,
                            cellHeight = cellHeight,
                            collapseFraction = collapseFraction,
                            onClick = { if (cell.inMonth) onSelect(cell.day) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    inMonth: Boolean,
    data: SwimDayData?,
    isSelected: Boolean,
    isToday: Boolean,
    dow: Int,
    /** 접힘에 따라 화면에서 계산된 셀 높이 (풀=정사각, 접힘=32dp). */
    cellHeight: Dp,
    /** 접힘 진행도 0(풀)~1(간단) — 거리 텍스트 페이드용. */
    collapseFraction: Float,
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(10.dp)

    // 선택은 배경을 바꾸지 않고 테두리만 강조한다 (디자인 확정).
    // 빈 칸 배경은 더 연하게, 이전/다음 달 칸은 배경 없이 (칙칙함 피드백 반영).
    val bg = when {
        data != null -> colors.surface1
        isToday -> colors.accentBlue.copy(alpha = 0.08f)
        inMonth -> if (colors.isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
        else -> Color.Transparent
    }
    val borderColor = if (isSelected) colors.accentBlue else Color.Transparent
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    val dayColor = when {
        !inMonth -> colors.textTertiary
        isSelected || isToday -> colors.accentBlue
        dow == 0 -> SundayColor
        dow == 6 -> SaturdayColor
        else -> colors.textPrimary
    }

    Column(
        modifier = Modifier
            // 접힘 진행도가 만든 높이를 그대로 사용 — 스크롤을 1:1로 따라 얇아진다
            .fillMaxWidth()
            .height(cellHeight)
            .alpha(if (inMonth) 1f else 0.55f)
            .clip(shape)
            .background(bg)
            .border(borderWidth, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = inMonth,
                onClick = onClick,
            )
            .padding(horizontal = 3.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text = day.toString(),
            fontSize = 11.sp,
            fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = dayColor,
            lineHeight = 11.sp,
        )
        if (data != null) {
            // 가운데 남는 공간에 거리 텍스트 — 접힐수록 페이드아웃, 그래프는 하단 고정
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (collapseFraction < 0.65f) {
                    Text(
                        text = "${formatNumber(data.distanceM)}m",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.accentBlue else colors.textSecondary,
                        fontFamily = JetBrainsMonoFamily,
                        letterSpacing = (-0.2).sp,
                        lineHeight = 8.sp,
                        maxLines = 1,
                        modifier = Modifier.alpha(1f - (collapseFraction / 0.65f)),
                    )
                }
            }
            Box(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                StrokeRatioBar(meters = barMeters(data), barHeight = 7.dp, compact = true)
            }
        }
    }
}

// ── 영법 비율 막대 — 6개 영법 전부 표시 ─────────────────────────
@Composable
private fun StrokeRatioBar(meters: List<Int>, barHeight: Dp = 12.dp, compact: Boolean = false) {
    val colors = SoodalDesign.colors
    val total = meters.sum().coerceAtLeast(1)
    val bgColor = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor),
    ) {
        meters.forEachIndexed { i, m ->
            if (m > 0) {
                Box(
                    modifier = Modifier
                        .weight(m.toFloat() / total)
                        .fillMaxHeight()
                        .background(BAR_COLORS[i].copy(alpha = if (compact) 0.95f else 1f)),
                )
            }
        }
    }
}

// ── 영법 범례 ────────────────────────────────────────────────────
@Composable
private fun StrokeLegend() {
    val colors = SoodalDesign.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.glassBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LEGEND_STROKES.forEach { (label, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(9.dp).clip(RoundedCornerShape(2.dp)).background(color))
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            }
        }
    }
}

// ── 선택한 날 상세 ───────────────────────────────────────────────
@Composable
private fun DayDetailCard(
    year: Int,
    month: Int,
    day: Int?,
    data: SwimDayData?,
    onEdit: (SwimSessionData) -> Unit,
    chartExpanded: Boolean,
    onToggleChart: () -> Unit,
) {
    val colors = SoodalDesign.colors

    // 기록 있는 날 ↔ 없는 날을 오갈 때 카드 높이가 탁 바뀌지 않고 부드럽게 변한다.
    SoodalCard(modifier = Modifier.fillMaxWidth().animateContentSize(tween(220))) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (day != null) "${year}년 ${monthNames[month - 1]} ${day}일" else "날짜를 선택해 주세요",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.alignByBaseline(),
                )
                // 세션이 하나면 날짜 옆에 운동 시각을 작게 — 여러 세션은 각 회차 라벨에 표시
                val single = data?.sessions?.singleOrNull()
                val singleStart = single?.startEpochSec
                if (single != null && singleStart != null) {
                    Text(
                        formatSessionTimeRange(singleStart, single.durationSec),
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                        modifier = Modifier.alignByBaseline(),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            if (data == null) {
                Text(
                    text = "이 날은 수영 기록이 없어요",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                )
                return@Column
            }

            // 하루 여러 세션 가능 — 각 세션을 자체 블록으로 모두 보여준다
            data.sessions.forEachIndexed { index, session ->
                if (index > 0) {
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))
                    Spacer(Modifier.height(16.dp))
                }
                if (data.sessions.size > 1) {
                    SessionTimeLabel(index = index, startEpochSec = session.startEpochSec, durationSec = session.durationSec)
                    Spacer(Modifier.height(10.dp))
                }
                SessionDetail(
                    session = session,
                    onEdit = { onEdit(session) },
                    chartExpanded = chartExpanded,
                    onToggleChart = onToggleChart,
                )
            }

            // 조개 획득 (일 단위 지급)
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.accentGold.copy(alpha = 0.08f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SoodalIcon(icon = SoodalIcons.Shell, tint = colors.accentGold, size = 18.dp)
                Text("조개 ${data.shellReward}개 획득!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.accentGold)
            }
        }
    }
}

/** "1회차 · 오전 6:00 ~ 6:45" 라벨 — 같은 날 여러 세션 구분 + 운동 시각. */
@Composable
private fun SessionTimeLabel(index: Int, startEpochSec: Long?, durationSec: Int) {
    val colors = SoodalDesign.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(colors.accentBlue))
        Text("${index + 1}회차", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accentBlue)
        if (startEpochSec != null) {
            Text(
                formatSessionTimeRange(startEpochSec, durationSec),
                fontSize = 11.sp,
                color = colors.textTertiary,
            )
        }
    }
}

/** "오전 6:00 ~ 6:45" — 세션 시작~끝 시각. 시작·끝의 오전/오후가 다르면 끝에도 표기한다. */
private fun formatSessionTimeRange(startEpochSec: Long, durationSec: Int): String {
    val zone = java.time.ZoneId.systemDefault()
    fun label(epoch: Long): Pair<String, String> {
        val t = java.time.Instant.ofEpochSecond(epoch).atZone(zone).toLocalTime()
        val ampm = if (t.hour < 12) "오전" else "오후"
        val hm = "%d:%02d".format(if (t.hour % 12 == 0) 12 else t.hour % 12, t.minute)
        return ampm to hm
    }
    val (sAmpm, sHm) = label(startEpochSec)
    val (eAmpm, eHm) = label(startEpochSec + durationSec)
    val end = if (sAmpm == eAmpm) eHm else "$eAmpm $eHm"
    return "$sAmpm $sHm ~ $end"
}

/** 한 세션의 상세 블록 — 거리/시간/칼로리 + 심박/페이스 + 심박 곡선 + 영법 비율. */
@Composable
private fun SessionDetail(
    session: SwimSessionData,
    onEdit: () -> Unit,
    chartExpanded: Boolean,
    onToggleChart: () -> Unit,
) {
    val colors = SoodalDesign.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        // 거리 / 시간 / 칼로리
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MetricCol("거리", formatNumber(session.distanceM), "m", colors.accentBlue)
            MetricCol("시간", session.durationMin.toString(), "분", colors.textPrimary)
            MetricCol("칼로리", session.kcal.toString(), "kcal", colors.success)
        }

        // 최대·최소·평균 심박(HC 심박 기록이 있을 때) + 심박 그래프 펼치기.
        // 페이스 표시는 보류 — 실운동시간 기반 페이스(칼로리 보정)의 신뢰도 확보 전까지 비활성.
        // val pace = session.activeSec?.let { paceSecPer100m(session.distanceM, it) }
        val avgHr = averageHr(session.hrSeries)
        val hasHr = session.maxHr != null && session.minHr != null
        val hasChart = session.hrSeries.size >= 2
        if (hasHr || avgHr != null) {
            Spacer(Modifier.height(14.dp))
            VitalsRow(
                maxHr = session.maxHr,
                minHr = session.minHr,
                avgHr = avgHr,
                chartExpanded = chartExpanded,
                onToggleChart = if (hasChart) onToggleChart else null,
            )
        }

        // 세션 심박 곡선 — 화살표로 펼쳤을 때만
        if (hasChart) {
            AnimatedVisibility(visible = chartExpanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HrChart(points = session.hrSeries, restRanges = session.hrRestRanges)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 영법 비율 헤더 + 수정 버튼
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("영법 비율", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary, letterSpacing = 0.4.sp)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.accentBlue.copy(alpha = 0.12f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEdit,
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SoodalIcon(icon = SoodalIcons.Edit, tint = colors.accentBlue, size = 12.dp)
                Text("수정", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accentBlue)
            }
        }

        Spacer(Modifier.height(8.dp))
        StrokeRatioBar(meters = barMeters(session))
        Spacer(Modifier.height(10.dp))

        // 영법별 % 그리드 — 거리 상위 4개만, 동률은 자>평>배>접>혼>킥 우선 (DECISIONS 2026-06-10).
        // 비율은 전체(6영법 합) 기준.
        val totalMeters = barMeters(session).sum().coerceAtLeast(1)
        val entries = topStrokes(
            listOf(
                ("자유형" to StrokePalette.Free) to session.freeM,
                ("평영" to StrokePalette.Breast) to session.breastM,
                ("배영" to StrokePalette.Back) to session.backM,
                ("접영" to StrokePalette.Fly) to session.flyM,
                ("혼영" to StrokePalette.Medley) to session.mixedM,
                ("킥판" to StrokePalette.Kick) to session.kickM,
            ),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            entries.forEach { (labelColor, meters) ->
                val (label, color) = labelColor
                val pct = strokePercent(meters, totalMeters)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(SoodalShape.sm)
                        .background(
                            if (pct > 0) {
                                if (colors.isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.03f)
                            } else {
                                Color.Transparent
                            },
                        )
                        .alpha(if (pct > 0) 1f else 0.4f)
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$pct%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        // 텍스트는 파스텔 대신 고채도 변형 — 흰 카드 위 가독성
                        color = if (pct > 0) strokeTextColorOf(label) else colors.textTertiary,
                        fontFamily = JetBrainsMonoFamily,
                    )
                    Spacer(Modifier.height(1.dp))
                    Text("${meters}m", fontSize = 9.sp, color = colors.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun MetricCol(label: String, value: String, unit: String, valueColor: Color) {
    val colors = SoodalDesign.colors
    Column {
        Text(label, fontSize = 10.sp, color = colors.textSecondary)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
            Text(unit, fontSize = 10.sp, color = colors.textSecondary, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
        }
    }
}

/** 차트 표시용 사전 계산 — 공백 압축 좌표계 (세그먼트 + 표시 단위). */
private class HrChartLayout(
    val segs: List<List<Pair<Int, Int>>>,
    val segStartUnit: FloatArray,
    val totalUnits: Float,
) {
    fun unitOf(si: Int, p: Pair<Int, Int>): Float =
        segStartUnit[si] + (p.first - segs[si].first().first)

    /** 표시 비율(0~1) 위치에서 가장 가까운 포인트와 그 표시 단위. */
    fun nearest(frac: Float): Pair<Pair<Int, Int>, Float>? {
        var best: Pair<Pair<Int, Int>, Float>? = null
        var bestDist = Float.MAX_VALUE
        val unit = frac.coerceIn(0f, 1f) * totalUnits
        segs.forEachIndexed { si, seg ->
            seg.forEach { p ->
                val u = unitOf(si, p)
                val d = kotlin.math.abs(u - unit)
                if (d < bestDist) {
                    bestDist = d
                    best = p to u
                }
            }
        }
        return best
    }
}

private fun buildHrChartLayout(points: List<Pair<Int, Int>>): HrChartLayout {
    val rawSpan = (points.last().first - points.first().first).toFloat().coerceAtLeast(1f)
    // 일시정지(샘플 공백) 경계 — 평균 간격의 3배(최소 60초) 이상 벌어지면 세그먼트 분리.
    val breakGap = (rawSpan / points.size * 3f).coerceAtLeast(60f)
    val segments = mutableListOf<MutableList<Pair<Int, Int>>>()
    points.forEach { p ->
        val current = segments.lastOrNull()
        if (current == null || p.first - current.last().first > breakGap) {
            segments.add(mutableListOf(p))
        } else {
            current.add(p)
        }
    }
    val segs = segments.filter { it.size >= 2 }
    if (segs.isEmpty()) return HrChartLayout(emptyList(), FloatArray(0), 1f)

    // 공백 압축 x축: 세그먼트 내부는 실제 시간 비례, 세그먼트 사이는 고정 틈.
    val spans = FloatArray(segs.size) { (segs[it].last().first - segs[it].first().first).toFloat().coerceAtLeast(1f) }
    val spacer = (spans.sum() * 0.03f).coerceAtLeast(30f)
    val starts = FloatArray(segs.size)
    var acc = 0f
    for (i in segs.indices) {
        starts[i] = acc
        acc += spans[i] + spacer
    }
    return HrChartLayout(segs, starts, spans.sum() + spacer * (segs.size - 1))
}

/** 세션 시작 기준 경과를 분:초(1시간 넘으면 시:분:초)로 표기. */
private fun formatChartTime(sec: Int): String =
    if (sec >= 3600) "%d:%02d:%02d".format(sec / 3600, sec % 3600 / 60, sec % 60)
    else "%d:%02d".format(sec / 60, sec % 60)

/**
 * 세션 심박 곡선 — 단일 로즈 라인. (휴식 구간 색 구분 표시는 보류 — 주석 처리)
 * 일시정지(샘플 공백)는 x축에서 제거하고 세그먼트 사이 작은 틈으로만 표시한다.
 * 꾹 눌러 끌면 해당 지점의 경과 시간·심박을 보여준다.
 */
@Composable
private fun HrChart(points: List<Pair<Int, Int>>, restRanges: List<IntRange>) {
    val rose = Color(0xFFF43F5E)
    val colors = SoodalDesign.colors
    // val restColor = colors.textTertiary // 휴식 밴드 색 — 보류
    val layout = remember(points) { buildHrChartLayout(points) }
    var scrubFrac by remember(points) { mutableStateOf<Float?>(null) }
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(rose.copy(alpha = 0.04f))
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .pointerInput(points) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { pos -> scrubFrac = pos.x / size.width },
                    onDragEnd = { scrubFrac = null },
                    onDragCancel = { scrubFrac = null },
                ) { change, _ -> scrubFrac = change.position.x / size.width }
            },
    ) {
        if (layout.segs.isEmpty()) return@Canvas
        val minBpm = points.minOf { it.second }.toFloat()
        val maxBpm = points.maxOf { it.second }.toFloat()
        val bpmSpan = (maxBpm - minBpm).coerceAtLeast(1f)

        fun py(bpm: Int) = size.height - (bpm - minBpm) / bpmSpan * size.height
        fun pxUnit(u: Float) = u / layout.totalUnits * size.width

        layout.segs.forEachIndexed { si, seg ->
            fun px(p: Pair<Int, Int>) = pxUnit(layout.unitOf(si, p))

            // 휴식 구간 색 구분 표시는 보류 — 세그먼트를 단일 로즈 라인+그라디언트로 그린다.
            // (복구 시 restRanges 기반 isRest 서브런 분할 + 회색 밴드 로직 부활)
            // fun isRest(p: Pair<Int, Int>) = restRanges.any { p.first in it }

            val line = Path().apply {
                seg.forEachIndexed { ri, p ->
                    if (ri == 0) moveTo(px(p), py(p.second)) else lineTo(px(p), py(p.second))
                }
            }
            val area = Path().apply {
                addPath(line)
                lineTo(px(seg.last()), size.height)
                lineTo(px(seg.first()), size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(rose.copy(alpha = 0.22f), rose.copy(alpha = 0f))))
            drawPath(line, rose, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        // 스크럽 오버레이 — 꾹 누른 지점의 시간·심박
        scrubFrac?.let { frac ->
            val (p, unit) = layout.nearest(frac) ?: return@let
            val x = pxUnit(unit)
            val y = py(p.second)
            drawLine(
                color = colors.textSecondary.copy(alpha = 0.6f),
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            drawCircle(color = rose, radius = 3.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
            drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))

            val label = "${formatChartTime(p.first)} · ${p.second}bpm"
            val text = textMeasurer.measure(
                androidx.compose.ui.text.AnnotatedString(label),
                androidx.compose.ui.text.TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFamily,
                    color = Color.White,
                ),
            )
            val pad = 4.dp.toPx()
            val boxW = text.size.width + pad * 2
            val boxH = text.size.height + pad
            val boxX = (x - boxW / 2).coerceIn(0f, size.width - boxW)
            drawRoundRect(
                color = Color(0xCC1A2438),
                topLeft = androidx.compose.ui.geometry.Offset(boxX, 0f),
                size = androidx.compose.ui.geometry.Size(boxW, boxH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
            )
            drawText(
                textMeasurer = textMeasurer,
                text = androidx.compose.ui.text.AnnotatedString(label),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMonoFamily,
                    color = Color.White,
                ),
                topLeft = androidx.compose.ui.geometry.Offset(boxX + pad, pad / 2),
            )
        }
    }
}

/**
 * 심박(최대/최소/평균) + 심박 그래프 펼치기 토글 행.
 * 라벨(11sp)과 수치(17sp 모노)는 폰트 패딩 차이로 베이스라인 정렬한다.
 * 페이스 표시는 보류 — 칼로리 보정 신뢰도 확보 전까지 주석 처리(아래 참조).
 */
@Composable
private fun VitalsRow(
    maxHr: Int?,
    minHr: Int?,
    avgHr: Int?,
    chartExpanded: Boolean,
    onToggleChart: (() -> Unit)?,
) {
    val colors = SoodalDesign.colors
    val rose = Color(0xFFF43F5E)
    val hasHr = maxHr != null && minHr != null
    val accent = if (hasHr) rose else colors.accentBlue
    val arrowRotation by animateFloatAsState(if (chartExpanded) 90f else 0f, label = "hrChartArrow")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasHr) rose.copy(alpha = 0.06f) else colors.accentBlue.copy(alpha = 0.05f))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SoodalIcon(
            icon = if (hasHr) SoodalIcons.Heart else SoodalIcons.Swimmer,
            tint = accent,
            size = 16.dp,
        )
        Spacer(Modifier.width(8.dp))
        if (hasHr) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("최대", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                Text("$maxHr", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = rose, fontFamily = JetBrainsMonoFamily, modifier = Modifier.alignByBaseline())
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.width(1.dp).height(16.dp).background(colors.glassBorder))
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("최소", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                Text("$minHr", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary, fontFamily = JetBrainsMonoFamily, modifier = Modifier.alignByBaseline())
            }
        }
        // 평균 심박 — 휴식 포함 전체 평균
        if (avgHr != null) {
            if (hasHr) {
                Spacer(Modifier.width(8.dp))
                Box(Modifier.width(1.dp).height(16.dp).background(colors.glassBorder))
                Spacer(Modifier.width(8.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("평균", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
                Text("$avgHr", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary, fontFamily = JetBrainsMonoFamily, modifier = Modifier.alignByBaseline())
                Text("bpm", fontSize = 10.sp, color = colors.textTertiary, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
            }
        }
        Spacer(Modifier.weight(1f))
        // 페이스 표시 보류 — 신뢰도 확보 전까지 비활성. 복구 시 paceSec 파라미터 + 아래 블록 부활.
        // if (paceSec != null) {
        //     Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        //         Text("페이스", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold, modifier = Modifier.alignByBaseline())
        //         Text(formatPace(paceSec), fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary, fontFamily = JetBrainsMonoFamily, modifier = Modifier.alignByBaseline())
        //     }
        // }
        // 심박 그래프 펼치기/접기 화살표 — 그래프가 있을 때만
        if (onToggleChart != null) {
            SoodalIcon(
                icon = SoodalIcons.ArrowRight,
                tint = accent,
                size = 18.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onToggleChart,
                    )
                    .padding(4.dp)
                    .rotate(arrowRotation),
            )
        }
    }
}

private fun formatNumber(n: Int): String {
    if (n < 1000) return n.toString()
    val s = n.toString()
    return buildString {
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) append(',')
            append(c)
        }
    }
}
