package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign

/**
 * 서버 동기화 중 화면 조작을 막는 전체 화면 로딩 오버레이.
 *
 * @param message 표시할 메시지 (예: "동기화 중이에요...")
 */
@Composable
fun SyncLoadingOverlay(message: String = "동기화 중이에요...") {
    DimTabBarWhileVisible()
    val colors = SoodalDesign.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SoodalDimAlpha))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = colors.accentBlue,
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
