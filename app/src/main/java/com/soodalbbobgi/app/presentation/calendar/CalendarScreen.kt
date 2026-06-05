package com.soodalbbobgi.app.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import com.soodalbbobgi.app.core.ui.SoodalIcons
import java.time.LocalDate

// 캘린더 강조색 — 앱 기본 포인트(시안)와 별개로 캘린더는 생기있는 블루로 통일 (디자인 확정).
private val CalAccent = Color(0xFF2563EB)
private val SundayColor = Color(0xFFFF9B9B)
private val SaturdayColor = Color(0xFF9BC4FF)

// 영법 파스텔 팔레트 — 디자인 확정. 순서: 자유형/평영/배영/접영/킥판/혼영.
private val StrokeFree = Color(0xFF7DD3FC)
private val StrokeBreast = Color(0xFFC4B5FD)
private val StrokeBack = Color(0xFF5CD69B)
private val StrokeFly = Color(0xFFFDA4AF)
private val StrokeKick = Color(0xFF94A3B8)
private val StrokeMedley = Color(0xFFFCD34D)

private val DISPLAY_STROKES = listOf(
    "자유형" to StrokeFree,
    "평영" to StrokeBreast,
    "배영" to StrokeBack,
    "접영" to StrokeFly,
)

private val monthNames = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월")

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val weekly by viewModel.weeklyActivity.collectAsState()
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    // 영법 비율 수정 시트 — 어느 날을 수정 중인지.
    var editDay by remember { mutableStateOf<Int?>(null) }
    // 선택한 날이 바뀌면 열린 수정 시트는 닫는다.
    LaunchedEffect(state.selectedDay) { editDay = null }

    Box(modifier = Modifier.fillMaxSize().background(colors.bgDeep)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4, vertical = spacing.s4),
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

            // ── 그리드 (좌우 스와이프로 월 이동) ─────────────
            CalendarGrid(
                year = state.year,
                month = state.month,
                selectedDay = state.selectedDay,
                swimData = state.swimData,
                onSelect = viewModel::selectDay,
                onSwipePrev = viewModel::previousMonth,
                onSwipeNext = viewModel::nextMonth,
            )

            // ── 영법 범례 ──────────────────────────────────
            Spacer(Modifier.height(12.dp))
            StrokeLegend()

            // ── 선택한 날 상세 ──────────────────────────────
            SectionLabel(text = "선택한 날")
            Spacer(Modifier.height(12.dp))
            DayDetailCard(
                year = state.year,
                month = state.month,
                day = state.selectedDay,
                data = state.selectedDay?.let { state.swimData[it] },
                onEdit = { state.selectedDay?.let { editDay = it } },
            )

            // ── 이번 주 활동 ────────────────────────────────
            SectionLabel(
                text = "이번 주 활동",
                action = { TrendBadge(weekly.trendPercent) },
            )
            Spacer(Modifier.height(12.dp))
            WeeklyActivityCard(weekly)

            // ── 이번 달 수영 ────────────────────────────────
            SectionLabel(text = "이번 달 수영")
            Spacer(Modifier.height(12.dp))
            MonthSwimStats(state.swimData)

            Spacer(Modifier.height(12.dp))
        }

        // ── 기록 수정 바텀시트 ──────────────────────────────
        val day = editDay
        val data = day?.let { state.swimData[it] }
        if (day != null && data != null) {
            StrokeEditSheet(
                dateLabel = "${state.year}년 ${monthNames[state.month - 1]} ${day}일",
                data = data,
                onDismiss = { editDay = null },
                onSave = { free, breast, back, fly, kick, mixed ->
                    viewModel.saveStrokes(day, free, breast, back, fly, kick, mixed)
                    editDay = null
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
            HeaderStat(label = "수영", value = "$swimDays", unit = "일", color = CalAccent, first = true)
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
    onSelect: (Int) -> Unit,
    onSwipePrev: () -> Unit,
    onSwipeNext: () -> Unit,
) {
    val cells = remember(year, month) { buildMonthCells(year, month) }
    val today = LocalDate.now()
    val isCurrentMonth = year == today.year && month == today.monthValue

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(year, month) {
                var dragTotal = 0f
                val threshold = 56.dp.toPx()
                detectHorizontalDragGestures(
                    onDragStart = { dragTotal = 0f },
                    onDragEnd = {
                        if (dragTotal > threshold) onSwipePrev()
                        else if (dragTotal < -threshold) onSwipeNext()
                    },
                ) { _, dragAmount -> dragTotal += dragAmount }
            },
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
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(10.dp)

    // 선택은 배경을 바꾸지 않고 테두리만 강조한다 (디자인 확정).
    val bg = when {
        data != null -> colors.surface1
        isToday -> CalAccent.copy(alpha = 0.08f)
        inMonth -> if (colors.isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.035f)
        else -> if (colors.isDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.02f)
    }
    val borderColor = if (isSelected) CalAccent else Color.Transparent
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    val dayColor = when {
        !inMonth -> colors.textTertiary
        isSelected || isToday -> CalAccent
        dow == 0 -> SundayColor
        dow == 6 -> SaturdayColor
        else -> colors.textPrimary
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
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
            Spacer(Modifier.height(3.dp))
            Text(
                text = "${formatNumber(data.distanceM)}m",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) CalAccent else colors.textSecondary,
                fontFamily = JetBrainsMonoFamily,
                letterSpacing = (-0.2).sp,
                lineHeight = 8.sp,
            )
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                StrokeRatioBar(strokes = data.strokes, barHeight = 7.dp, compact = true)
            }
        }
    }
}

