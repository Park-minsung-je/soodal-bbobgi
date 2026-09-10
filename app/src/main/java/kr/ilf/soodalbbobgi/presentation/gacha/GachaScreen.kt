package kr.ilf.soodalbbobgi.presentation.gacha

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
import androidx.compose.foundation.LocalIndication
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import kr.ilf.soodalbbobgi.R
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.theme.SoodalShape
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.AppOverlay
import kr.ilf.soodalbbobgi.core.ui.AssetImage
import kr.ilf.soodalbbobgi.core.ui.GlassInfoGroup
import kr.ilf.soodalbbobgi.core.ui.GlassInfoSegment
import kr.ilf.soodalbbobgi.core.ui.LocalHazeContent
import dev.chrisbanes.haze.hazeSource
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.core.ui.TabBarClearance
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sin

// ── 인양 장면 기하 (dp) ──
private const val SCENE_H = 412f
private const val SURFACE_Y = 103f        // 수면 y
private const val CHEST_CY = 261f         // 떠다니는 상자들의 세로 중심
private const val CHEST_W = 92f
private const val RAFT_W = 132f
private const val RAFT_H = 16f
private const val CHEST_HANG = 40f                                // 닻 끝 → 매달린 상자 중심
private const val ANCHOR_RAISED_Y = SURFACE_Y - 8f                // 다 감아올린 닻 위치 (상자가 수면에 반쯤 걸린다)
private const val REEL_DROP_FRAC = 0.22f                          // 인양 타임라인 중 '닻 내리기' 구간

