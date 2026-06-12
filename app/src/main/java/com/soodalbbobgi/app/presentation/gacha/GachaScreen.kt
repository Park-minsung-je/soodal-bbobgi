package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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

// ── 인양 장면 기하 (dp) ──
private const val SCENE_H = 412f
private const val SURFACE_Y = 103f        // 수면 y
private const val CHEST_CY = 261f         // 떠다니는 상자들의 세로 중심
private const val CHEST_W = 92f
private const val RAFT_W = 132f
private const val RAFT_H = 16f
private const val ROPE_TOP = SURFACE_Y + 2f                       // 로프가 뗏목 아래에서 시작
private const val ROPE_IDLE_LEN = CHEST_CY - CHEST_W / 2f - 14f - ROPE_TOP   // 평소: 상자 위에서 대기
private const val ROPE_ATTACH_LEN = CHEST_CY - 40f - ROPE_TOP                // 닻이 상자 뚜껑에 닿는 길이
private const val ROPE_MIN_LEN = 6f                               // 다 감아올린 길이
private const val CHEST_HANG = 40f                                // 닻 끝 → 매달린 상자 중심
private const val REEL_DROP_FRAC = 0.22f                          // 인양 타임라인 중 '닻 내리기' 구간

/**
 * 보물 인양소 — 조개를 좋아하는 인양꾼 수달이 바닷속 보물상자를 건져 올리는 뽑기 화면.
 * 평소엔 닻이 상자들 위에서 대기하다가, 뽑기가 멈추면 로프가 내려가 상자를 걸고
 * 감아올리는 2단 인양 모션 후 결과가 열린다.
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
 * 바다 단면 인양 장면 — 수면 위 뗏목의 수달, 닻 로프, 심해를 떠다니는 보물상자들.
 *
 * @param offset 룰렛 오프셋(dp 단위 누적값) — [GachaViewModel]의 스핀 로직과 공유
 * @param risingBox Reeling 단계에서 닻에 걸려 올라오는 상자
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
            targetValue = if (phase == GachaPhase.Idle || phase == GachaPhase.Spinning) 1f else 0.32f,
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

        // ── 인양 진행률 (닻 내리기 → 감아올리기) ──
        val reel = remember { Animatable(0f) }
        LaunchedEffect(phase, risingBox) {
            if (phase == GachaPhase.Reeling && risingBox != null) {
                reel.snapTo(0f)
                reel.animateTo(1f, tween(REEL_DURATION_MS.toInt(), easing = LinearEasing))
            } else if (phase == GachaPhase.Idle) {
                reel.snapTo(0f)
            }
        }
        val reeling = phase != GachaPhase.Idle && phase != GachaPhase.Spinning && risingBox != null
        // 로프 길이: 평소 대기 길이 → (A) 상자까지 내려감 → (B) 수면까지 감아올림
        val ropeLen = if (!reeling) {
            ROPE_IDLE_LEN
        } else {
            val p = if (phase == GachaPhase.Reeling) reel.value else 1f
            if (p < REEL_DROP_FRAC) {
                val t = FastOutSlowInEasing.transform(p / REEL_DROP_FRAC)
                lerp(ROPE_IDLE_LEN, ROPE_ATTACH_LEN, t)
            } else {
                val t = CubicBezierEasing(0.32f, 0.86f, 0.4f, 1f).transform((p - REEL_DROP_FRAC) / (1f - REEL_DROP_FRAC))
                lerp(ROPE_ATTACH_LEN, ROPE_MIN_LEN, t)
            }
        }

        // ── 로프 + 닻 (수달 캐릭터의 닻과 같은 디자인) ──
        val ropeSway by infinite.animateFloat(
            initialValue = -1.4f, targetValue = 1.4f,
            animationSpec = infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Reverse),
            label = "rope",
        )
        val maxRopeCanvasH = ROPE_ATTACH_LEN + 26f
        Canvas(
            Modifier
                .offset(x = (centerX - 10).dp, y = ROPE_TOP.dp)
                .size(20.dp, maxRopeCanvasH.dp)
                .graphicsLayer {
                    // 짐을 매달면 (인양 중) 흔들리지 않고 곧게 당겨진다
                    rotationZ = if (reeling) 0f else ropeSway
                    transformOrigin = TransformOrigin(0.5f, 0f)
                },
        ) {
            val cx = size.width / 2f
            val endY = ropeLen.dp.toPx()
            // 줄 — 5dp 간격 두 색 꼬임 줄무늬
            val ropeW = 3.dp.toPx()
            val seg = 5.dp.toPx()
            var y = 0f
            var dark = false
            while (y < endY) {
                drawRect(
                    color = if (dark) Color(0xFFB98F56) else Color(0xFFD8B27E),
                    topLeft = Offset(cx - ropeW / 2f, y),
                    size = Size(ropeW, minOf(seg, endY - y)),
                )
                dark = !dark
                y += seg
            }
            // 닻 — 수달 캐릭터가 든 닻과 같은 메탈 톤 + 진한 외곽선 + 녹 자국
            val outline = Color(0xFF433A52)
            val metal = Color(0xFF8F9585)
            val rust = Color(0xFF8B6B4A)

            // 고리 (로프 매듭)
            val ringCenter = Offset(cx, endY + 3.dp.toPx())
            drawCircle(color = outline, radius = 3.dp.toPx(), center = ringCenter, style = Stroke(width = 4.dp.toPx()))
            drawCircle(color = metal, radius = 3.dp.toPx(), center = ringCenter, style = Stroke(width = 2.dp.toPx()))

            val shankTop = endY + 6.dp.toPx()
            val crownY = endY + 22.dp.toPx()
            val stockY = endY + 9.5.dp.toPx()
            val armSpan = 8.dp.toPx()
            val flukeY = endY + 15.5.dp.toPx()

            val shank = Path().apply {
                moveTo(cx, shankTop)
                lineTo(cx, crownY)
            }
            val stock = Path().apply {
                moveTo(cx - 5.5.dp.toPx(), stockY)
                lineTo(cx + 5.5.dp.toPx(), stockY)
            }
            val arms = Path().apply {
                moveTo(cx - armSpan, flukeY)
                cubicTo(cx - armSpan, crownY - 1.dp.toPx(), cx - armSpan * 0.55f, crownY, cx, crownY)
                cubicTo(cx + armSpan * 0.55f, crownY, cx + armSpan, crownY - 1.dp.toPx(), cx + armSpan, flukeY)
            }
            val anchorParts = listOf(shank to 3.dp.toPx(), stock to 2.5.dp.toPx(), arms to 3.dp.toPx())
            // 외곽선 전체 → 메탈 전체 순서로 겹쳐 그려야 교차부 외곽선이 메탈을 가르지 않는다
            anchorParts.forEach { (p, w) ->
                drawPath(p, color = outline, style = Stroke(width = w + 2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            anchorParts.forEach { (p, w) ->
                drawPath(p, color = metal, style = Stroke(width = w, cap = StrokeCap.Round))
            }
            // 플루크 (양끝 위로 뾰족한 미늘)
            val flukeH = 5.dp.toPx()
            val flukeW = 3.2.dp.toPx()
            listOf(-1f, 1f).forEach { s ->
                val tipX = cx + s * armSpan
                val tri = Path().apply {
                    moveTo(tipX, flukeY - flukeH)
                    lineTo(tipX - flukeW, flukeY + 1.dp.toPx())
                    lineTo(tipX + flukeW, flukeY + 1.dp.toPx())
                    close()
                }
                drawPath(tri, color = outline, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                drawPath(tri, color = metal)
            }
            // 녹 자국
            drawCircle(rust, 1.1.dp.toPx(), Offset(cx - 0.8.dp.toPx(), endY + 12.dp.toPx()))
            drawCircle(rust, 0.9.dp.toPx(), Offset(cx + armSpan * 0.55f, crownY - 1.5.dp.toPx()))
        }

        // ── 닻에 걸려 올라오는 상자 ──
        if (reeling && risingBox != null) {
            val p = if (phase == GachaPhase.Reeling) reel.value else 1f
            val attachT = ((p - 0.18f) / 0.10f).coerceIn(0f, 1f) // 닻이 닿을 즈음 페이드 인
            val liftT = ((p - REEL_DROP_FRAC) / (1f - REEL_DROP_FRAC)).coerceIn(0f, 1f)
            val chestScale = lerp(1f, 1.15f, liftT)
            val wiggle = sin(p * 18f) * (1f - liftT) * 2.2f
            val chestCenterY = ROPE_TOP + ropeLen + CHEST_HANG

            Box(
                Modifier
                    .offset(x = (centerX - 50).dp, y = (chestCenterY - 50).dp)
                    .size(100.dp)
                    .graphicsLayer {
                        scaleX = chestScale
                        scaleY = chestScale
                        rotationZ = wiggle
                        alpha = attachT
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
                            tween(900 + i * 150, easing = LinearEasing),
                            RepeatMode.Restart,
                            initialStartOffset = StartOffset(i * 80),
                        ),
                        label = "trail$i",
                    )
                    Box(
                        Modifier
                            .offset(
                                x = sceneW * (0.45f + i * 0.033f),
                                y = (chestCenterY + 30f - 90f * bp).dp,
                            )
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

        // ── 뗏목 (통나무 판자 + 밧줄 묶음 + 물그림자) ──
        Canvas(
            Modifier
                .offset(x = (centerX - RAFT_W / 2f).dp, y = (SURFACE_Y - 5).dp)
                .size(RAFT_W.dp, (RAFT_H + 10f).dp),
        ) {
            val raftH = RAFT_H.dp.toPx()
            // 물에 잠긴 그림자
            drawOval(
                color = Color(0xFF04141F).copy(alpha = 0.4f),
                topLeft = Offset(size.width * 0.06f, raftH - 2.dp.toPx()),
                size = Size(size.width * 0.88f, 9.dp.toPx()),
            )
            // 판자 6장 — 라운드 + 윗면 하이라이트 + 외곽선
            val plankCount = 6
            val gapW = 2.dp.toPx()
            val plankW = (size.width - gapW * (plankCount - 1)) / plankCount
            val corner = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            repeat(plankCount) { i ->
                val x = i * (plankW + gapW)
                drawRoundRect(
                    color = Color(0xFFB07F4E),
                    topLeft = Offset(x, 0f),
                    size = Size(plankW, raftH),
                    cornerRadius = corner,
                )
                drawRoundRect(
                    color = Color(0xFFCB9660).copy(alpha = 0.85f),
                    topLeft = Offset(x + 1.dp.toPx(), 1.dp.toPx()),
                    size = Size(plankW - 2.dp.toPx(), 4.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                )
                drawRoundRect(
                    color = Color(0xFF7A5532).copy(alpha = 0.7f),
                    topLeft = Offset(x, 0f),
                    size = Size(plankW, raftH),
                    cornerRadius = corner,
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
            // 밧줄 묶음 두 줄
            listOf(0.17f, 0.83f).forEach { fx ->
                val bx = size.width * fx
                drawRect(
                    color = Color(0xFFD8B27E),
                    topLeft = Offset(bx - 2.5.dp.toPx(), -1.dp.toPx()),
                    size = Size(5.dp.toPx(), raftH + 2.dp.toPx()),
                )
                listOf(0.28f, 0.62f).forEach { fy ->
                    drawRect(
                        color = Color(0xFFB98F56),
                        topLeft = Offset(bx - 2.5.dp.toPx(), raftH * fy),
                        size = Size(5.dp.toPx(), 1.5.dp.toPx()),
                    )
                }
            }
        }

        // ── 인양꾼 수달 (여백 트리밍본, 발이 뗏목에 닿게) ──
        val isTugging = phase == GachaPhase.Reeling
        val otterY by infinite.animateFloat(
            initialValue = 0f, targetValue = if (isTugging) -6f else -3f,
            animationSpec = infiniteRepeatable(
                tween(if (isTugging) 140 else 1900, easing = LinearEasing),
                RepeatMode.Reverse,
            ),
            label = "otterY",
        )
        val otterRot by infinite.animateFloat(
            initialValue = if (isTugging) -2f else -1.6f,
            targetValue = if (isTugging) 2f else 1.6f,
            animationSpec = infiniteRepeatable(
                tween(if (isTugging) 280 else 1900, easing = LinearEasing),
                RepeatMode.Reverse,
            ),
            label = "otterRot",
        )
        val otterBitmap = ImageBitmap.imageResource(R.drawable.otter_salvager)
        val otterH = 92f
        val otterW = otterH * otterBitmap.width / otterBitmap.height.toFloat()
        // 왼손의 닻이 왼쪽으로 튀어나온 에셋이라 몸통 중심은 비트맵 가로 63% 지점 —
        // 몸통이 뗏목 가운데 오도록 배치하고 회전 피벗도 같은 지점의 발끝으로 잡는다.
        val otterBodyCenterFrac = 0.63f
        Image(
            bitmap = otterBitmap,
            contentDescription = "인양꾼 수달",
            filterQuality = FilterQuality.None, // 도트가 뭉개지지 않게
            modifier = Modifier
                .offset(
                    x = (centerX - otterW * otterBodyCenterFrac).dp,
                    y = (SURFACE_Y - 5f - otterH + 3f + otterY).dp,
                )
                .size(otterW.dp, otterH.dp)
                .graphicsLayer {
                    rotationZ = otterRot
                    transformOrigin = TransformOrigin(otterBodyCenterFrac, 1f) // 발끝 기준으로 흔들리게
                },
        )

        // ── 수달 말풍선 ──
        val otterLine = when (phase) {
            GachaPhase.Reeling -> "올라온다…!"
            GachaPhase.Spinning -> "영차… 영차…!"
            // 인양 완료 — 무슨 상자를 건졌는지 자랑한다 (잠시 외친 뒤 결과 팝업)
            GachaPhase.Celebrating, GachaPhase.Result ->
                risingBox?.let { "우와! ${it.label}야!" } ?: "우와! 보물 상자야!"
            else -> "조개를 주면 보물을 하나 건져줄게!"
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = 14.dp, end = 5.dp)
                // 고정 폭 — 문구 길이가 바뀌어도 말풍선(과 꼬리) 위치가 흔들리지 않게
                .width(150.dp)
                .drawBehind {
                    // 사각형 + 꼬리를 한 패스로 합쳐 그린다 (반투명이 겹치며 생기는 경계선 제거)
                    val body = Path().apply {
                        addRoundRect(
                            androidx.compose.ui.geometry.RoundRect(
                                0f, 0f, size.width, size.height,
                                CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                            ),
                        )
                    }
                    val tail = Path().apply {
                        moveTo(-7.dp.toPx(), 21.dp.toPx())
                        lineTo(4.dp.toPx(), 13.dp.toPx())
                        lineTo(4.dp.toPx(), 27.dp.toPx())
                        close()
                    }
                    val merged = Path().apply {
                        op(body, tail, androidx.compose.ui.graphics.PathOperation.Union)
                    }
                    drawPath(merged, Color.White.copy(alpha = 0.95f))
                }
                .padding(horizontal = 13.dp, vertical = 9.dp),
        ) {
            Text(
                text = otterLine,
                fontSize = 12.5.sp, fontWeight = FontWeight.Bold,
                color = Color(0xFF14324A), lineHeight = 17.sp,
                // 한국어가 음절 단위로 끊기지 않게 어절 단위 줄바꿈 (테마 폰트는 유지)
                style = androidx.compose.material3.LocalTextStyle.current.copy(
                    lineBreak = androidx.compose.ui.text.style.LineBreak(
                        strategy = androidx.compose.ui.text.style.LineBreak.Strategy.Balanced,
                        strictness = androidx.compose.ui.text.style.LineBreak.Strictness.Normal,
                        wordBreak = androidx.compose.ui.text.style.LineBreak.WordBreak.Phrase,
                    ),
                ),
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
