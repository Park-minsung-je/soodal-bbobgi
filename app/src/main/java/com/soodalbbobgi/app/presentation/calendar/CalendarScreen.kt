package com.soodalbbobgi.app.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.GlassPanel
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalTabBar
import java.time.DayOfWeek
import java.time.YearMonth

@Composable
fun CalendarScreen(
    onNavigateToTab: (String) -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
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
            // ── Month Header ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MonthNavButton(text = "◀", onClick = { viewModel.previousMonth() })
                Text(
                    text = "${state.year}년 ${state.month}월",
                    style = SoodalDesign.typography.lg,
                    color = colors.textPrimary,
                )
                MonthNavButton(text = "▶", onClick = { viewModel.nextMonth() })
            }

            Spacer(Modifier.height(spacing.s4))

            // ── Day Headers ─────────────────────────────────────
            val dayLabels = listOf("월", "화", "수", "목", "금", "토", "일")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEachIndexed { index, label ->
                    val textColor = when (index) {
                        5 -> colors.accentCyan       // Saturday
                        6 -> colors.warn             // Sunday
                        else -> colors.textTertiary
                    }
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor,
                    )
                }
            }

            Spacer(Modifier.height(spacing.s2))

            // ── Calendar Grid (42 cells) ────────────────────────
            val yearMonth = YearMonth.of(state.year, state.month)
            val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek
            // Monday-based offset: Monday=0, Tuesday=1, ..., Sunday=6
            val offset = (firstDayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val daysInMonth = yearMonth.lengthOfMonth()

            for (week in 0 until 6) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = week * 7 + col
                        val day = cellIndex - offset + 1
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.85f),
                        ) {
                            if (day in 1..daysInMonth) {
                                DayCell(
                                    day = day,
                                    swimData = state.swimData[day],
                                    isSelected = state.selectedDay == day,
                                    isWeekend = col >= 5,
                                    onClick = { viewModel.selectDay(day) },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.s3))

            // ── Stroke Legend ────────────────────────────────────
            StrokeLegend()

            Spacer(Modifier.height(spacing.s4))

            // ── Day Detail Card (when selected) ─────────────────
            val selectedData = state.selectedDay?.let { state.swimData[it] }
            if (state.selectedDay != null && selectedData != null) {
                DayDetailCard(day = state.selectedDay!!, data = selectedData)
                Spacer(Modifier.height(spacing.s4))
            }

            // ── Month Summary ───────────────────────────────────
            MonthSummaryCard(swimData = state.swimData)

            Spacer(Modifier.height(spacing.s4))
        }

        // ── Tab Bar ─────────────────────────────────────────
        SoodalTabBar(activeTab = "calendar", onTabSelected = onNavigateToTab)
    }
}

@Composable
private fun MonthNavButton(text: String, onClick: () -> Unit) {
    val colors = SoodalDesign.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(colors.surface2)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 14.sp, color = colors.textPrimary)
    }
}

@Composable
private fun DayCell(
    day: Int,
    swimData: SwimDayData?,
    isSelected: Boolean,
    isWeekend: Boolean,
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val bgColor = when {
        isSelected -> colors.accentCyan.copy(alpha = 0.18f)
        swimData != null -> colors.surface2
        else -> Color.Transparent
    }
    val borderColor = if (isSelected) colors.accentCyan.copy(alpha = 0.5f) else Color.Transparent
    val dayColor = when {
        isSelected -> colors.accentCyan
        isWeekend -> colors.warn.copy(alpha = 0.7f)
        else -> colors.textPrimary
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(1.dp)
            .clip(SoodalShape.sm)
            .background(bgColor)
            .border(1.dp, borderColor, SoodalShape.sm)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = day.toString(),
            fontSize = 11.sp,
            fontWeight = if (swimData != null) FontWeight.Bold else FontWeight.Normal,
            color = dayColor,
        )
        if (swimData != null) {
            Text(
                text = "${swimData.distanceM}m",
                fontSize = 8.sp,
                color = colors.accentCyan,
            )
            Spacer(Modifier.height(1.dp))
            // Stroke ratio bar
            StrokeRatioBar(strokes = swimData.strokes)
        }
    }
}

