package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.R
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.AssetImage
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.TabBarClearance
import kotlin.math.roundToInt
import kotlin.math.sin

// ── 인양 장면 기하 (디자인 확정값, dp) ──
private const val SCENE_H = 412f
private const val SURFACE_Y = 103f      // 수면 y
private const val CHEST_CY = 261f       // 떠다니는 상자들의 세로 중심
private const val CHEST_W = 92f
private val REEL_DIST = -(CHEST_CY - SURFACE_Y - 4f) // 상자 → 수면 이동 거리

/**
 * 보물 인양소 — 조개를 좋아하는 인양꾼 수달이 바닷속 보물상자를 건져 올리는 뽑기 화면.
 * 바다 단면 장면(수면 위 수달 + 로프 + 심해의 상자들) 위에서 스핀이 돌고,
 * 멈춘 상자가 로프를 타고 수면으로 끌려 올라온 뒤 결과가 열린다.
 */
@Composable
fun GachaScreen(
    viewModel: GachaViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val boxes by viewModel.boxes.collectAsState()
    val colors = SoodalDesign.colors

    Box(Modifier.fillMaxSize()) {
        // 헤더가 고정인 화면 — 루트에서 상태바 인셋 처리.
        Column(Modifier.fillMaxSize().background(colors.bgDeep).statusBarsPadding()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 12.dp),
            ) {
                // -- Header --
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SoodalIcon(icon = SoodalIcons.Gacha, size = 24.dp, tint = colors.accentBlue)
                            Text("보물 인양소", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                        }
                        Text(
                            "인양꾼 수달이 바닷속 보물을 건져 올려요",
                            fontSize = 11.5.sp, fontWeight = FontWeight.Medium,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 4.dp, start = 32.dp),
                        )
                    }
                    SoodalChip(state.shells.toString(), color = ChipColor.Gold, iconType = SoodalIcons.Shell, label = "조개", large = true)
                }

                Spacer(Modifier.height(14.dp))

                // -- Ocean salvage scene --
                Box(Modifier.padding(horizontal = 14.dp)) {
                    SalvageScene(
                        boxes = boxes,
                        offset = state.offset,
                        phase = state.phase,
                        risingBox = state.risingBox,
                    )
                }

                // -- Story caption --
                Text(
                    text = buildString {
                        append("조개라면 사족을 못 쓰는 인양꾼 수달.\n")
                        append("조개를 건네면 바닷속에 잠든 보물상자를 대신 건져 올려줘요.")
                    },
                    fontSize = 12.5.sp, lineHeight = 20.sp,
                    color = colors.textSecondary, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                )

                // -- Salvage buttons --
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SalvageButton(
                        title = "한 번 인양", cost = "조개 1개", badgeText = null,
                        bgColors = listOf(Color(0xFFFFC845), Color(0xFFF09000)),
                        textColor = colors.btnGoldText, glowColor = colors.glowGold,
                        badgeAccent = Color(0xFFB8860B),
                        enabled = state.phase == GachaPhase.Idle && state.shells >= 1,
                        onClick = { viewModel.spin(1) },
                        modifier = Modifier.weight(1f),
                    )
                    SalvageButton(
                        title = "10연 인양", cost = "조개 10개", badgeText = "×10",
                        bgColors = listOf(Color(0xFFB57BFF), Color(0xFF6E3BD8)),
                        textColor = colors.btnPurpleText, glowColor = colors.glowPurple,
                        badgeAccent = Color(0xFF6E3BD8),
                        enabled = state.phase == GachaPhase.Idle && state.shells >= 10,
                        onClick = { viewModel.spin(10) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(11.dp))

                // -- Footer --
                Text(
                    "※ 확률 정보는 상점 → FAQ에서 확인할 수 있어요",
                    fontSize = 11.sp, color = colors.textTertiary,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )

                Spacer(Modifier.height(TabBarClearance))
            }
        }

        // -- Result Modal (공용 오버레이: 인덱스/전체보기 내부 관리) --
        if (state.phase == GachaPhase.Result && state.results.isNotEmpty()) {
            GachaResultOverlay(
                results = state.results,
                onClose = { viewModel.closeResults() },
            )
        }
    }
}

