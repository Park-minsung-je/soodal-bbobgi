package com.soodalbbobgi.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// 축하 모달은 테마와 무관한 다크 카드 — 디자인 고정값.
private val CardTop = Color(0xFF1F2A44)
private val CardBottom = Color(0xFF131A2C)
private val PopupTxt1 = Color(0xFFE8EAF6)
private val PopupTxt2 = Color(0xFF8892A4)
private val PopupTxt3 = Color(0xFF6B7689)
private val PopupBlue = Color(0xFF5AC8FF)
private val PopupGold = Color(0xFFFFD60A)

// 디자인의 shell-fly 이징 — 살짝 튕기는 오버슈트.
private val ShellFlyEasing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)

/**
 * 조개 보상 팝업 (디자인 확정) — Health Connect 동기화로 받은 조개를 축하 연출과 함께 보여준다.
 * 2.6초 후 자동 닫힘, 탭하면 즉시 닫힘.
 *
 * @param shellCount 획득한 조개 수
 * @param distanceM 기록 요약에 보여줄 거리(m). null이면 요약 줄 생략
 * @param durationMin 기록 요약에 보여줄 시간(분). null이면 요약 줄 생략
 */
@Composable
fun ShellRewardPopup(
    shellCount: Int,
    distanceM: Int? = null,
    durationMin: Int? = null,
    onDismiss: () -> Unit,
) {
    var visible by remember { mutableStateOf(true) }

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.3f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 300f),
        label = "popup-scale",
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "popup-alpha",
    )

    LaunchedEffect(Unit) {
        delay(2600)
        visible = false
        delay(400)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f * bgAlpha))
            .alpha(bgAlpha)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    visible = false
                    onDismiss()
                },
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .scale(scale)
                .shadow(
                    elevation = 24.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = PopupGold.copy(alpha = 0.45f),
                    spotColor = PopupGold.copy(alpha = 0.45f),
                )
                .background(
                    Brush.linearGradient(listOf(CardTop, CardBottom)),
                    RoundedCornerShape(24.dp),
                )
                .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 출처 칩
            Row(
                modifier = Modifier
                    .background(PopupBlue.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SoodalIcon(icon = SoodalIcons.Sync, tint = PopupBlue, size = 13.dp)
                Spacer(Modifier.size(6.dp))
                Text("Health Connect 동기화", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PopupBlue, letterSpacing = 0.3.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text("수영 기록 도착!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PopupTxt1, letterSpacing = (-0.1).sp)

            // 조개 버스트
            Spacer(Modifier.height(18.dp))
            ShellBurst(shellCount)
            Spacer(Modifier.height(12.dp))

            // 획득량
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "+$shellCount",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Black,
                    color = PopupGold,
                    letterSpacing = (-0.8).sp,
                )
                Text(
                    text = "조개",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PopupTxt2,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                )
            }

            // 기록 요약
            if (distanceM != null && durationMin != null) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(18.dp),
                ) {
                    Row {
                        Text(String.format("%,d", distanceM), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PopupBlue, fontFamily = JetBrainsMonoFamily)
                        Text(" m", fontSize = 12.sp, color = PopupTxt2, fontFamily = JetBrainsMonoFamily)
                    }
                    Row {
                        Text("$durationMin", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = PopupBlue, fontFamily = JetBrainsMonoFamily)
                        Text(" 분", fontSize = 12.sp, color = PopupTxt2, fontFamily = JetBrainsMonoFamily)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text("탭하면 닫혀요", fontSize = 11.sp, color = PopupTxt3, letterSpacing = 0.3.sp)
        }
    }
}

/** 가운데 큰 조개 + 방사형으로 날아가는 작은 조개들 + 골드 글로우 펄스. */
@Composable
private fun ShellBurst(shellCount: Int) {
    // 1개여도 여러 개가 흩어지는 느낌이 나도록 최소 3개 버스트.
    val bursts = shellCount.coerceAtLeast(3)

    Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
        // 글로우 펄스
        val pulse by rememberInfiniteTransition(label = "glow").animateFloat(
            initialValue = 0.85f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label = "glow-pulse",
        )
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(pulse)
                .background(
                    Brush.radialGradient(listOf(PopupGold.copy(alpha = 0.30f), Color.Transparent)),
                    RoundedCornerShape(999.dp),
                ),
        )

        // 작은 조개들 — 시차를 두고 바깥으로 튕겨 나간다.
        repeat(bursts) { i ->
            key(i) {
                val progress = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(180L + i * 60L)
                    progress.animateTo(1f, tween(durationMillis = 500, easing = ShellFlyEasing))
                }
                val angle = (i.toFloat() / bursts) * 2f * PI.toFloat() - PI.toFloat() / 2f
                val xDp = 56.dp * cos(angle)
                val yDp = 56.dp * sin(angle)
                Box(
                    modifier = Modifier
                        .offset { IntOffset((xDp * progress.value).roundToPx(), (yDp * progress.value).roundToPx()) }
                        .alpha(progress.value.coerceIn(0f, 1f)),
                ) {
                    SoodalIcon(icon = SoodalIcons.Shell, tint = PopupGold, size = 20.dp)
                }
            }
        }

        // 가운데 큰 조개 — 스프링 팝.
        val centerScale = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            delay(100)
            centerScale.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 380f))
        }
        Box(modifier = Modifier.scale(centerScale.value)) {
            SoodalIcon(icon = SoodalIcons.Shell, tint = PopupGold, size = 68.dp)
        }
    }
}
