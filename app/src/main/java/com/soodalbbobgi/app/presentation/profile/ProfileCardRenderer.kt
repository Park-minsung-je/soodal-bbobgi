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
    val charX: Float = 0.2f,
    val charY: Float = 0.3f,
    val charScale: Float = 0.7f,
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

        // Layer 1: Background — bitmap or solid color
        if (layers.bgBitmap != null) {
            canvas.drawBitmap(layers.bgBitmap, null, RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat()), paint)
        } else {
            paint.color = layers.bgColor.toArgb()
            canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), paint)
        }

        // Layer 3: Character (먼저 → 테두리 위에 안 가리도록)
        if (layers.charBitmap != null) {
            val charSize = CARD_HEIGHT * 0.85f * layers.charScale
            val charCx = layers.charX * CARD_WIDTH * 0.6f + CARD_WIDTH * 0.05f
            val charCy = layers.charY * CARD_HEIGHT * 0.6f + CARD_HEIGHT * 0.05f
            val dst = RectF(
                charCx,
                charCy,
                charCx + charSize,
                charCy + charSize,
            )
            canvas.drawBitmap(layers.charBitmap, null, dst, paint)
        } else {
            // 폴백: 동그라미 + 눈
            val charSize = CARD_HEIGHT * 0.5f * layers.charScale
            val charCx = layers.charX * CARD_WIDTH * 0.6f + CARD_WIDTH * 0.1f
            val charCy = layers.charY * CARD_HEIGHT * 0.6f + CARD_HEIGHT * 0.15f
            paint.color = layers.charColor.toArgb()
            canvas.drawCircle(charCx, charCy, charSize, paint)
            paint.color = android.graphics.Color.WHITE
            canvas.drawCircle(charCx - charSize * 0.2f, charCy - charSize * 0.1f, charSize * 0.12f, paint)
            canvas.drawCircle(charCx + charSize * 0.2f, charCy - charSize * 0.1f, charSize * 0.12f, paint)
            paint.color = android.graphics.Color.BLACK
            canvas.drawCircle(charCx - charSize * 0.15f, charCy - charSize * 0.08f, charSize * 0.05f, paint)
            canvas.drawCircle(charCx + charSize * 0.15f, charCy - charSize * 0.08f, charSize * 0.05f, paint)
        }

        // Layer 2 (캐릭터 위에 덮어쓰기): 테두리
        if (layers.frameBitmap != null) {
            canvas.drawBitmap(layers.frameBitmap, null, RectF(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat()), paint)
        } else {
            paint.color = layers.borderColor.toArgb()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 16f
            canvas.drawRect(8f, 8f, CARD_WIDTH - 8f, CARD_HEIGHT - 8f, paint)
            paint.style = Paint.Style.FILL
            val cs = 24f
            canvas.drawRect(4f, 4f, 4f + cs, 4f + cs, paint)
            canvas.drawRect(CARD_WIDTH - 4f - cs, 4f, CARD_WIDTH - 4f, 4f + cs, paint)
            canvas.drawRect(4f, CARD_HEIGHT - 4f - cs, 4f + cs, CARD_HEIGHT - 4f, paint)
            canvas.drawRect(CARD_WIDTH - 4f - cs, CARD_HEIGHT - 4f - cs, CARD_WIDTH - 4f, CARD_HEIGHT - 4f, paint)
        }

        // Layer 4: Text (오른쪽)
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.LEFT
            typeface = Typeface.DEFAULT_BOLD
        }
        val textX = CARD_WIDTH * 0.55f

        textPaint.textSize = 72f
        textPaint.setShadowLayer(4f, 2f, 2f, android.graphics.Color.argb(128, 0, 0, 0))
        canvas.drawText(layers.nickname, textX, CARD_HEIGHT * 0.45f, textPaint)

        textPaint.textSize = 32f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(layers.tagline, textX, CARD_HEIGHT * 0.58f, textPaint)

        textPaint.textSize = 28f
        textPaint.color = Color(0xFF00F5FF).toArgb()
        canvas.drawText(layers.stats, textX, CARD_HEIGHT * 0.72f, textPaint)

        textPaint.textSize = 20f
        textPaint.color = Color(0xFF00A8B8).toArgb()
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.clearShadowLayer()
        canvas.drawText("SOODAL.CARD", CARD_WIDTH - 40f, 50f, textPaint)

        return bitmap
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
 *
 * @param bgAsset 배경 에셋 상대 경로
 * @param charAsset 캐릭터 에셋 상대 경로
 * @param frameAsset 테두리 에셋 상대 경로
 */
@Composable
fun ProfileCardComposite(
    layers: CardLayers,
    bgAsset: String? = null,
    charAsset: String? = null,
    frameAsset: String? = null,
    modifier: Modifier = Modifier,
) {
    val bgBitmap = rememberAssetBitmap(bgAsset)
    val charBitmap = rememberAssetBitmap(charAsset)
    val frameBitmap = rememberAssetBitmap(frameAsset)

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