/**
 * 바다 단면 인양 장면 — 수면 위 뗏목의 수달, 로프, 심해를 떠다니는 보물상자들.
 *
 * @param offset 룰렛 오프셋(dp 단위 누적값) — [GachaViewModel]의 스핀 로직과 공유
 * @param risingBox Reeling 단계에서 로프를 타고 올라오는 상자
 */
@Composable
private fun SalvageScene(
    boxes: List<BoxInfo>,
    offset: Float,
    phase: GachaPhase,
    risingBox: BoxInfo?,
) {
    val infinite = rememberInfiniteTransition(label = "ocean")

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(SCENE_H.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF1C5E7C),
                    0.18f to Color(0xFF114A66),
                    0.40f to Color(0xFF0E3E57),
                    0.64f to Color(0xFF0A2F47),
                    1f to Color(0xFF061F31),
                ),
            ),
    ) {
        val sceneW = maxWidth
        val centerX = sceneW.value / 2f

        // ── 수면에서 비치는 빛줄기 ──
        listOf(0.18f, 0.44f, 0.70f).forEachIndexed { i, fx ->
            val rayAlpha by infinite.animateFloat(
                initialValue = 0.28f, targetValue = 0.5f,
                animationSpec = infiniteRepeatable(
                    tween((7000 + i * 1600), easing = LinearEasing),
                    RepeatMode.Reverse,
                    initialStartOffset = StartOffset(i * 800),
                ),
                label = "ray$i",
            )
            Box(
                Modifier
                    .offset(x = sceneW * fx, y = (-10).dp)
                    .size((26 + i * 6).dp, (SCENE_H * 0.74f).dp)
                    .graphicsLayer {
                        rotationZ = -9f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        alpha = rayAlpha
                    }
                    .background(
                        Brush.verticalGradient(
                            0f to Color(0xFFB4EBFF).copy(alpha = 0.20f),
                            0.78f to Color.Transparent,
                        ),
                    ),
            )
        }

        // ── 올라오는 기포들 ──
        repeat(7) { i ->
            val left = (8 + (i * 13.5f) % 84f) / 100f
            val bSize = (4 + (i % 3) * 3).dp
            val dur = (4500 + (i % 4) * 900)
            val p by infinite.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(dur, easing = LinearEasing),
                    RepeatMode.Restart,
                    initialStartOffset = StartOffset(((i * 700) % 4000)),
                ),
                label = "bubble$i",
            )
            val bAlpha = if (p < 0.18f) 0.55f * (p / 0.18f) else 0.55f * (1f - (p - 0.18f) / 0.82f)
            Box(
                Modifier
                    .offset(x = sceneW * left, y = (SCENE_H - 40f - 150f * p).dp)
                    .size(bSize)
                    .alpha(bAlpha.coerceIn(0f, 1f))
                    .background(Color(0xFFDCF5FF).copy(alpha = 0.6f), CircleShape),
            )
        }

        // ── 해저 ──
        Box(
            Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color(0xFF283A2E).copy(alpha = 0.55f),
                        1f to Color(0xFF1C2A24).copy(alpha = 0.85f),
                    ),
                ),
        )

        // ── 로프 아래 중심 글로우 ──
        Box(
            Modifier
                .offset(x = (centerX - 75).dp, y = (CHEST_CY - 75).dp)
                .size(150.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFF78D2F0).copy(alpha = 0.28f), Color.Transparent),
                    ),
                    CircleShape,
                ),
        )

        // ── 심해를 떠다니는 상자 줄 (룰렛) ──
        val stripAlpha by animateFloatAsState(
            targetValue = if (phase == GachaPhase.Reeling || phase == GachaPhase.Result) 0.32f else 1f,
            animationSpec = tween(400), label = "strip",
        )
        val bobT by infinite.animateFloat(
            initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
            label = "bob",
        )
        if (boxes.isNotEmpty()) {
            val slotW = ITEM_WIDTH_WITH_GAP
            val visibleSlots = (centerX / slotW).toInt() + 2
            Box(Modifier.fillMaxSize().alpha(stripAlpha)) {
                for (di in -visibleSlots..visibleSlots) {
                    val i = ((offset / slotW).roundToInt()) + di
                    val boxIndex = ((i % boxes.size) + boxes.size) % boxes.size
                    val box = boxes[boxIndex]
                    val x = centerX + (i * slotW - offset) - CHEST_W / 2f
                    val bobY = sin(bobT + boxIndex * 1.3f) * 2.5f
                    val bobRot = sin(bobT + boxIndex * 1.3f) * 1.2f

                    Box(
                        Modifier
                            .offset(x = x.dp, y = (CHEST_CY - CHEST_W / 2f + bobY).dp)
                            .size(CHEST_W.dp)
                            .graphicsLayer { rotationZ = bobRot },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!box.iconAsset.isNullOrBlank()) {
                            AssetImage(
                                imageAsset = box.iconAsset,
                                contentDescription = box.label,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            SoodalIcon(icon = box.icon, tint = box.color, size = 56.dp)
                        }
                    }
                }
            }
        }

        // ── 좌우 가장자리 페이드 (수면 아래 영역만) ──
        val edgeColor = Color(0xFF092A3C).copy(alpha = 0.96f)
        Box(
            Modifier
                .offset(y = SURFACE_Y.dp)
                .size(48.dp, (SCENE_H - SURFACE_Y - 52f).dp)
                .background(Brush.horizontalGradient(listOf(edgeColor, Color.Transparent))),
        )
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(y = SURFACE_Y.dp)
                .size(48.dp, (SCENE_H - SURFACE_Y - 52f).dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, edgeColor))),
        )

        // ── 인양 로프 + 갈고리 ──
        val ropeSway by infinite.animateFloat(
            initialValue = -1.4f, targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Reverse),
            label = "rope",
        )
        val ropeAlpha by animateFloatAsState(
            targetValue = if (phase == GachaPhase.Result) 0f else 1f,
            animationSpec = tween(300), label = "ropeAlpha",
        )
        val ropeH = CHEST_CY - SURFACE_Y - 6f
        androidx.compose.foundation.Canvas(
            Modifier
                .offset(x = (centerX - 7).dp, y = (SURFACE_Y - 4).dp)
                .size(14.dp, (ropeH + 12).dp)
                .graphicsLayer {
                    rotationZ = ropeSway
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    alpha = ropeAlpha
                },
        ) {
            // 줄 — 5dp 간격 두 색 줄무늬
            val ropeW = 3.dp.toPx()
            val xLeft = (size.width - ropeW) / 2f
            val seg = 5.dp.toPx()
            var y = 0f
            var dark = false
            val ropeEnd = ropeH.dp.toPx()
            while (y < ropeEnd) {
                drawRect(
                    color = if (dark) Color(0xFFB98F56) else Color(0xFFD8B27E),
                    topLeft = Offset(xLeft, y),
                    size = Size(ropeW, minOf(seg, ropeEnd - y)),
                )
                dark = !dark
                y += seg
            }
            // 갈고리 — 줄 끝의 U자 호
            val hookR = 5.5.dp.toPx()
            drawArc(
                color = Color(0xFFC79A5E),
                startAngle = 0f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(size.width / 2f - hookR, ropeEnd - hookR),
                size = Size(hookR * 2, hookR * 2),
                style = Stroke(width = 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )
        }

        // ── 인양되는 상자 (로프를 타고 수면으로) ──
        if ((phase == GachaPhase.Reeling || phase == GachaPhase.Result) && risingBox != null) {
            val reel = remember { Animatable(0f) }
            LaunchedEffect(risingBox) {
                reel.snapTo(0f)
                reel.animateTo(
                    1f,
                    tween(REEL_DURATION_MS.toInt(), easing = CubicBezierEasing(0.32f, 0.86f, 0.4f, 1f)),
                )
            }
            val p = reel.value
            val travel = (p / 0.72f).coerceAtMost(1f)
            val chestScale = if (p < 0.72f) lerp(0.8f, 1.18f, p / 0.72f) else lerp(1.18f, 1.12f, (p - 0.72f) / 0.28f)
            val chestRot = if (p < 0.72f) lerp(-6f, 4f, p / 0.72f) else lerp(4f, 0f, (p - 0.72f) / 0.28f)
            val chestAlpha = (p / 0.16f).coerceIn(0f, 1f)

            Box(
                Modifier
                    .offset(x = (centerX - 50).dp, y = (CHEST_CY - 50 + REEL_DIST * travel).dp)
                    .size(100.dp)
                    .graphicsLayer {
                        scaleX = chestScale
                        scaleY = chestScale
                        rotationZ = chestRot
                        alpha = chestAlpha
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (!risingBox.iconAsset.isNullOrBlank()) {
                    AssetImage(
                        imageAsset = risingBox.iconAsset,
                        contentDescription = risingBox.label,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SoodalIcon(icon = risingBox.icon, tint = risingBox.color, size = 64.dp)
                }
            }

            // 끌려 올라오며 흘리는 기포
            if (phase == GachaPhase.Reeling) {
                repeat(4) { i ->
                    val bp by infinite.animateFloat(
                        initialValue = 0f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(1000 + i * 150, easing = LinearEasing),
                            RepeatMode.Restart,
                            initialStartOffset = StartOffset(i * 80),
                        ),
                        label = "trail$i",
                    )
                    Box(
                        Modifier
                            .offset(x = sceneW * (0.46f + i * 0.03f), y = (CHEST_CY - 110f * bp).dp)
                            .size((5 + (i % 2) * 3).dp)
                            .alpha((1f - bp) * 0.7f)
                            .background(Color(0xFFDCF5FF).copy(alpha = 0.7f), CircleShape),
                    )
                }
            }
        }

        // ── 수면 띠 ──
        val shimmer by infinite.animateFloat(
            initialValue = 0.45f, targetValue = 0.85f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
            label = "shimmer",
        )
        Box(
            Modifier
                .offset(y = (SURFACE_Y - 7).dp)
                .fillMaxWidth()
                .height(16.dp)
                .alpha(shimmer)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFB4EBFF).copy(alpha = 0.32f),
                            Color(0xFF78C8E6).copy(alpha = 0.10f),
                        ),
                    ),
                ),
        )

        // ── 뗏목 ──
        androidx.compose.foundation.Canvas(
            Modifier
                .offset(x = (centerX - 62).dp, y = (SURFACE_Y - 4).dp)
                .size(124.dp, 13.dp)
                .clip(RoundedCornerShape(7.dp)),
        ) {
            // 널빤지 무늬 — 9dp 판자 + 2dp 틈
            val plank = 9.dp.toPx()
            val gapW = 2.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawRect(Color(0xFFB07F4E), topLeft = Offset(x, 0f), size = Size(minOf(plank, size.width - x), size.height))
                if (x + plank < size.width) {
                    drawRect(Color(0xFF9C6D3F), topLeft = Offset(x + plank, 0f), size = Size(minOf(gapW, size.width - x - plank), size.height))
                }
                x += plank + gapW
            }
        }

        // ── 인양꾼 수달 ──
        val isTugging = phase == GachaPhase.Reeling
        val otterY by infinite.animateFloat(
            initialValue = 0f, targetValue = if (isTugging) -7f else -3f,
            animationSpec = infiniteRepeatable(
                tween(if (isTugging) 130 else 1900, easing = LinearEasing),
                RepeatMode.Reverse,
            ),
            label = "otterY",
        )
        val otterRot by infinite.animateFloat(
            initialValue = if (isTugging) -2f else -1.6f,
            targetValue = if (isTugging) 2f else 1.6f,
            animationSpec = infiniteRepeatable(
                tween(if (isTugging) 260 else 1900, easing = LinearEasing),
                RepeatMode.Reverse,
            ),
            label = "otterRot",
        )
        val otterBitmap = ImageBitmap.imageResource(R.drawable.otter_swim)
        Image(
            bitmap = otterBitmap,
            contentDescription = "인양꾼 수달",
            filterQuality = FilterQuality.None, // 도트가 뭉개지지 않게
            modifier = Modifier
                .offset(
                    x = (centerX - 53).dp,
                    y = (SURFACE_Y - 4 - 104 + otterY).dp,
                )
                .size(106.dp)
                .graphicsLayer { rotationZ = otterRot },
        )

        // ── 수달 말풍선 ──
        val otterLine = when (phase) {
            GachaPhase.Reeling -> "올라온다…!"
            GachaPhase.Spinning -> "영차… 영차…!"
            else -> "조개 주면 보물 하나 건져 올게!"
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 16.dp)
                .widthIn(max = 156.dp)
                .drawBehind {
                    // 왼쪽 꼬리 삼각형
                    val tail = Path().apply {
                        moveTo(-7.dp.toPx(), 25.dp.toPx())
                        lineTo(1.dp.toPx(), 18.dp.toPx())
                        lineTo(1.dp.toPx(), 32.dp.toPx())
                        close()
                    }
                    drawPath(tail, Color.White.copy(alpha = 0.95f))
                }
                .background(Color.White.copy(alpha = 0.95f), RoundedCornerShape(14.dp))
                .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
            Text(
                text = otterLine,
                fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF14324A), lineHeight = 17.sp,
            )
        }
    }
}

