package com.soodalbbobgi.app.core.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * 팝업/모달 등장 진행값(0→1)을 반환한다.
 *
 * 최초 컴포지션 직후 0→1로 **살짝 오버슈트하는 바운스 스프링**으로 애니메이션된다.
 * 반환값을 배경 딤 alpha와 콘텐츠의 scale/alpha에 함께 적용하면 "톡 튀어오르는" 등장이 된다.
 * (1을 넘길 수 있으므로 alpha에는 `coerceIn(0f,1f)`, 딤에도 coerce를 걸어 쓴다.)
 *
 * 예) `val p = rememberPopupEnter()` → 배경 `alpha = 0.45f * p.coerceIn(0f,1f)`,
 * 콘텐츠 `graphicsLayer { scaleX = 0.82f + 0.18f*p; scaleY = 동일; alpha = p.coerceIn(0f,1f) }`
 */
@Composable
fun rememberPopupEnter(): Float {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 380f),
        label = "popupEnter",
    )
    return progress
}
