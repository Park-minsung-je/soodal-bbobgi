package kr.ilf.soodalbbobgi.presentation.update

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.ui.ButtonStyle
import kr.ilf.soodalbbobgi.core.ui.GlassSheen
import kr.ilf.soodalbbobgi.core.ui.LocalHazeContent
import kr.ilf.soodalbbobgi.core.ui.SoodalButton
import kr.ilf.soodalbbobgi.core.ui.SoodalDimAlpha
import kr.ilf.soodalbbobgi.core.ui.glassFrost
import kr.ilf.soodalbbobgi.core.ui.motion.popupEnterScale
import kr.ilf.soodalbbobgi.core.ui.motion.rememberPopupEnter

/** 안내 본문 — 유연 업데이트는 이미 내려받았고 재시작만 남았다는 점을 짚는다. */
const val UPDATE_READY_MESSAGE =
    "새 버전을 내려받았어요. 지금 다시 시작하면 바로 적용돼요.\n" +
        "나중에 해도 다음에 앱을 열 때 적용할 수 있어요."

/**
 * 유연 인앱 업데이트를 다 내려받은 뒤 띄우는 "다시 시작" 안내 팝업.
 * 홈의 연결 안내 팝업과 같은 스크림·글래스 패널 룩. 스크림 탭·뒤로가기는 "나중에"와 같다.
 *
 * @param onLater 안내를 접는다 — 다음 실행 때 다시 뜬다
 * @param onRestart 설치를 마치고 앱을 다시 시작한다
 */
@Composable
fun UpdateReadyDialog(onLater: () -> Unit, onRestart: () -> Unit) {
    val colors = SoodalDesign.colors
    BackHandler { onLater() }
    val p = rememberPopupEnter()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SoodalDimAlpha * p.coerceIn(0f, 1f)))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLater)
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        val panelShape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val s = popupEnterScale(p)
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .glassFrost(colors, panelShape, LocalHazeContent.current)
                .border(1.dp, colors.glassBorder, panelShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        ) {
            GlassSheen(panelShape)
            Column(Modifier.padding(22.dp)) {
                Text("업데이트가 준비됐어요", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Text(UPDATE_READY_MESSAGE, fontSize = 13.sp, color = colors.textSecondary, lineHeight = 20.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoodalButton(
                        text = "나중에",
                        onClick = onLater,
                        style = ButtonStyle.Secondary,
                        backgroundOverride = SolidColor(Color.White),
                        heightOverride = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                    SoodalButton(
                        text = "다시 시작",
                        onClick = onRestart,
                        style = ButtonStyle.Primary,
                        heightOverride = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
