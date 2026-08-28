package com.soodalbbobgi.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.SoodalDesign
import kotlinx.coroutines.delay

/** 조개를 받은 계기 — 팝업 문구가 이 값에 따라 갈린다. */
enum class ShellRewardKind {
    /** 수영 기록이 들어와 받은 조개 (동기화·수동 등록). */
    SwimRecord,

    /** 영법별 거리를 채워 받은 보너스 조개. */
    StrokeBonus,
}

/**
 * 조개 보상 팝업 (디자인 확정) — 받은 조개를 축하 연출과 함께 보여준다.
 * 색은 테마 토큰을 따라 라이트/다크 자동 전환된다.
 *
 * **닫힘 방식이 두 가지다.** [onEditStrokes]가 있으면 사용자에게 선택을 요구하므로
 * 자동으로 닫지 않는다 — 고르기 전에 사라지면 안 되기 때문. 없으면 2.6초 뒤 자동으로 닫힌다.
 * 어느 쪽이든 바깥을 탭하면 닫힌다.
 *
 * @param shellCount 획득한 조개 수
 * @param kind 조개를 받은 계기 — 제목·설명 문구가 달라진다
 * @param distanceM 기록 요약에 보여줄 거리(m). null이면 요약 줄 생략
 * @param durationMin 기록 요약에 보여줄 시간(분). null이면 요약 줄 생략
 * @param onEditStrokes 영법 입력으로 넘어가는 동작. null이면 유도 문구와 버튼을 숨기고 자동 닫힘
 */
@Composable
fun ShellRewardPopup(
    shellCount: Int,
    kind: ShellRewardKind = ShellRewardKind.SwimRecord,
    distanceM: Int? = null,
    durationMin: Int? = null,
    onEditStrokes: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val colors = SoodalDesign.colors
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // 뽑기 팝업 계열 등장 — 보상 팝업답게 조금 더 크게 튀어오르는 스프링 (0.82→1, 살짝 오버슈트).
    val p by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 380f),
        label = "popup-enter",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "popup-alpha",
    )
    // (탭바 dim 불필요 — 오버레이 레이어로 호이스팅되어 스크림이 탭바까지 직접 덮는다)

    // 백키 = 팝업 닫기 우선 (오버레이가 화면보다 나중에 컴포즈되므로 이 핸들러가 우선한다).
    androidx.activity.compose.BackHandler { visible = false; onDismiss() }

    // 고를 게 있으면 저절로 닫지 않는다 — 버튼을 누르기도 전에 사라지면 선택지가 아니다.
    if (onEditStrokes == null) {
        LaunchedEffect(Unit) {
            delay(2600)
            visible = false
            delay(400)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SoodalDimAlpha * bgAlpha))
            .alpha(bgAlpha)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                // 딤은 컨트롤이 아니다 — 누름 틴트를 얹으면 닫히는 중에도 깜빡인다.
                indication = null,
                onClick = {
                    visible = false
                    onDismiss()
                },
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 글래스 패널 — 뒤 콘텐츠 프로스트(블러) + 흰 하이라이트 보더 + 상단 sheen.
        // 등장: 뽑기 팝업과 동일 (graphicsLayer scale 0.9→1 + alpha).
        val panelShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    // 첫 프레임은 스케일 1로 배치 — 축소 상태 배치는 블러 위치가 어긋난 채 굳는다.
                    val s = com.soodalbbobgi.app.core.ui.motion.popupEnterScale(p)
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                    // alpha<1일 때 오프스크린 합성이 경계 밖 그림자를 잘라 스프링 정착 중
                    // 그림자가 깜빡인다 — 클립 없는 알파 변조로 그린다.
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                }
                .glassShadow(24.dp, colors)
                .glassFrost(colors, panelShape, LocalHazeContent.current)
                .border(1.dp, colors.glassBorder, panelShape),
        ) {
        GlassSheen(panelShape)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, top = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 출처 칩
            Row(
                modifier = Modifier
                    .background(colors.accentBlue.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SoodalIcon(
                    icon = if (kind == ShellRewardKind.StrokeBonus) SoodalIcons.Edit else SoodalIcons.Sync,
                    tint = colors.accentBlue,
                    size = 13.dp,
                )
                Spacer(Modifier.size(6.dp))
                Text(
                    if (kind == ShellRewardKind.StrokeBonus) "영법 보너스" else "수영 기록 동기화",
                    fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = colors.accentBlue, letterSpacing = 0.3.sp,
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                if (kind == ShellRewardKind.StrokeBonus) "영법 입력 완료!" else "수영 기록 도착!",
                fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = colors.textPrimary, letterSpacing = (-0.1).sp,
            )

            // 버스트(왼쪽) + 획득량/기록 요약(오른쪽) — 세로로 길어지지 않게 가로 배치
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                ShellStack(shellCount)
                Column {
                    // 옆에 조개 그림이 이미 쌓여 있으므로 "조개"라고 덧붙이지 않는다.
                    Text(
                        text = "+$shellCount",
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.accentGold,
                        letterSpacing = (-0.8).sp,
                    )
                    // 기록 요약
                    if (distanceM != null && durationMin != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .background((if (colors.isDark) Color.White else Color.Black).copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
                        ) {
                            Row {
                                Text(String.format("%,d", distanceM), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentBlue, fontFamily = JetBrainsMonoFamily)
                                Text(" m", fontSize = 12.sp, color = colors.textSecondary, fontFamily = JetBrainsMonoFamily)
                            }
                            Row {
                                Text("$durationMin", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentBlue, fontFamily = JetBrainsMonoFamily)
                                Text(" 분", fontSize = 12.sp, color = colors.textSecondary, fontFamily = JetBrainsMonoFamily)
                            }
                        }
                    }
                }
            }

            if (onEditStrokes != null) {
                // 영법을 채우면 조개가 하나 더 붙는다는 걸 여기서 알린다 — 안 그러면
                // 사용자가 이 버튼을 눌러야 할 이유를 알 방법이 없다.
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        "영법을 수정하고",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary,
                    )
                    SoodalIcon(icon = SoodalIcons.Shell, tint = colors.accentGold, size = 14.dp)
                    Text(
                        "을 하나 더 받을 수 있어요",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                ) {
                    SoodalButton(
                        text = "나중에",
                        onClick = { visible = false; onDismiss() },
                        style = ButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                        heightOverride = 44.dp,
                    )
                    SoodalButton(
                        text = "입력하고 받기",
                        onClick = onEditStrokes,
                        style = ButtonStyle.Primary,
                        modifier = Modifier.weight(1f),
                        heightOverride = 44.dp,
                    )
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Text("탭하면 닫혀요", fontSize = 11.sp, color = colors.textTertiary, letterSpacing = 0.3.sp)
            }
        }
        } // 글래스 패널 Box
    }
}

