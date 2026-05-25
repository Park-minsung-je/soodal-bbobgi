package com.soodalbbobgi.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalCard

@Composable
fun OnboardingPermissionScreen(onConnect: () -> Unit, onSkip: () -> Unit) {
    val colors = SoodalDesign.colors
    Column(Modifier.fillMaxSize().background(colors.bgDeep).padding(24.dp)) {
        Text("STEP 2 / 2", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = colors.accentCyan, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(16.dp))
        Text("수영 기록을\n자동으로 가져올게요", style = SoodalDesign.typography.xl, color = colors.textPrimary)
        Text("Health Connect와 연동하면 수영 후 자동으로 기록이 등록되고 조개를 받을 수 있어요.",
            fontSize = 14.sp, color = colors.textSecondary, lineHeight = 22.sp,
            modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(36.dp))
        Text("🏊", fontSize = 64.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(30.dp))
        SoodalCard(Modifier.fillMaxWidth()) {
            Column {
                Text("❤️‍🔥 Health Connect", fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text("수영 운동 데이터(거리·시간·칼로리)를 읽어오는 권한이 필요합니다.",
                    fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp)
            }
        }
        Spacer(Modifier.weight(1f))
        SoodalButton("Health Connect 연결하기", onClick = onConnect, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SoodalButton("나중에 하기", onClick = onSkip, style = ButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
    }
}
