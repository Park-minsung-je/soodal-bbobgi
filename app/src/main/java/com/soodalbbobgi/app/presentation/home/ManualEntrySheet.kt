package com.soodalbbobgi.app.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.GlassSheen
import com.soodalbbobgi.app.core.ui.LocalHazeContent
import com.soodalbbobgi.app.core.ui.glassFrost
import kotlinx.coroutines.launch

// 시트 고정 색 — 프로스트 위 입력창은 디자인 `.input`처럼 흰 배경 + 또렷한 잉크 테두리.
private val FieldTxt1 = Color(0xFF1A2438)
private val FieldTxt3 = Color(0xFFA7B0BF)
private val FieldBg = Color.White
private val FieldBorder = Color(0xFF1E3C64).copy(alpha = 0.12f)

/**
 * 수영 기록 수동 입력 시트 — 거리/시간(필수) + 칼로리/심박(선택) 입력 후 [인증하고 등록].
 * 인증은 v1에서 즉시 통과(사진 인증은 추후) — 버튼을 누르면 바로 등록된다.
 *
 * @param onSubmit 등록 콜백 (거리 m, 시간 분, 칼로리?, 최대심박?, 최소심박?)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onSubmit: (distanceMeters: Int, durationMin: Int, calories: Int?, maxHr: Int?, minHr: Int?) -> Unit,
    /** 입력 대상 날짜 안내 (예: "5월 12일") — null이면 "오늘". */
    dateLabel: String? = null,
) {
    val colors = SoodalDesign.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var distanceText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var kcalText by remember { mutableStateOf("") }
    var maxHrText by remember { mutableStateOf("") }
    var minHrText by remember { mutableStateOf("") }
    val distance = distanceText.toIntOrNull() ?: 0
    val duration = durationText.toIntOrNull() ?: 0
    val kcal = kcalText.toIntOrNull()
    val maxHr = maxHrText.toIntOrNull()
    val minHr = minHrText.toIntOrNull()
    val hrValid = (maxHr == null || maxHr in 40..240) &&
        (minHr == null || minHr in 30..220) &&
        (maxHr == null || minHr == null || minHr <= maxHr)
    val valid = distance in 25..30000 && duration in 1..600 &&
        (kcal == null || kcal in 1..5000) && hrValid

    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        shape = sheetShape,
        dragHandle = null,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .glassFrost(colors, sheetShape, LocalHazeContent.current)
                .border(1.dp, colors.glassBorder, sheetShape),
        ) {
            GlassSheen(sheetShape)
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                // 핸들
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .height(4.dp)
                            .fillMaxWidth(0.1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF27384B).copy(alpha = 0.28f)),
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text("직접 기록", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = FieldTxt1)
                Spacer(Modifier.height(2.dp))
                Text("${dateLabel ?: "오늘"} 수영한 기록을 입력해 주세요", fontSize = 11.sp, color = FieldTxt3)

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(Modifier.weight(1f), "거리", distanceText, "m") { distanceText = it }
                    NumberField(Modifier.weight(1f), "시간", durationText, "분") { durationText = it }
                }

                Spacer(Modifier.height(12.dp))
                Text("선택 입력 — 비우면 자동 계산/생략돼요", fontSize = 11.sp, color = FieldTxt3)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(Modifier.weight(1f), "칼로리", kcalText, "kcal") { kcalText = it }
                    NumberField(Modifier.weight(1f), "최대 심박", maxHrText, "bpm") { maxHrText = it }
                    NumberField(Modifier.weight(1f), "최소 심박", minHrText, "bpm") { minHrText = it }
                }

                Spacer(Modifier.height(16.dp))
                // 인증하고 등록 — 탭 시 즉시 통과 후 등록.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF2563EB))))
                        .alpha(if (valid) 1f else 0.45f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = valid,
                        ) {
                            scope.launch {
                                sheetState.hide()
                                onSubmit(distance, duration, kcal, maxHr, minHr)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("인증하고 등록", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.3.sp)
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

/** 숫자 입력 필드 — 라벨 + 모노 숫자 + 단위. */
@Composable
private fun NumberField(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    onChange: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(FieldBg)
            .border(1.dp, FieldBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FieldTxt3)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            BasicTextField(
                value = value,
                onValueChange = { new -> if (new.length <= 5 && new.all { it.isDigit() }) onChange(new) },
                textStyle = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = JetBrainsMonoFamily,
                    color = FieldTxt1,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF38BDF8)),
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text("0", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = JetBrainsMonoFamily, color = FieldTxt3)
                        }
                        inner()
                    }
                },
            )
            Text(unit, fontSize = 11.sp, color = FieldTxt3, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
        }
    }
}