/**
 * 인양 버튼 — 큰 조개 배지(원형 반투명) + 라벨/비용 스택.
 *
 * @param badgeText 배지 우하단에 띄울 작은 텍스트 (예: "×10"), null이면 미표시
 */
@Composable
private fun SalvageButton(
    title: String, cost: String, badgeText: String?,
    bgColors: List<Color>, textColor: Color, glowColor: Color, badgeAccent: Color,
    enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val shape = SoodalShape.md
    Row(
        modifier = modifier
            .height(60.dp)
            .shadow(if (enabled) 12.dp else 0.dp, shape, ambientColor = glowColor, spotColor = glowColor)
            .clip(shape)
            .drawBehind {
                drawRect(Brush.linearGradient(bgColors, start = Offset.Zero, end = Offset(size.width, size.height)))
                if (!enabled) drawRect(Color.Black.copy(alpha = 0.3f))
            }
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .background(Color.White.copy(alpha = if (enabled) 0.26f else 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            SoodalIcon(
                icon = SoodalIcons.Shell,
                tint = if (enabled) textColor else textColor.copy(alpha = 0.35f),
                size = 24.dp,
            )
            if (badgeText != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 5.dp, y = 4.dp)
                        .size(18.dp)
                        .background(Color.White.copy(alpha = if (enabled) 1f else 0.5f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(badgeText, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = badgeAccent)
                }
            }
        }
        Spacer(Modifier.width(11.dp))
        Column {
            Text(
                title, fontSize = 15.5.sp, fontWeight = FontWeight.Bold,
                color = if (enabled) textColor else textColor.copy(alpha = 0.4f),
            )
            Text(
                cost, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                color = if (enabled) textColor.copy(alpha = 0.85f) else textColor.copy(alpha = 0.3f),
            )
        }
    }
}
