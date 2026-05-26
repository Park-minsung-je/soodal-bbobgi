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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalTextField

@Composable
fun OnboardingNicknameScreen(onNext: (String) -> Unit) {
    val colors = SoodalDesign.colors
    var nickname by remember { mutableStateOf("") }
    val validPattern = Regex("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]*$")
    val valid = nickname.isNotBlank() && validPattern.matches(nickname)
    val hasSpecialChar = nickname.isNotEmpty() && !validPattern.matches(nickname)

    Column(Modifier.fillMaxSize().background(colors.bgDeep).padding(24.dp)) {
        Text("STEP 1 / 2", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = colors.accentCyan, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(16.dp))
        Text("반가워요!\n닉네임을 알려주세요 🦦", style = SoodalDesign.typography.xl, color = colors.textPrimary)
        Text("닉네임은 프로필 카드와 뽑기 기록에 표시됩니다.", fontSize = 14.sp,
            color = colors.textSecondary, modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(48.dp))
        Text("✏️", fontSize = 64.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(36.dp))
        SoodalTextField(nickname, { nickname = it }, placeholder = "닉네임 입력 (최대 10자)",
            maxLength = 10, modifier = Modifier.fillMaxWidth())
        if (hasSpecialChar) {
            Text("특수문자는 사용할 수 없어요.", fontSize = 12.sp, color = colors.warn,
                modifier = Modifier.padding(top = 10.dp))
        }
        Text("${nickname.length}/10", fontSize = 11.sp, color = colors.textTertiary,
            modifier = Modifier.align(Alignment.End).padding(top = 6.dp))
        Spacer(Modifier.weight(1f))
        SoodalButton("다음 →", onClick = { onNext(nickname) },
            enabled = valid, modifier = Modifier.fillMaxWidth())
    }
}
