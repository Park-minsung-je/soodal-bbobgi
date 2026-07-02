package com.soodalbbobgi.app.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

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
    hazeState: HazeState? = null,
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
            // 반투명 글래스 서피스 (디자인 v3.0 — 물빛 배경이 살짝 비치는 프로스트 바).
            // hazeState가 있으면 진짜 backdrop blur(뒤 콘텐츠 샘플링·블러) — API 31+에서 동작,
            // 이하는 Haze가 자동으로 틴트 폴백. 없으면 기존 반투명 배경.
            .then(
                if (hazeState != null) {
                    Modifier.hazeEffect(state = hazeState) {
                        backgroundColor = if (colors.isDark) Color(0xFF0E1426) else Color.White
                        // 틴트를 옅게(0.38) — 블러가 프로스트를 만들어주므로 흰끼를 줄이고 투명감을 살린다.
                        tints = listOf(HazeTint(colors.tabbarBg.copy(alpha = 0.38f)))
                        blurRadius = 14.dp
                        noiseFactor = 0f
                    }
                } else {
                    Modifier.background(colors.tabbarBg)
                },
            )
            // 1px 흰 하이라이트 보더 (카드와 동일한 .glass 처리)
            .border(1.dp, colors.glassBorder, shape)
            // 내부 패딩 8 — 액티브 pill이 바 가장자리에서 사방 균등하게 8dp 띄워진다.
            .padding(8.dp),
    ) {
        val tabWidth = maxWidth / tabs.size

        // (상단 sheen은 패딩 안쪽 박스 위에 경계선처럼 보여 제거 — 글래스감은 보더+반투명+그림자로 충분)

        // 활성 탭 뒤를 따라다니는 라벤더 솔리드 필 (디자인 v3.0 — 보라 채움 + 흰 아이콘/라벨).
        // 탭 칸 전체 크기, 바 라운드(22)와 동심이 되도록 14dp 라운드(22 − 패딩 8).
        // 그림자: 디자인 `0 6px 14px -4px rgba(110,85,201,.5)` — pill이 살짝 떠 있는 느낌.
        Box(
            modifier = Modifier
                .offset(x = tabWidth * pillIndex)
                .width(tabWidth)
                .fillMaxHeight()
                .drawBehind {
                    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                        color = android.graphics.Color.TRANSPARENT
                        setShadowLayer(
                            10.dp.toPx(), 0f, 6.dp.toPx(),
                            android.graphics.Color.argb(128, 110, 85, 201),
                        )
                    }
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawRoundRect(
                            0f, 0f, size.width, size.height,
                            14.dp.toPx(), 14.dp.toPx(), paint,
                        )
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(colors.accentPurple, colors.accentPurple.copy(alpha = 0.90f)),
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
                        tint = if (isActive) androidx.compose.ui.graphics.Color.White else colors.textTertiary,
                        size = 23.dp,
                    )
                    Text(
                        text = tab.label,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        color = if (isActive) androidx.compose.ui.graphics.Color.White else colors.textTertiary,
                    )
                }
            }
        }
    }
}
