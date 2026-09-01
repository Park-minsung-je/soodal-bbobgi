package kr.ilf.soodalbbobgi.presentation.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil.Coil
import coil.request.ImageRequest
import kr.ilf.soodalbbobgi.core.ui.AssetStoreEntryPoint
import kr.ilf.soodalbbobgi.core.ui.resolveAssetModel
import kr.ilf.soodalbbobgi.core.util.LruMemoizer
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * 프로필 카드 합성에 필요한 4레이어 데이터.
 * Bitmap이 제공되면 우선 사용하고, null이면 색상으로 폴백한다.
 */
data class CardLayers(
    val bgBitmap: Bitmap? = null,
    val charBitmap: Bitmap? = null,
    val frameBitmap: Bitmap? = null,
    val bgColor: Color = Color(0xFF87CEEB),
    val borderColor: Color = Color(0xFF00A8B8),
    val charColor: Color = Color(0xFF8B6914),
    val nickname: String = "Soodal",
    val tagline: String = "수영을 사랑하는 수달",
    val stats: String = "12,540m · 89회",
    val charX: Float = 0.5f,
    val charY: Float = 0.5f,
    val charScale: Float = 1.0f,
    /** 캐릭터 부유 그림자(엘리베이션) 표시 여부. */
    val showShadow: Boolean = true,
    // ── 요소별 글꼴 스타일 ("REGULAR" | "BOLD" | "ITALIC" | "BOLD_ITALIC") ──
    val nicknameStyle: String = "BOLD",
    val taglineStyle: String = "REGULAR",
    val statsStyle: String = "REGULAR",
    // ── 요소별 표시/위치(중심 앵커 0~1)/크기 단계 — 닉네임·한마디·기록 독립 커스텀 ──
    val showNickname: Boolean = true,
    val nicknameX: Float = 0.83f,
    val nicknameY: Float = 0.40f,
    val nicknameScaleStep: Int = 3,
    val showTagline: Boolean = true,
    val taglineX: Float = 0.83f,
    val taglineY: Float = 0.57f,
    val taglineScaleStep: Int = 3,
    /** 기록(통계) 칩 표시 여부. false면 칩을 그리지 않는다. */
    val showStats: Boolean = true,
    val statsX: Float = 0.16f,
    val statsY: Float = 0.90f,
    val statsScaleStep: Int = 3,
    /** 닉네임 알약 스타일 ("NONE" | "BLACK" | "WHITE" | "BLUR"). */
    val nicknamePill: String = "WHITE",
    /** 소개 알약 스타일. */
    val taglinePill: String = "NONE",
    /** 기록 칩 알약 스타일. */
    val statsPill: String = "BLUR",
    /** 닉네임 색상 ("#RRGGBB"). */
    val nicknameColor: String = "#000000",
    /** 소개 줄 색상 ("#RRGGBB"). */
    val taglineColor: String = "#000000",
    /** 기록 줄 색상 ("#RRGGBB"). */
    val statsColor: String = "#000000",
    // ── 요소별 글자 외곽선(테두리) — 알약 없음(NONE) 스타일에서만 그려진다 ──
    val nicknameOutline: Boolean = false,
    val taglineOutline: Boolean = false,
    val statsOutline: Boolean = false,
)

/**
 * 2752×1536 도트아트 프로필 카드를 Canvas로 합성한다.
 * 배경 → 테두리 → 캐릭터 → 텍스트 순서로 4레이어를 그린다.
 */
object ProfileCardRenderer {
    const val CARD_WIDTH = 2752
    const val CARD_HEIGHT = 1536

    // 배경 칩(알약) 내부 여백 — 글자 크기 대비 배율. 디자인 시안의 여유 있는 캡슐 비율 기준.
    private const val PILL_PAD_H = 0.80f
    private const val PILL_PAD_V = 0.45f

    /** 알약 패딩 증가를 누르기 시작하는 글자 크기(합성 기준 px, ×u 이전). 사다리 4단계. */
    private const val PILL_PAD_CAP_BASE = 48f

    // 텍스트 요소 기준 크기(px, 1472폭 기준) — 카드 합성과 GPU 프리뷰가 공유한다.
    const val NICKNAME_BASE_SIZE = 60f
    const val TAGLINE_BASE_SIZE = 32f
    const val STATS_BASE_SIZE = 30f

    // 캐릭터 부유 그림자(엘리베이션) 파라미터 — 캐릭터 크기 대비 비율.
    /** 실루엣 블러가 밖으로 번질 여유 (소스 크기 대비) — 블러 반경도 이 값에 비례한다. */
    const val SHADOW_MARGIN_FRAC = 0.035f
    /** 그림자 세로 오프셋 — 캐릭터 바로 뒤에서 아주 살짝만 아래로. */
    const val SHADOW_OFFSET_FRAC = 0.012f
    /** 그림자 진하기 (0~255) — 좁고 진한 윤곽으로 캐릭터를 또렷하게 띄운다. */
    private const val SHADOW_ALPHA = 140

    // 캐릭터 비트맵 → 블러 실루엣 그림자 캐시. 같은 캐릭터면 재계산하지 않는다.
    private val shadowCache = LruMemoizer<Bitmap, Bitmap>(maxSize = 6)

