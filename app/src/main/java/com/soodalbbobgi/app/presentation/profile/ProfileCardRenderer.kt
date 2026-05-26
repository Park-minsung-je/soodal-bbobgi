package com.soodalbbobgi.app.presentation.profile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb

data class CardLayers(
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

object ProfileCardRenderer {
    const val CARD_WIDTH = 1472
    const val CARD_HEIGHT = 704

    fun render(layers: CardLayers): Bitmap {
        val bitmap = Bitmap.createBitmap(CARD_WIDTH, CARD_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { isAntiAlias = false }

        // Layer 1: Background
        paint.color = layers.bgColor.toArgb()
        canvas.drawRect(0f, 0f, CARD_WIDTH.toFloat(), CARD_HEIGHT.toFloat(), paint)

        // Layer 2: Border/Frame (16px thick stroke)
        paint.color = layers.borderColor.toArgb()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 16f
        canvas.drawRect(8f, 8f, CARD_WIDTH - 8f, CARD_HEIGHT - 8f, paint)
        // Corner decorations (small squares at each corner)
        paint.style = Paint.Style.FILL
        val cs = 24f
        canvas.drawRect(4f, 4f, 4f + cs, 4f + cs, paint)
        canvas.drawRect(CARD_WIDTH - 4f - cs, 4f, CARD_WIDTH - 4f, 4f + cs, paint)
        canvas.drawRect(4f, CARD_HEIGHT - 4f - cs, 4f + cs, CARD_HEIGHT - 4f, paint)
        canvas.drawRect(CARD_WIDTH - 4f - cs, CARD_HEIGHT - 4f - cs, CARD_WIDTH - 4f, CARD_HEIGHT - 4f, paint)

        // Layer 3: Character placeholder (circle with eyes)
        val charSize = (CARD_HEIGHT * 0.5f * layers.charScale)
        val charCx = layers.charX * CARD_WIDTH * 0.6f + CARD_WIDTH * 0.1f
        val charCy = layers.charY * CARD_HEIGHT * 0.6f + CARD_HEIGHT * 0.15f
        paint.color = layers.charColor.toArgb()
        canvas.drawCircle(charCx, charCy, charSize, paint)
        // Eyes
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(charCx - charSize * 0.2f, charCy - charSize * 0.1f, charSize * 0.12f, paint)
        canvas.drawCircle(charCx + charSize * 0.2f, charCy - charSize * 0.1f, charSize * 0.12f, paint)
        // Pupils
        paint.color = android.graphics.Color.BLACK
        canvas.drawCircle(charCx - charSize * 0.15f, charCy - charSize * 0.08f, charSize * 0.05f, paint)
        canvas.drawCircle(charCx + charSize * 0.15f, charCy - charSize * 0.08f, charSize * 0.05f, paint)

        // Layer 4: Text (right side)
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textAlign = Paint.Align.LEFT
            typeface = Typeface.DEFAULT_BOLD
        }
        val textX = CARD_WIDTH * 0.55f

        // Nickname
        textPaint.textSize = 72f
        textPaint.setShadowLayer(4f, 2f, 2f, android.graphics.Color.argb(128, 0, 0, 0))
        canvas.drawText(layers.nickname, textX, CARD_HEIGHT * 0.45f, textPaint)

        // Tagline
        textPaint.textSize = 32f
        textPaint.typeface = Typeface.DEFAULT
        canvas.drawText(layers.tagline, textX, CARD_HEIGHT * 0.58f, textPaint)

        // Stats
        textPaint.textSize = 28f
        textPaint.color = Color(0xFF00F5FF).toArgb()
        canvas.drawText(layers.stats, textX, CARD_HEIGHT * 0.72f, textPaint)

        // Badge (top-right)
        textPaint.textSize = 20f
        textPaint.color = Color(0xFF00A8B8).toArgb()
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.clearShadowLayer()
        canvas.drawText("SOODAL.CARD / 002", CARD_WIDTH - 40f, 50f, textPaint)

        return bitmap
    }
}

@Composable
fun ProfileCardComposite(
    layers: CardLayers,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(layers) { ProfileCardRenderer.render(layers) }
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "프로필 카드",
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1472f / 704f),
    )
}
