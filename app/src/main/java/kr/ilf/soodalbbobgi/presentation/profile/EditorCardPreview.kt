package kr.ilf.soodalbbobgi.presentation.profile

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * 프로필 편집 중 라이브 프리뷰 — 비트맵 재합성 없이 Compose 레이어(GPU)로 카드를 그린다.
 *
 * 슬라이더 드래그는 graphicsLayer 변환/offset만 바꾸므로 픽셀 재계산이 없어 프레임이 매끄럽다:
 * - 배경/캐릭터: 로드된 비트맵을 Image 레이어로 배치, 위치·크기는 GPU 변환
 * - 캐릭터 그림자: [ProfileCardRenderer.elevationShadowOf] (캐릭터당 1회 생성) 재사용
 * - 텍스트 요소: [ProfileCardRenderer.renderElementBitmap]으로 요소 단위 비트맵을 만들어 배치.
 *   스타일(내용/크기/칩/색/글꼴)이 바뀔 때만 재생성되고, 위치 슬라이더는 offset만 갱신한다.
 *
 * 저장 카드(홈/전체보기)와의 좌표·크기 수식은 렌더러 상수를 공유해 결과가 일치한다.
 * 유일한 차이는 BLUR 칩 — 배경 실시간 샘플 대신 프로스트 폴백으로 보이며, 저장하면 진짜 블러가 된다.
 */
