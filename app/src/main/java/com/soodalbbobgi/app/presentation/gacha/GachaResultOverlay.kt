package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.AssetImage
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.DimTabBarWhileVisible
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.motion.rememberPopupEnter
import com.soodalbbobgi.app.domain.model.Grade

/** 카테고리 → 한국어 라벨. */
private fun kindLabel(kind: String): String = when (kind) {
    "char" -> "캐릭터"
    "bg" -> "배경"
    "frame" -> "테두리"
    else -> "아이템"
}

/** 카테고리 → 대표 아이콘 (이미지 없을 때 폴백). */
private fun kindIcon(kind: String): SoodalIcons = when (kind) {
    "char" -> SoodalIcons.Otter
    "bg" -> SoodalIcons.Aurora
    "frame" -> SoodalIcons.Frame
    else -> SoodalIcons.Gift
}

@Composable
private fun gradeColor(grade: Grade) = when (grade) {
    Grade.SSR -> SoodalDesign.colors.accentGold
    Grade.SR -> SoodalDesign.colors.accentPurple
    Grade.R -> SoodalDesign.colors.accentBlue
    Grade.N -> SoodalDesign.colors.textSecondary
}

/**
 * 뽑기/상점 박스 결과 공용 오버레이.
 *
 * 결과를 한 장씩 넘겨 보거나(다회), "전체 결과 보기"로 그리드 목록을 펼칠 수 있다.
 * 인덱스/전체보기 상태는 내부에서 관리하므로 호출부는 결과 리스트와 닫기 콜백만 넘기면 된다.
 *
 * @param results 표시할 결과 목록 (비어 있으면 아무것도 그리지 않음)
 * @param onClose 닫기/계속 콜백
 * @param onApplyProfile 캐릭터 결과에서 "프로필 적용"을 눌렀을 때 (null이면 버튼 숨김)
 */
@Composable
fun GachaResultOverlay(
    results: List<GachaResultItem>,
    onClose: () -> Unit,
    onApplyProfile: (() -> Unit)? = null,
) {
    if (results.isEmpty()) return
    val colors = SoodalDesign.colors
    val p = rememberPopupEnter()
    // 탭바 dim이 이 오버레이의 등장 스크림과 같은 박자로 움직이도록 진행도를 그대로 전달
    DimTabBarWhileVisible(alpha = p)

    var index by remember(results) { mutableIntStateOf(0) }
    var showAll by remember(results) { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f * p))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.graphicsLayer {
                scaleX = 0.9f + 0.1f * p
                scaleY = 0.9f + 0.1f * p
                alpha = p
            },
            contentAlignment = Alignment.Center,
        ) {
            if (showAll) {
                ResultGrid(results = results, onClose = onClose)
            } else {
                ResultSingle(
                    results = results,
                    index = index,
                    onNext = { if (index < results.size - 1) index++ },
                    onShowAll = { showAll = true },
                    onClose = onClose,
                    onApplyProfile = onApplyProfile,
                )
            }
        }
    }
}

