package kr.ilf.soodalbbobgi.presentation.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.core.ui.glassFrost
import kr.ilf.soodalbbobgi.core.ui.motion.popupEnterScale
import kr.ilf.soodalbbobgi.core.ui.motion.rememberPopupEnter

/**
 * 기존 회원 설정 안내 팝업(R30) — 재설치·재로그인으로 HC 연결이나 수영 기록 알림이
 * 꺼져 있을 때 홈에서 설정 > 연동·알림으로 안내한다.
 *
 * 설정 다이얼로그(`DialogScrim`)와 같은 스크림·글래스 패널 룩. 스크림 탭·뒤로가기는
 * "나중에"와 같다. "다시 보지 않음" 체크는 어느 버튼으로 닫아도 함께 전달한다.
 *
 * @param nudge 표시할 항목 (둘 다 false인 값은 호출자가 걸러야 한다)
 * @param onLater 닫기 — 인자는 "다시 보지 않음" 체크 여부
 * @param onGoToSettings 닫고 설정 화면으로 — 인자는 "다시 보지 않음" 체크 여부
 */
@Composable
fun SetupNudgeDialog(
    nudge: SetupNudge,
    onLater: (dontShowAgain: Boolean) -> Unit,
    onGoToSettings: (dontShowAgain: Boolean) -> Unit,
) {
    val colors = SoodalDesign.colors
    var dontShowAgain by remember { mutableStateOf(false) }
    val dismiss = { onLater(dontShowAgain) }

    // 백키 = 팝업 닫기 우선 (체크 상태는 그대로 저장).
    BackHandler { dismiss() }
    // 뽑기·설정 팝업과 동일한 등장 (스크림 페이드 + 패널 스케일).
    val p = rememberPopupEnter()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SoodalDimAlpha * p.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                // 딤은 컨트롤이 아니다 — 누름 틴트를 얹으면 닫히는 중에도 깜빡인다.
                indication = null,
                onClick = dismiss,
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        val panelShape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    // 첫 프레임은 스케일 1로 배치 — 축소 상태 배치는 블러 위치가 어긋난 채 굳는다.
                    val s = popupEnterScale(p)
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                    // alpha<1일 때 오프스크린 합성이 경계 밖 그림자를 잘라 정착 중 깜빡인다.
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
                .glassFrost(colors, panelShape, LocalHazeContent.current)
                .border(1.dp, colors.glassBorder, panelShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}, // 카드 내부 탭이 스크림 닫기로 전파되는 것 차단
                ),
        ) {
            GlassSheen(panelShape)
            Column(Modifier.padding(22.dp)) {
                Text("설정을 확인해 주세요", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Spacer(Modifier.height(10.dp))
                Text(
                    setupNudgeMessage(nudge),
                    fontSize = 13.sp, color = colors.textSecondary, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(14.dp))
                DontShowAgainRow(checked = dontShowAgain, onToggle = { dontShowAgain = !dontShowAgain })
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoodalButton(
                        text = "나중에",
                        onClick = dismiss,
                        // 확인 팝업의 흰 버튼과 같은 룩 — Secondary+override 경로가 테두리를 그린다.
                        style = ButtonStyle.Secondary,
                        backgroundOverride = SolidColor(Color.White),
                        heightOverride = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                    SoodalButton(
                        text = "설정으로",
                        onClick = { onGoToSettings(dontShowAgain) },
                        style = ButtonStyle.Primary,
                        heightOverride = 48.dp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 안내 본문 — 해당 항목만 줄로 나열하고 마지막 줄에 설정 위치를 붙인다.
 *
 * @param nudge 표시할 항목
 * @return 줄바꿈으로 이은 본문
 */
internal fun setupNudgeMessage(nudge: SetupNudge): String = buildList {
    if (nudge.hcMissing) add("Health Connect가 연결되지 않아 수영 기록을 가져오지 못해요.")
    if (nudge.newRecordOff) add("수영 기록 알림이 꺼져 있어요.")
    add("설정 > 연동·알림에서 켤 수 있어요.")
}.joinToString("\n")

/** "다시 보지 않음" 행 — 앱 룩에 맞춘 작은 사각 체크 + 라벨. 행 전체가 탭 영역이다. */
@Composable
private fun DontShowAgainRow(checked: Boolean, onToggle: () -> Unit) {
    val colors = SoodalDesign.colors
    val boxShape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val boxModifier = if (checked) {
            Modifier.background(colors.gradBlueVivid, boxShape)
        } else {
            Modifier.background(Color.White, boxShape).border(1.5.dp, colors.inputBorder, boxShape)
        }
        Box(Modifier.size(22.dp).then(boxModifier), contentAlignment = Alignment.Center) {
            if (checked) SoodalIcon(SoodalIcons.Check, tint = Color.White, size = 14.dp)
        }
        Spacer(Modifier.width(10.dp))
        Text("다시 보지 않음", fontSize = 13.sp, color = colors.textSecondary)
    }
}
