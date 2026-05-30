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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.GlassPanel
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val WeekendPinkColor = Color(0xFFFF9B9B)

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    val monthNames = listOf("1월", "2월", "3월", "4월", "5월", "6월", "7월", "8월", "9월", "10월", "11월", "12월")

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
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${state.year}년 ${monthNames[state.month - 1]}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.01).sp,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MonthNavButton(icon = SoodalIcons.ArrowLeft, onClick = { viewModel.previousMonth() })
                    MonthNavButton(icon = SoodalIcons.ArrowRight, onClick = { viewModel.nextMonth() })
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── Day Headers ─────────────────────────────────────
            val dayLabels = listOf("월", "화", "수", "목", "금", "토", "일")
            Row(modifier = Modifier.fillMaxWidth()) {
                dayLabels.forEachIndexed { index, label ->
                    val textColor = if (index >= 5) WeekendPinkColor else colors.textTertiary
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = 0.4.sp,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // ── Calendar Grid ───────────────────────────────────
            val yearMonth = YearMonth.of(state.year, state.month)
            val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek
            val offset = (firstDayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
            val daysInMonth = yearMonth.lengthOfMonth()

            // Previous month's days
            val prevYearMonth = yearMonth.minusMonths(1)
            val daysInPrev = prevYearMonth.lengthOfMonth()

            // Build 42-cell grid (same as JSX)
            data class CalendarCell(val day: Int, val inMonth: Boolean)
            val cells = mutableListOf<CalendarCell>()
            // Leading days from previous month
            for (i in 0 until offset) {
                cells.add(CalendarCell(day = daysInPrev - offset + 1 + i, inMonth = false))
            }
            // Current month days
            for (d in 1..daysInMonth) {
                cells.add(CalendarCell(day = d, inMonth = true))
            }
            // Trailing days from next month
            while (cells.size < 42) {
                cells.add(CalendarCell(day = cells.size - daysInMonth - offset + 1, inMonth = false))
            }

            // Determine "today"
            val today = LocalDate.now()
            val isCurrentMonth = state.year == today.year && state.month == today.monthValue

            // Render grid in rows of 7
            for (week in 0 until 6) {
                // Skip empty trailing weeks
                val weekStart = week * 7
                val weekCells = cells.subList(weekStart, weekStart + 7)
                if (weekCells.all { !it.inMonth }) continue

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = week * 7 + col
                        val cell = cells[cellIndex]
                        val dow = col

                        Box(modifier = Modifier.weight(1f)) {
                            DayCell(
                                day = cell.day,
                                inMonth = cell.inMonth,
                                swimData = if (cell.inMonth) state.swimData[cell.day] else null,
                                isSelected = cell.inMonth && cell.day == state.selectedDay,
                                isToday = cell.inMonth && isCurrentMonth && cell.day == today.dayOfMonth,
                                isWeekend = dow >= 5,
                                onClick = {
                                    if (cell.inMonth) viewModel.selectDay(cell.day)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Stroke Legend ────────────────────────────────────
            Spacer(Modifier.height(12.dp))
            StrokeLegend()

            // ── Day Detail Section (always visible) ─────────────
            Spacer(Modifier.height(22.dp))
            SectionLabel(text = "선택한 날")
            Spacer(Modifier.height(12.dp))
            DayDetailCard(
                year = state.year,
                month = state.month,
                day = state.selectedDay,
                data = state.selectedDay?.let { state.swimData[it] },
                monthNames = monthNames,
            )

            // ── Month Summary Section (always visible) ──────────
            Spacer(Modifier.height(22.dp))
            SectionLabel(text = "이번 달 요약")
            Spacer(Modifier.height(12.dp))
            MonthSummaryPanel(swimData = state.swimData)

            Spacer(Modifier.height(12.dp))
        }
    }
}

// ── Section Label ───────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    val colors = SoodalDesign.colors
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textSecondary,
        letterSpacing = 0.7.sp,
    )
}

// ── Month Nav Button ────────────────────────────────────────────
@Composable
private fun MonthNavButton(icon: SoodalIcons, onClick: () -> Unit) {
    val colors = SoodalDesign.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(SoodalShape.md)
            .background(colors.glassBg)
            .border(1.dp, colors.glassBorder, SoodalShape.md)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        SoodalIcon(icon = icon, tint = colors.textPrimary, size = 14.dp)
    }
}

// ── Day Cell ────────────────────────────────────────────────────
@Composable
private fun DayCell(
    day: Int,
    inMonth: Boolean,
    swimData: SwimDayData?,
    isSelected: Boolean,
    isToday: Boolean,
    isWeekend: Boolean,
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val cellShape = RoundedCornerShape(10.dp)

    val bgColor = when {
        isSelected -> colors.accentCyan.copy(alpha = 0.18f)
        swimData != null -> colors.surface1
        isToday -> colors.accentCyan.copy(alpha = 0.08f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> colors.accentCyan
        isToday -> colors.accentCyan.copy(alpha = 0.4f)
        swimData != null -> if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
        else -> Color.Transparent
    }
    val borderWidth = if (isSelected) 1.5.dp else 1.dp

    val dayTextColor = when {
        !inMonth -> colors.textTertiary
        isSelected -> colors.accentCyan
        isToday -> colors.accentCyan
        isWeekend -> WeekendPinkColor
        else -> colors.textPrimary
    }

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .alpha(if (inMonth) 1f else 0.3f)
            .clip(cellShape)
            .background(bgColor)
            .border(borderWidth, borderColor, cellShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = inMonth,
                onClick = onClick,
            )
            .padding(
                start = if (swimData != null) 4.dp else 0.dp,
                end = if (swimData != null) 4.dp else 0.dp,
                top = if (swimData != null) 5.dp else 0.dp,
                bottom = if (swimData != null) 5.dp else 0.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (swimData != null) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        Text(
            text = day.toString(),
            fontSize = 11.sp,
            fontWeight = if (isToday || isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = dayTextColor,
            lineHeight = 11.sp,
        )
        if (swimData != null) {
            Text(
                text = "${formatNumber(swimData.distanceM)}m",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) colors.accentCyan else colors.textSecondary,
                fontFamily = JetBrainsMonoFamily,
                letterSpacing = (-0.2).sp,
                lineHeight = 8.sp,
            )
            Box(Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                StrokeRatioBar(strokes = swimData.strokes, barHeight = 7.dp, compact = true)
            }
        }
    }
}

// ── Stroke Ratio Bar ────────────────────────────────────────────
@Composable
private fun StrokeRatioBar(
    strokes: StrokeBreakdown,
    barHeight: Dp = 12.dp,
    compact: Boolean = false,
) {
    val colors = SoodalDesign.colors
    val strokeColors = listOf(
        colors.accentCyan,    // free
        colors.accentPurple,  // breast
        colors.accentGold,    // back
        colors.success,       // fly
    )
    val ratios = listOf(strokes.freestyle, strokes.breaststroke, strokes.backstroke, strokes.butterfly)
    val total = ratios.sum().coerceAtLeast(0.01f)

    val bgColor = if (colors.isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(bgColor),
    ) {
        ratios.forEachIndexed { index, ratio ->
            if (ratio > 0f) {
                Box(
                    modifier = Modifier
                        .weight(ratio / total)
                        .height(barHeight)
                        .background(strokeColors[index].copy(alpha = if (compact) 0.95f else 1f)),
                )
            }
        }
    }
}

// ── Stroke Legend ───────────────────────────────────────────────
@Composable
private fun StrokeLegend() {
    val colors = SoodalDesign.colors
    val legendItems = listOf(
        "자유형" to colors.accentCyan,
        "평영" to colors.accentPurple,
        "배영" to colors.accentGold,
        "접영" to colors.success,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.glassBg)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            legendItems.forEach { (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color),
                    )
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

// ── Day Detail Card ─────────────────────────────────────────────
@Composable
private fun DayDetailCard(
    year: Int,
    month: Int,
    day: Int?,
    data: SwimDayData?,
    monthNames: List<String>,
) {
    val colors = SoodalDesign.colors

    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Date header
            Text(
                text = if (day != null) "${year}년 ${monthNames[month - 1]} ${day}일" else "날짜를 선택해 주세요",
                fontSize = 14.sp,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(12.dp))

            if (data != null) {
                // 3-column metric grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    // Distance
                    Column {
                        Text(
                            text = "거리",
                            fontSize = 10.sp,
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = formatNumber(data.distanceM),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.accentCyan,
                            )
                            Text(
                                text = "m",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                            )
                        }
                    }

                    // Duration
                    Column {
                        Text(
                            text = "시간",
                            fontSize = 10.sp,
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = data.durationMin.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = "분",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                            )
                        }
                    }

                    // Calories (success/green color, NOT gold!)
                    Column {
                        Text(
                            text = "칼로리",
                            fontSize = 10.sp,
                            color = colors.textSecondary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = data.kcal.toString(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.success,
                            )
                            Text(
                                text = "kcal",
                                fontSize = 10.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = 2.dp, bottom = 3.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Stroke breakdown section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "영법 비율",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        letterSpacing = 0.4.sp,
                    )
                    Text(
                        text = "${formatNumber(data.distanceM)}m",
                        fontSize = 11.sp,
                        fontFamily = JetBrainsMonoFamily,
                        color = colors.textTertiary,
                    )
                }

                Spacer(Modifier.height(8.dp))
                StrokeRatioBar(strokes = data.strokes, barHeight = 12.dp)
                Spacer(Modifier.height(10.dp))

                // Stroke percentage grid (4 columns)
                val strokeEntries = listOf(
                    Triple("자유형", data.strokes.freestyle, colors.accentCyan),
                    Triple("평영", data.strokes.breaststroke, colors.accentPurple),
                    Triple("배영", data.strokes.backstroke, colors.accentGold),
                    Triple("접영", data.strokes.butterfly, colors.success),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    strokeEntries.forEach { (label, ratio, color) ->
                        val pct = (ratio * 100).toInt()
                        val meters = (data.distanceM * ratio).toInt()
                        val bgAlpha = if (pct > 0) {
                            if (colors.isDark) 0.03f else 0.04f
                        } else 0f

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SoodalShape.sm)
                                .background(
                                    if (bgAlpha > 0f) {
                                        if (colors.isDark) Color.White.copy(alpha = bgAlpha) else Color.Black.copy(alpha = bgAlpha)
                                    } else Color.Transparent
                                )
                                .alpha(if (pct > 0) 1f else 0.4f)
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color),
                                )
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textSecondary,
                                )
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
                            Text(
                                text = "${meters}m",
                                fontSize = 9.sp,
                                color = colors.textTertiary,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Shell reward banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentGold.copy(alpha = 0.08f))
                        .border(1.dp, colors.accentGold.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Shell, tint = colors.accentGold, size = 18.dp)
                    Text(
                        text = "조개 ${data.shellReward}개 획득!",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentGold,
                    )
                }
            } else {
                // No swim data
                Text(
                    text = "이 날은 수영 기록이 없어요",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

// ── Month Summary Panel ─────────────────────────────────────────
@Composable
private fun MonthSummaryPanel(swimData: Map<Int, SwimDayData>) {
    val colors = SoodalDesign.colors
    val swimDays = swimData.size
    val totalShells = swimData.values.sumOf { it.shellReward }
    val totalDistanceKm = swimData.values.sumOf { it.distanceM } / 1000f

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Top,
        ) {
            // Swim days
            SummaryItem(
                icon = SoodalIcons.Swimmer,
                iconColor = colors.accentCyan,
                value = swimDays.toString(),
                unit = "일",
                label = "수영",
            )
            // Shells
            SummaryItem(
                icon = SoodalIcons.Shell,
                iconColor = colors.accentGold,
                value = totalShells.toString(),
                unit = "개",
                label = "조개",
            )
            // Distance
            SummaryItem(
                icon = SoodalIcons.Ruler,
                iconColor = colors.accentPurple,
                value = String.format("%.1f", totalDistanceKm),
                unit = "km",
                label = "거리",
            )
        }
    }
}

@Composable
private fun SummaryItem(
    icon: SoodalIcons,
    iconColor: Color,
    value: String,
    unit: String,
    label: String,
) {
    val colors = SoodalDesign.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SoodalIcon(icon = icon, tint = iconColor, size = 26.dp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
            )
            Text(
                text = unit,
                fontSize = 10.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = 2.dp, bottom = 1.dp),
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = colors.textSecondary,
        )
    }
}

// ── Utility ─────────────────────────────────────────────────────
private fun formatNumber(n: Int): String {
    return if (n >= 1000) {
        val s = n.toString()
        buildString {
            s.forEachIndexed { i, c ->
                if (i > 0 && (s.length - i) % 3 == 0) append(',')
                append(c)
            }
        }
    } else {
        n.toString()
    }
}
