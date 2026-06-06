package com.soodalbbobgi.app.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soodalbbobgi.app.R
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons

/**
 * 로그인 화면.
 * 카카오/Google OAuth 버튼을 제공하며, 인증 성공 시 신규/기존 사용자를 구분하여 콜백을 호출한다.
 *
 * @param onAuthedNewUser 신규 사용자 인증 완료 시 호출 (온보딩으로 이동)
 * @param onAuthedExistingUser 기존 사용자 인증 완료 시 호출 (홈으로 이동)
 */
@Composable
fun AuthScreen(
    onNavigate: (AuthRoute) -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val activity = LocalContext.current as android.app.Activity
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is AuthUiState.Success) {
            onNavigate(state.route)
        }
    }

    val isLoading = uiState is AuthUiState.Loading
    val loadingProvider = (uiState as? AuthUiState.Loading)?.provider
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    // 자동 로그인 중이면 빈 화면 (깜빡임 방지)
    if (loadingProvider == "auto") {
        Box(Modifier.fillMaxSize().background(colors.bgDeep))
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.bgDeep),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(80.dp))
        Image(
            painter = painterResource(R.drawable.otter_swim),
            contentDescription = "수달 캐릭터",
            modifier = Modifier.size(160.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("수달 뽑기", fontSize = 30.sp, fontWeight = FontWeight.Black, color = colors.accentBlue)
        Text("수영하고, 뽑고, 모아요", fontSize = 14.sp, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))

        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("시작하려면 로그인이 필요해요", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("기록과 컬렉션은 서버에 안전하게 보관됩니다.", fontSize = 12.sp,
                color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

            // 에러 메시지 표시
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = colors.warn,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.clearError() },
                        ),
                )
            }

            Spacer(Modifier.height(6.dp))

            // 카카오 공식 로그인 버튼 이미지
            Image(
                painter = painterResource(R.drawable.kakao_login_medium_wide),
                contentDescription = "카카오로 시작하기",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        enabled = !isLoading,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { viewModel.loginWithKakao(activity) },
                    ),
            )

            AuthButton(
                text = if (loadingProvider == "google") "Google 연결 중…" else "Google로 시작하기",
                iconContent = { Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4)) },
                bgColor = Color.White,
                textColor = Color(0xFF1F1F1F),
                borderColor = Color(0xFFDADCE0),
                enabled = !isLoading,
                onClick = { viewModel.loginWithGoogle(activity) },
            )

            Spacer(Modifier.height(6.dp))
            Text("계속하면 이용약관 및 개인정보처리방침에 동의한 것으로 간주됩니다.",
                fontSize = 11.sp, color = colors.textSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun AuthButton(
    text: String, iconContent: @Composable () -> Unit, bgColor: Color, textColor: Color,
    borderColor: Color? = null, enabled: Boolean, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth().height(52.dp)
            .clip(shape)
            .background(if (enabled) bgColor else bgColor.copy(alpha = 0.5f))
            .then(if (borderColor != null) Modifier.border(1.dp, borderColor, shape) else Modifier)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        iconContent()
        Spacer(Modifier.padding(end = 8.dp))
        Text(text, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            color = if (enabled) textColor else textColor.copy(alpha = 0.45f))
    }
}
