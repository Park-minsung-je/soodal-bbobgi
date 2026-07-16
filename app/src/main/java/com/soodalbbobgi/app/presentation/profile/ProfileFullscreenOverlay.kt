package com.soodalbbobgi.app.presentation.profile

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.GlassSheen
import com.soodalbbobgi.app.core.ui.ProfileFrameCorner
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.soodalScreenBackdrop
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.motion.Motion
import kotlinx.coroutines.launch

/**
 * 프로필 카드 전체보기 오버레이.
 *
 * 홈 카드가 숨겨진 동안, 같은 위치·크기에서 시작한 단일 카드 한 장이 90도 회전하며
 * 화면을 채우도록 확대된다([fullscreenCardTransform]). 진입은 progress 0→1,
 * 닫기는 1→0 후 [onClosed]. 변환 중 화면에 카드는 이 한 장뿐이라 착시가 아니다.
 * 배경(버튼 제외)을 탭하면 하단 컨트롤이 페이드로 숨고/복귀하며, 숨김 중에도 뒤로가기로 닫힌다.
 *
 * @param onReady 오버레이 카드가 측정되어 홈 카드 자리를 덮을 준비가 됐을 때 호출(홈 카드 숨김 타이밍)
 * @param onClosed 닫힘 애니메이션이 끝난 뒤 호출(홈이 fullscreenOpen=false로 되돌릴 때 사용)
 */
