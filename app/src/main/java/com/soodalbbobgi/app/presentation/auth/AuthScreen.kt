package com.soodalbbobgi.app.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.R
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onAuthed: () -> Unit) {
    val colors = SoodalDesign.colors
    var loading by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val handleAuth: (String) -> Unit = { provider ->
        if (loading == null) {
            loading = provider
            scope.launch { delay(1100); loading = null; onAuthed() }
        }
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
        Text("수달 뽑기", fontSize = 30.sp, fontWeight = FontWeight.Black, color = colors.accentCyan)
        Text("수영하고, 뽑고, 모아요", fontSize = 14.sp, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))

        Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("시작하려면 로그인이 필요해요", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("기록과 컬렉션은 서버에 안전하게 보관됩니다.", fontSize = 12.sp,
                color = colors.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))

            AuthButton(
                text = if (loading == "kakao") "카카오 연결 중…" else "카카오로 시작하기",
                iconContent = { SoodalIcon(SoodalIcons.Otter, tint = Color(0xFF191919), size = 20.dp) },
                bgColor = Color(0xFFFEE500),
                textColor = Color(0xFF191919),
                enabled = loading == null || loading == "kakao",
                onClick = { handleAuth("kakao") },
            )

            AuthButton(
                text = if (loading == "google") "Google 연결 중…" else "Google로 시작하기",
                iconContent = { Text("G", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4285F4)) },
                bgColor = Color.White,
                textColor = Color(0xFF1F1F1F),
                borderColor = Color(0xFFDADCE0),
                enabled = loading == null || loading == "google",
                onClick = { handleAuth("google") },
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
            .background(bgColor)
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
