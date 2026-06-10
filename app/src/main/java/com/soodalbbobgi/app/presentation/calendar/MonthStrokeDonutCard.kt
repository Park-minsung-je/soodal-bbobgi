package com.soodalbbobgi.app.presentation.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.StrokePalette
import com.soodalbbobgi.app.core.ui.SoodalCard
import kotlin.math.cos
import kotlin.math.sin

/**
 * 월간 영법별 기록 도넛 카드 — 두꺼운 링 + 리더선 라벨(영법명, m·%) + 중앙 총거리 +
 * 하단 인사이트 한 줄. 캘린더에서 선택한 달 기준으로 표시한다.
 *
 * @param monthLabel 헤더 라벨 (예: "6월")
 * @param subjectLabel 인사이트 문장 주어 ("이번 달" 또는 "6월")
 * @param strokeMeters (영법명, 거리m, 색) 목록 — 표시 순서대로, 0m은 자동 제외
 * @param totalDistanceM 그 달 총 거리(m) — 중앙/헤더 표기
 * @param sessions 그 달 수영 횟수 — 평균 거리 계산용
 */
@Composable
fun MonthStrokeDonutCard(
    monthLabel: String,
    subjectLabel: String,
    strokeMeters: List<Triple<String, Int, Color>>,
    totalDistanceM: Int,
    sessions: Int,
) {
    val colors = SoodalDesign.colors
    val textMeasurer = rememberTextMeasurer()
    val segs = remember(strokeMeters) { donutSegments(strokeMeters.map { it.first to it.second }) }
    val colorOf = remember(strokeMeters) { strokeMeters.associate { it.first to it.third } }

    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // 헤더: "N월 영법별 기록" + 총 거리
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$monthLabel 영법별 기록", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Text(
                    text = buildAnnotatedString {
                        pushStyle(SpanStyle(color = colors.textSecondary))
                        append("총 ")
                        pop()
                        pushStyle(SpanStyle(color = colors.accentBlue, fontWeight = FontWeight.ExtraBold))
                        append(String.format("%,dm", totalDistanceM))
                    },
                    fontSize = 12.sp,
                    fontFamily = JetBrainsMonoFamily,
                )
            }

            if (segs.isEmpty()) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "이 달은 영법 기록이 없어요",
                    fontSize = 12.sp,
                    color = colors.textTertiary,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                )
            } else {
                Spacer(Modifier.height(6.dp))
                Canvas(modifier = Modifier.fillMaxWidth().height(196.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val outerR = 64.dp.toPx()
                    val thickness = 30.dp.toPx()
                    val midR = outerR - thickness / 2f
                    val gapDeg = if (segs.size > 1) 1.6f else 0f

                    // 12시 기준 시계방향 각도 → 화면 좌표
                    fun pointAt(radius: Float, angleDeg: Float): Offset {
                        val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
                        return Offset(cx + radius * cos(rad).toFloat(), cy + radius * sin(rad).toFloat())
                    }

                    // 세그먼트 링 (사이 살짝 띄워 경계를 깔끔하게)
                    segs.forEach { s ->
                        val color = colorOf[s.label] ?: StrokePalette.Free
                        drawArc(
                            color = color,
                            startAngle = s.startAngle - 90f + gapDeg / 2f,
                            sweepAngle = (s.sweepAngle - gapDeg).coerceAtLeast(0.5f),
                            useCenter = false,
                            topLeft = Offset(cx - midR, cy - midR),
                            size = Size(midR * 2, midR * 2),
                            style = Stroke(width = thickness, cap = StrokeCap.Butt),
                        )
                    }

                    // 리더선 라벨 — 같은 쪽 라벨은 세로 간격을 벌려 충돌 방지
                    val elbow = 12.dp.toPx()
                    val arm = 14.dp.toPx()
                    val minGap = 30.dp.toPx()
                    listOf(true, false).forEach { side ->
                        val sideSegs = segs.filter { it.onRight == side }
                        if (sideSegs.isEmpty()) return@forEach
                        val rawYs = sideSegs.map { pointAt(outerR + elbow, it.midAngle).y }
                        val ys = spreadLabelYs(rawYs, minGap)
                        sideSegs.forEachIndexed { i, s ->
                            val color = colorOf[s.label] ?: StrokePalette.Free
                            val start = pointAt(outerR - 1.dp.toPx(), s.midAngle)
                            val bend = Offset(pointAt(outerR + elbow, s.midAngle).x, ys[i])
                            val endX = if (side) bend.x + arm else bend.x - arm
                            val path = Path().apply {
                                moveTo(start.x, start.y)
                                lineTo(bend.x, bend.y)
                                lineTo(endX, bend.y)
                            }
                            drawPath(path, color, style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round))
                            drawCircle(color, radius = 2.dp.toPx(), center = Offset(endX, bend.y))

                            val nameLayout = textMeasurer.measure(
                                AnnotatedString(s.label),
                                TextStyle(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary),
                            )
                            val valueLayout = textMeasurer.measure(
                                AnnotatedString(String.format("%,dm · %d%%", s.meters, s.pct)),
                                TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, fontFamily = JetBrainsMonoFamily),
                            )
                            val pad = 5.dp.toPx()
                            val nameX = if (side) endX + pad else endX - pad - nameLayout.size.width
                            val valueX = if (side) endX + pad else endX - pad - valueLayout.size.width
                            drawText(nameLayout, topLeft = Offset(nameX, bend.y - nameLayout.size.height + 2.dp.toPx()))
                            drawText(valueLayout, topLeft = Offset(valueX, bend.y + 3.dp.toPx()))
                        }
                    }

                    // 중앙 총거리 (km)
                    val kmLayout = textMeasurer.measure(
                        AnnotatedString(String.format("%.1f", totalDistanceM / 1000f)),
                        TextStyle(fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary, fontFamily = JetBrainsMonoFamily),
                    )
                    drawText(kmLayout, topLeft = Offset(cx - kmLayout.size.width / 2f, cy - kmLayout.size.height + 2.dp.toPx()))
                    val unitLayout = textMeasurer.measure(
                        AnnotatedString("km 총거리"),
                        TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = colors.textTertiary),
                    )
                    drawText(unitLayout, topLeft = Offset(cx - unitLayout.size.width / 2f, cy + 3.dp.toPx()))
                }

                // 인사이트 — 가장 많이 한 영법 + 평균 거리
                val top = segs.maxByOrNull { it.meters }
                if (top != null && sessions > 0) {
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("${subjectLabel} 가장 많이 한 영법은 ")
                            pushStyle(SpanStyle(color = colorOf[top.label] ?: colors.accentBlue, fontWeight = FontWeight.ExtraBold))
                            append(top.label)
                            pop()
                            append(", 평균 ")
                            pushStyle(SpanStyle(color = colors.textPrimary, fontWeight = FontWeight.ExtraBold))
                            append(String.format("%,dm", totalDistanceM / sessions))
                            pop()
                            append("씩 ${sessions}회 수영했어요.")
                        },
                        fontSize = 12.5.sp, color = colors.textSecondary, lineHeight = 19.sp,
                    )
                }
            }
        }
    }
}
