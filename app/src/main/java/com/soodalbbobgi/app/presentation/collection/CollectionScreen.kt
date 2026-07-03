package com.soodalbbobgi.app.presentation.collection

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.AssetImage
import com.soodalbbobgi.app.core.ui.GlassBox
import com.soodalbbobgi.app.core.ui.GlassCorner
import com.soodalbbobgi.app.core.ui.GlassSheen
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.glass
import com.soodalbbobgi.app.core.ui.soodalScreenBackdrop
import com.soodalbbobgi.app.domain.model.Grade

private val KIND_LABEL = mapOf("char" to "캐릭터", "bg" to "배경", "frame" to "액자")

/** 등급 → 잉크색 (칩·링 텍스트). */
@Composable
private fun gradeInk(grade: Grade): Color = when (grade) {
    Grade.SSR -> SoodalDesign.colors.accentGold
    Grade.SR -> SoodalDesign.colors.accentPurple
    Grade.R -> SoodalDesign.colors.accentBlue
    Grade.N -> SoodalDesign.colors.textSecondary
}

/** 등급 → 옅은 배경 틴트. */
@Composable
private fun gradeSoft(grade: Grade): Color = when (grade) {
    Grade.N -> Color(0xFF1E3C64).copy(alpha = 0.08f)
    else -> gradeInk(grade).copy(alpha = 0.16f)
}

/**
 * 내 컬렉션 화면 (디자인 screen-collection) — 수집률 히어로 + 카테고리 탭 + 3열 도감 그리드.
 * 미보유 아이템은 흐리게 + 자물쇠 + "？？？"로 가려 보여준다.
 */
