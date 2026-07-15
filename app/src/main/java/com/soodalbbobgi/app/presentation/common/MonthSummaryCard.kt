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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.StrokePalette
import com.soodalbbobgi.app.core.ui.SoodalCard

/** 문장 속 거리 강조 파랑 — 디자인 v3 가독성 개정: accentBlue보다 진한 본문 강조색. */
private val EmphasisBlue = Color(0xFF2563EB)

/** 영법 이름 → 영법 고유 파스텔 색 (그래프/색칩용). 모르는 이름은 자유형 색 폴백. */
fun strokeColorOf(name: String): Color = when (name) {
    "자유형" -> StrokePalette.Free
    "평영" -> StrokePalette.Breast
    "배영" -> StrokePalette.Back
    "접영" -> StrokePalette.Fly
    "혼영" -> StrokePalette.Medley
    "킥판" -> StrokePalette.Kick
    else -> StrokePalette.Free
}

/** 영법 이름 → 텍스트용 고채도 색 (%·영법명 등 글자용). */
fun strokeTextColorOf(name: String): Color = when (name) {
    "자유형" -> StrokePalette.FreeText
    "평영" -> StrokePalette.BreastText
    "배영" -> StrokePalette.BackText
    "접영" -> StrokePalette.FlyText
    "혼영" -> StrokePalette.MedleyText
    "킥판" -> StrokePalette.KickText
    else -> StrokePalette.FreeText
}

/**
 * 월 수영 요약 카드 — 월/델타% 헤더 + 문장(주력 영법 포함) + 지난달 2줄 비교.
 *
 * 비교 기준은 지난달 '같은 기간'(1일~오늘 일자) 페이스다 — 진행 중인 달을 지난달
 * 전체와 비교하면 월 중반엔 항상 뒤처져 보이기 때문. (기간 산출은 호출 측 담당)
 *
 * @param monthLabel 헤더 좌측 라벨 (예: "6월")
 * @param subjectLabel 문장 주어 (현재 달이면 "이번 달", 과거 달이면 "6월" 등)
 * @param lastMonthDistance 비교 기준 지난달 같은 기간 거리(m). 0이면 델타% 미표시
 */
@Composable
fun MonthSummaryCard(
    monthLabel: String,
    subjectLabel: String,
    distanceM: Int,
    sessions: Int,
    kcal: Int,
    lastMonthDistance: Int,
    lastMonthSessions: Int,
    topStroke: String?,
    onClick: (() -> Unit)? = null,
) {
    val colors = SoodalDesign.colors
    val distanceDelta = distanceM - lastMonthDistance
    val countDelta = sessions - lastMonthSessions
    val pct = if (lastMonthDistance > 0) Math.round(distanceDelta.toFloat() / lastMonthDistance * 100) else 0
    val tapModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    SoodalCard(modifier = Modifier.fillMaxWidth().then(tapModifier)) {
        Column {
            // 헤더: 월 + 지난달 대비 거리 %
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(monthLabel, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                if (lastMonthDistance > 0) {
                    Text(
                        "${if (pct >= 0) "+" else ""}$pct% ${if (pct >= 0) "↑" else "↓"}",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (pct >= 0) colors.success else colors.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            // 문장 (수치 색 강조 + 주력 영법은 영법 고유색)
            Text(
                text = buildAnnotatedString {
                    append("${subjectLabel}은 ")
                    pushStyle(SpanStyle(color = colors.accentPurple, fontWeight = FontWeight.ExtraBold))
                    append("${sessions}회")
                    pop()
                    append(" 수영해서 ")
                    pushStyle(SpanStyle(color = EmphasisBlue, fontWeight = FontWeight.ExtraBold))
                    append("${formatThousands(distanceM)}m")
                    pop()
                    append(" 헤엄치고, ")
                    pushStyle(SpanStyle(color = colors.success, fontWeight = FontWeight.ExtraBold))
                    append("${formatThousands(kcal)}kcal")
                    pop()
                    append("를 태웠어요.")
                    if (topStroke != null) {
                        append(" ")
                        pushStyle(SpanStyle(color = strokeTextColorOf(topStroke), fontWeight = FontWeight.ExtraBold))
                        append(topStroke)
                        pop()
                        append("을 가장 많이 했어요.")
                    }
                },
                fontSize = 15.sp, color = colors.textPrimary, lineHeight = 24.sp,
            )
            // 지난달 비교 — 상단 구분선 + 2줄
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))
            Spacer(Modifier.height(12.dp))
            Text("지난달 이맘때보다", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    pushStyle(SpanStyle(color = colors.accentPurple, fontWeight = FontWeight.ExtraBold))
                    append("${Math.abs(countDelta)}회")
                    pop()
                    append(if (countDelta >= 0) " 더 했어요." else " 덜 했어요.")
                },
                fontSize = 13.sp, color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildAnnotatedString {
                    pushStyle(SpanStyle(color = EmphasisBlue, fontWeight = FontWeight.ExtraBold))
                    append("${formatThousands(Math.abs(distanceDelta))}m")
                    pop()
                    append(if (distanceDelta >= 0) " 더 헤엄쳤어요." else " 덜 헤엄쳤어요.")
                },
                fontSize = 13.sp, color = colors.textPrimary,
            )
        }
    }
}

private fun formatThousands(n: Int): String = String.format("%,d", n)