// ── 영법 비율 막대 ───────────────────────────────────────────────
@Composable
private fun StrokeRatioBar(strokes: StrokeBreakdown, barHeight: Dp = 12.dp, compact: Boolean = false) {
    val colors = SoodalDesign.colors
    val parts = listOf(
        strokes.freestyle to StrokeFree,
        strokes.breaststroke to StrokeBreast,
        strokes.backstroke to StrokeBack,
        strokes.butterfly to StrokeFly,
    )
    val total = parts.sumOf { it.first.toDouble() }.toFloat().coerceAtLeast(0.0001f)
    val bgColor = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor),
    ) {
        parts.forEach { (ratio, color) ->
            if (ratio > 0f) {
                Box(
                    modifier = Modifier
                        .weight(ratio / total)
                        .fillMaxHeight()
                        .background(color.copy(alpha = if (compact) 0.95f else 1f)),
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
        DISPLAY_STROKES.forEach { (label, color) ->
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
    onEdit: () -> Unit,
) {
    val colors = SoodalDesign.colors

    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = if (day != null) "${year}년 ${monthNames[month - 1]} ${day}일" else "날짜를 선택해 주세요",
                fontSize = 14.sp,
                color = colors.textSecondary,
            )
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

            // 거리 / 시간 / 칼로리
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MetricCol("거리", formatNumber(data.distanceM), "m", CalAccent)
                MetricCol("시간", data.durationMin.toString(), "분", colors.textPrimary)
                MetricCol("칼로리", data.kcal.toString(), "kcal", colors.success)
            }

            // 최대·최소 심박 (Health Connect 심박 연동 시 표시)
            if (data.maxHr != null && data.minHr != null) {
                Spacer(Modifier.height(14.dp))
                HeartRateRow(maxHr = data.maxHr, minHr = data.minHr)
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
                        .background(CalAccent.copy(alpha = 0.12f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onEdit,
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Edit, tint = CalAccent, size = 12.dp)
                    Text("수정", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CalAccent)
                }
            }

            Spacer(Modifier.height(8.dp))
            StrokeRatioBar(strokes = data.strokes, barHeight = 12.dp)
            Spacer(Modifier.height(10.dp))

            // 영법별 % 그리드 (4열)
            val ratios = listOf(
                Triple("자유형", data.strokes.freestyle, StrokeFree),
                Triple("평영", data.strokes.breaststroke, StrokeBreast),
                Triple("배영", data.strokes.backstroke, StrokeBack),
                Triple("접영", data.strokes.butterfly, StrokeFly),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ratios.forEach { (label, ratio, color) ->
                    val pct = Math.round(ratio * 100)
                    val meters = (data.distanceM * ratio).toInt()
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
                            color = if (pct > 0) color else colors.textTertiary,
                            fontFamily = JetBrainsMonoFamily,
                        )
                        Spacer(Modifier.height(1.dp))
                        Text("${meters}m", fontSize = 9.sp, color = colors.textTertiary)
                    }
                }
            }

            // 조개 획득
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

@Composable
private fun HeartRateRow(maxHr: Int, minHr: Int) {
    val colors = SoodalDesign.colors
    val rose = Color(0xFFF43F5E)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rose.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SoodalIcon(icon = SoodalIcons.Heart, tint = rose, size = 18.dp)
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("최대", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
            Text("$maxHr", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = rose, fontFamily = JetBrainsMonoFamily)
        }
        Box(Modifier.width(1.dp).height(16.dp).background(colors.glassBorder))
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("최소", fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
            Text("$minHr", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary, fontFamily = JetBrainsMonoFamily)
        }
        Spacer(Modifier.weight(1f))
        Text("bpm", fontSize = 10.sp, color = colors.textTertiary, fontWeight = FontWeight.SemiBold)
    }
}

