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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
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
    TabItem("gacha", SoodalIcons.Gacha, "뽑기"),
    TabItem("shop", SoodalIcons.Shop, "상점"),
)

/** 플로팅 탭바가 덮는 하단 영역(바 62 + 마진 14 + 숨통 16) — 탭 화면 콘텐츠 끝에 이만큼 여백을 둔다. */
val TabBarClearance = 92.dp

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
    val shape = RoundedCornerShape(22.dp)
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
            // 좌우 16dp — 화면 콘텐츠의 가로 패딩(spacing.s4)과 폭을 맞춘다
            .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
            .height(62.dp)
            // 카드 위를 지나갈 때도 층이 구분되도록 카드(8dp)보다 또렷한 그림자
            .shadow(18.dp, shape, ambientColor = colors.cardShadow.copy(alpha = 0.45f), spotColor = colors.cardShadow.copy(alpha = 0.35f))
            .clip(shape)
            // 불투명 서피스 — 뒤 콘텐츠가 비치지 않는다
            .background(colors.cardBg)
            .padding(6.dp),
    ) {
        val tabWidth = maxWidth / tabs.size
        // 필은 바 내부 높이에 꽉 차는 정원 — 활성 탭 칸 가운데에 위치
        val pillSize = 50.dp

        // 활성 탭 뒤를 따라다니는 필 하이라이트
        Box(
            modifier = Modifier
                .offset(x = tabWidth * pillIndex + (tabWidth - pillSize) / 2)
                .size(pillSize)
                .clip(CircleShape)
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
