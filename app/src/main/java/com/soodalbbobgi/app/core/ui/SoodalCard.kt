package com.soodalbbobgi.app.core.ui

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 공용 카드 컨테이너 — 디자인 표준 글래스([GlassBox])로 구현.
 * 반투명 프로스트 + 흰 하이라이트 보더 + 소프트 블루틴트 그림자 + 상단 inset 하이라이트.
 * 곡률은 [GlassBox] 기본값([GlassCorner]=24, App Canvas 콘텐츠 카드).
 *
 * @param contentPadding 카드 내부 패딩. 이미지가 주인공인 셀처럼 공간 활용이
 *   중요한 곳에서만 기본값(16dp)보다 줄여서 사용한다.
 */
@Composable
fun SoodalCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 16.dp,
    /** 탭 동작. modifier로 clickable을 넘기지 말고 이 값을 쓴다 — 누름 스크림이 모서리에 맞는다. */
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassBox(
        modifier = modifier,
        contentPadding = contentPadding,
        onClick = onClick,
        content = content,
    )
}

/** 글래스 패널 — 카드와 동일 글래스, 밀집 패딩(14). */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassBox(
        modifier = modifier,
        contentPadding = 14.dp,
        content = content,
    )
}
