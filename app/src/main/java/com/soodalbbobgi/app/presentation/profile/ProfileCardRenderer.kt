package com.soodalbbobgi.app.presentation.profile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import dagger.hilt.android.EntryPointAccessors
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
    /** 텍스트 글꼴 스타일 ("REGULAR" | "BOLD" | "ITALIC"). 세 줄 전체에 적용. */
    val textStyle: String = "REGULAR",
    /** 텍스트 블록 내부 줄 정렬 ("LEFT" | "RIGHT"). */
    val textAlign: String = "RIGHT",
    /** 텍스트 블록 가로 위치 (0~1). 정렬에 따라 좌/우 앵커 기준. */
    val textX: Float = 0.95f,
    /** 텍스트 블록 세로 중심 위치 (0~1). */
    val textY: Float = 0.5f,
    /** 텍스트 블록 크기 단계 (1~5). 3 = 기본 배율. */
    val textScaleStep: Int = 3,
    /** 기록(통계) 줄 표시 여부. false면 줄과 여백 모두 생략. */
    val showStats: Boolean = true,
    /** 닉네임 색상 ("#RRGGBB"). */
    val nicknameColor: String = "#FFFFFF",
    /** 소개 줄 색상 ("#RRGGBB"). */
    val taglineColor: String = "#FFFFFF",
    /** 기록 줄 색상 ("#RRGGBB"). */
    val statsColor: String = "#00F5FF",
)

/**
 * 1472×704 도트아트 프로필 카드를 Canvas로 합성한다.
 * 배경 → 테두리 → 캐릭터 → 텍스트 순서로 4레이어를 그린다.
 */
object ProfileCardRenderer {
    const val CARD_WIDTH = 1472
    const val CARD_HEIGHT = 704

    fun render(layers: CardLayers): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false; isFilterBitmap = false }

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

        // Layer 4: Text — 닉네임·소개·기록 3줄을 하나의 블록으로 그린다.
        // 정렬(좌/우) + 블록 크기(단계) + 색상 + 표시여부를 한 묶음으로 처리하고,
        // 세 줄을 폰트 크기에 비례한 좁은 간격으로 수직 중앙 정렬해 응집감 있게 배치한다.
        // 글꼴 스타일을 닉네임·소개·기록 세 줄 전체에 일괄 적용한다.
        // REGULAR면 닉네임도 굵게 처리하지 않는다 (이전엔 닉네임이 하드코딩 BOLD였음).
        val blockTypeface = when (layers.textStyle) {
            "BOLD" -> Typeface.DEFAULT_BOLD
            "ITALIC" -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            else -> Typeface.DEFAULT
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            typeface = blockTypeface
        }

        // 블록 크기 단계(1~5) → 배율. 3이 기준(1.2).
        val scaleMul = floatArrayOf(0.8f, 1.0f, 1.2f, 1.5f, 1.8f)[
            (layers.textScaleStep - 1).coerceIn(0, 4)
        ]
        val nicknameSize = 72f * scaleMul
        val taglineSize = 32f * scaleMul
        val statsSize = 28f * scaleMul
        // 줄 간 여백 — 각 줄 폰트 크기에 비례한 작은 간격으로 블록을 조밀하게 묶는다.
        val gapAfterNickname = nicknameSize * 0.35f
        val gapAfterTagline = taglineSize * 0.55f

        // 블록 전체 높이 = 세 줄 높이 + 줄 간 여백 (showStats=false면 기록 줄/여백 제외).
        val blockHeight = nicknameSize + gapAfterNickname + taglineSize +
            (if (layers.showStats) gapAfterTagline + statsSize else 0f)
        // textY는 블록의 세로 '중심'. 슬라이더로 블록 전체를 위아래로 옮긴다.
        val blockTop = layers.textY * CARD_HEIGHT - blockHeight / 2f

        // 정렬은 블록 내부 줄 정렬(Paint.Align). textX가 앵커 가로 위치를 정한다.
        // LEFT면 textX가 좌측 모서리(왼쪽 정렬), RIGHT면 textX가 우측 모서리(오른쪽 정렬).
        val isRight = layers.textAlign != "LEFT"
        val textX = layers.textX * CARD_WIDTH
        textPaint.textAlign = if (isRight) Paint.Align.RIGHT else Paint.Align.LEFT

        val shadow = android.graphics.Color.argb(128, 0, 0, 0)

        // 닉네임
        var baseline = blockTop + nicknameSize
        textPaint.textSize = nicknameSize
        textPaint.color = parseColorOrDefault(layers.nicknameColor, android.graphics.Color.WHITE)
        textPaint.setShadowLayer(4f, 2f, 2f, shadow)
        canvas.drawText(layers.nickname, textX, baseline, textPaint)

        // 소개
        baseline += gapAfterNickname + taglineSize
        textPaint.textSize = taglineSize
        textPaint.color = parseColorOrDefault(layers.taglineColor, android.graphics.Color.WHITE)
        canvas.drawText(layers.tagline, textX, baseline, textPaint)

        // 기록 (표시 옵션 ON일 때만)
        if (layers.showStats) {
            baseline += gapAfterTagline + statsSize
            textPaint.textSize = statsSize
            textPaint.color = parseColorOrDefault(layers.statsColor, Color(0xFF00F5FF).toArgb())
            canvas.drawText(layers.stats, textX, baseline, textPaint)
        }

        // 브랜드 워터마크 — 사용자 글꼴 스타일과 무관하게 항상 기본 글꼴로 고정.
        textPaint.textSize = 20f
        textPaint.typeface = Typeface.DEFAULT
        textPaint.color = Color(0xFF00A8B8).toArgb()
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.clearShadowLayer()
        canvas.drawText("SOODAL.CARD", CARD_WIDTH - 40f, 50f, textPaint)

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

/** 홈↔전체보기 프로필 카드 공유요소(SharedTransition) 매칭 키. */
const val PROFILE_CARD_SHARED_KEY = "profileCard"

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

    val finalLayers = layers.copy(
        bgBitmap = bgBitmap,
        charBitmap = charBitmap,
        frameBitmap = frameBitmap,
    )
    val bitmap = remember(finalLayers) { ProfileCardRenderer.render(finalLayers) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "프로필 카드",
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1472f / 704f),
    )
}