    /**
     * 캐릭터 비트맵의 부유 그림자(크게 블러된 실루엣)를 만든다 — 카드 합성과 GPU 프리뷰 공용.
     * 결과 비트맵은 소스보다 사방 [SHADOW_MARGIN_FRAC]만큼 큰 캔버스에 그려진다
     * (배치 시 같은 비율로 확장해서 그리면 스케일이 일치한다).
     *
     * @param src 캐릭터 비트맵 (투명 배경 전제)
     */
    fun elevationShadowOf(src: Bitmap): Bitmap = shadowCache.getOrPut(src) { bmp ->
        val margin = (bmp.width * SHADOW_MARGIN_FRAC).toInt().coerceAtLeast(8)
        val out = Bitmap.createBitmap(bmp.width + margin * 2, bmp.height + margin * 2, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val silhouette = bmp.extractAlpha()
        val p = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(SHADOW_ALPHA, 0, 0, 0)
            // 반경을 마진에 맞춰 크게 — 캐릭터 모양이 은은하게 뭉개져 elevation처럼 읽힌다
            maskFilter = BlurMaskFilter(margin * 0.85f, BlurMaskFilter.Blur.NORMAL)
        }
        c.drawBitmap(silhouette, margin.toFloat(), margin.toFloat(), p)
        silhouette.recycle()
        out
    }

    // 텍스트·워터마크 px는 원래 1472폭 기준으로 튜닝됐다. 카드 해상도가 바뀌어도
    // 화면상 크기가 유지되도록 기준폭(1472) 대비 배율(u)로 환산해 그린다.
    private const val TEXT_REF_WIDTH = 1472f

    // 합성 비트맵 캐시 — 같은 입력(CardLayers×해상도)이면 재합성 없이 재사용한다.
    // 홈↔편집↔전체보기 재진입·탭 전환마다 2752×1536 비트맵을 다시 그리는 끊김을 막는다.
    // 편집 라이브 프리뷰와 홈 카드가 같은 입력이면 캐시를 공유하므로, 저장 후 복귀 시에도 재합성이 없다.
    private val cache = LruMemoizer<Pair<CardLayers, Float>, Bitmap>(maxSize = 4)

    /**
     * [render] 결과를 (CardLayers, 해상도 배율) 단위로 캐시해 돌려준다. 같은 입력이면 재합성하지 않는다.
     *
     * @param layers 합성에 쓸 4레이어 데이터(캐시 키)
     * @param resolutionScale 합성 해상도 배율 (1 = 2752×1536). 편집 라이브 프리뷰는 낮춰서
     *   슬라이더 틱마다의 재합성 비용을 줄인다.
     */
    fun renderCached(layers: CardLayers, resolutionScale: Float = 1f): Bitmap =
        cache.getOrPut(layers to resolutionScale) { (l, s) -> render(l, s) }

    /**
     * 캐시에 합성 결과가 있으면 즉시 반환하고, 없으면 null을 반환한다(새로 합성하지 않음).
     * 백그라운드 합성 전 직전 결과를 즉시 보여주는 용도.
     *
     * @param layers 합성에 쓸 4레이어 데이터(캐시 키)
     * @param resolutionScale 합성 해상도 배율 — [renderCached]와 같은 키로 조회한다
     */
    fun peek(layers: CardLayers, resolutionScale: Float = 1f): Bitmap? = cache.get(layers to resolutionScale)

    /**
     * 카드 둘레에 유리 매트 베젤(전체보기 프레임과 같은 톤)을 두른 저장/공유용 비트맵.
     *
     * 전체보기에서 보이는 프레임을 갤러리 저장 이미지에도 그대로 담기 위한 것.
     * 라이브 배경 블러 대신 밝은 파스텔 그라데이션 + 흰 틴트로 프로스트 유리 느낌을 근사한다.
     *
     * @param layers 카드 레이어
     * @return 카드 + 베젤 프레임이 합쳐진 비트맵 (사방 여백 포함)
     */
    fun renderFramed(layers: CardLayers): Bitmap {
        val card = renderCached(layers)
        val margin = (CARD_HEIGHT * 0.05f)
        val outW = (card.width + margin * 2f).toInt()
        val outH = (card.height + margin * 2f).toInt()
        val out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val frameCorner = CARD_HEIGHT * 0.10f
        val cardCorner = frameCorner - margin
        val frameRect = RectF(0f, 0f, outW.toFloat(), outH.toFloat())

        // 프레임 바탕 — 앱 라이트 배경과 같은 파스텔 그라데이션(165°)
        val framePaint = Paint().apply {
            isAntiAlias = true
            shader = android.graphics.LinearGradient(
                outW * 0.16f, 0f, outW * 0.84f, outH.toFloat(),
                intArrayOf(0xFFCFE9FA.toInt(), 0xFFD6F0E6.toInt(), 0xFFE4DCF8.toInt()),
                floatArrayOf(0f, 0.5f, 1f), android.graphics.Shader.TileMode.CLAMP,
            )
        }
        c.drawRoundRect(frameRect, frameCorner, frameCorner, framePaint)
        // 흰 유리 틴트 — 프로스트 매트 느낌
        c.drawRoundRect(frameRect, frameCorner, frameCorner, Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(120, 255, 255, 255)
        })

        // 카드 — 라운드 클립 후 여백만큼 안쪽에 배치
        val cardLeft = margin
        val cardTop = margin
        val cardRect = RectF(cardLeft, cardTop, cardLeft + card.width, cardTop + card.height)
        val save = c.save()
        c.clipPath(Path().apply { addRoundRect(cardRect, cardCorner, cardCorner, Path.Direction.CW) })
        c.drawBitmap(card, cardLeft, cardTop, Paint().apply { isFilterBitmap = true })
        c.restoreToCount(save)

