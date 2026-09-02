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
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ilf.soodalbbobgi.BuildConfig
import kr.ilf.soodalbbobgi.R
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.core.ui.soodalScreenBackdrop
import kr.ilf.soodalbbobgi.core.util.LegalPages
import kr.ilf.soodalbbobgi.core.util.openInBrowser

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

            // 카카오 로그인 — 규격(컨테이너 #FEE500, 검은 심볼, 라벨 85% 블랙)대로 직접 그린다.
            AuthButton(
                text = if (loadingProvider == "kakao") "카카오 연결 중…" else "카카오톡 계정으로 로그인",
                iconContent = { KakaoSymbol(20.dp) },
                bgColor = Color(0xFFFEE500),
                textColor = Color(0xD9000000),
                enabled = !isLoading,
                onClick = { viewModel.loginWithKakao(activity) },
            )

            // Google 로그인 — 흰 컨테이너(보더는 디자인 판단으로 생략) + 공식 G 로고 에셋.
            AuthButton(
                text = if (loadingProvider == "google") "Google 연결 중…" else "Google 계정으로 로그인",
                iconContent = {
                    Image(
                        painter = painterResource(R.drawable.google_g_logo),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                },
                bgColor = Color.White,
                textColor = Color(0xFF1F1F1F),
                enabled = !isLoading,
                onClick = { viewModel.loginWithGoogle(activity) },
            )

            Spacer(Modifier.height(6.dp))
            LegalConsentText()
        }
        Spacer(Modifier.height(32.dp))
    }
}



/**
 * 로그인 하단 동의 안내 — "이용약관"·"개인정보처리방침"만 강조색+밑줄 링크로 그리고,
 * 탭하면 서버의 공개 페이지를 브라우저로 연다. 나머지 글자는 안내 문구 색 그대로.
 */
@Composable
private fun LegalConsentText(modifier: Modifier = Modifier) {
    val colors = SoodalDesign.colors
    val context = LocalContext.current
    val accent = colors.accentBlue
    // 기본 UriHandler 대신 설정 화면과 같은 ACTION_VIEW 경로를 쓰고, 실패 시 앱이 죽지 않게 한다.
    val openLink = remember(context) {
        LinkInteractionListener { link ->
            (link as? LinkAnnotation.Url)?.let { openInBrowser(context, it.url) }
        }
    }
    val text = remember(accent, openLink) {
        val linkStyles = TextLinkStyles(
            style = SpanStyle(color = accent, textDecoration = TextDecoration.Underline),
            pressedStyle = SpanStyle(color = accent.copy(alpha = 0.6f), textDecoration = TextDecoration.Underline),
        )
        buildAnnotatedString {
            append("계속하면 ")
            withLink(LinkAnnotation.Url(LegalPages.termsUrl(BuildConfig.BASE_URL), linkStyles, openLink)) { append("이용약관") }
            append(" 및 ")
            withLink(LinkAnnotation.Url(LegalPages.privacyUrl(BuildConfig.BASE_URL), linkStyles, openLink)) { append("개인정보처리방침") }
            append("에 동의한 것으로 간주됩니다.")
        }
    }
    Text(
        text = text,
        fontSize = 11.sp,
        color = colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
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
