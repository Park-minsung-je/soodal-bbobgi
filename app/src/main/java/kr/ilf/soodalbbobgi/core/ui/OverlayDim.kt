package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/** 팝업 스크림과 동일한 탭바 dim 색. */
private val TabBarDimColor = Color.Black.copy(alpha = SoodalDimAlpha)

/**
 * 탭바 dim 요청 모음.
 *
 * 팝업이 자기 스크림 알파(0~1)를 매 프레임 전달하고, 탭바 dim은 자체 애니메이션 없이
 * 그 값을 그대로 그린다 — 화면 스크림과 탭바 dim이 항상 같은 박자로 움직인다.
 * 팝업이 겹치면 가장 진한 값을 쓰고, 마지막 요청이 사라질 때까지 입력을 차단한다.
 */
@Stable
class TabBarDimState {
    private val requests = mutableStateMapOf<Any, Float>()

    /** 현재 탭바 dim 알파 — 살아있는 요청 중 최댓값 (없으면 0). */
    val alpha: Float get() = requests.values.maxOrNull() ?: 0f

    /** 탭바 입력 차단 여부 — 요청이 하나라도 살아있으면 차단. */
    val blocking: Boolean get() = requests.isNotEmpty()

    /** [key] 팝업의 현재 스크림 알파를 갱신한다. */
    fun set(key: Any, alpha: Float) {
        requests[key] = alpha.coerceIn(0f, 1f)
    }

    /** [key] 팝업의 요청을 해제한다 — 컴포지션에서 빠질 때 호출. */
    fun remove(key: Any) {
        requests.remove(key)
    }
}

/** [AppNavHost]가 제공하는 탭바 dim 상태. 미제공 컨텍스트(프리뷰 등)에선 null로 무시된다. */
val LocalTabBarDim = staticCompositionLocalOf<TabBarDimState?> { null }

/**
 * 이 컴포저블이 컴포지션에 있는 동안 하단 플로팅 탭바를 dim 처리한다.
 *
 * 탭바가 화면 콘텐츠보다 위 레이어에 그려져 화면 안 풀스크린 팝업의 스크림이
 * 탭바를 덮지 못하므로, 팝업 컴포저블이 이 효과를 호출해 탭바 쪽 dim을 함께 켠다.
 *
 * @param alpha 팝업 스크림의 현재 알파 진행도(0~1) — 페이드 인/아웃하는 팝업은
 *   애니메이션 값을 넘겨 탭바 dim이 정확히 같은 박자로 움직이게 한다. 즉시 뜨는 팝업은 기본값 1.
 */
@Composable
fun DimTabBarWhileVisible(alpha: Float = 1f) {
    val dim = LocalTabBarDim.current ?: return
    val key = remember { Any() }
    // 컴포지션 밖에서 상태를 갱신 — 스크림 알파가 바뀔 때마다 따라간다
    SideEffect { dim.set(key, alpha) }
    DisposableEffect(dim) {
        onDispose { dim.remove(key) }
    }
}

/**
 * 탭바와 같은 모양(라운드 모서리·마진)으로 탭바 위를 덮는 dim 레이어.
 *
 * [state]의 알파를 자체 애니메이션 없이 그대로 그려 화면 스크림과 동기화한다.
 * 요청이 살아있는 동안 탭바로 가는 포인터 입력을 전부 소비해 비활성화한다.
 *
 * @param state 탭바 dim 상태 ([LocalTabBarDim]에 제공한 것과 동일 인스턴스)
 * @param modifier 탭바를 감싼 Box에서 Modifier.matchParentSize()로 전달해 탭바와 정확히 겹치게 한다
 */
@Composable
fun TabBarDimLayer(state: TabBarDimState, modifier: Modifier = Modifier) {
    val alpha = state.alpha
    if (alpha <= 0.005f && !state.blocking) return
    Box(
        modifier = modifier
            .padding(start = TabBarMargin, end = TabBarMargin, bottom = tabBarBottomPadding())
            .graphicsLayer { this.alpha = alpha }
            .clip(TabBarShape)
            .background(TabBarDimColor)
            .then(
                if (state.blocking) {
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