@Composable
fun EditorCardPreview(
    layers: CardLayers,
    bgAsset: String?,
    charAsset: String?,
    modifier: Modifier = Modifier,
) {
    val bgBitmap = rememberAssetBitmap(bgAsset) ?: layers.bgBitmap
    val charBitmap = rememberAssetBitmap(charAsset) ?: layers.charBitmap

    // 에셋 경로가 있는데 비트맵이 아직 로딩 전이면 아무것도 그리지 않는다(투명 유지).
    // 이 프리뷰는 홈에서 항상 합성 카드 위에 겹쳐지므로, 로딩 전 흰 베이스가 아래 카드를
    // 덮어 흰 깜빡임을 만들지 않고 아래 카드가 그대로 비쳐 보이게 한다.
    val assetPending = (!bgAsset.isNullOrBlank() && bgBitmap == null) ||
        (!charAsset.isNullOrBlank() && charBitmap == null)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .aspectRatio(ProfileCardRenderer.CARD_WIDTH.toFloat() / ProfileCardRenderer.CARD_HEIGHT.toFloat()),
    ) {
        if (assetPending) return@BoxWithConstraints
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        // 카드 좌표(2752×1536 px) → 화면 px 배율
        val s = widthPx / ProfileCardRenderer.CARD_WIDTH

        // ── 배경 (렌더러 Layer 0/1과 동일: 흰 베이스 + 배경 이미지) ──
        Box(Modifier.fillMaxSize().background(Color.White))
        if (bgBitmap != null) {
            Image(
                bitmap = bgBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // ── 캐릭터 + 부유 그림자 — 위치/크기는 GPU 변환만 (재합성 없음) ──
        if (charBitmap != null) {
            val shadow = remember(charBitmap) { ProfileCardRenderer.elevationShadowOf(charBitmap) }
            // 렌더러와 동일 수식: charSize = CARD_HEIGHT * 0.85 * scale (정사각 배치)
            val baseSizePx = ProfileCardRenderer.CARD_HEIGHT * 0.85f * s
            val baseSizeDp = with(density) { baseSizePx.toDp() }
            // 중심 이동량 — 카드 중앙 기준
            fun charLayer(extraScale: Float, extraOffsetYFrac: Float) = Modifier
                .graphicsLayer {
                    translationX = (layers.charX - 0.5f) * widthPx
                    translationY = (layers.charY - 0.5f) * heightPx +
                        baseSizePx * layers.charScale * extraOffsetYFrac
                    scaleX = layers.charScale * extraScale
                    scaleY = layers.charScale * extraScale
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                }
            // 그림자 비트맵은 사방 SHADOW_MARGIN_FRAC 만큼 큰 캔버스 — 같은 비율로 확대해 스케일을 맞춘다
            if (layers.showShadow) {
                Image(
                    bitmap = shadow.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(baseSizeDp)
                        .then(charLayer(
                            extraScale = 1f + ProfileCardRenderer.SHADOW_MARGIN_FRAC * 2f,
                            extraOffsetYFrac = ProfileCardRenderer.SHADOW_OFFSET_FRAC,
                        )),
                )
            }
            Image(
                bitmap = charBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(baseSizeDp)
                    .then(charLayer(extraScale = 1f, extraOffsetYFrac = 0f)),
            )
        }

        // ── 텍스트 3요소 — 요소 비트맵(스타일 바뀔 때만 재생성) + 중심 앵커 offset 배치 ──
        if (layers.showNickname) {
            ElementLayer(
                text = layers.nickname, baseSize = ProfileCardRenderer.NICKNAME_BASE_SIZE,
                scaleStep = layers.nicknameScaleStep, pill = layers.nicknamePill,
                color = layers.nicknameColor, fontStyle = layers.nicknameStyle,
                outline = layers.nicknameOutline, outlineColor = layers.nicknameOutlineColor,
                cx = layers.nicknameX, cy = layers.nicknameY,
                widthPx = widthPx, heightPx = heightPx, s = s, bgBitmap = bgBitmap,
            )
        }
        if (layers.showTagline) {
            ElementLayer(
                text = layers.tagline, baseSize = ProfileCardRenderer.TAGLINE_BASE_SIZE,
                scaleStep = layers.taglineScaleStep, pill = layers.taglinePill,
                color = layers.taglineColor, fontStyle = layers.taglineStyle,
                outline = layers.taglineOutline, outlineColor = layers.taglineOutlineColor,
                cx = layers.taglineX, cy = layers.taglineY,
                widthPx = widthPx, heightPx = heightPx, s = s, bgBitmap = bgBitmap,
            )
        }
        if (layers.showStats) {
            ElementLayer(
                text = layers.stats, baseSize = ProfileCardRenderer.STATS_BASE_SIZE,
                scaleStep = layers.statsScaleStep, pill = layers.statsPill,
                color = layers.statsColor, fontStyle = layers.statsStyle,
                outline = layers.statsOutline, outlineColor = layers.statsOutlineColor,
                cx = layers.statsX, cy = layers.statsY,
                widthPx = widthPx, heightPx = heightPx, s = s, bgBitmap = bgBitmap,
            )
        }
    }
}

/**
 * 텍스트 요소 하나의 프리뷰 레이어 — 요소 비트맵을 중심 앵커(cx, cy)에 offset 배치한다.
 * 비트맵은 스타일 키가 바뀔 때만 재생성 (위치 이동은 graphicsLayer translation만 갱신).
 */
@Composable
private fun BoxScope.ElementLayer(
    text: String,
    baseSize: Float,
    scaleStep: Int,
    pill: String,
    color: String,
    fontStyle: String,
    outline: Boolean,
    outlineColor: String?,
    cx: Float,
    cy: Float,
    widthPx: Float,
    heightPx: Float,
    s: Float,
    bgBitmap: Bitmap?,
) {
    val colorRaw = remember(color) {
        try {
            android.graphics.Color.parseColor(color)
        } catch (e: IllegalArgumentException) {
            android.graphics.Color.WHITE
        }
    }
    // BLUR 칩은 편집 프리뷰에서 실배경 블러가 불가하므로, 칩 뒤 배경 근사색으로 프로스트를 입힌다.
    val blurFallback = remember(pill, bgBitmap, cx, cy) {
        if (pill == "BLUR" && bgBitmap != null) ProfileCardRenderer.sampleBgColor(bgBitmap, cx, cy) else null
    }
    val bmp: Bitmap = remember(text, baseSize, scaleStep, pill, colorRaw, fontStyle, outline, outlineColor, blurFallback) {
        ProfileCardRenderer.renderElementBitmap(
            text, baseSize, scaleStep, pill, colorRaw, fontStyle, outline,
            outlineColorHex = outlineColor, blurFallbackColor = blurFallback,
        )
    }
    val density = LocalDensity.current
    val wDp = with(density) { (bmp.width * s).toDp() }
    val hDp = with(density) { (bmp.height * s).toDp() }
    Image(
        bitmap = bmp.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .align(Alignment.Center)
            .size(wDp, hDp)
            .graphicsLayer {
                // 요소 중심 = 비트맵 중심 — 중앙 정렬 후 앵커까지 이동만 한다
                translationX = (cx - 0.5f) * widthPx
                translationY = (cy - 0.5f) * heightPx
            },
    )
}
