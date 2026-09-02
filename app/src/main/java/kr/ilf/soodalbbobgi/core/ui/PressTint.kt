package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.invalidateDraw
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

/** 누름 스크림 농도 — 밝은 표면은 어둡게, 어두운 표면은 밝게 덮는다. */
private const val PRESS_TINT_LIGHT = 0.07f
private const val PRESS_TINT_DARK = 0.10f

/**
 * 앱 공용 누름 피드백 — 손가락이 **닿는 순간** 표면 위에 얇은 스크림을 덮는다.
 *
 * 안드로이드 기본 리플(물결 퍼짐)은 이 앱의 유리 표면과 맞지 않아 쓰지 않는다.
 * 대신 눌린 동안만 색을 한 겹 얹어 "눌리는 대상"임을 즉시 알린다 — 손을 떼면 사라진다.
 *
 * 스크롤 제스처로 넘어가면 Compose가 press를 취소하므로 스크롤 중 스침에는 반응하지 않는다.
 *
 * @param tint 덮을 색 (라이트=검정, 다크=흰색)
 * @param alpha 스크림 농도
 */
private data class PressTintIndication(
    private val tint: Color,
    private val alpha: Float,
) : IndicationNodeFactory {

    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressTintNode(interactionSource, tint, alpha)
}

/** 스크림 해제 페이드 시간(ms) — 닿을 땐 즉시, 뗄 땐 이 시간에 걸쳐 사라진다. */
private const val PRESS_FADE_OUT_MS = 180

/** 눌림 여부를 추적해 콘텐츠 위에 스크림을 그리는 노드 — 해제 시 서서히 사라진다. */
private class PressTintNode(
    private val interactionSource: InteractionSource,
    private val tint: Color,
    private val alpha: Float,
) : Modifier.Node(), DrawModifierNode {

    // 0~1 진행값 — 누름은 반응성이 생명이라 즉시 1, 해제는 부드럽게 0으로.
    private val progress = androidx.compose.animation.core.Animatable(0f)
    private var fadeJob: kotlinx.coroutines.Job? = null

    override fun onAttach() {
        coroutineScope.launch {
            // 누른 손가락 수를 세어, 여러 포인터가 얽혀도 마지막 하나가 떨어질 때 풀리게 한다.
            var count = 0
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> count++
                    is PressInteraction.Release, is PressInteraction.Cancel -> count--
                }
                val pressedNow = count > 0
                fadeJob?.cancel()
                fadeJob = coroutineScope.launch {
                    if (pressedNow) {
                        progress.snapTo(1f)
                        invalidateDraw()
                    } else {
                        progress.animateTo(0f, androidx.compose.animation.core.tween(PRESS_FADE_OUT_MS)) {
                            invalidateDraw()
                        }
                    }
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val a = progress.value
        if (a > 0f) drawRect(color = tint.copy(alpha = alpha * a))
    }
}

/**
 * 누름 피드백이 **손가락이 닿는 즉시** 뜨는 클릭 처리.
 *
 * `Modifier.clickable`은 스크롤 가능한 부모 안에 있으면 press 표시를 150ms 미룬다
 * (스크롤하다 스쳐도 번쩍이지 않게 하려는 기본 동작). 그 지연이 "반응이 늦다"로 느껴져
 * 여기서는 탭 제스처를 직접 잡아 down 순간에 press를 올린다.
 * 제스처가 스크롤로 넘어가면 `tryAwaitRelease`가 false를 돌려줘 곧바로 취소된다.
 *
 * 모서리를 둥글게 하려면 **이 모디파이어보다 앞에서 `clip`** 해야 한다 — 그래야 스크림이
 * 잘린다 (유리 버튼은 `.glass()`가 이미 클립한다).
 *
 * @param enabled false면 누름 반응도 클릭도 하지 않는다
 * @param onClick 탭 동작
 */
@Composable
fun Modifier.pressable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val indication = LocalIndication.current
    return this
        .indication(interactionSource, indication)
        // clickable을 쓰지 않으므로 스크린리더용 역할·동작은 직접 알린다.
        .semantics {
            role = Role.Button
            onClick { onClick(); true }
        }
        .pointerInput(enabled, onClick) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = { offset ->
                    val press = PressInteraction.Press(offset)
                    interactionSource.emit(press)
                    if (tryAwaitRelease()) {
                        interactionSource.emit(PressInteraction.Release(press))
                    } else {
                        interactionSource.emit(PressInteraction.Cancel(press))
                    }
                },
                onTap = { onClick() },
            )
        }
}

/**
 * 현재 테마에 맞는 누름 피드백을 만든다. 앱 루트에서 `LocalIndication`에 깔아 쓴다.
 *
 * @return 테마별 스크림 색·농도가 적용된 [Indication]
 */
@Composable
fun rememberPressTint(): Indication {
    val colors = SoodalDesign.colors
    val isDark = colors.isDark
    return remember(isDark) {
        if (isDark) {
            PressTintIndication(Color.White, PRESS_TINT_DARK)
        } else {
            PressTintIndication(Color.Black, PRESS_TINT_LIGHT)
        }
    }
}
