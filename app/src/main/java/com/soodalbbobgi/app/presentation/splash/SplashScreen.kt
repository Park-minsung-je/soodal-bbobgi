package com.soodalbbobgi.app.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soodalbbobgi.app.R
import com.soodalbbobgi.app.core.theme.SoodalDesign
import kotlinx.coroutines.delay

/**
 * 스플래시 화면. 로딩 애니메이션을 보여주면서 자동 로그인을 체크한다.
 * 토큰이 유효하면 Auth 화면을 건너뛰고 바로 Home으로 이동한다.
 *
 * @param onNavigate 목적지가 결정되면 호출되는 콜백
 */
@Composable
fun SplashScreen(
    onNavigate: (SplashDestination) -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(100), label = "splash",
    )

    // 로딩 애니메이션 + 최소 1초 대기 후 네비게이션
    LaunchedEffect(destination) {
        // 로딩 애니메이션 진행
        repeat(25) {
            delay(40)
            progress = (it + 1) / 25f
        }
        // destination이 아직 Loading이면 결정될 때까지 대기
        if (destination == SplashDestination.Loading) return@LaunchedEffect
        delay(200)
        onNavigate(destination)
    }

    Box(Modifier.fillMaxSize().background(colors.bgDeep), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(R.drawable.otter_swim),
                contentDescription = "수달 뽑기",
                modifier = Modifier.size(120.dp),
            )
            Spacer(Modifier.height(28.dp))
            Text("수달 뽑기", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentCyan)
            Text("수영하고, 뽑고, 수달을 모아요", fontSize = 13.sp, color = colors.textSecondary)
            Spacer(Modifier.height(40.dp))
            Box(Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(colors.surface2)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).clip(RoundedCornerShape(999.dp)).background(colors.gradCyan))
            }
        }
    }
}