/** 한 화면에 겹쳐 보여줄 조개 최대 개수 — 정확한 수는 옆의 "+N"이 말해준다. */
private const val MAX_STACKED_SHELLS = 5

/** 큰 조개 한 개 크기. */
private val StackedShellSize = 56.dp

/**
 * 받은 개수만큼 큰 조개를 겹쳐 쌓아 보여준다 + 골드 글로우 펄스.
 *
 * 조개가 하나씩 시차를 두고 튀어나와 개수가 눈으로 세어진다.
 * 많이 받아도 폭이 넘치지 않도록 겹치는 간격을 줄이고, [MAX_STACKED_SHELLS]까지만 그린다.
 */
@Composable
private fun ShellStack(shellCount: Int) {
    val gold = SoodalDesign.colors.accentGold
    val shown = shellCount.coerceIn(1, MAX_STACKED_SHELLS)
    // 그룹 전체 폭이 상자(112dp) 안에 들어오도록 개수가 늘면 간격을 좁힌다.
    val step = if (shown <= 1) 0.dp else minOf(16.dp, 56.dp / (shown - 1))

    Box(modifier = Modifier.size(112.dp), contentAlignment = Alignment.Center) {
        // 글로우 펄스
        val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 0.85f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "glow-pulse",
        )
        Box(
            modifier = Modifier
                .size(112.dp)
                .scale(pulse)
                .background(
                    Brush.radialGradient(listOf(gold.copy(alpha = 0.30f), Color.Transparent)),
                    RoundedCornerShape(999.dp),
                ),
        )

        repeat(shown) { i ->
            key(i) {
                val popIn = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(100L + i * 90L)
                    popIn.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
                }
                // 그룹의 가운데가 상자 중앙에 오도록 좌우로 반씩 민다.
                val xOffset = step * (i - (shown - 1) / 2f)
                Box(
                    modifier = Modifier
                        .offset { IntOffset(xOffset.roundToPx(), 0) }
                        .scale(popIn.value),
                ) {
                    SoodalIcon(icon = SoodalIcons.Shell, tint = gold, size = StackedShellSize)
                }
            }
        }
    }
}
