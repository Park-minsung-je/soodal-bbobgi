package com.soodalbbobgi.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.SoodalTextField
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingNicknameScreen(
    onNext: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    var nickname by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf<String?>(null) }
    var ageRange by remember { mutableStateOf<String?>(null) }
    val validPattern = Regex("^[a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ]*$")
    val valid = nickname.isNotBlank() && validPattern.matches(nickname)
    val hasSpecialChar = nickname.isNotEmpty() && !validPattern.matches(nickname)

    // 저장 성공 시 다음 화면으로 이동
    LaunchedEffect(saveState) {
        if (saveState is OnboardingSaveState.Success) onNext()
    }

    Column(Modifier.fillMaxSize().background(colors.bgDeep).statusBarsPadding().padding(24.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text("STEP 1 / 3", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = colors.accentBlue, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(16.dp))
            Text("반가워요!\n닉네임을 알려주세요", style = SoodalDesign.typography.xl, color = colors.textPrimary)
            Text("닉네임은 프로필 카드와 뽑기 기록에 표시됩니다.", fontSize = 14.sp,
                color = colors.textSecondary, modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.height(48.dp))
            SoodalIcon(SoodalIcons.Edit, tint = colors.accentBlue, size = 64.dp,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(36.dp))
            SoodalTextField(nickname, { nickname = it }, placeholder = "닉네임 입력 (최대 10자)",
                maxLength = 10, modifier = Modifier.fillMaxWidth())
            if (hasSpecialChar) {
                Text("특수문자는 사용할 수 없어요.", fontSize = 12.sp, color = colors.warn,
                    modifier = Modifier.padding(top = 10.dp))
            }
            Text("${nickname.length}/10", fontSize = 11.sp, color = colors.textTertiary,
                modifier = Modifier.align(Alignment.End).padding(top = 6.dp))

            // 성별 선택 (선택사항)
            Spacer(Modifier.height(32.dp))
            Text("성별", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Text("선택사항이에요", fontSize = 11.sp, color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male" to "남성", "female" to "여성").forEach { (value, label) ->
                    SelectChip(
                        text = label,
                        selected = gender == value,
                        onClick = { gender = if (gender == value) null else value },
                    )
                }
            }

            // 연령대 선택 (선택사항)
            Spacer(Modifier.height(24.dp))
            Text("연령대", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            Text("선택사항이에요", fontSize = 11.sp, color = colors.textTertiary,
                modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10s" to "10대", "20s" to "20대", "30s" to "30대", "40s" to "40대", "50s+" to "50대+").forEach { (value, label) ->
                    SelectChip(
                        text = label,
                        selected = ageRange == value,
                        onClick = { ageRange = if (ageRange == value) null else value },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val isSaving = saveState is OnboardingSaveState.Saving
        SoodalButton(
            text = if (isSaving) "저장 중…" else "다음 →",
            onClick = { viewModel.saveProfile(nickname, gender, ageRange) },
            enabled = valid && !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(20.dp)
    val bgColor = if (selected) colors.accentBlue.copy(alpha = 0.15f) else colors.surface1
    val borderColor = if (selected) colors.accentBlue else colors.glassBorder
    val textColor = if (selected) colors.accentBlue else colors.textSecondary

    Box(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}
