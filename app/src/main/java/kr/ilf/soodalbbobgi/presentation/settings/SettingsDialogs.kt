package kr.ilf.soodalbbobgi.presentation.settings

import androidx.compose.foundation.LocalIndication
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.ButtonStyle
import kr.ilf.soodalbbobgi.core.ui.GlassSheen
import kr.ilf.soodalbbobgi.core.ui.LocalHazeContent
import kr.ilf.soodalbbobgi.core.ui.SoodalButton
import kr.ilf.soodalbbobgi.core.ui.SoodalTextField
import kr.ilf.soodalbbobgi.core.ui.glassFrost

/**
 * 닉네임 변경 다이얼로그 — 입력 + 검증/서버 에러 인라인 표시 + 3개월 규칙 안내.
 *
 * @param initial 현재 닉네임 (입력 초기값)
 * @param changeableAt 다음 변경 가능 시각(epoch ms). null이면 바로 가능
 * @param state 저장 진행 상태 (Saving 중엔 버튼 비활성)
 * @param onSave 저장 버튼 콜백 (입력값 전달)
 * @param onDismiss 닫기 (바깥 탭/취소)
 */
@Composable
fun NicknameEditDialog(
    initial: String,
    changeableAt: Long?,
    state: NicknameSaveState,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SoodalDesign.colors
    var input by remember { mutableStateOf(initial) }
    val saving = state is NicknameSaveState.Saving
    val errorMessage = (state as? NicknameSaveState.Error)?.message
    // 연 시점 기준으로 잠근다 — 열어 둔 채 시각이 넘어가는 경우는 서버 판정이 막는다.
    val locked = remember(changeableAt) { isNicknameCooldownActive(changeableAt, System.currentTimeMillis()) }
    val hint = if (locked && changeableAt != null) nicknameCooldownMessage(changeableAt) else NICKNAME_COOLDOWN_HINT

    DialogScrim(onDismiss = { if (!saving) onDismiss() }) {
        Text("닉네임 변경", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text(hint, fontSize = 12.sp, lineHeight = 16.sp, color = if (locked) colors.warn else colors.textTertiary)
        Spacer(Modifier.height(12.dp))
        SoodalTextField(
            value = input,
            onValueChange = { input = it },
            placeholder = "닉네임 입력 (최대 10자)",
            maxLength = 10,
            enabled = !locked && !saving,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
            if (errorMessage != null) {
                Text(errorMessage, fontSize = 12.sp, color = colors.warn, modifier = Modifier.weight(1f))
            } else {
                Spacer(Modifier.weight(1f))
            }
            Text("${input.length}/10", fontSize = 11.sp, color = colors.textTertiary)
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoodalButton(
                text = "취소",
                onClick = onDismiss,
                style = ButtonStyle.Ghost,
                enabled = !saving,
                heightOverride = 48.dp,
                modifier = Modifier.weight(1f),
            )
            SoodalButton(
                text = if (saving) "저장 중…" else "저장",
                onClick = { onSave(input) },
                // 같은 값 저장은 서버가 no-op으로 통과시키지만 UX상 막는다.
                enabled = !saving && !locked && input.isNotBlank() && input != initial,
                heightOverride = 48.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 위험 동작 확인 다이얼로그 (로그아웃/계정 탈퇴 공용).
 *
 * @param title 제목
 * @param message 안내 문구
 * @param confirmText 확인 버튼 라벨
 * @param working 진행 중 여부 (버튼 비활성)
 * @param errorMessage 실패 시 표시할 에러 (null이면 미표시)
 * @param onConfirm 확인 콜백
 * @param onDismiss 닫기 콜백
 */
@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String,
    working: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SoodalDesign.colors

    DialogScrim(onDismiss = { if (!working) onDismiss() }) {
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        Spacer(Modifier.height(10.dp))
        Text(message, fontSize = 13.sp, color = colors.textSecondary, lineHeight = 20.sp)
        if (errorMessage != null) {
            Spacer(Modifier.height(8.dp))
            Text(errorMessage, fontSize = 12.sp, color = colors.warn)
        }
        Spacer(Modifier.height(18.dp))
        // 안전한 쪽(취소)이 강조색, 파괴 동작은 흰 바탕 — 습관적으로 강조 버튼을 누르는 손이
        // 계정을 지우지 않게 한다. 흰 버튼은 상점 구매 팝업의 '취소'와 같은 룩(불투명 흰색 + 잉크 테두리).
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoodalButton(
                text = "취소",
                onClick = onDismiss,
                style = ButtonStyle.Primary,
                enabled = !working,
                heightOverride = 48.dp,
                modifier = Modifier.weight(1f),
            )
            SoodalButton(
                text = if (working) "처리 중…" else confirmText,
                onClick = onConfirm,
                style = ButtonStyle.Secondary,
                // ShopScreen PurchaseConfirmOverlay의 '취소'와 동일 — Secondary+override 경로가 테두리를 그린다.
                backgroundOverride = SolidColor(Color.White),
                enabled = !working,
                heightOverride = 48.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 리마인더 시간 선택 다이얼로그 — 24시간제 타임피커.
 *
 * @param initialHour 현재 설정된 시 (0~23)
 * @param initialMinute 현재 설정된 분 (0~59)
 * @param onSave 저장 콜백 (시, 분)
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onSave: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val timeState = androidx.compose.material3.rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true,
    )

    DialogScrim(onDismiss = onDismiss) {
        Text("알림 시간", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.TimePicker(state = timeState)
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SoodalButton(
                text = "취소",
                onClick = onDismiss,
                style = ButtonStyle.Ghost,
                heightOverride = 48.dp,
                modifier = Modifier.weight(1f),
            )
            SoodalButton(
                text = "저장",
                onClick = { onSave(timeState.hour, timeState.minute) },
                heightOverride = 48.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 풀스크린 스크림 + 가운데 카드 — 설정 다이얼로그 공통 셸. 카드 탭은 닫힘으로 전파되지 않는다. */
@Composable
private fun DialogScrim(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = SoodalDesign.colors
    // 백키 = 다이얼로그 닫기 우선.
    androidx.activity.compose.BackHandler { onDismiss() }
    // 뽑기 팝업과 동일한 등장 (스크림 페이드 + 패널 스케일 0.9→1).
    val p = kr.ilf.soodalbbobgi.core.ui.motion.rememberPopupEnter()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = kr.ilf.soodalbbobgi.core.ui.SoodalDimAlpha * p.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                // 딤은 컨트롤이 아니다 — 누름 틴트를 얹으면 닫히는 중에도 깜빡인다.
                indication = null,
                onClick = onDismiss,
            )
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 글래스 패널 — 콘텐츠 프로스트(블러) + 흰 하이라이트 보더 + 상단 sheen (프로스트 팝업).
        val panelShape = RoundedCornerShape(20.dp)
        Box(
            modifier = Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    // 첫 프레임은 스케일 1로 배치 — 축소 상태 배치는 블러 위치가 어긋난 채 굳는다.
                    val s = kr.ilf.soodalbbobgi.core.ui.motion.popupEnterScale(p)
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                    // alpha<1일 때 오프스크린 합성이 경계 밖 그림자를 잘라 스프링 정착 중
                    // 그림자가 깜빡인다 — 클립 없는 알파 변조로 그린다.
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
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
                content()
            }
        }
    }
}
