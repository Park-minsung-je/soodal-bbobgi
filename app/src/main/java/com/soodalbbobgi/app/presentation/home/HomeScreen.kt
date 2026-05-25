package com.soodalbbobgi.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.SoodalTabBar

@Composable
fun HomeScreen(
    onNavigateToTab: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfileFullscreen: () -> Unit,
    onNavigateToProfileEditor: () -> Unit,
) {
    val colors = SoodalDesign.colors
    Column(Modifier.fillMaxSize().background(colors.bgDeep)) {
        Box(Modifier.weight(1f).padding(16.dp)) {
            Column {
                Text("안녕하세요,", fontSize = 13.sp, color = colors.textSecondary)
                Text("Soodal 🦦", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Text("\nHome 화면 (Phase 4B에서 구현)", fontSize = 14.sp, color = colors.textTertiary)
            }
        }
        SoodalTabBar(activeTab = "home", onTabSelected = onNavigateToTab)
    }
}