@Composable
private fun StrokeRatioBar(strokes: StrokeBreakdown) {
    val colors = SoodalDesign.colors
    val strokeColors = listOf(
        colors.accentCyan,    // freestyle - 자유형
        colors.accentPurple,  // breaststroke - 평영
        colors.accentGold,    // backstroke - 배영
        colors.success,       // butterfly - 접영
    )
    val ratios = listOf(strokes.freestyle, strokes.backstroke, strokes.breaststroke, strokes.butterfly)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(SoodalShape.sm),
    ) {
        ratios.forEachIndexed { index, ratio ->
            if (ratio > 0f) {
                Box(
                    modifier = Modifier
                        .weight(ratio)
                        .height(4.dp)
                        .background(strokeColors[index]),
                )
            }
        }
    }
}

@Composable
private fun StrokeLegend() {
    val colors = SoodalDesign.colors
    val items = listOf(
        "자유형" to colors.accentCyan,
        "평영" to colors.accentPurple,
        "배영" to colors.accentGold,
        "접영" to colors.success,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { index, (label, color) ->
            if (index > 0) Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                color = colors.textTertiary,
            )
        }
    }
}

@Composable
private fun DayDetailCard(day: Int, data: SwimDayData) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${day}일 수영 기록",
                style = SoodalDesign.typography.md,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(spacing.s3))

            // Metrics row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                MetricItem(icon = "🏊", label = "거리", value = "${data.distanceM}m")
                MetricItem(icon = "⏱️", label = "시간", value = "${data.durationMin}분")
                MetricItem(icon = "🔥", label = "칼로리", value = "${data.kcal}kcal")
            }

            Spacer(Modifier.height(spacing.s3))

            // Stroke breakdown
            Text(
                text = "영법 비율",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(spacing.s2))
            StrokeRatioBar(strokes = data.strokes)
            Spacer(Modifier.height(spacing.s2))

            val strokeDetails = listOf(
                Triple("자유형", data.strokes.freestyle, colors.accentCyan),
                Triple("평영", data.strokes.breaststroke, colors.accentPurple),
                Triple("배영", data.strokes.backstroke, colors.accentGold),
                Triple("접영", data.strokes.butterfly, colors.success),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                strokeDetails.forEach { (label, ratio, color) ->
                    val pct = (ratio * 100).toInt()
                    val meters = (data.distanceM * ratio).toInt()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(8.dp).clip(SoodalShape.sm).background(color))
                            Text(label, fontSize = 10.sp, color = colors.textSecondary, fontWeight = FontWeight.SemiBold)
                        }
                        Text("$pct%", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (pct > 0) color else colors.textTertiary)
                        Text("${meters}m", fontSize = 9.sp, color = colors.textTertiary)
                    }
                }
            }

            Spacer(Modifier.height(spacing.s3))

            // Shell reward
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(text = "🐚", fontSize = 14.sp)
                Text(
                    text = "조개 ${data.shellReward}개 획득",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentGold,
                )
            }
        }
    }
}

@Composable
private fun MetricItem(icon: String, label: String, value: String) {
    val colors = SoodalDesign.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accentCyan,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = colors.textTertiary,
        )
    }
}

@Composable
private fun MonthSummaryCard(swimData: Map<Int, SwimDayData>) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing
    val swimDays = swimData.size
    val totalShells = swimData.values.sumOf { it.shellReward }
    val totalDistanceKm = swimData.values.sumOf { it.distanceM } / 1000f

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "이번 달 요약",
                style = SoodalDesign.typography.md,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(spacing.s3))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                SummaryItem(icon = "📅", label = "수영 일수", value = "${swimDays}일")
                SummaryItem(icon = "🐚", label = "획득 조개", value = "${totalShells}개")
                SummaryItem(
                    icon = "🏊",
                    label = "총 거리",
                    value = String.format("%.1fkm", totalDistanceKm),
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(icon: String, label: String, value: String) {
    val colors = SoodalDesign.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accentCyan,
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = colors.textTertiary,
        )
    }
}