@Composable
fun CollectionScreen(
    onBack: () -> Unit,
    viewModel: CollectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = SoodalDesign.colors
    var tab by rememberSaveable { mutableStateOf("all") }

    // 푸시 서브 화면 — 슬라이드 전환 중 뒤 화면(홈)이 비치지 않게 자체 배경을 칠한다.
    Box(Modifier.fillMaxSize().soodalScreenBackdrop()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // ── 헤더: 뒤로가기 + 타이틀 ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val backShape = RoundedCornerShape(13.dp)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .glass(colors, 13.dp, backShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    SoodalIcon(icon = SoodalIcons.ArrowLeft, tint = colors.textPrimary, size = 18.dp)
                    GlassSheen(backShape)
                }
                Text("내 컬렉션", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                // ── 수집률 히어로 ───────────────────────────────
                GlassBox(modifier = Modifier.fillMaxWidth(), cornerDp = GlassCorner, contentPadding = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        val pct = if (state.totalCount > 0) state.ownedCount * 100 / state.totalCount else 0
                        CompletionRing(pct = pct)
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "${state.ownedCount}",
                                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                                    fontFamily = JetBrainsMonoFamily, color = colors.textPrimary,
                                )
                                Text(
                                    " / ${state.totalCount}",
                                    fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textTertiary,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                                Text(
                                    "  수집 완료",
                                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(Grade.SSR, Grade.SR, Grade.R, Grade.N).forEach { g ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .background(gradeSoft(g))
                                            .padding(horizontal = 9.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Text(g.name, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = gradeInk(g))
                                        Text(
                                            "${state.ownedByGrade(g)}",
                                            fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                            fontFamily = JetBrainsMonoFamily, color = gradeInk(g),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 카테고리 탭 ─────────────────────────────────
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf(
                        "all" to "전체",
                        "char" to "캐릭터",
                        "bg" to "배경",
                        "frame" to "액자",
                    ).forEach { (id, label) ->
                        val on = tab == id
                        val count = if (id == "all") state.ownedCount else state.ofKind(id).count { it.owned }
                        val tabShape = RoundedCornerShape(14.dp)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(tabShape)
                                .then(
                                    if (on) Modifier.background(colors.accentPurple)
                                    else Modifier.glass(colors, 14.dp, tabShape),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { tab = id }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (on) Color.White else colors.textSecondary)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "$count",
                                fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = JetBrainsMonoFamily,
                                color = if (on) Color.White.copy(alpha = 0.85f) else colors.textTertiary,
                            )
                        }
                    }
                }

                // ── 카테고리 그룹 (3열 그리드) ───────────────────
                val kinds = if (tab == "all") listOf("char", "bg", "frame") else listOf(tab)
                kinds.forEach { kind ->
                    val items = state.ofKind(kind)
                    if (items.isEmpty()) return@forEach
                    Spacer(Modifier.height(20.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(KIND_LABEL[kind] ?: kind, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "${items.count { it.owned }}",
                                fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = JetBrainsMonoFamily, color = colors.accentBlue,
                            )
                            Text(
                                " / ${items.size}",
                                fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                fontFamily = JetBrainsMonoFamily, color = colors.textTertiary,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    items.chunked(3).forEachIndexed { rowIndex, rowItems ->
                        if (rowIndex > 0) Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowItems.forEach { entry -> CollectionCell(entry, Modifier.weight(1f)) }
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // ── 푸터 힌트 ───────────────────────────────────
                val remaining = state.totalCount - state.ownedCount
                if (remaining > 0) {
                    Spacer(Modifier.height(22.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.accentPurple.copy(alpha = 0.12f))
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SoodalIcon(icon = SoodalIcons.Sparkle, tint = colors.accentPurple, size = 15.dp)
                        Spacer(Modifier.size(7.dp))
                        Text(
                            text = buildString { append("뽑기로 ") },
                            fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary,
                        )
                        Text("${remaining}종", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentPurple)
                        Text("을 더 모을 수 있어요", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** 수집률 원형 진행 링 — 가운데 % + "수집률". */
@Composable
private fun CompletionRing(pct: Int, size: androidx.compose.ui.unit.Dp = 92.dp, stroke: androidx.compose.ui.unit.Dp = 9.dp) {
    val colors = SoodalDesign.colors
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = stroke.toPx()
            val arcSize = Size(this.size.width - sw, this.size.height - sw)
            val topLeft = Offset(sw / 2f, sw / 2f)
            drawArc(
                color = Color(0xFF1E3C64).copy(alpha = 0.10f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = sw),
            )
            drawArc(
                color = colors.accentBlue,
                startAngle = -90f, sweepAngle = 360f * pct / 100f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$pct", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = JetBrainsMonoFamily, color = colors.textPrimary)
                Text("%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary, modifier = Modifier.padding(bottom = 3.dp))
            }
            Text("수집률", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
        }
    }
}

/** 도감 셀 — 썸네일(등급 링) + 이름 + 상태. 미보유는 흐림 + 자물쇠 + ？？？. */
@Composable
private fun CollectionCell(entry: CollectionEntry, modifier: Modifier = Modifier) {
    val colors = SoodalDesign.colors
    val ink = gradeInk(entry.grade)
    val cellShape = RoundedCornerShape(16.dp)
    Column(modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(cellShape)
                .background(if (entry.owned) gradeSoft(entry.grade) else Color(0xFF1E3C64).copy(alpha = 0.05f))
                .border(
                    1.5.dp,
                    if (entry.owned) ink.copy(alpha = 0.45f) else Color(0xFF1E3C64).copy(alpha = 0.08f),
                    cellShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!entry.imageAsset.isNullOrBlank()) {
                AssetImage(
                    imageAsset = entry.imageAsset,
                    contentDescription = entry.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .alpha(if (entry.owned) 1f else 0.3f),
                )
            }
            // 등급 태그 — 좌상단
            Text(
                text = entry.grade.name,
                fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = ink,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            )
            // 장착 중 체크 — 우상단
            if (entry.equipped) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accentBlue),
                    contentAlignment = Alignment.Center,
                ) {
                    SoodalIcon(icon = SoodalIcons.Check, tint = Color.White, size = 11.dp)
                }
            }
            // 잠금 오버레이 — 미보유
            if (!entry.owned) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.White.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center,
                ) {
                    SoodalIcon(icon = SoodalIcons.Lock, tint = colors.textSecondary, size = 15.dp)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (entry.owned) entry.name else "？？？",
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = if (entry.owned) colors.textPrimary else colors.textTertiary,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp),
        )
        Text(
            text = when {
                entry.equipped -> "착용 중"
                entry.owned -> "보유"
                else -> "미획득"
            },
            fontSize = 10.sp, fontWeight = if (entry.equipped) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (entry.equipped) colors.accentBlue else colors.textTertiary,
            modifier = Modifier.padding(start = 2.dp, top = 1.dp),
        )
    }
}