// ── 이번 주 활동 ─────────────────────────────────────────────────
@Composable
private fun WeeklyActivityCard(weekly: WeeklyActivity) {
    val colors = SoodalDesign.colors
    val strokeColors = listOf(StrokeFree, StrokeBreast, StrokeBack, StrokeFly, StrokeKick, StrokeMedley)
    val maxV = (weekly.days.maxOfOrNull { it.distanceM } ?: 0).coerceAtLeast(1)
    val chartHeight = 84.dp

    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatNumber(weekly.totalMeters), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = CalAccent)
                    Text("m", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(start = 3.dp, bottom = 2.dp))
                }
                Text("${weekly.activeDays}일 운동 · 주간", fontSize = 11.sp, color = colors.textSecondary)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().height(chartHeight + 22.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                weekly.days.forEach { bar ->
                    val frac = if (bar.distanceM > 0) (bar.distanceM.toFloat() / maxV).coerceAtLeast(0.08f) else 0.04f
                    val barHeight = chartHeight * frac
                    val strokeSum = bar.strokeMeters.sum()
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Box(
                            modifier = Modifier.height(chartHeight).fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            // 기록 없는 날 — 은은한 빈 막대
                                            bar.distanceM == 0 ->
                                                if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
                                            // 거리만 있고 영법 정보 없는 날 — 단색 폴백 (스택 세그먼트가 덮음)
                                            else -> CalAccent.copy(alpha = 0.45f)
                                        },
                                    )
                                    .alpha(if (bar.isToday) 1f else 0.9f),
                            ) {
                                if (bar.distanceM > 0 && strokeSum > 0) {
                                    bar.strokeMeters.forEachIndexed { i, m ->
                                        if (m > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(m.toFloat())
                                                    .background(strokeColors[i]),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = bar.label,
                            fontSize = 10.sp,
                            fontWeight = if (bar.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (bar.isToday) CalAccent else colors.textTertiary,
                        )
                    }
                }
            }
        }
    }
}

// ── 이번 달 수영 통계 ────────────────────────────────────────────
@Composable
private fun MonthSwimStats(swimData: Map<Int, SwimDayData>) {
    val colors = SoodalDesign.colors
    val totalDistance = swimData.values.sumOf { it.distanceM }
    val sessions = swimData.size
    val totalKcal = swimData.values.sumOf { it.kcal }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CalStatCard(Modifier.weight(1f), "누적거리", formatNumber(totalDistance), "m", CalAccent)
        CalStatCard(Modifier.weight(1f), "수영 횟수", "$sessions", "회", colors.textPrimary)
        CalStatCard(Modifier.weight(1f), "칼로리", formatNumber(totalKcal), "kcal", colors.success)
    }
}

@Composable
private fun CalStatCard(modifier: Modifier, label: String, value: String, unit: String, valueColor: Color) {
    val colors = SoodalDesign.colors
    SoodalCard(modifier = modifier) {
        Column {
            Text(label, fontSize = 10.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = valueColor)
                Spacer(Modifier.width(3.dp))
                Text(unit, fontSize = 10.sp, color = colors.textSecondary)
            }
        }
    }
}

// ── 공용 ─────────────────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String, action: (@Composable () -> Unit)? = null) {
    val colors = SoodalDesign.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary, letterSpacing = 0.7.sp)
        action?.invoke()
    }
}

@Composable
private fun TrendBadge(trendPercent: Int?) {
    val colors = SoodalDesign.colors
    if (trendPercent == null) return
    val up = trendPercent >= 0
    Text(
        text = "${if (up) "+" else ""}$trendPercent% ${if (up) "↑" else "↓"}",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (up) colors.success else Color(0xFFF43F5E),
    )
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
