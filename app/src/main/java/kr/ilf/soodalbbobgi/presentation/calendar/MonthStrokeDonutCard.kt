package kr.ilf.soodalbbobgi.presentation.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ilf.soodalbbobgi.core.theme.JetBrainsMonoFamily
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.theme.StrokePalette
import kr.ilf.soodalbbobgi.core.ui.SoodalCard
import kr.ilf.soodalbbobgi.presentation.common.strokeTextColorOf
import kotlin.math.cos
import kotlin.math.sin

/**
 * 월간 영법별 기록 카드 — 좌측 도넛(작은 구멍) + 우측 범례 리스트(거리 내림차순) +
 * 하단 인사이트 한 줄. 캘린더에서 선택한 달 기준으로 표시한다.
 *
 * @param monthLabel 헤더 라벨 (예: "6월")
 * @param subjectLabel 인사이트 문장 주어 ("이번 달" 또는 "6월")
 * @param strokeMeters (영법명, 거리m, 색) 목록 — 0m은 자동 제외, 표시 정렬은 거리 내림차순
 * @param totalDistanceM 그 달 총 거리(m) — 헤더 표기
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
    // 거리 내림차순으로 정렬 — 도넛 세그먼트(12시부터 시계방향)와 범례 순서가 일치한다
    val sorted = remember(strokeMeters) { strokeMeters.filter { it.second > 0 }.sortedByDescending { it.second } }
    val segs = remember(sorted) { donutSegments(sorted.map { it.first to it.second }) }
    val colorOf = remember(sorted) { sorted.associate { it.first to it.third } }

    SoodalCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            // 헤더: "N월 영법별 기록" + 총 거리
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$monthLabel 영법별 기록", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Text(
                    text = buildAnnotatedString {
                        pushStyle(SpanStyle(color = colors.textSecondary))
                        append("총 ")
                        pop()
                        pushStyle(SpanStyle(color = colors.accentBlue, fontWeight = FontWeight.ExtraBold))
                        append(String.format("%,dm", totalDistanceM))
                    },
                    fontSize = 13.sp,
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
                Spacer(Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // 좌측 도넛 — 구멍이 작은 파이 형태.
                    // 세그먼트는 꽉 채워 그리고, 경계를 카드 배경색 직선으로 갈라
                    // 갭이 부채꼴이 아닌 일정한 폭(평행)으로 보이게 한다.
                    val cardBg = colors.cardBg
                    Canvas(modifier = Modifier.size(132.dp)) {
                        val outerR = size.minDimension / 2f
                        val holeR = outerR * 0.18f
                        val thickness = outerR - holeR
                        val midR = holeR + thickness / 2f
                        segs.forEach { s ->
                            drawArc(
                                color = colorOf[s.label] ?: StrokePalette.Free,
                                startAngle = s.startAngle - 90f,
                                sweepAngle = s.sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - midR, center.y - midR),
                                size = Size(midR * 2, midR * 2),
                                style = Stroke(width = thickness, cap = StrokeCap.Butt),
                            )
                        }
                        // 경계 분리선 — 중심→바깥 방향 직선이라 갭 폭이 어디서나 같다
                        if (segs.size > 1) {
                            fun pointAt(radius: Float, angleDeg: Float): Offset {
                                val rad = Math.toRadians(angleDeg.toDouble() - 90.0)
                                return Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat())
                            }
                            segs.forEach { s ->
                                drawLine(
                                    color = cardBg,
                                    start = pointAt(holeR - 1.dp.toPx(), s.startAngle),
                                    end = pointAt(outerR + 1.dp.toPx(), s.startAngle),
                                    strokeWidth = 1.dp.toPx(),
                                    cap = StrokeCap.Butt,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(18.dp))

                    // 우측 범례 — 색 칩 + 영법명 + 거리(우정렬) + %(영법색, 우정렬)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        segs.forEach { s ->
                            val color = colorOf[s.label] ?: StrokePalette.Free
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(12.dp).clip(RoundedCornerShape(4.dp)).background(color))
                                Spacer(Modifier.width(8.dp))
                                Text(s.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    String.format("%,dm", s.meters),
                                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary, fontFamily = JetBrainsMonoFamily,
                                )
                                Text(
                                    "${s.pct}%",
                                    fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                    color = strokeTextColorOf(s.label), fontFamily = JetBrainsMonoFamily,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.width(42.dp),
                                )
                            }
                        }
                    }
                }

                // 인사이트 — 가장 많이 한 영법 + 평균 거리
                if (sessions > 0) {
                    val top = segs.first()
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = buildAnnotatedString {
                            append("$subjectLabel 가장 많이 한 영법은 ")
                            pushStyle(SpanStyle(color = strokeTextColorOf(top.label), fontWeight = FontWeight.ExtraBold))
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
