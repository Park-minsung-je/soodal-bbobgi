package com.soodalbbobgi.app.presentation.profile

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons

/**
 * 프로필 카드를 90도 회전하여 전체화면으로 표시한다.
 * 갤러리 저장, 공유, 편집 화면 이동 기능을 제공한다.
 *
 * 진입/복귀 시 카드가 세로(0°)에서 가로(90°)로 회전하며 확대/축소된다.
 *
 * @param animatedVisibilityScope NavHost composable이 제공하는 전환 스코프(진입↔표시 진행도)
 * @param onBack 돌아가기 콜백
 * @param onEdit 편집 화면 이동 콜백
 */
@Composable
fun ProfileFullscreenScreen(
    animatedVisibilityScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: ProfileFullscreenViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    val saveState by viewModel.saveState.collectAsState()
    val cardState by viewModel.cardState.collectAsState()
    val context = LocalContext.current
    val config = LocalConfiguration.current

    // bg/char/frame Bitmap을 여기서 직접 비동기 로딩해 layers에 주입한다. 저장/공유용
    // Bitmap(render)과 화면 표시(ProfileCardComposite)가 동일한 layers를 공유하므로,
    // 저장/공유 PNG에도 배경/캐릭터/테두리 이미지가 그대로 합성된다. CardLayers는 data
    // class라 Bitmap 로딩 완료 시 layers equality가 바뀌어 remove/Composite 모두 재렌더된다.
    val bgBitmap = rememberAssetBitmap(cardState.bgAsset)
    val charBitmap = rememberAssetBitmap(cardState.charAsset)
    val frameBitmap = rememberAssetBitmap(cardState.frameAsset)

    val layers = CardLayers(
        bgBitmap = bgBitmap,
        charBitmap = charBitmap,
        frameBitmap = frameBitmap,
        nickname = cardState.nickname,
        tagline = cardState.tagline,
        stats = cardState.statsText,
        charX = cardState.charX,
        charY = cardState.charY,
        charScale = cardState.charScale,
        textStyle = cardState.textStyle,
        textAlign = cardState.textAlign,
        textX = cardState.textX,
        textY = cardState.textY,
        textScaleStep = cardState.textScaleStep,
        showStats = cardState.showStats,
        nicknameColor = cardState.nicknameColor,
        taglineColor = cardState.taglineColor,
        statsColor = cardState.statsColor,
    )
    val bitmap = remember(layers) { ProfileCardRenderer.render(layers) }

    // 회전 후: 카드 가로(1472) → 세로 방향, 카드 세로(704) → 가로 방향
    // 세로 방향 기준으로 스케일을 계산하여 화면을 최대한 채운다
    val screenW = config.screenWidthDp.toFloat()
    val screenH = config.screenHeightDp.toFloat()
    val fitScale = screenH / screenW

    // 진입↔표시 진행도(0→1). 카드를 세로(0°·1.0배)에서 가로(90°·fitScale배)로 회전·확대한다.
    val zoom by animatedVisibilityScope.transition.animateFloat(
        transitionSpec = { tween(durationMillis = 320) },
        label = "cardZoom",
    ) { state -> if (state == EnterExitState.Visible) 1f else 0f }
    val cardRotation = 90f * zoom
    val cardScale = 1f + (fitScale - 1f) * zoom

    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                Toast.makeText(context, "갤러리에 저장했어요!", Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            is SaveState.Error -> {
                Toast.makeText(context, (saveState as SaveState.Error).message, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // -- Center Card (landscape rotation) --
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            // layers에 이미 Bitmap을 주입했으므로 asset 경로는 넘기지 않는다(중복 로딩 방지).
            // Composite는 null 경로일 때 layers의 Bitmap을 그대로 사용한다.
            ProfileCardComposite(
                layers = layers,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationZ = cardRotation
                        scaleX = cardScale
                        scaleY = cardScale
                    },
            )
        }

        // -- Bottom Controls --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = spacing.s4, vertical = spacing.s5),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(SoodalShape.md)
                        .background(colors.glassBg, SoodalShape.md)
                        .border(1.dp, colors.glassBorder, SoodalShape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.saveToGallery(bitmap) },
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Save, tint = colors.textPrimary, size = 14.dp)
                    Text("저장", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(SoodalShape.md)
                        .background(colors.glassBg, SoodalShape.md)
                        .border(1.dp, colors.glassBorder, SoodalShape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.share(bitmap) },
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Share, tint = colors.textPrimary, size = 14.dp)
                    Text("공유", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            }
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(SoodalShape.md)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SoodalIcon(icon = SoodalIcons.ArrowLeft, tint = colors.textSecondary, size = 14.dp)
                Text("돌아가기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            }
        }
    }
}