        // 유리 하이라이트 보더
        c.drawRoundRect(frameRect, frameCorner, frameCorner, Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = android.graphics.Color.argb(150, 255, 255, 255)
        })
        return out
    }

    fun render(layers: CardLayers, resolutionScale: Float = 1f): Bitmap {
        val bitmap = Bitmap.createBitmap(
            (CARD_WIDTH * resolutionScale).toInt().coerceAtLeast(1),
            (CARD_HEIGHT * resolutionScale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        // 좌표계는 항상 2752×1536 기준 — 저해상도 합성 시 캔버스 스케일로 흡수한다.
        if (resolutionScale != 1f) canvas.scale(resolutionScale, resolutionScale)
        // 에셋이 도트아트에서 일반 래스터로 전환됨 — 필터링(bilinear)을 켜야
        // 배율이 안 맞는 축소/확대에서 경계가 계단지지 않는다.
        val paint = Paint().apply { isAntiAlias = true; isFilterBitmap = true }

        // 이미지 레이어가 하나도 없으면(카드 데이터/에셋 로딩 전) 흰 베이스+텍스트만 그려져
        // 카드가 흰색으로 깜빡인다. 특히 produceState는 새 합성이 끝날 때까지 이 흰 결과를
        // 그대로 보여주므로, 여기서 빈(투명) 비트맵을 반환해 흰 카드가 어떤 경로로도 안 보이게 한다.
        // 캐릭터는 정상 카드에 항상 장착되므로 완성된 카드는 영향이 없다.
        if (layers.bgBitmap == null && layers.charBitmap == null && layers.frameBitmap == null) {
            return bitmap
        }

        // Layer 0: 흰색 불투명 베이스 — 카드가 절대 투명해지지 않도록 전체를 흰색으로 채운다.
        // "배경 선택안함"이면 이 흰 바탕이 그대로 드러난다.
        paint.color = android.graphics.Color.WHITE
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), paint)

        // Layer 1: Background — bitmap이 있으면 그리고, 없으면 흰 바탕이 비친다 (폴백 도형 없음).
        if (layers.bgBitmap != null) {
            canvas.drawBitmap(layers.bgBitmap, null, RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat()), paint)
        }

        // Layer 3: Character (먼저 → 테두리 위에 안 가리도록)
        // charBitmap == null은 항상 일시적 상태다 — 비동기 로딩 중이거나 ProfileCard
        // 데이터가 아직 준비되지 않은 경우. 서버 주도 모델에서 캐릭터는 기본값이 항상
        // 장착되므로 "캐릭터 없음" 최종 상태는 존재하지 않는다. 따라서 폴백 도형을 그리면
        // 홈 진입/캐릭터 변경 시 깜빡임만 유발하므로, bitmap이 있을 때만 그린다.
        // charX/charY는 카드 전체 기준 캐릭터 '중심'의 정규화 비율(0..1)이다.
        // → charX=0.5, charY=0.5면 카드 정중앙, scale 확대는 중심에서 균등하게 커진다.
        if (layers.charBitmap != null) {
            val charSize = CARD_HEIGHT * 0.85f * layers.charScale
            val centerX = layers.charX * CARD_WIDTH
            val centerY = layers.charY * CARD_HEIGHT
            val dst = RectF(
                centerX - charSize / 2f,
                centerY - charSize / 2f,
                centerX + charSize / 2f,
                centerY + charSize / 2f,
            )
            // 캐릭터 부유 그림자(엘리베이션) — 실루엣을 크게 블러해 캐릭터가 배경에서
            // 살짝 떠 보이게 강조한다. 블러 비트맵은 캐릭터 비트맵 단위로 캐시(합성마다 재계산 없음).
            if (layers.showShadow) {
                val shadow = elevationShadowOf(layers.charBitmap)
                val expand = charSize * SHADOW_MARGIN_FRAC
                val shadowDst = RectF(dst).apply {
                    inset(-expand, -expand)
                    offset(0f, charSize * SHADOW_OFFSET_FRAC)
                }
                canvas.drawBitmap(shadow, null, shadowDst, paint)
            }

            canvas.drawBitmap(layers.charBitmap, null, dst, paint)
        }

        // Layer 2 (캐릭터 위에 덮어쓰기): 테두리 — bitmap이 있으면 그리고, 없으면 아무것도 안 그린다 (폴백 없음).
        if (layers.frameBitmap != null) {
            canvas.drawBitmap(layers.frameBitmap, null, RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat()), paint)
        }

        // Layer 4: 이름표 — 디자인 신구조: 흰 알약(닉네임) + 아래 소개 텍스트.
        // 요소 드로잉은 오브젝트 레벨 [drawElementOn]을 사용한다 (GPU 프리뷰의 요소 비트맵과 공유).
        val textPaint = Paint().apply { isAntiAlias = true }
        val u = CARD_WIDTH / TEXT_REF_WIDTH

        /** 요소 하나를 중심 앵커(cx, cy — 0~1 비율) 기준으로 그린다 — 글꼴/외곽선도 요소별 적용. */
        fun drawElementCentered(
            text: String, textSize: Float, style: String, colorRaw: Int,
            cx: Float, cy: Float, fontStyle: String, outline: Boolean,
        ) {
            // 폭/높이 측정 전에 글꼴을 먼저 적용해야 기울임·굵기의 잉크 폭이 정확하다
            textPaint.typeface = typefaceOf(fontStyle)
            val w = measureElementWidth(textPaint, text, textSize, style)
            val h = elementHeightOf(textSize, style)
            drawElementOn(
                canvas = canvas, textPaint = textPaint, u = u,
                backdrop = bitmap, backdropScale = resolutionScale,
                text = text, textSize = textSize,
                top = cy * CARD_HEIGHT - h / 2f,
                style = style, colorRaw = colorRaw,
                leftAligned = true,
                anchor = cx * CARD_WIDTH - w / 2f,
                outline = outline,
            )
        }

        // ── 텍스트 3요소 — 각각 표시/위치/크기/알약/색/글꼴/외곽선을 독립 커스텀 ──
        if (layers.showNickname) {
            drawElementCentered(
                text = layers.nickname,
                textSize = elementTextSize(NICKNAME_BASE_SIZE, layers.nicknameScaleStep) * u,
                style = layers.nicknamePill,
                colorRaw = parseColorOrDefault(layers.nicknameColor, android.graphics.Color.BLACK),
                cx = layers.nicknameX, cy = layers.nicknameY,
                fontStyle = layers.nicknameStyle, outline = layers.nicknameOutline,
            )
        }
        if (layers.showTagline) {
            drawElementCentered(
                text = layers.tagline,
                textSize = elementTextSize(TAGLINE_BASE_SIZE, layers.taglineScaleStep) * u,
                style = layers.taglinePill,
                colorRaw = parseColorOrDefault(layers.taglineColor, android.graphics.Color.BLACK),
                cx = layers.taglineX, cy = layers.taglineY,
                fontStyle = layers.taglineStyle, outline = layers.taglineOutline,
            )
        }
        if (layers.showStats) {
            drawElementCentered(
                text = layers.stats,
                textSize = elementTextSize(STATS_BASE_SIZE, layers.statsScaleStep) * u,
                style = layers.statsPill,
                colorRaw = parseColorOrDefault(layers.statsColor, android.graphics.Color.BLACK),
                cx = layers.statsX, cy = layers.statsY,
                fontStyle = layers.statsStyle, outline = layers.statsOutline,
            )
        }

        return bitmap
    }

    /**
     * "#RRGGBB" 문자열을 Color int로 파싱한다. 형식이 잘못됐으면 [fallback]을 반환한다.
     *
     * @param hex "#RRGGBB" 색상 문자열
     * @param fallback 파싱 실패 시 사용할 기본 색상 int
     */
    private fun parseColorOrDefault(hex: String, fallback: Int): Int =
        try {
            android.graphics.Color.parseColor(hex)
        } catch (e: IllegalArgumentException) {
            fallback
        }

    /**
     * 글자색 밝기에 맞춘 대비 그림자(글로우) 색을 만든다.
     * 밝은 글자는 어두운 글로우, 어두운 글자는 흰 글로우로 어떤 배경에서도 가독성을 살린다.
     * (이전엔 검은 그림자 고정이라 어두운 글자에서 그림자가 묻혀 잘 안 보였다)
     *
     * @param textColor 글자색 int
     */
    private fun contrastShadow(textColor: Int): Int {
        val lum = 0.299 * android.graphics.Color.red(textColor) +
            0.587 * android.graphics.Color.green(textColor) +
            0.114 * android.graphics.Color.blue(textColor)
        return if (lum >= 140) {
            android.graphics.Color.argb(150, 0, 0, 0)
        } else {
            android.graphics.Color.argb(200, 255, 255, 255)
        }
    }

    /** 요소 글꼴 스타일 문자열 → Typeface. */
    private fun typefaceOf(style: String): Typeface = when (style) {
        "BOLD" -> Typeface.DEFAULT_BOLD
        "ITALIC" -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        "BOLD_ITALIC" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
        else -> Typeface.DEFAULT
    }

    /**
     * 통일 글자 크기 사다리(합성 기준 px) — 한마디·기록은 1~7단계를 그대로 타고,
     * 닉네임은 1~4단계가 사다리 4~7단계에 대응한다. 그래서 닉네임 1·2·3단계는
     * 한마디·기록의 4·5·6단계와 크기가 같고, 닉네임 4단계 = 나머지 7단계다.
     */
    private val TEXT_SIZE_LADDER = floatArrayOf(25.6f, 32f, 38.4f, 48f, 60.8f, 76.8f, 96f)

    /** 요소 종류(기준 크기로 구분)와 단계 → 합성 기준 px. 닉네임은 사다리를 3칸 위에서 탄다. */
    fun elementTextSize(baseSize: Float, step: Int): Float {
        val ladderStep = if (baseSize == NICKNAME_BASE_SIZE) step.coerceIn(1, 4) + 3 else step
        return TEXT_SIZE_LADDER[(ladderStep - 1).coerceIn(0, 6)]
    }

    /**
     * 알약 패딩 기준 크기 — 캡(사다리 4단계)까지는 글자 크기에 비례하고,
     * 그보다 큰 글자에서는 증가분을 40%만 반영해 여백이 비대해지지 않게 한다.
     */
    private fun pillPadBasis(textSize: Float): Float {
        val cap = PILL_PAD_CAP_BASE * (CARD_WIDTH / TEXT_REF_WIDTH)
        return if (textSize <= cap) textSize else cap + (textSize - cap) * 0.4f
    }

    /** 스타일별 요소 높이 예측 (중심 앵커 배치용). */
    private fun elementHeightOf(textSize: Float, style: String): Float =
        if (style == "NONE") textSize * 1.15f
        else textSize + pillPadBasis(textSize) * PILL_PAD_V * 2f

    /** 스타일별 요소 폭 예측 — 알약은 좌우 패딩 포함, 폭은 잉크 경계 기준. typeface가 설정된 페인트 필요. */
    private fun measureElementWidth(textPaint: Paint, text: String, textSize: Float, style: String): Float {
        textPaint.textSize = textSize
        val ink = Rect()
        textPaint.getTextBounds(text, 0, text.length, ink)
        val w = ink.width().toFloat()
        return if (style == "NONE") w else w + pillPadBasis(textSize) * PILL_PAD_H * 2f
    }

    /**
     * 텍스트 요소 하나를 알약 스타일에 맞춰 [canvas]에 그린다 — 카드 합성과 요소 비트맵이 공유하는 단일 구현.
     *
     * @param u 기준폭(1472) 대비 배율 — 그림자/보더 두께 환산용
     * @param backdrop BLUR 칩이 샘플할 합성 중 카드 비트맵. null이면 반투명 프로스트 폴백으로 그린다.
     * @param backdropScale [backdrop]의 해상도 배율 — 사용자 좌표(rect)를 backdrop 픽셀 좌표로 변환
     * @param style "NONE"(맨글자) | "BLACK" | "WHITE" | "BLUR"
     * @param outline 글자 외곽선 표시 여부 — 알약 없음(NONE)에서만 그려진다
     * @return 그린 요소의 전체 높이(px)
     */
    private fun drawElementOn(
        canvas: Canvas,
        textPaint: Paint,
        u: Float,
        backdrop: Bitmap?,
        backdropScale: Float,
        text: String,
        textSize: Float,
        top: Float,
        style: String,
        colorRaw: Int,
        leftAligned: Boolean,
        anchor: Float,
        outline: Boolean,
        /** BLUR 칩에 backdrop이 없을 때(편집 프리뷰) 프로스트 틴트로 쓸 배경 근사색. null이면 흰색. */
        blurFallbackColor: Int? = null,
    ): Float {
        textPaint.textSize = textSize
        textPaint.textAlign = Paint.Align.LEFT
        textPaint.style = Paint.Style.FILL
        textPaint.clearShadowLayer()
        // 기울임(이탤릭)은 글자가 진행 폭(measureText) 밖으로 기울어 나가 우측 여백이
        // 좁아 보인다 — 실제 잉크 경계로 폭을 재고 좌측 오프셋을 보정해 양쪽을 맞춘다.
        val ink = Rect()
        textPaint.getTextBounds(text, 0, text.length, ink)
        val textW = ink.width().toFloat()
        val inkLeft = ink.left.toFloat()

        if (style == "NONE") {
            // 알약 없음 — 맨글자 + 대비 그림자(옵션 시 외곽선).
            val baseline = top + textSize
            val x = (if (leftAligned) anchor else anchor - textW) - inkLeft
            if (outline) {
                textPaint.style = Paint.Style.STROKE
                textPaint.strokeWidth = textSize * 0.09f
                textPaint.color = outlineColor(colorRaw)
                canvas.drawText(text, x, baseline, textPaint)
                textPaint.style = Paint.Style.FILL
            }
            textPaint.color = colorRaw
            textPaint.setShadowLayer(7f * u, 0f, 2f * u, contrastShadow(colorRaw))
            canvas.drawText(text, x, baseline, textPaint)
            textPaint.clearShadowLayer()
            return textSize * 1.15f
        }

        // ── 알약(캡슐) 계열 ──
        val padH = pillPadBasis(textSize) * PILL_PAD_H
        val padV = pillPadBasis(textSize) * PILL_PAD_V
        val pillW = textW + padH * 2f
        val pillH = textSize + padV * 2f
        val left = if (leftAligned) anchor else anchor - pillW
        val rect = RectF(left, top, left + pillW, top + pillH)
        val radius = pillH / 2f

        when (style) {
            "BLACK" -> {
                val pillPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(210, 31, 42, 55)
                    setShadowLayer(10f * u, 0f, 3f * u, android.graphics.Color.argb(70, 0, 20, 40))
                }
                canvas.drawRoundRect(rect, radius, radius, pillPaint)
                // 사용자가 고른 색 그대로 — 검정 칩+검정 글자 같은 조합도 허용한다.
                textPaint.color = colorRaw
            }
            "BLUR" -> {
                // 이미 합성된 카드(배경/캐릭터)를 영역 블러 → 진짜 프로스트 알약.
                val shadowPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(10f * u, 0f, 3f * u, android.graphics.Color.argb(60, 0, 20, 40))
                }
                canvas.drawRoundRect(rect, radius, radius, shadowPaint)
                // backdrop이 저해상도 합성본이면 픽셀 좌표계로 변환해 샘플한다
                val sampleRect = if (backdropScale == 1f) rect else RectF(
                    rect.left * backdropScale, rect.top * backdropScale,
                    rect.right * backdropScale, rect.bottom * backdropScale,
                )
                val blurred = backdrop?.let {
                    blurRegion(it, sampleRect, (14f * u * backdropScale).toInt().coerceAtLeast(8))
                }
                if (blurred != null) {
                    val save = canvas.save()
                    val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
                    canvas.clipPath(clip)
                    canvas.drawBitmap(blurred, null, rect, Paint().apply { isFilterBitmap = true })
                    canvas.drawRect(rect, Paint().apply { color = android.graphics.Color.argb(115, 255, 255, 255) })
                    canvas.restoreToCount(save)
                } else {
                    // 배경 샘플 불가(요소 단독 렌더/영역이 카드 밖) — 프로스트 폴백.
                    // 편집 프리뷰는 칩 뒤 배경 근사색(blurFallbackColor)을 흰색과 섞어 젖빛 유리처럼
                    // 보이게 한다(무지 흰 칩 대신). 근사색이 없으면 흰 반투명.
                    val frost = if (blurFallbackColor != null) {
                        android.graphics.Color.argb(
                            205,
                            (android.graphics.Color.red(blurFallbackColor) + 255) / 2,
                            (android.graphics.Color.green(blurFallbackColor) + 255) / 2,
                            (android.graphics.Color.blue(blurFallbackColor) + 255) / 2,
                        )
                    } else {
                        android.graphics.Color.argb(180, 255, 255, 255)
                    }
                    canvas.drawRoundRect(rect, radius, radius, Paint().apply {
                        isAntiAlias = true
                        color = frost
                    })
                }
                // 유리 하이라이트 보더. (this.style — 함수 파라미터 style이 가리므로 명시)
                canvas.drawRoundRect(rect, radius, radius, Paint().apply {
                    isAntiAlias = true
                    this.style = Paint.Style.STROKE
                    strokeWidth = 2.2f * u
                    color = android.graphics.Color.argb(165, 255, 255, 255)
                })
                textPaint.color = colorRaw
            }
            else -> { // WHITE
                val pillPaint = Paint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.argb(230, 255, 255, 255)
                    setShadowLayer(10f * u, 0f, 3f * u, android.graphics.Color.argb(70, 0, 20, 40))
                }
                canvas.drawRoundRect(rect, radius, radius, pillPaint)
                // 사용자가 고른 색 그대로 — 흰 칩+흰 글자 같은 조합도 허용한다.
                textPaint.color = colorRaw
            }
        }

        val baseline = top + padV + textSize - textPaint.descent() * 0.35f
        canvas.drawText(text, left + padH - inkLeft, baseline, textPaint)
        return pillH
    }

    /** [renderElementBitmap] 결과의 사방 여백(px, 카드 좌표) — 칩/글자 그림자가 잘리지 않을 여유. */
    val ELEMENT_BITMAP_MARGIN = 16f * (CARD_WIDTH / TEXT_REF_WIDTH)

    /**
     * 배경 비트맵의 (cx, cy) 지점(0~1 비율) 주변 작은 영역 평균색 — 편집 프리뷰 BLUR 칩 틴트용.
     *
     * @param bg 배경 비트맵
     * @param cx 가로 위치 비율 (0~1)
     * @param cy 세로 위치 비율 (0~1)
     */
    fun sampleBgColor(bg: Bitmap, cx: Float, cy: Float): Int {
        val px = (cx * bg.width).toInt().coerceIn(0, bg.width - 1)
        val py = (cy * bg.height).toInt().coerceIn(0, bg.height - 1)
        val r = (minOf(bg.width, bg.height) * 0.05f).toInt().coerceAtLeast(1)
        var sr = 0L; var sg = 0L; var sb = 0L; var n = 0L
        var y = (py - r).coerceAtLeast(0)
        while (y <= (py + r).coerceAtMost(bg.height - 1)) {
            var x = (px - r).coerceAtLeast(0)
            while (x <= (px + r).coerceAtMost(bg.width - 1)) {
                val c = bg.getPixel(x, y)
                sr += android.graphics.Color.red(c); sg += android.graphics.Color.green(c); sb += android.graphics.Color.blue(c)
                n++; x += 3
            }
            y += 3
        }
        if (n == 0L) return android.graphics.Color.WHITE
        return android.graphics.Color.rgb((sr / n).toInt(), (sg / n).toInt(), (sb / n).toInt())
    }

    /**
     * 텍스트 요소 하나를 자체 비트맵으로 렌더한다 — GPU 편집 프리뷰가 Compose 레이어로 배치한다.
     * 요소 중심 = 비트맵 중심(여백이 사방 동일). 크기는 카드(2752×1536) 픽셀 기준.
     * BLUR 칩은 배경 샘플이 불가능하므로 반투명 프로스트 폴백으로 그려진다(저장 시 진짜 블러로 대체).
     *
     * @param baseSize 기준 글자 크기 ([NICKNAME_BASE_SIZE] 등, 1472폭 기준 px)
     */
    fun renderElementBitmap(
        text: String,
        baseSize: Float,
        scaleStep: Int,
        pill: String,
        colorRaw: Int,
        fontStyle: String,
        outline: Boolean,
        /** BLUR 칩 프로스트 폴백 틴트에 쓸 배경 근사색(편집 프리뷰). null이면 흰색. */
        blurFallbackColor: Int? = null,
    ): Bitmap {
        val u = CARD_WIDTH / TEXT_REF_WIDTH
        val textSize = elementTextSize(baseSize, scaleStep) * u
        val textPaint = Paint().apply {
            isAntiAlias = true
            typeface = typefaceOf(fontStyle)
        }
        val w = measureElementWidth(textPaint, text, textSize, pill)
        val h = elementHeightOf(textSize, pill)
        val margin = ELEMENT_BITMAP_MARGIN
        val bmp = Bitmap.createBitmap(
            (w + margin * 2).toInt().coerceAtLeast(1),
            (h + margin * 2).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        drawElementOn(
            canvas = Canvas(bmp), textPaint = textPaint, u = u,
            backdrop = null, backdropScale = 1f,
            text = text, textSize = textSize, top = margin,
            style = pill, colorRaw = colorRaw,
            leftAligned = true, anchor = margin, outline = outline,
            blurFallbackColor = blurFallbackColor,
        )
        return bmp
    }

    /**
     * 글자색 밝기에 맞춘 불투명 외곽선(테두리) 색을 만든다.
     * 밝은 글자는 짙은 남색 테두리, 어두운 글자는 흰 테두리로 글자 경계를 또렷하게 한다.
     * [contrastShadow]와 동일한 luminance 기준을 쓰되 알파는 항상 불투명(255)이다.
     *
     * @param textColor 글자색 int
     */
    private fun outlineColor(textColor: Int): Int {
        val lum = 0.299 * android.graphics.Color.red(textColor) +
            0.587 * android.graphics.Color.green(textColor) +
            0.114 * android.graphics.Color.blue(textColor)
        return if (lum >= 140) {
            android.graphics.Color.argb(255, 40, 40, 60)
        } else {
            android.graphics.Color.argb(255, 255, 255, 255)
        }
    }

    /**
     * 카드 비트맵의 [rect] 영역을 잘라 분리형 박스 블러를 적용해 돌려준다 (블러 알약용).
     * 영역이 카드 밖으로 나가 유효 크기가 없으면 null.
     */
    private fun blurRegion(src: Bitmap, rect: RectF, radius: Int): Bitmap? {
        val l = rect.left.toInt().coerceIn(0, src.width - 1)
        val t = rect.top.toInt().coerceIn(0, src.height - 1)
        val r = rect.right.toInt().coerceIn(l + 1, src.width)
        val b = rect.bottom.toInt().coerceIn(t + 1, src.height)
        val w = r - l
        val h = b - t
        if (w < 4 || h < 4) return null

        val px = IntArray(w * h)
        src.getPixels(px, 0, w, l, t, w, h)
        val tmp = IntArray(w * h)
        val win = radius * 2 + 1

        // 가로 패스
        for (y in 0 until h) {
            var sr = 0; var sg = 0; var sb = 0
            val row = y * w
            for (x in -radius..radius) {
                val c = px[row + x.coerceIn(0, w - 1)]
                sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
            }
            for (x in 0 until w) {
                tmp[row + x] = (0xFF shl 24) or ((sr / win) shl 16) or ((sg / win) shl 8) or (sb / win)
                val add = px[row + (x + radius + 1).coerceIn(0, w - 1)]
                val sub = px[row + (x - radius).coerceIn(0, w - 1)]
                sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                sb += (add and 0xFF) - (sub and 0xFF)
            }
        }
        // 세로 패스
        for (x in 0 until w) {
            var sr = 0; var sg = 0; var sb = 0
            for (y in -radius..radius) {
                val c = tmp[y.coerceIn(0, h - 1) * w + x]
                sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
            }
            for (y in 0 until h) {
                px[y * w + x] = (0xFF shl 24) or ((sr / win) shl 16) or ((sg / win) shl 8) or (sb / win)
                val add = tmp[(y + radius + 1).coerceIn(0, h - 1) * w + x]
                val sub = tmp[(y - radius).coerceIn(0, h - 1) * w + x]
                sr += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                sb += (add and 0xFF) - (sub and 0xFF)
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(px, 0, w, 0, 0, w, h)
        return out
    }
}

/**
 * 매니페스트 상대 경로로부터 Bitmap을 비동기 로딩하고 Compose 상태로 보관한다.
 *
 * 로컬 에셋이 있으면 디스크에서 바로 읽고, 없으면 fallback URL로 네트워크 다운로드.
 * 빈 문자열/null이면 null 반환.
 *
 * @param imageAsset 매니페스트 기준 상대 경로 (예: "characters/ssr_01.png").
 */
@Composable
fun rememberAssetBitmap(imageAsset: String?): Bitmap? {
    if (imageAsset.isNullOrBlank()) return null
    val context = LocalContext.current
    // 경로가 바뀌어도 상태를 리셋하지 않는다(remember 키에 imageAsset 없음). 새 비트맵이
    // 로드되기 전까지 직전 비트맵을 유지해, 편집 중 배경/캐릭터 교체 시 미리보기가 잠깐
    // 비면서 아래 저장 카드(옛 값)가 비쳐 카드 전체가 깜빡이던 현상을 막는다.
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(imageAsset) {
        try {
            val assetStore = EntryPointAccessors
                .fromApplication(context.applicationContext, AssetStoreEntryPoint::class.java)
                .assetStore()
            val model = resolveAssetModel(assetStore, imageAsset) ?: return@LaunchedEffect
            // Coil ImageLoader 싱글톤을 사용 — 매 호출마다 새 인스턴스를 만들면 캐시/스레드풀이 분산된다.
            val loader = Coil.imageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(model)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            // 새 비트맵이 준비된 순간에만 교체 (로딩 중엔 이전 비트맵 유지 → 깜빡임 없음)
            (result.drawable as? BitmapDrawable)?.bitmap?.let { bmp = it }
        } catch (e: Exception) {
            Timber.w(e, "에셋 비트맵 로딩 실패: $imageAsset")
        }
    }
    return bmp
}

/**
 * ProfileCardRenderer 결과를 Compose Image로 표시한다.
 *
 * bg/char/frame 경로가 있으면 로컬 우선(없으면 네트워크 폴백) Bitmap을 받아 layers에 주입한다.
 * 에셋 경로가 null이면 해당 레이어는 호출 측이 [layers]에 미리 주입한 Bitmap을 그대로 사용한다.
 * (null 경로가 이미 로딩된 [layers]의 Bitmap을 덮어쓰지 않도록 `?: layers.xxxBitmap` 폴백)
 *
 * @param bgAsset 배경 에셋 상대 경로 (null이면 layers.bgBitmap 사용)
 * @param charAsset 캐릭터 에셋 상대 경로 (null이면 layers.charBitmap 사용)
 * @param frameAsset 테두리 에셋 상대 경로 (null이면 layers.frameBitmap 사용)
 */
@Composable
fun ProfileCardComposite(
    layers: CardLayers,
    bgAsset: String? = null,
    charAsset: String? = null,
    frameAsset: String? = null,
    modifier: Modifier = Modifier,
    /**
     * 합성 해상도 배율 — 편집 라이브 프리뷰처럼 입력이 빠르게 바뀌는 화면은 0.5로 낮춰
     * 슬라이더 틱마다의 재합성 비용(픽셀 수)을 1/4로 줄인다. 표시 크기가 프리뷰 수준이면
     * 화질 차이는 보이지 않는다.
     */
    resolutionScale: Float = 1f,
    /**
     * 현재 입력(layers/에셋)에 해당하는 비트맵을 실제로 표시 중인지 알림.
     * 홈 카드가 편집 프리뷰 오버레이를 내릴 타이밍(합성 준비 완료) 판단에 쓴다.
     */
    onUpToDateChange: ((Boolean) -> Unit)? = null,
) {
    val bgBitmap = rememberAssetBitmap(bgAsset) ?: layers.bgBitmap
    val charBitmap = rememberAssetBitmap(charAsset) ?: layers.charBitmap
    val frameBitmap = rememberAssetBitmap(frameAsset) ?: layers.frameBitmap

    // 에셋 경로가 지정됐는데 그 비트맵이 아직 로딩 전이면, 합성하면 흰 베이스(render Layer0)만
    // 그려진다 → 카드가 흰색으로 깜빡인다. 이때는 카드를 그리지 않고 빈 자리(투명)만 유지한다.
    // 경로가 null인 경우(배경 '선택안함')는 의도된 흰 배경이므로 보류하지 않는다.
    val assetPending = (!bgAsset.isNullOrBlank() && bgBitmap == null) ||
        (!charAsset.isNullOrBlank() && charBitmap == null) ||
        (!frameAsset.isNullOrBlank() && frameBitmap == null)

    // 이미지 레이어가 하나도 없으면(전부 null) render는 흰 베이스+텍스트만 만든다.
    // 카드 데이터 로딩 전이라 에셋 경로조차 아직 null인 경우(assetPending이 못 잡는 구간)도
    // 여기서 흰 카드를 막는다. 캐릭터는 항상 기본 장착되므로 정상 카드는 영향이 없다.
    val hasAnyImage = bgBitmap != null || charBitmap != null || frameBitmap != null

    val finalLayers = layers.copy(
        bgBitmap = bgBitmap,
        charBitmap = charBitmap,
        frameBitmap = frameBitmap,
    )
    // 합성은 백그라운드 스레드에서 — 슬라이더 조작·화면 진입 시 메인 스레드를 막지 않는다.
    // 캐시에 있으면 즉시 표시(initialValue)하고, 없으면 새 비트맵이 나올 때까지 직전 비트맵을
    // 유지해 카드가 흰색으로 깜빡이지 않게 한다. finalLayers가 바뀌면 그 사이 이전 카드를 보여준다.
    val bitmap by produceState<Bitmap?>(
        initialValue = ProfileCardRenderer.peek(finalLayers, resolutionScale),
        finalLayers,
        resolutionScale,
    ) {
        value = withContext(Dispatchers.Default) { ProfileCardRenderer.renderCached(finalLayers, resolutionScale) }
    }

    val cardModifier = modifier
        .fillMaxWidth()
        .aspectRatio(ProfileCardRenderer.CARD_WIDTH.toFloat() / ProfileCardRenderer.CARD_HEIGHT.toFloat())
    val current = bitmap

    // 표시 중인 비트맵이 현재 입력의 합성 결과와 동일 인스턴스인지로 최신 여부를 판정한다.
    // (produceState는 키가 바뀌어도 직전 비트맵을 유지하므로 null 아님만으로는 부족)
    val upToDate = current != null && hasAnyImage && !assetPending &&
        ProfileCardRenderer.peek(finalLayers, resolutionScale) === current
    val onUpToDate by rememberUpdatedState(onUpToDateChange)
    LaunchedEffect(upToDate) { onUpToDate?.invoke(upToDate) }

    if (current != null && hasAnyImage && !assetPending) {
        Image(
            bitmap = current.asImageBitmap(),
            contentDescription = "프로필 카드",
            modifier = cardModifier,
        )
    } else {
        // 합성 전/에셋 로딩 전 잠깐의 빈 자리 — 흰 베이스 카드 대신 투명 자리를 같은 크기로 둔다.
        Box(modifier = cardModifier)
    }
}
