package com.soodalbbobgi.app.core.ui

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 앱 공용 글래스 바텀시트 — ModalBottomSheet 대신 오버레이 레이어(AppOverlay)에 직접 그린다.
 *
 * ModalBottomSheet의 예측 뒤로가기는 시트가 '축소'되는 M3 내장 애니메이션이라 바꿀 수 없어,
 * 여기서는 제스처 진행도에 맞춰 시트가 아래로 따라 내려가고, 확정 시 그대로 슬라이드-다운으로
 * 닫힌다. 핸들을 아래로 드래그해서 닫을 수도 있다.
 *
 * @param onDismiss 취소 계열 닫힘(스크림 탭/뒤로가기/드래그) 확정 콜백 — 컴포지션 제거는 호출자 담당
 * @param content 시트 내용. close(after)로 슬라이드-다운 애니메이션 후 after를 실행할 수 있다
 *   (등록/저장처럼 닫힘 애니메이션이 끝난 뒤 상태를 반영해야 할 때 사용)
 */
@Composable
fun SoodalBottomSheet(
    onDismiss: () -> Unit,
    content: @Composable (close: (after: () -> Unit) -> Unit) -> Unit,
) {
    val colors = SoodalDesign.colors
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

    var sheetHeight by remember { mutableIntStateOf(0) }
    // 0=완전 표시, sheetHeight=완전 숨김. 등장/퇴장/드래그/예측 뒤로가기가 모두 이 값을 움직인다.
    val offsetY = remember { Animatable(0f) }
    var entered by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }

    // 높이가 측정되면 화면 밖(아래)에서 스프링으로 올라온다.
    LaunchedEffect(sheetHeight) {
        if (sheetHeight > 0 && !entered) {
            offsetY.snapTo(sheetHeight.toFloat())
            entered = true
            offsetY.animateTo(0f, spring(dampingRatio = 0.85f, stiffness = 380f))
        }
    }

    val close: (after: () -> Unit) -> Unit = { after ->
        if (!closing) {
            closing = true
            keyboard?.hide()
            scope.launch {
                if (sheetHeight > 0) offsetY.animateTo(sheetHeight.toFloat(), tween(200))
                after()
            }
        }
    }

    // 예측 뒤로가기 — 진행도에 따라 시트가 아래로 따라 내려가고, 확정 시 끝까지 내려간 뒤 닫힌다.
    PredictiveBackHandler(enabled = !closing) { progress ->
        try {
            progress.collect { event ->
                if (sheetHeight > 0) offsetY.snapTo(event.progress * sheetHeight * 0.5f)
            }
            closing = true
            keyboard?.hide()
            if (sheetHeight > 0) offsetY.animateTo(sheetHeight.toFloat(), tween(180))
            onDismiss()
        } catch (e: CancellationException) {
            // 제스처 취소 — 제자리로 복귀
            scope.launch { offsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f)) }
        }
    }

    // 오버레이 레이어는 내비게이션 바 인셋만큼 패딩돼 있어 시트가 화면 바닥까지 못 닿는다 —
    // 자식을 인셋만큼 더 크게 측정해 물리 화면 바닥까지 그린다 (딤도 함께 덮인다).
    val navBottom = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val extended = constraints.maxHeight + navBottom
                val placeable = measurable.measure(
                    constraints.copy(minHeight = extended, maxHeight = extended),
                )
                layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(0, 0) }
            },
    ) {
        val shown = if (sheetHeight > 0) (1f - offsetY.value / sheetHeight).coerceIn(0f, 1f) else 0f

        // 딤 + 스크림 탭 닫기
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f * shown))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { close(onDismiss) },
        )

        // 시트 본체 — 키보드가 열리면 통째로 위로 올라간다.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .imePadding()
                .onSizeChanged { sheetHeight = it.height }
                .graphicsLayer {
                    translationY = offsetY.value.coerceAtLeast(0f)
                    alpha = if (entered) 1f else 0f
                }
                .glassFrost(colors, sheetShape, LocalHazeContent.current)
                .border(1.dp, colors.glassBorder, sheetShape),
        ) {
            GlassSheen(sheetShape)
            Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
                // 핸들 — 이 영역을 아래로 드래그하면 시트가 따라 내려가고, 충분히 내리면 닫힌다.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(sheetHeight) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    if (sheetHeight > 0 && offsetY.value > sheetHeight * 0.28f) {
                                        close(onDismiss)
                                    } else {
                                        scope.launch {
                                            offsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                                        }
                                    }
                                },
                                onDragCancel = {
                                    scope.launch {
                                        offsetY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 500f))
                                    }
                                },
                            ) { change, dy ->
                                change.consume()
                                val target = (offsetY.value + dy).coerceAtLeast(0f)
                                scope.launch { offsetY.snapTo(target) }
                            }
                        }
                        .padding(top = 10.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(width = 40.dp, height = 4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFF12263F).copy(alpha = 0.28f)),
                    )
                }
                content(close)
            }
        }
    }
}
