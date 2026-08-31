package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 스크롤 경계 페이드의 표준 높이 — 고정 헤더 아래로 콘텐츠가 사라지는 구간. */
val FadeEdgeHeight: Dp = 20.dp

/**
 * 페이드 알파 곡선 — smoothstep(3t²−2t³).
 *
 * 직선 알파는 위쪽 끝까지 같은 속도로 떨어지다 0에 부딪혀, 다 사라지기 직전에 확 꺼지는
 * 느낌이 난다. 이 곡선은 양 끝의 기울기가 0이라 나타날 때도 사라질 때도 스르륵 넘어간다.
 */
private val FadeCurve: Array<Pair<Float, Color>> = arrayOf(
    0.000f to Color.Transparent,
    0.125f to Color.Black.copy(alpha = 0.043f),
    0.250f to Color.Black.copy(alpha = 0.156f),
    0.375f to Color.Black.copy(alpha = 0.316f),
    0.500f to Color.Black.copy(alpha = 0.500f),
    0.625f to Color.Black.copy(alpha = 0.684f),
    0.750f to Color.Black.copy(alpha = 0.844f),
    0.875f to Color.Black.copy(alpha = 0.957f),
    1.000f to Color.Black,
)

/**
 * 스크롤 영역 위쪽 경계를 **알파 마스크로 흐려** 콘텐츠가 잘리지 않고 사라지게 한다.
 *
 * 배경색으로 덮는 그라데이션과 달리 마스크(`DstIn`)라서 배경이 이미지든 그라데이션이든
 * 그대로 비친다 — 앱 배경이 물빛 이미지라 색으로 덮는 방식은 이음매가 생긴다.
 *
 * 스크롤 컨테이너 **바깥쪽**(`verticalScroll`보다 앞)에 붙여야 뷰포트 경계에 걸린다.
 *
 * @param height 페이드 구간 높이 (기본 [FadeEdgeHeight])
 */
fun Modifier.topFadeEdge(height: Dp = FadeEdgeHeight): Modifier = this
    // 마스크를 적용하려면 콘텐츠를 별도 레이어에 그려야 한다.
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = FadeCurve,
                startY = 0f,
                endY = height.toPx(),
            ),
            topLeft = Offset.Zero,
            blendMode = BlendMode.DstIn,
        )
    }
