package com.soodalbbobgi.app.core.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/** 팝업 스크림과 동일한 탭바 dim 색. */
private val TabBarDimColor = Color.Black.copy(alpha = 0.55f)

/** 탭바 dim 페이드 인/아웃 시간(ms). */
private const val TabBarDimFadeMs = 250

/**
 * 탭바 dim 요청 참조 카운터.
 *
 * 풀스크린 dim 팝업이 겹쳐 떠도(중첩) 마지막 팝업이 닫힐 때까지
 * 탭바 dim이 유지되도록 acquire/release 쌍으로 관리한다.
 */
@Stable
class TabBarDimState {
    private var count by mutableIntStateOf(0)

    /** 살아있는 dim 요청이 하나라도 있으면 true. */
    val dimmed: Boolean get() = count > 0

    /** dim 요청 시작 — 팝업이 컴포지션에 들어올 때 호출한다. */
    fun acquire() {
        count++
    }

    /** dim 요청 해제 — 팝업이 컴포지션에서 빠질 때 호출한다. 0 밑으로 내려가지 않는다. */
    fun release() {
        count = (count - 1).coerceAtLeast(0)
    }
}

/** [AppNavHost]가 제공하는 탭바 dim 상태. 미제공 컨텍스트(프리뷰 등)에선 null로 무시된다. */
val LocalTabBarDim = staticCompositionLocalOf<TabBarDimState?> { null }

/**
 * 이 컴포저블이 컴포지션에 있는 동안 하단 플로팅 탭바를 dim 처리한다.
 *
 * 탭바가 화면 콘텐츠보다 위 레이어에 그려져 화면 안 풀스크린 팝업의 스크림이
 * 탭바를 덮지 못하므로, 팝업 컴포저블이 이 효과를 호출해 탭바 쪽 dim을 함께 켠다.
 */
@Composable
fun DimTabBarWhileVisible() {
    val dim = LocalTabBarDim.current
    DisposableEffect(dim) {
        dim?.acquire()
        onDispose { dim?.release() }
    }
}

/**
 * 탭바와 같은 모양(라운드 모서리·마진)으로 탭바 위를 덮는 dim 레이어.
 *
 * [dimmed]가 true면 페이드 인하며 탭바로 가는 포인터 입력을 전부 소비해 비활성화하고,
 * false면 페이드 아웃 후 사라져 탭바가 다시 동작한다.
 *
 * @param dimmed dim 표시 여부 ([TabBarDimState.dimmed])
 * @param modifier 탭바를 감싼 Box에서 Modifier.matchParentSize()로 전달해 탭바와 정확히 겹치게 한다
 */
@Composable
fun TabBarDimLayer(dimmed: Boolean, modifier: Modifier = Modifier) {
    val alpha by animateFloatAsState(
        targetValue = if (dimmed) 1f else 0f,
        animationSpec = tween(TabBarDimFadeMs),
        label = "tabBarDimAlpha",
    )
    if (alpha <= 0f) return
    Box(
        modifier = modifier
            .padding(start = TabBarMargin, end = TabBarMargin, bottom = tabBarBottomPadding())
            .graphicsLayer { this.alpha = alpha }
            .clip(TabBarShape)
            .background(TabBarDimColor)
            .then(
                if (dimmed) {
                    // 페이드 아웃 중(요청 해제 후)에는 소비하지 않아 탭바가 즉시 다시 눌린다.
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    }
                } else {
                    Modifier
                }
            ),
    )
}
