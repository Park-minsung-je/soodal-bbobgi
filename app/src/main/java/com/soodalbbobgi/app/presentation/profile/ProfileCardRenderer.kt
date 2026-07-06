package com.soodalbbobgi.app.presentation.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil.Coil
import coil.request.ImageRequest
import com.soodalbbobgi.app.core.ui.AssetStoreEntryPoint
import com.soodalbbobgi.app.core.ui.resolveAssetModel
import com.soodalbbobgi.app.core.util.LruMemoizer
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
    /** 텍스트 글꼴 스타일 ("REGULAR" | "BOLD" | "ITALIC"). 세 요소 전체에 적용. */
    val textStyle: String = "REGULAR",
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
    val nicknameColor: String = "#FFFFFF",
    /** 소개 줄 색상 ("#RRGGBB"). */
    val taglineColor: String = "#FFFFFF",
    /** 기록 줄 색상 ("#RRGGBB"). */
    val statsColor: String = "#00F5FF",
    /** 텍스트 외곽선(테두리) 표시 여부. true면 세 줄 모두 글자 둘레에 스트로크를 그린다. */
    val textOutline: Boolean = false,
)

/**
 * 2752×1536 도트아트 프로필 카드를 Canvas로 합성한다.
 * 배경 → 테두리 → 캐릭터 → 텍스트 순서로 4레이어를 그린다.
 */
object ProfileCardRenderer {
    const val CARD_WIDTH = 2752
    const val CARD_HEIGHT = 1536

    // 텍스트·워터마크 px는 원래 1472폭 기준으로 튜닝됐다. 카드 해상도가 바뀌어도
    // 화면상 크기가 유지되도록 기준폭(1472) 대비 배율(u)로 환산해 그린다.
    private const val TEXT_REF_WIDTH = 1472f

    // 합성 비트맵 캐시 — 같은 입력(CardLayers)이면 재합성 없이 재사용한다.
    // 홈↔편집↔전체보기 재진입·탭 전환마다 2752×1536 비트맵을 다시 그리는 끊김을 막는다.
    // 편집 라이브 프리뷰와 홈 카드가 같은 입력이면 캐시를 공유하므로, 저장 후 복귀 시에도 재합성이 없다.
    private val cache = LruMemoizer<CardLayers, Bitmap>(maxSize = 4)

    /**
     * [render] 결과를 [CardLayers] 단위로 캐시해 돌려준다. 같은 입력이면 재합성하지 않는다.
     *
     * @param layers 합성에 쓸 4레이어 데이터(캐시 키)
     */
    fun renderCached(layers: CardLayers): Bitmap = cache.getOrPut(layers) { render(it) }

    /**
     * 캐시에 합성 결과가 있으면 즉시 반환하고, 없으면 null을 반환한다(새로 합성하지 않음).
     * 백그라운드 합성 전 직전 결과를 즉시 보여주는 용도.
     *
     * @param layers 합성에 쓸 4레이어 데이터(캐시 키)
     */
    fun peek(layers: CardLayers): Bitmap? = cache.get(layers)

