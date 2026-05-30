package com.soodalbbobgi.app.core.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 누르는 동안 살짝 줄어드는 스케일 피드백(iOS 손맛).
 *
 * 적용은 보류 상태 — 토큰 역할로 먼저 정의해두고 화면별 적용은 확인 후.
 *
 * @param pressedScale 눌렀을 때 스케일 (기본 0.96)
 * @param interactionSource 클릭과 공유할 InteractionSource (없으면 내부 생성)
 */
fun Modifier.pressableScale(
    pressedScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null,
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = tween(120),
        label = "pressableScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}
