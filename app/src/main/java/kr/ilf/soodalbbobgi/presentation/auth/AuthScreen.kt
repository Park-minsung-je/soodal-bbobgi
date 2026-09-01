package kr.ilf.soodalbbobgi.presentation.auth

import androidx.compose.foundation.LocalIndication
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import kr.ilf.soodalbbobgi.R
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.core.ui.soodalScreenBackdrop

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
        Box(Modifier.fillMaxSize().soodalScreenBackdrop())
        return
    }

    // 자체 배경 필수 — 투명이면 슬라이드 전환 중 이전 화면과 겹쳐 보인다 (설정 화면과 동일 패턴).
    Column(
        modifier = Modifier.fillMaxSize().soodalScreenBackdrop().statusBarsPadding(),
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
            // 에러 메시지 표시
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = colors.warn,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressable(onClick = { viewModel.clearError() }),
                )
            }

            Spacer(Modifier.height(6.dp))

            // 카카오 로그인 — 이미지 에셋을 늘리는 대신 규격(컨테이너 #FEE500, 검은 심볼,
            // 라벨 85% 블랙)대로 직접 그린다. 이미지 스트레치로 비율이 깨지던 문제도 없어진다.
            AuthButton(
                text = if (loadingProvider == "kakao") "카카오 연결 중…" else "카카오 로그인",
                iconContent = { KakaoSymbol(20.dp) },
                bgColor = Color(0xFFFEE500),
                textColor = Color(0xD9000000),
                enabled = !isLoading,
                onClick = { viewModel.loginWithKakao(activity) },
            )

            // Google 로그인 — 흰 컨테이너 + 4색 G 로고를 직접 그린다 (보더는 디자인 판단으로 생략).
            AuthButton(
                text = if (loadingProvider == "google") "Google 연결 중…" else "Google로 시작하기",
                iconContent = { GoogleGLogo(20.dp) },
                bgColor = Color.White,
                textColor = Color(0xFF1F1F1F),
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

/** 카카오 심볼(말풍선) — 규격 형태를 코드로 그린 근사. 심볼색은 검정. */
@Composable
private fun KakaoSymbol(size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            addOval(androidx.compose.ui.geometry.Rect(0f, 0f, w, h * 0.82f))
            moveTo(w * 0.30f, h * 0.68f)
            lineTo(w * 0.18f, h)
            lineTo(w * 0.50f, h * 0.80f)
            close()
        }
        drawPath(path, Color.Black)
    }
}

/** Google 4색 G 로고 — 규격 색상 4호를 아크로 그린다 (블루 바 포함). */
@Composable
private fun GoogleGLogo(size: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.Canvas(Modifier.size(size)) {
        val stroke = this.size.width * 0.22f
        val inset = stroke / 2
        val arcSize = androidx.compose.ui.geometry.Size(this.size.width - stroke, this.size.height - stroke)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        val style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
        // 0° = 3시 방향, 시계 방향. 315~325° 사이가 G의 트임.
        drawArc(Color(0xFFEA4335), 225f, 90f, false, topLeft, arcSize, style = style)  // 레드: 위
        drawArc(Color(0xFFFBBC05), 135f, 90f, false, topLeft, arcSize, style = style)  // 옐로: 왼쪽
        drawArc(Color(0xFF34A853), 45f, 90f, false, topLeft, arcSize, style = style)   // 그린: 아래
        drawArc(Color(0xFF4285F4), -35f, 80f, false, topLeft, arcSize, style = style)  // 블루: 오른쪽
        // 블루 가로 바 — 중심에서 오른쪽 바깥 지름까지
        drawRect(
            Color(0xFF4285F4),
            topLeft = androidx.compose.ui.geometry.Offset(this.size.width / 2f, this.size.height / 2f - stroke / 2f),
            size = androidx.compose.ui.geometry.Size(this.size.width / 2f, stroke),
        )
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
                indication = LocalIndication.current, onClick = onClick)
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
