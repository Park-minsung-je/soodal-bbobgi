package com.soodalbbobgi.app.presentation.splash

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soodalbbobgi.app.R
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.data.asset.AssetSyncProgress
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
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    val serverError by viewModel.serverError.collectAsStateWithLifecycle()
    val assetProgress by viewModel.assetSyncProgress.collectAsStateWithLifecycle()

    // 실제 에셋 동기화 진행률을 progress bar에 바인딩. Error 상태는 마지막 값을 유지한다
    // (실패 시 시각적으로 0으로 되돌아가지 않도록 직전 값을 기억).
    var lastNonErrorFraction by remember { mutableFloatStateOf(0f) }
    val targetFraction = assetProgressValue(assetProgress, lastNonErrorFraction)
    LaunchedEffect(targetFraction, assetProgress) {
        if (assetProgress !is AssetSyncProgress.Error) lastNonErrorFraction = targetFraction
    }
    val animatedProgress by animateFloatAsState(
        targetValue = targetFraction, animationSpec = tween(200), label = "splash",
    )

    // 시스템 스플래시(킥판 수달 아이콘)에서 자연스럽게 이어지도록 콘텐츠를 페이드인하고
    // 수달은 살짝 커지며 등장한다 — 같은 배경색이라 끊김 없이 넘어온다.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enter by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "splashEnter",
    )

    // destination이 결정되고 + 에셋 동기화가 끝나야(또는 Error여도 graceful degradation) 전환한다.
    LaunchedEffect(destination, assetProgress) {
        if (destination == SplashDestination.Loading) return@LaunchedEffect
        if (assetProgress is AssetSyncProgress.FetchingManifest ||
            assetProgress is AssetSyncProgress.Downloading
        ) return@LaunchedEffect
        // 사용자가 로딩 상태를 인지할 수 있는 최소 시간 보장
        delay(300)
        onNavigate(destination)
    }

    Box(Modifier.fillMaxSize().background(colors.bgDeep).statusBarsPadding(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer { alpha = enter },
        ) {
            Image(
                painter = painterResource(R.drawable.otter_swim),
                contentDescription = "수달 뽑기",
                modifier = Modifier.size(120.dp).graphicsLayer {
                    val s = 0.9f + 0.1f * enter
                    scaleX = s
                    scaleY = s
                },
            )
            Spacer(Modifier.height(28.dp))
            Text("수달 뽑기", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentBlue)
            Text("수영하고, 뽑고, 수달을 모아요", fontSize = 13.sp, color = colors.textSecondary)
            Spacer(Modifier.height(40.dp))
            Box(Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(colors.surface2)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).clip(RoundedCornerShape(999.dp)).background(colors.gradBlue))
            }
            // 라벨이 비는 순간에도 같은 공간을 차지해 가운데 정렬이 출렁이지 않게
            // 고정 높이 슬롯으로 항상 렌더링한다.
            Spacer(Modifier.height(10.dp))
            Box(Modifier.height(16.dp), contentAlignment = Alignment.Center) {
                val label = assetProgressLabel(assetProgress)
                if (label != null) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = if (assetProgress is AssetSyncProgress.Error) colors.warn else colors.textTertiary,
                    )
                }
            }
            if (syncError != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = syncError!!,
                    fontSize = 12.sp,
                    color = colors.warn,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            // 서버 장애 안내 — 로그인 세션은 살아 있으니 재로그인 대신 재시도를 유도한다.
            if (serverError) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "서버에 일시적인 문제가 있어요",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.warn,
                )
                Text(
                    text = "잠시 후 다시 시도해주세요",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(14.dp))
                SoodalButton(
                    text = "다시 시도",
                    onClick = viewModel::retry,
                    style = ButtonStyle.Secondary,
                )
            }
        }
    }
}

/**
 * [AssetSyncProgress]를 0..1 진행률로 매핑한다.
 *
 * - Idle / FetchingManifest: 0.0 (시작 직전)
 * - Downloading: completed / max(total, 1)
 * - Done: 1.0
 * - Error: 직전 값(lastNonError) 유지 — 시각적으로 0으로 스냅백되지 않도록.
 *
 * @param lastNonError 직전 비-Error 진행률. Error 상태일 때 반환된다.
 */
private fun assetProgressValue(state: AssetSyncProgress, lastNonError: Float): Float = when (state) {
    AssetSyncProgress.Idle, AssetSyncProgress.FetchingManifest -> 0f
    is AssetSyncProgress.Downloading -> {
        val total = if (state.total <= 0) 1 else state.total
        state.completed.toFloat() / total.toFloat()
    }
    is AssetSyncProgress.Done -> 1f
    is AssetSyncProgress.Error -> lastNonError
}

/**
 * [AssetSyncProgress]를 사용자에게 보여줄 한국어 라벨로 매핑한다.
 *
 * Idle/Done(누락 없음)은 별도 라벨이 없고(공간 절약), 진행 중에만 안내한다.
 * Done에 누락 파일이 있으면 정보성 경고 문구를 표시한다 — 전체 진행은 차단하지 않는다.
 * Error는 부드럽게 안내하되 화면 진입은 막지 않는다 — 네트워크 폴백으로 동작 가능.
 */
private fun assetProgressLabel(state: AssetSyncProgress): String? = when (state) {
    AssetSyncProgress.Idle -> null
    AssetSyncProgress.FetchingManifest -> "에셋 매니페스트 확인 중…"
    is AssetSyncProgress.Downloading -> "에셋 동기화 중… ${state.completed}/${state.total}"
    is AssetSyncProgress.Done -> if (state.skipped > 0) "준비 완료 (이미지 ${state.skipped}개 누락)" else null
    is AssetSyncProgress.Error -> "에셋 업데이트에 실패했어요"
}
