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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons

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
        SoodalIcon(SoodalIcons.Swimmer, tint = colors.accentCyan, size = 64.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(30.dp))

        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
            // Health Connect — 필수
            SoodalCard(Modifier.fillMaxWidth()) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SoodalIcon(icon = SoodalIcons.Heart, tint = colors.warn, size = 26.dp)
                    Column(Modifier.weight(1f)) {
                        androidx.compose.foundation.layout.Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Health Connect", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            SoodalChip("필수", color = ChipColor.Cyan)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("수영 운동 데이터(거리·시간·칼로리)를 읽어오는 권한이 필요합니다.",
                            fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp)
                    }
                }
            }

            // 카메라 — 선택 (비활성)
            SoodalCard(Modifier.fillMaxWidth().then(Modifier.alpha(0.45f))) {
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    SoodalIcon(icon = SoodalIcons.Camera, tint = colors.textTertiary, size = 26.dp)
                    Column(Modifier.weight(1f)) {
                        Text("카메라 (선택)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("수동 입력 인증 시 사용됩니다. 추후 업데이트 예정.",
                            fontSize = 12.sp, color = colors.textSecondary, lineHeight = 18.sp)
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        SoodalButton("Health Connect 연결하기", onClick = onConnect, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SoodalButton("나중에 하기", onClick = onSkip, style = ButtonStyle.Ghost, modifier = Modifier.fillMaxWidth())
    }
}