@Composable
fun ProfileFullscreenOverlay(
    onReady: () -> Unit,
    onClosed: () -> Unit,
    viewModel: ProfileFullscreenViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing
    val context = LocalContext.current
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // 전체보기 동안 시스템 바를 숨겨 전체화면으로 만든다. 닫히면(dispose) 원복. (화면 회전은 안 함)
    DisposableEffect(Unit) {
        val activity = context as? Activity
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        window?.let { WindowCompat.setDecorFitsSystemWindows(it, false) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.let { WindowCompat.setDecorFitsSystemWindows(it, true) }
        }
    }

    val saveState by viewModel.saveState.collectAsState()
    val cardStateOrNull by viewModel.cardState.collectAsState()
    // 그리기용 — 아직 로드 전(null)이면 기본값으로 그리되, 아래 assetsReady가 false라 숨겨진다.
    val cardState = cardStateOrNull ?: FullscreenCardState()

    val bgBitmap = rememberAssetBitmap(cardState.bgAsset)
    val charBitmap = rememberAssetBitmap(cardState.charAsset)

    // 카드 데이터가 결합되고(cardStateOrNull != null) 에셋 이미지가 모두 로드되기 전에는
    // 카드가 흰 베이스로 그려진다(ProfileCardRenderer Layer0). 그 전에는 홈 카드를 숨기지 않고
    // (onReady 보류) 진입도 시작하지 않아 흰 깜빡을 막는다.
    val assetsReady = cardStateOrNull != null &&
        (cardState.bgAsset.isNullOrBlank() || bgBitmap != null) &&
        (cardState.charAsset.isNullOrBlank() || charBitmap != null)

    val saved = cardState.card
    val layers = CardLayers(
        bgBitmap = bgBitmap,
        charBitmap = charBitmap,
        nickname = cardState.nickname,
        tagline = cardState.tagline,
        stats = cardState.statsText,
        charX = cardState.charX,
        charY = cardState.charY,
        charScale = cardState.charScale,
        nicknameStyle = saved?.nicknameStyle ?: "REGULAR",
        taglineStyle = saved?.taglineStyle ?: "REGULAR",
        statsStyle = saved?.statsStyle ?: "REGULAR",
        nicknameOutline = saved?.nicknameOutline ?: false,
        taglineOutline = saved?.taglineOutline ?: false,
        statsOutline = saved?.statsOutline ?: false,
        showNickname = saved?.showNickname ?: true,
        nicknameX = saved?.nicknameX ?: 0.83f, nicknameY = saved?.nicknameY ?: 0.40f,
        nicknameScaleStep = saved?.nicknameScaleStep ?: 3,
        showTagline = saved?.showTagline ?: true,
        taglineX = saved?.taglineX ?: 0.83f, taglineY = saved?.taglineY ?: 0.57f,
        taglineScaleStep = saved?.taglineScaleStep ?: 3,
        showStats = saved?.showStats ?: true,
        statsX = saved?.statsX ?: 0.16f, statsY = saved?.statsY ?: 0.90f,
        statsScaleStep = saved?.statsScaleStep ?: 3,
        nicknamePill = saved?.nicknamePill ?: "WHITE",
        taglinePill = saved?.taglinePill ?: "NONE",
        statsPill = saved?.statsPill ?: "BLUR",
        nicknameColor = saved?.nicknameColor ?: "#FFFFFF",
        taglineColor = saved?.taglineColor ?: "#FFFFFF",
        statsColor = saved?.statsColor ?: "#00F5FF",
    )
    val bitmap = remember(layers) { ProfileCardRenderer.renderCached(layers) }

    // 진입 시 0→1, 닫기 시 1→0. 한 progress로 회전·확대·이동·페이드를 모두 구동.
    // 진입 애니는 오버레이 카드 위치가 측정된 뒤 시작한다(아래 LaunchedEffect) — 미측정 첫 프레임에
    // 잘못된 위치로 출발하지 않도록.
    val progress = remember { Animatable(0f) }

    // 배경(카드 포함, 버튼 제외) 탭으로 토글되는 크롬(하단 컨트롤) 표시 여부 —
    // 숨기면 검은 화면에 카드만 남는다. 숨김 중에도 뒤로가기는 닫기로 동작한다.
    var chromeVisible by remember { mutableStateOf(true) }

    val close: () -> Unit = {
        scope.launch {
            progress.animateTo(0f, tween(Motion.DUR_ZOOM, easing = Motion.easeEmphasized))
            onClosed()
        }
    }
    BackHandler(enabled = true) { close() }

    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                Toast.makeText(context, "갤러리에 저장했어요!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is SaveState.Error -> {
                Toast.makeText(context, (saveState as SaveState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // 홈 카드 측정 크기(px) → dp. 없으면 폴백으로 화면폭-32dp 가로 비율 카드.
    val homeSize = ProfileCardBounds.homeCardSize
    val cardWidthDp = if (homeSize != null) with(density) { homeSize.width.toDp() }
        else (config.screenWidthDp - 32).dp
    val cardHeightDp = if (homeSize != null) with(density) { homeSize.height.toDp() }
        else (config.screenWidthDp - 32).dp * 1536f / 2752f

    val screenWpx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHpx = with(density) { config.screenHeightDp.dp.toPx() }
    val cardWpx = with(density) { cardWidthDp.toPx() }
    val cardHpx = with(density) { cardHeightDp.toPx() }
    // 홈 카드와 같은 유리 매트 프레임 두께 — 배율은 프레임 포함 크기로 계산해
    // 전체화면에서 프레임이 화면 밖으로 잘리지 않게 한다.
    val frameDp = 6.dp
    val framePx = with(density) { frameDp.toPx() }
    val fullscreenScale =
        fullscreenCardScale(cardWpx + framePx * 2, cardHpx + framePx * 2, screenWpx, screenHpx)

    var overlayCenter by remember { mutableStateOf<Offset?>(null) }

    // 오버레이 카드가 (1) 위치 측정되고 (2) 에셋까지 로드돼 정상 카드로 그려질 준비가 된 뒤에야
    // 홈 카드 숨김을 알리고(onReady) 진입 애니를 시작한다. 그 전엔 홈 카드가 자리를 지켜
    // 빈 프레임/흰 깜빡이 없다.
    LaunchedEffect(overlayCenter != null, assetsReady) {
        if (overlayCenter != null && assetsReady) {
            onReady()
            progress.animateTo(1f, tween(Motion.DUR_ZOOM, easing = Motion.easeEmphasized))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 오버레이가 떠 있는 동안 모든 포인터 입력을 루트에서 가로채
            // 뒤 화면(홈)으로 탭/스크롤이 전파되지 않게 한다. 버튼 탭은 자식이 우선 처리하고,
            // 그 외 영역 탭은 크롬 토글로 동작한다.
            .pointerInput(Unit) { detectTapGestures { chromeVisible = !chromeVisible } },
    ) {
        // 검은 배경: 카드와 분리해 progress로만 페이드 → 카드는 또렷하게 유지.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
                .background(Color.Black),
        )

        // 단일 카드: 홈 카드와 같은 크기로 그려 progress=0에서 홈 카드와 픽셀 일치.
        // 홈처럼 라운드 + 유리 매트 프레임 — 프레임은 progress로 페이드 인해서 진입 시점엔
        // 뒤에 남아 있는 홈 GlassBox 프레임과 자연스럽게 교차된다. 검은 배경 위라 배경 블러
        // 대신 반투명 흰 유리를 직접 그린다 (어두운 유리 매트로 읽힘).
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val frameShape = RoundedCornerShape(ProfileFrameCorner)
            Box(
                modifier = Modifier
                    .size(cardWidthDp + frameDp * 2, cardHeightDp + frameDp * 2)
                    .onGloballyPositioned { overlayCenter = it.boundsInWindow().center }
                    .graphicsLayer {
                        val home = ProfileCardBounds.homeCardCenter
                        val oc = overlayCenter
                        if (home == null || oc == null || !assetsReady) {
                            // 위치 측정 전이거나 에셋 로딩 전 — 잘못된 자리/흰 베이스가 보이지 않게 숨긴다.
                            alpha = 0f
                        } else {
                            alpha = 1f
                            val t = fullscreenCardTransform(
                                progress = progress.value,
                                homeCenterX = home.x,
                                homeCenterY = home.y,
                                overlayCenterX = oc.x,
                                overlayCenterY = oc.y,
                                fullscreenScale = fullscreenScale,
                            )
                            rotationZ = t.rotationZ
                            scaleX = t.scale
                            scaleY = t.scale
                            translationX = t.translationX
                            translationY = t.translationY
                        }
                    },
            ) {
                // 검은 배경에선 비칠 게 없어 반투명 유리가 죽어 보인다 — 홈과 같은 톤의 앱 배경을
                // 프레임 안에 직접 그리고 홈 카드와 같은 프로스트 틴트(흰 0.55)를 얹어
                // '홈 배경 위 유리 카드'의 모습을 통째로 구워 넣는다.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = progress.value }
                        .clip(frameShape)
                        .border(1.dp, colors.glassBorder, frameShape),
                ) {
                    // 배경(그라데이션+블롭)을 프레임에 채우고 블러 — 홈 카드 프로스트의 젖빛 질감 재현
                    // (blur는 API 31+에서만 동작하지만, 배경 자체가 이미 부드러워 미만에서도 자연스럽다)
                    Box(
                        Modifier
                            .matchParentSize()
                            .blur(24.dp)
                            .soodalScreenBackdrop(),
                    )
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(if (colors.isDark) colors.glassBg else Color.White.copy(alpha = 0.55f)),
                    )
                    GlassSheen(frameShape)
                }
                ProfileCardComposite(
                    layers = layers,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(cardWidthDp, cardHeightDp)
                        .clip(RoundedCornerShape(ProfileFrameCorner - frameDp)),
                )
            }
        }

        // 하단 컨트롤: 카드와 달리 progress로 함께 페이드.
        // 배경 탭으로 숨김/복귀 — 숨김 중엔 컴포지션에서 빠져 버튼이 눌리지 않는다.
        AnimatedVisibility(
            visible = chromeVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = progress.value }
                    .padding(horizontal = spacing.s4, vertical = spacing.s5),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(SoodalShape.md)
                            .background(colors.glassBg, SoodalShape.md)
                            .border(1.dp, colors.glassBorder, SoodalShape.md)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.saveToGallery(bitmap) },
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SoodalIcon(icon = SoodalIcons.Save, tint = colors.textPrimary, size = 14.dp)
                        Text("저장", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                    Row(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(SoodalShape.md)
                            .background(colors.glassBg, SoodalShape.md)
                            .border(1.dp, colors.glassBorder, SoodalShape.md)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.share(bitmap) },
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SoodalIcon(icon = SoodalIcons.Share, tint = colors.textPrimary, size = 14.dp)
                        Text("공유", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                }
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(SoodalShape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = close,
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.ArrowLeft, tint = colors.textSecondary, size = 14.dp)
                    Text("돌아가기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                }
            }
        }
    }
}
