package com.soodalbbobgi.app.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign

data class TabItem(
    val key: String,
    val icon: SoodalIcons,
    val label: String,
)

val MainTabs = listOf(
    TabItem("home", SoodalIcons.Home, "홈"),
    TabItem("calendar", SoodalIcons.Calendar, "캘린더"),
    TabItem("gacha", SoodalIcons.Gacha, "인양소"),
    TabItem("shop", SoodalIcons.Shop, "상점"),
)

/** 플로팅 탭바가 덮는 하단 영역(바 62 + 마진 14 + 숨통 16) — 탭 화면 콘텐츠 끝에 이만큼 여백을 둔다. */
val TabBarClearance = 92.dp

/** 탭바 라운드 모양 — 탭바 위를 덮는 레이어(dim 등)가 같은 모양을 쓰도록 공유한다. */
internal val TabBarShape = RoundedCornerShape(22.dp)

/** 탭바 좌우/하단 기본 마진 — 화면 콘텐츠의 가로 패딩(spacing.s4)과 폭을 맞춘다. */
internal val TabBarMargin = 16.dp

/**
 * 탭바 하단 패딩 — 내비게이션 인셋 포함 총 하단 여백이 [TabBarMargin]이 되도록 보정해
 * 바의 라운드가 디스플레이 모서리 곡률과 동심원으로 맞는다.
 *
 * @return 인셋을 뺀 나머지 하단 패딩(최소 0dp)
 */
@Composable
internal fun tabBarBottomPadding(): Dp {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return (TabBarMargin - navBottom).coerceAtLeast(0.dp)
}

/**
 * 하단 탭바 — 화면 가장자리에서 띄운 둥근 글래스 바 (디자인 확정).
 * 활성 탭 뒤에는 부드러운 스프링으로 미끄러지는 필(pill) 하이라이트가 따라다닌다.
 */
@Composable
fun SoodalTabBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<TabItem> = MainTabs,
) {
    val colors = SoodalDesign.colors
    val shape = TabBarShape
    val activeIndex = tabs.indexOfFirst { it.key == activeTab }.coerceAtLeast(0)
    // 디자인의 오버슈트 이징(cubic-bezier 0.34,1.4,0.5,1)에 가까운 스프링
    val pillIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "tabPill",
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = TabBarMargin, end = TabBarMargin, bottom = tabBarBottomPadding())
            .height(62.dp)
            // 커스텀 그림자 — Compose shadow()는 시스템이 알파를 추가로 깎아 흐릿해서,
            // 네이티브 shadowLayer로 진하기를 직접 제어한다 (카드 위에서도 층 구분).
            .drawBehind {
                val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                    color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        13.dp.toPx(), 0f, 3.dp.toPx(),
                        android.graphics.Color.argb(34, 10, 20, 40),
                    )
                }
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRoundRect(
                        0f, 0f, size.width, size.height,
                        22.dp.toPx(), 22.dp.toPx(), paint,
                    )
                }
            }
            .clip(shape)
            // 불투명 서피스 — 뒤 콘텐츠가 비치지 않는다
            .background(colors.cardBg)
            .padding(6.dp),
    ) {
        val tabWidth = maxWidth / tabs.size

        // 활성 탭 뒤를 따라다니는 필 하이라이트 — 탭 칸 전체 크기,
        // 바 라운드(22dp)와 동심이 되도록 16dp 라운드(22 − 패딩 6).
        Box(
            modifier = Modifier
                .offset(x = tabWidth * pillIndex)
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(colors.accentBlue.copy(alpha = 0.16f), colors.accentBlue.copy(alpha = 0.09f)),
                    ),
                ),
        )

        Row(Modifier.fillMaxSize()) {
            tabs.forEach { tab ->
                val isActive = tab.key == activeTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onTabSelected(tab.key) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    SoodalIcon(
                        icon = tab.icon,
                        tint = if (isActive) colors.accentBlue else colors.textTertiary,
                        size = 23.dp,
                    )
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        color = if (isActive) colors.accentBlue else colors.textTertiary,
                    )
                }
            }
        }
    }
}
