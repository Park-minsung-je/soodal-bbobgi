package com.soodalbbobgi.app.presentation.profile

import android.widget.Toast
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
 * @param onBack 돌아가기 콜백
 * @param onEdit 편집 화면 이동 콜백
 */
@Composable
fun ProfileFullscreenScreen(
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

    // AppState에서 가져온 실제 카드 데이터로 layers를 구성. bg/char/frame Bitmap은
    // ProfileCardComposite 내부에서 비동기 로딩되므로 여기서는 null로 두고 텍스트/
    // 위치 등 메타만 채운다. 저장/공유용 Bitmap은 동일 layers로 별도 렌더.
    val layers = CardLayers(
        nickname = cardState.nickname,
        tagline = cardState.tagline,
        stats = cardState.statsText,
        charX = cardState.charX,
        charY = cardState.charY,
        charScale = cardState.charScale,
    )
    val bitmap = remember(layers) { ProfileCardRenderer.render(layers) }

    // 회전 후: 카드 가로(1472) → 세로 방향, 카드 세로(704) → 가로 방향
    // 세로 방향 기준으로 스케일을 계산하여 화면을 최대한 채운다
    val screenW = config.screenWidthDp.toFloat()
    val screenH = config.screenHeightDp.toFloat()
    val fitScale = screenH / screenW

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
            ProfileCardComposite(
                layers = layers,
                bgAsset = cardState.bgAsset,
                charAsset = cardState.charAsset,
                frameAsset = cardState.frameAsset,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        rotationZ = 90f
                        scaleX = fitScale
                        scaleY = fitScale
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
