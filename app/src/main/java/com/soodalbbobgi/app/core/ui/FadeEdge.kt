package com.soodalbbobgi.app.core.ui

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
                colors = listOf(Color.Transparent, Color.Black),
                startY = 0f,
                endY = height.toPx(),
            ),
            topLeft = Offset.Zero,
            blendMode = BlendMode.DstIn,
        )
    }
