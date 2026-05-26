package com.soodalbbobgi.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
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

@Composable
fun SoodalTabBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    tabs: List<TabItem> = MainTabs,
) {
    val colors = SoodalDesign.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(colors.tabbarBg)
            .border(width = 1.dp, color = colors.tabbarBorder),
    ) {
        tabs.forEach { tab ->
            val isActive = tab.key == activeTab
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
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
                    tint = if (isActive) colors.accentCyan else colors.textTertiary,
                    size = 22.dp,
                )
                Text(
                    text = tab.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.4.sp,
                    color = if (isActive) colors.accentCyan else colors.textTertiary,
                )
            }
        }
    }
}