    fun render(layers: CardLayers): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }

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
            canvas.drawBitmap(layers.charBitmap, null, dst, paint)
        }

        // Layer 2 (캐릭터 위에 덮어쓰기): 테두리 — bitmap이 있으면 그리고, 없으면 아무것도 안 그린다 (폴백 없음).
        if (layers.frameBitmap != null) {
            canvas.drawBitmap(layers.frameBitmap, null, RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat()), paint)
        }

        // Layer 4: 이름표 — 디자인 신구조: 흰 알약(닉네임) + 아래 소개 텍스트.
        // 위치(textX/textY 앵커)·크기 단계·표시여부는 기존 커스텀 값을 그대로 따른다.
        val blockTypeface = when (layers.textStyle) {
            "BOLD" -> Typeface.DEFAULT_BOLD
            "ITALIC" -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            "BOLD_ITALIC" -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            else -> Typeface.DEFAULT
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            typeface = blockTypeface
        }

        // 카드 해상도 대비 텍스트 크기 보정 — 기준폭 1472 대비 현재 폭 비율.
        val u = CARD_WIDTH / TEXT_REF_WIDTH
        // 요소 크기 단계(1~5) → 배율. 3이 기준(1.2).
        fun scaleMulOf(step: Int): Float =
            floatArrayOf(0.8f, 1.0f, 1.2f, 1.5f, 1.8f)[(step - 1).coerceIn(0, 4)]

        /**
         * 텍스트 요소 하나를 알약 스타일에 맞춰 그린다.
         *
         * @param style "NONE"(맨글자) | "BLACK" | "WHITE" | "BLUR"
         * @return 그린 요소의 전체 높이(px)
         */
        fun drawElement(
            text: String,
            textSize: Float,
            top: Float,
            style: String,
            colorRaw: Int,
            leftAligned: Boolean,
            anchor: Float,
        ): Float {
            textPaint.textSize = textSize
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.style = Paint.Style.FILL
            textPaint.clearShadowLayer()
            val textW = textPaint.measureText(text)

            if (style == "NONE") {
                // 알약 없음 — 맨글자 + 대비 그림자(옵션 시 외곽선).
                val baseline = top + textSize
                val x = if (leftAligned) anchor else anchor - textW
                if (layers.textOutline) {
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
            val padH = textSize * 0.55f
            val padV = textSize * 0.30f
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
                    // 어두운 커스텀 색은 검정 알약 위에서 안 보여 흰색 폴백.
                    textPaint.color = if (isNearBlack(colorRaw)) android.graphics.Color.WHITE else colorRaw
                }
                "BLUR" -> {
                    // 이미 합성된 카드(배경/캐릭터)를 영역 블러 → 진짜 프로스트 알약.
                    val shadowPaint = Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(10f * u, 0f, 3f * u, android.graphics.Color.argb(60, 0, 20, 40))
                    }
                    canvas.drawRoundRect(rect, radius, radius, shadowPaint)
                    val blurred = blurRegion(bitmap, rect, (14f * u).toInt().coerceAtLeast(8))
                    if (blurred != null) {
                        val save = canvas.save()
                        val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
                        canvas.clipPath(clip)
                        canvas.drawBitmap(blurred, null, rect, paint)
                        canvas.drawRect(rect, Paint().apply { color = android.graphics.Color.argb(115, 255, 255, 255) })
                        canvas.restoreToCount(save)
                    } else {
                        // 영역이 카드 밖 등으로 잘리면 흰 반투명 폴백.
                        canvas.drawRoundRect(rect, radius, radius, Paint().apply {
                            isAntiAlias = true
                            color = android.graphics.Color.argb(180, 255, 255, 255)
                        })
                    }
                    // 유리 하이라이트 보더. (this.style — 함수 파라미터 style이 가리므로 명시)
                    canvas.drawRoundRect(rect, radius, radius, Paint().apply {
                        isAntiAlias = true
                        this.style = Paint.Style.STROKE
                        strokeWidth = 2.2f * u
                        color = android.graphics.Color.argb(165, 255, 255, 255)
                    })
                    textPaint.color = if (isNearWhite(colorRaw)) Color(0xFF1A2438).toArgb() else colorRaw
                }
                else -> { // WHITE
                    val pillPaint = Paint().apply {
                        isAntiAlias = true
                        color = android.graphics.Color.argb(230, 255, 255, 255)
                        setShadowLayer(10f * u, 0f, 3f * u, android.graphics.Color.argb(70, 0, 20, 40))
                    }
                    canvas.drawRoundRect(rect, radius, radius, pillPaint)
                    textPaint.color = if (isNearWhite(colorRaw)) Color(0xFF1A2438).toArgb() else colorRaw
                }
            }

            val baseline = top + padV + textSize - textPaint.descent() * 0.35f
            canvas.drawText(text, left + padH, baseline, textPaint)
            return pillH
        }

        /** 스타일별 요소 높이 예측 (중심 앵커 배치용). */
        fun elementHeight(textSize: Float, style: String): Float =
            if (style == "NONE") textSize * 1.15f else textSize * 1.60f

        /** 스타일별 요소 폭 예측 (중심 앵커 배치용) — 알약은 좌우 패딩 포함. */
        fun elementWidth(text: String, textSize: Float, style: String): Float {
            textPaint.textSize = textSize
            val w = textPaint.measureText(text)
            return if (style == "NONE") w else w + textSize * 0.55f * 2f
        }

        /** 요소 하나를 중심 앵커(cx, cy — 0~1 비율) 기준으로 그린다. */
        fun drawElementCentered(text: String, textSize: Float, style: String, colorRaw: Int, cx: Float, cy: Float) {
            val w = elementWidth(text, textSize, style)
            val h = elementHeight(textSize, style)
            drawElement(
                text = text, textSize = textSize,
                top = cy * CARD_HEIGHT - h / 2f,
                style = style, colorRaw = colorRaw,
                leftAligned = true,
                anchor = cx * CARD_WIDTH - w / 2f,
            )
        }

        // ── 텍스트 3요소 — 각각 표시/위치/크기/알약/색을 독립 커스텀 ──
        if (layers.showNickname) {
            drawElementCentered(
                text = layers.nickname,
                textSize = 60f * u * scaleMulOf(layers.nicknameScaleStep),
                style = layers.nicknamePill,
                colorRaw = parseColorOrDefault(layers.nicknameColor, android.graphics.Color.WHITE),
                cx = layers.nicknameX, cy = layers.nicknameY,
            )
        }
        if (layers.showTagline) {
            drawElementCentered(
                text = layers.tagline,
                textSize = 32f * u * scaleMulOf(layers.taglineScaleStep),
                style = layers.taglinePill,
                colorRaw = parseColorOrDefault(layers.taglineColor, android.graphics.Color.WHITE),
                cx = layers.taglineX, cy = layers.taglineY,
            )
        }
        if (layers.showStats) {
            drawElementCentered(
                text = layers.stats,
                textSize = 30f * u * scaleMulOf(layers.statsScaleStep),
                style = layers.statsPill,
                colorRaw = parseColorOrDefault(layers.statsColor, Color(0xFF00F5FF).toArgb()),
                cx = layers.statsX, cy = layers.statsY,
            )
        }

        // 브랜드 워터마크 — 사용자 글꼴 스타일과 무관하게 항상 기본 글꼴로 고정.
        // 위 외곽선 처리가 남긴 STROKE가 새지 않도록 FILL로 되돌린다.
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 20f * u
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color(0xFF00A8B8).toArgb()
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.clearShadowLayer()
        canvas.drawText("SOODAL.CARD", CARD_WIDTH - 40f * u, 50f * u, textPaint)

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

    /** 흰 알약/칩 위에서 안 보일 만큼 밝은 색인지 — 기존 저장값(흰색/네온) 호환 폴백용. */
    private fun isNearWhite(color: Int): Boolean {
        val lum = 0.299 * android.graphics.Color.red(color) +
            0.587 * android.graphics.Color.green(color) +
            0.114 * android.graphics.Color.blue(color)
        return lum >= 190
    }

    /** 검정 알약 위에서 안 보일 만큼 어두운 색인지. */
    private fun isNearBlack(color: Int): Boolean {
        val lum = 0.299 * android.graphics.Color.red(color) +
            0.587 * android.graphics.Color.green(color) +
            0.114 * android.graphics.Color.blue(color)
        return lum <= 80
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
    var bmp by remember(imageAsset) { mutableStateOf<Bitmap?>(null) }
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
            bmp = (result.drawable as? BitmapDrawable)?.bitmap
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
        initialValue = ProfileCardRenderer.peek(finalLayers),
        finalLayers,
    ) {
        value = withContext(Dispatchers.Default) { ProfileCardRenderer.renderCached(finalLayers) }
    }

    val cardModifier = modifier
        .fillMaxWidth()
        .aspectRatio(ProfileCardRenderer.CARD_WIDTH.toFloat() / ProfileCardRenderer.CARD_HEIGHT.toFloat())
    val current = bitmap
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