/** 한 장씩 보기. */
@Composable
private fun ResultSingle(
    results: List<GachaResultItem>,
    index: Int,
    onNext: () -> Unit,
    onShowAll: () -> Unit,
    onClose: () -> Unit,
    onApplyProfile: (() -> Unit)?,
) {
    val colors = SoodalDesign.colors
    val item = results[index]
    val isLast = index == results.size - 1
    val gc = gradeColor(item.grade)
    val glow = gc.copy(alpha = if (item.grade == Grade.N) 0.1f else 0.55f)

    val bounceScale by animateFloatAsState(
        targetValue = 1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = 300f),
        label = "bounce",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // 모달 프레임 — 등급색 링 대신 공통 글래스 테두리(흰 하이라이트). 내부 배경은 화려하게 유지.
        Box(
            Modifier.padding(horizontal = 28.dp).fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = glow, spotColor = glow)
                .clip(RoundedCornerShape(24.dp))
                .drawBehind { drawRect(colors.gradCard) }
                .border(1.dp, colors.glassBorder, RoundedCornerShape(24.dp))
                .padding(28.dp, 24.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                if (results.size > 1) {
                    Text(
                        "${index + 1} / ${results.size}",
                        fontSize = 11.sp, color = colors.textSecondary, fontFamily = JetBrainsMonoFamily,
                        modifier = Modifier.align(Alignment.End)
                            .background((if (colors.isDark) Color.White else Color.Black).copy(alpha = 0.06f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                SoodalChip("${kindLabel(item.kind)} 상자 인양 성공!", color = ChipColor.Blue, iconType = SoodalIcons.Box)
                Spacer(Modifier.height(16.dp))
                GradeBadge(item.grade)
                Spacer(Modifier.height(20.dp))

                Box(
                    Modifier.size(80.dp).scale(bounceScale)
                        .clip(RoundedCornerShape(24.dp))
                        .background(glow.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!item.imageAsset.isNullOrBlank()) {
                        AssetImage(imageAsset = item.imageAsset, contentDescription = item.name, modifier = Modifier.fillMaxWidth())
                    } else {
                        SoodalIcon(icon = kindIcon(item.kind), tint = gc, size = 40.dp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(item.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Spacer(Modifier.height(6.dp))

                if (item.isNew) {
                    Text("${kindLabel(item.kind)} — 새로 획득!", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = gc)
                } else {
                    Text("이미 보유 중인 ${kindLabel(item.kind)}", fontSize = 12.sp, color = colors.textTertiary)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(listOf(colors.accentPurple.copy(alpha = 0.15f), colors.accentPurple.copy(alpha = 0.05f))),
                                RoundedCornerShape(12.dp),
                            )
                            .border(1.dp, colors.accentPurple.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SoodalIcon(icon = SoodalIcons.Pearl, tint = colors.accentPurple, size = 18.dp)
                        Text("진주 +${item.pearlsEarned}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, fontFamily = JetBrainsMonoFamily, color = colors.accentPurple)
                        Text("교환 완료", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.accentPurple.copy(alpha = 0.7f))
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isLast && results.size > 1) {
                        SoodalButton("다음 →", onClick = onNext, style = ButtonStyle.Primary, modifier = Modifier.weight(1f))
                    } else {
                        SoodalButton("계속", onClick = onClose, style = ButtonStyle.Secondary, modifier = Modifier.weight(1f))
                        if (item.kind == "char" && onApplyProfile != null) {
                            SoodalButton("프로필 적용", onClick = onApplyProfile, style = ButtonStyle.Primary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        if (!isLast && results.size > 1) {
            Spacer(Modifier.height(14.dp))
            SoodalButton("전체 결과 보기", onClick = onShowAll, style = ButtonStyle.Ghost)
        }
    }
}

/** 전체 결과 그리드 (2열 목록). */
@Composable
private fun ResultGrid(
    results: List<GachaResultItem>,
    onClose: () -> Unit,
) {
    val colors = SoodalDesign.colors
    Column(
        Modifier.padding(horizontal = 24.dp).fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .drawBehind { drawRect(colors.gradCard) }
            // 공통 글래스 테두리 — 단발 보기와 동일 프레임.
            .border(1.dp, colors.glassBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("전체 결과 (${results.size}개)", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        Spacer(Modifier.height(14.dp))

        Column(
            Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            results.chunked(2).forEach { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowItems.forEach { item ->
                        ResultGridCell(item = item, modifier = Modifier.weight(1f))
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SoodalButton("계속", onClick = onClose, style = ButtonStyle.Primary, modifier = Modifier.fillMaxWidth())
    }
}

/** 그리드 셀 1개. */
@Composable
private fun ResultGridCell(item: GachaResultItem, modifier: Modifier = Modifier) {
    val colors = SoodalDesign.colors
    val gc = gradeColor(item.grade)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background((if (colors.isDark) Color.White else Color.Black).copy(alpha = 0.05f))
            .border(1.dp, gc.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(gc.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.imageAsset.isNullOrBlank()) {
                AssetImage(imageAsset = item.imageAsset, contentDescription = item.name, modifier = Modifier.fillMaxWidth())
            } else {
                SoodalIcon(icon = kindIcon(item.kind), tint = gc, size = 22.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                GradeBadge(item.grade)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                item.name,
                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary, maxLines = 1,
            )
            if (item.isNew) {
                Text("NEW", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = gc)
            } else {
                Text("진주 +${item.pearlsEarned}", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = colors.accentPurple)
            }
        }
    }
}