/**
 * 보물 인양소 — 조개를 좋아하는 인양꾼 수달이 바닷속 보물상자를 건져 올리는 뽑기 화면.
 * 평소엔 수달이 닻을 손에 들고 있다가, 뽑기가 멈추면 손에서 닻이 내려가 상자를 걸고
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
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
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
                        Text("보물 인양소", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                        Text(
                            "인양꾼 수달이 바닷속 보물을 건져 올려요",
                            fontSize = 11.5.sp, fontWeight = FontWeight.Medium,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    // 통화 패널 — 홈/상점과 동일하게 한 유리 바에 묶는다.
                    GlassInfoGroup {
                        GlassInfoSegment(SoodalIcons.Shell, state.shells.toString(), colors.accentGold)
                        GlassInfoSegment(SoodalIcons.Pearl, state.pearls.toString(), colors.accentPurple)
                    }
                }

                // 인양 씬을 화면 중앙 쪽으로 내린다 (상단에 너무 붙지 않게).
                Spacer(Modifier.height(52.dp))

                // 씬~버튼을 z1 소스로 등록 — 결과 팝업/탭바의 블러에 실제 뒤 요소로 비치게 한다.
                // (이 화면엔 글래스 카드가 없어 소스가 하나도 없었고, 팝업 유리가 배경만 블러했다.
                //  헤더의 글래스 칩은 hazeEffect라 소스 안에 두면 렌더되지 않으므로 제외.)
                val haze = LocalHazeContent.current
                Column(if (haze != null) Modifier.hazeSource(haze, zIndex = 1f) else Modifier) {
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
                } // z1 소스 Column

                Spacer(Modifier.height(TabBarClearance))
            }
        }

        // -- Result Modal (오버레이 레이어로 호이스팅 — 스크림이 탭바까지 덮는다) --
        if (state.phase == GachaPhase.Result && state.results.isNotEmpty()) {
            AppOverlay {
                GachaResultOverlay(
                    results = state.results,
                    onClose = { viewModel.closeResults() },
                )
            }
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

        // ── 인양 진행률 (닻 내리기 → 감아올리기) — 룰렛의 중앙 상자 핸드오프와 공유 ──
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
        val reelP = if (phase == GachaPhase.Reeling) reel.value else 1f
        // 닻이 상자에 닿아 걸리는 구간(0.18~0.28) — 인양 상자 페이드 인과
        // 룰렛 중앙 상자 페이드 아웃이 같은 값으로 교차해 하나가 이어지는 것처럼 보인다.
        val attachT = ((reelP - 0.18f) / 0.10f).coerceIn(0f, 1f)

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
            Box(Modifier.fillMaxSize()) {
                for (di in -visibleSlots..visibleSlots) {
                    val i = ((offset / slotW).roundToInt()) + di
                    val boxIndex = ((i % boxes.size) + boxes.size) % boxes.size
                    val box = boxes[boxIndex]
                    val x = centerX + (i * slotW - offset) - CHEST_W / 2f
                    val bobY = sin(bobT + boxIndex * 1.3f) * 2.5f
                    val bobRot = sin(bobT + boxIndex * 1.3f) * 1.2f
                    // 중앙(멈춘) 상자는 딤 없이 유지하다가 닻이 걸리는 순간 사라진다 —
                    // 같은 자리에서 페이드 인하는 인양 상자가 이어받아 "그 상자가 올라가는" 연출.
                    val itemAlpha = if (reeling && di == 0) 1f - attachT else stripAlpha

                    Box(
                        Modifier
                            .offset(x = x.dp, y = (CHEST_CY - CHEST_W / 2f + bobY).dp)
                            .size(CHEST_W.dp)
                            .graphicsLayer {
                                rotationZ = bobRot
                                alpha = itemAlpha
                            },
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

        // ── 수달 배치 기하 — 손 좌표 계산과 아래 수달 그리기가 공유한다 ──
        val otterH = 92f
        val otterScale = otterH / 365f          // 원본 px → dp
        val otterW = 356f * otterScale
        // 닻/꼬리가 좌우로 뻗은 에셋이라 몸통 중심은 비트맵 가로 50.6% 지점
        val otterX = centerX - otterW * 0.506f
        val otterTopY = SURFACE_Y - 5f - otterH + 3f
        // 손에 쥔 로프 매듭이 끝나는 지점 (356×365 프레임 px 62, 150) — 인양 로프가 여기서 시작한다
        val handX = otterX + 62f * otterScale
        val handY = otterTopY + 150f * otterScale

        // ── 닻 위치: 손(대기) → 상자(내리기) → 수면 위(감아올리기) ──
        // 평소엔 수달이 닻을 들고 있고(이미지에 포함) 물밑엔 아무것도 없다.
        // 인양이 시작되면 닻 없는 수달로 바꾸고, 손에서 출발한 닻을 합성해 움직인다.
        val anchorPos = if (!reeling) {
            Offset(handX, handY)
        } else if (reelP < REEL_DROP_FRAC) {
            val t = FastOutSlowInEasing.transform(reelP / REEL_DROP_FRAC)
            Offset(lerp(handX, centerX, t), lerp(handY, CHEST_CY - CHEST_HANG, t))
        } else {
            val t = CubicBezierEasing(0.32f, 0.86f, 0.4f, 1f).transform((reelP - REEL_DROP_FRAC) / (1f - REEL_DROP_FRAC))
            Offset(centerX, lerp(CHEST_CY - CHEST_HANG, ANCHOR_RAISED_Y, t))
        }

        // ── 로프 + 닻 (수달 이미지에서 잘라낸 도트 스프라이트) — 인양 중에만 그린다 ──
        // zIndex: 끌어올리는 줄/상자가 수달·뗏목에 가려지지 않게 맨 위 레이어에 그린다
        val anchorImg = ImageBitmap.imageResource(R.drawable.salvage_anchor)
        val ropeImg = ImageBitmap.imageResource(R.drawable.salvage_rope)
        if (reeling) {
            Canvas(Modifier.matchParentSize().zIndex(1f)) {
                val dot = otterScale.dp.toPx()  // 수달과 같은 도트 배율 (원본 px → 화면 px)
                val pPx = Offset(handX.dp.toPx(), handY.dp.toPx())
                val aPx = Offset(anchorPos.x.dp.toPx(), anchorPos.y.dp.toPx())
                val ropeW = ropeImg.width * dot
                val tileH = ropeImg.height * dot

                // 로프 — 손에서 수직으로 출발해 닻 위로 수직 도착하는 곡선 (닻은 늘 똑바로 매달린다)
                val sag = ((aPx.y - pPx.y) * 0.45f).coerceIn(6.dp.toPx(), 40.dp.toPx())
                val c1 = Offset(pPx.x, pPx.y + sag)
                val c2 = Offset(aPx.x, aPx.y - sag)
                fun bez(t: Float): Offset {
                    val u = 1f - t
                    return Offset(
                        u * u * u * pPx.x + 3 * u * u * t * c1.x + 3 * u * t * t * c2.x + t * t * t * aPx.x,
                        u * u * u * pPx.y + 3 * u * u * t * c1.y + 3 * u * t * t * c2.y + t * t * t * aPx.y,
                    )
                }
                val span = (aPx.y - pPx.y) + abs(aPx.x - pPx.x)
                if (span > 1f) {
                    val steps = (span / tileH).roundToInt().coerceIn(2, 80)
                    var prev = pPx
                    for (i in 1..steps) {
                        val pt = bez(i / steps.toFloat())
                        val seg = pt - prev
                        val segLen = seg.getDistance()
                        if (segLen >= 0.5f) {
                            // 회전된 좌표계에서 '아래' 방향이 prev→pt 가 되도록 그린다
                            val deg = Math.toDegrees(atan2(-seg.x.toDouble(), seg.y.toDouble())).toFloat()
                            withTransform({ rotate(deg, pivot = prev) }) {
                                drawImage(
                                    image = ropeImg,
                                    // 0.54 = 타일 안에서 줄 중심 위치
                                    dstOffset = IntOffset((prev.x - ropeW * 0.54f).roundToInt(), prev.y.roundToInt()),
                                    dstSize = IntSize(ropeW.roundToInt(), segLen.roundToInt().coerceAtLeast(1)),
                                    // 래스터 에셋 — 부드럽게 축소되도록 필터링을 켠다
                                    filterQuality = FilterQuality.Medium,
                                )
                            }
                        }
                        prev = pt
                    }
                }
                // 닻 — 스프라이트 위쪽 줄 토막이 로프 끝과 이어진다
                val aw = anchorImg.width * dot
                val ah = anchorImg.height * dot
                drawImage(
                    image = anchorImg,
                    dstOffset = IntOffset((aPx.x - aw * 0.475f).roundToInt(), (aPx.y - tileH).roundToInt()),
                    dstSize = IntSize(aw.roundToInt(), ah.roundToInt()),
                    filterQuality = FilterQuality.None,
                )
            }
        }

        // ── 닻에 걸려 올라오는 상자 ── (attachT에 맞춰 페이드 인 — 룰렛 중앙 상자와 교차)
        if (reeling && risingBox != null) {
            val liftT = ((reelP - REEL_DROP_FRAC) / (1f - REEL_DROP_FRAC)).coerceIn(0f, 1f)
            val chestScale = lerp(1f, 1.15f, liftT)
            val wiggle = sin(reelP * 18f) * (1f - liftT) * 2.2f
            val chestCenterY = anchorPos.y + CHEST_HANG

            Box(
                Modifier
                    .zIndex(1f)
                    .offset(x = (anchorPos.x - 50).dp, y = (chestCenterY - 50).dp)
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
        val otterY by infinite.animateFloat(
            initialValue = 0f, targetValue = -3f,
            animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing), RepeatMode.Reverse),
            label = "otterY",
        )
        val otterRot by infinite.animateFloat(
            initialValue = -1.6f, targetValue = 1.6f,
            animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing), RepeatMode.Reverse),
            label = "otterRot",
        )
        // 인양 중엔 닻 없는 이미지로 교체하고 움직임을 멈춘다 —
        // 손에서 출발하는 합성 닻과 어긋나지 않게 (배치 기하는 위 손 좌표 계산과 공유)
        val otterBitmap = ImageBitmap.imageResource(
            if (reeling) R.drawable.otter_salvager_empty else R.drawable.otter_salvager,
        )
        Image(
            bitmap = otterBitmap,
            contentDescription = "인양꾼 수달",
            filterQuality = FilterQuality.Medium, // 래스터 에셋 — 축소 시 경계가 계단지지 않게
            modifier = Modifier
                .offset(
                    x = otterX.dp,
                    y = (otterTopY + if (reeling) 0f else otterY).dp,
                )
                .size(otterW.dp, otterH.dp)
                .graphicsLayer {
                    rotationZ = if (reeling) 0f else otterRot
                    transformOrigin = TransformOrigin(0.63f, 1f) // 몸통 발끝 기준으로 흔들리게
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
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = LocalIndication.current, onClick = onClick)
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
