package kr.ilf.soodalbbobgi.presentation.onboarding

import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.theme.SoodalShape
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.SoodalButton
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.core.ui.SoodalTextField
import kr.ilf.soodalbbobgi.core.ui.soodalScreenBackdrop
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
    // rememberSaveable — 다음 화면에 갔다 뒤로 와도 입력이 남는다.
    var nickname by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var gender by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var ageRange by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    // 완성형 글자만 허용 — 영문/숫자/완성형 한글. 자음·모음 단독(ㅇㅈ 등)은 미완성 글자다.
    val validPattern = Regex("^[a-zA-Z0-9가-힣]*$")
    val hasJamo = nickname.any { it in 'ㄱ'..'ㅣ' } // ㄱ~ㅣ 자모 영역
    val hasSpecialChar = nickname.isNotEmpty() && !hasJamo && !validPattern.matches(nickname)
    // 자모 안내는 제출할 때만 띄운다 — 한글은 조합 중("오"의 첫 타 "ㅇ")에도 자모라서
    // 실시간으로 검사하면 정상 입력 중에도 오류가 깜빡인다.
    var jamoError by remember { mutableStateOf(false) }
    // 화면에 띄울 검증 안내 — 없으면 null
    val nicknameError = when {
        jamoError -> "완성되지 않은 글자가 있어요."
        hasSpecialChar -> "특수문자는 사용할 수 없어요."
        else -> null
    }

    // 저장 성공 시 다음 화면으로 이동. 실패 메시지는 아래 필드에 표시한다.
    LaunchedEffect(saveState) {
        if (saveState is OnboardingSaveState.Success) onNext()
    }
    val saveError = (saveState as? OnboardingSaveState.Error)?.message

    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // 자체 배경 필수 — 투명이면 슬라이드 전환 중 이전 화면과 겹쳐 보인다 (설정 화면과 동일 패턴).
    // 빈 곳 탭 = 입력 포커스 해제 + 키보드 닫기 (자식이 소비한 탭에는 반응하지 않는다).
    Column(
        Modifier.fillMaxSize().soodalScreenBackdrop()
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                    keyboard?.hide()
                })
            }
            .statusBarsPadding().padding(24.dp),
    ) {
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
            SoodalTextField(nickname, { nickname = it; jamoError = false }, placeholder = "닉네임 입력 (최대 10자)",
                maxLength = 10, modifier = Modifier.fillMaxWidth())
            // 오류 메시지(왼쪽)와 글자수 카운터(오른쪽)를 한 줄에 둔다 — 오류가 나도 카운터가 밀리지 않게.
            val shownError = nicknameError ?: saveError
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = shownError ?: "",
                    fontSize = 12.sp, color = colors.warn,
                    modifier = Modifier.weight(1f),
                )
                Text("${nickname.length}/10", fontSize = 11.sp, color = colors.textTertiary)
            }

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
                        onClick = {
                            // 입력 중 칩을 고르면 키보드가 가리고 있을 이유가 없다
                            focusManager.clearFocus(); keyboard?.hide()
                            gender = if (gender == value) null else value
                        },
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
                        onClick = {
                            focusManager.clearFocus(); keyboard?.hide()
                            ageRange = if (ageRange == value) null else value
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        val isSaving = saveState is OnboardingSaveState.Saving
        SoodalButton(
            text = if (isSaving) "저장 중…" else "다음 →",
            onClick = {
                // 미완성 글자는 여기서 걸러 안내한다 — 입력 중 깜빡이지 않게 제출 시 1회 검사.
                if (hasJamo) jamoError = true
                else viewModel.saveProfile(nickname, gender, ageRange)
            },
            enabled = nickname.isNotBlank() && !hasSpecialChar && !isSaving,
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
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}
