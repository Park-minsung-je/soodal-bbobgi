package com.soodalbbobgi.app.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.StrokePalette
import com.soodalbbobgi.app.core.ui.SoodalCard

/** 섹션 제목 — 좌측 라벨 + 우측 액션(선택). 디자인 리듬: 위 22dp. */
@Composable
fun SectionLabel(text: String, action: (@Composable () -> Unit)? = null) {
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

/** 지난주 대비 추세 배지 — "+12% ↑"(녹색) / "-8% ↓"(로즈). null이면 미표시. */
@Composable
fun TrendBadge(trendPercent: Int?) {
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

/**
 * 최근 7일 활동 카드 — 총 거리 + 요일별 영법 스택 막대 (오늘 강조).
 * 트렌드 %는 카드 내부 우측 상단에 표시한다 (소제목 제거 디자인 — 카드가 자체 라벨을 가짐).
 *
 * @param trendPercent 지난주 대비 추세 %. null이면 미표시.
 * @param onTap 카드 탭 동작 (홈→캘린더 이동 등). null이면 탭 비활성.
 */
@Composable
fun WeeklyActivityCard(weekly: WeeklyActivity, trendPercent: Int? = null, onTap: (() -> Unit)? = null) {
    val colors = SoodalDesign.colors
    val maxV = (weekly.days.maxOfOrNull { it.distanceM } ?: 0).coerceAtLeast(1)
    val chartHeight = 84.dp

    val tapModifier = if (onTap != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onTap,
        )
    } else {
        Modifier
    }

    SoodalCard(modifier = Modifier.fillMaxWidth().then(tapModifier)) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(formatMeters(weekly.totalMeters), fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentBlue)
                    Text("m", fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.padding(start = 3.dp, bottom = 2.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (trendPercent != null) {
                        val up = trendPercent >= 0
                        Text(
                            text = "${if (up) "+" else ""}$trendPercent% ${if (up) "↑" else "↓"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (up) colors.success else Color(0xFFF43F5E),
                        )
                        Spacer(Modifier.height(3.dp))
                    }
                    Text("최근 7일 · ${weekly.activeDays}일 운동", fontSize = 11.sp, color = colors.textSecondary)
                }
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
                                            // 거리만 있고 영법 정보 없는 날 — 중립 회색 폴백 (영법 색과 헷갈리지 않게)
                                            else -> colors.textTertiary.copy(alpha = 0.55f)
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
                                                    .background(StrokePalette.ordered[i]),
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
                            color = if (bar.isToday) colors.accentBlue else colors.textTertiary,
                        )
                    }
                }
            }
        }
    }
}

private fun formatMeters(n: Int): String {
    if (n < 1000) return n.toString()
    val s = n.toString()
    return buildString {
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) append(',')
            append(c)
        }
    }
}
