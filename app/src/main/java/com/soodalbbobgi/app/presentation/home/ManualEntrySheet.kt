package com.soodalbbobgi.app.presentation.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.ui.SoodalBottomSheet
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.util.parseTimeDigits
import java.time.LocalTime

// 시트 고정 색 — 프로스트 위 입력창은 디자인 `.input`처럼 흰 배경 + 또렷한 잉크 테두리.
private val FieldTxt1 = Color(0xFF1A2438)
private val FieldTxt3 = Color(0xFFA7B0BF)
private val FieldBg = Color.White
private val FieldBorder = Color(0xFF1E3C64).copy(alpha = 0.12f)
private val FieldError = Color(0xFFE0524D)

/**
 * 수동 입력 시트의 등록 데이터 — 시트가 수집한 전체 입력값.
 *
 * @param startTime 시작 시각 (선택) — 없으면 등록 시각/정오로 대체된다
 * @param freeM~kickM 영법별 거리(m) — 미입력 잔여분은 혼영으로 등록된다
 */
data class ManualEntryInput(
    val distanceM: Int,
    val durationMin: Int,
    val calories: Int?,
    val maxHr: Int?,
    val avgHr: Int?,
    val minHr: Int?,
    val startTime: LocalTime? = null,
    val freeM: Int = 0,
    val breastM: Int = 0,
    val backM: Int = 0,
    val flyM: Int = 0,
    val kickM: Int = 0,
)

/**
 * 수영 기록 수동 입력 시트 — 1단계(거리/시간 필수 + 시각/칼로리/심박 선택),
 * 2단계(영법 구성, 선택)로 나뉘며 어느 단계에서든 바로 등록할 수 있다.
 * 인증은 v1에서 즉시 통과(사진 인증은 추후) — 버튼을 누르면 바로 등록된다.
 *
 * @param onSubmit 등록 콜백 — 닫힘 애니메이션이 끝난 뒤 호출된다
 * @param dateLabel 입력 대상 날짜 안내 (예: "5월 12일") — null이면 "오늘"
 */
@Composable
fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onSubmit: (ManualEntryInput) -> Unit,
    dateLabel: String? = null,
) {
    // 필드 상태는 시트 레벨에 둬 단계를 오가도 입력이 유지된다.
    var distanceText by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("") }
    var startText by remember { mutableStateOf("") }
    var kcalText by remember { mutableStateOf("") }
    var maxHrText by remember { mutableStateOf("") }
    var avgHrText by remember { mutableStateOf("") }
    var minHrText by remember { mutableStateOf("") }
    var freeText by remember { mutableStateOf("") }
    var breastText by remember { mutableStateOf("") }
    var backText by remember { mutableStateOf("") }
    var flyText by remember { mutableStateOf("") }
    var kickText by remember { mutableStateOf("") }
    var step by remember { mutableIntStateOf(1) }

    val distance = distanceText.toIntOrNull() ?: 0
    val duration = durationText.toIntOrNull() ?: 0
    val kcal = kcalText.toIntOrNull()
    val maxHr = maxHrText.toIntOrNull()
    val avgHr = avgHrText.toIntOrNull()
    val minHr = minHrText.toIntOrNull()
    val startTime = parseTimeDigits(startText)

    // 심박은 각각 범위 안이어야 하고, 함께 입력되면 최소 ≤ 평균 ≤ 최대 순서를 지켜야 한다.
    val hrValid = (maxHr == null || maxHr in 40..240) &&
        (avgHr == null || avgHr in 30..230) &&
        (minHr == null || minHr in 30..220) &&
        (maxHr == null || minHr == null || minHr <= maxHr) &&
        (maxHr == null || avgHr == null || avgHr <= maxHr) &&
        (minHr == null || avgHr == null || minHr <= avgHr)
    val timeValid = startText.isEmpty() || startTime != null
    val free = freeText.toIntOrNull() ?: 0
    val breast = breastText.toIntOrNull() ?: 0
    val back = backText.toIntOrNull() ?: 0
    val fly = flyText.toIntOrNull() ?: 0
    val kick = kickText.toIntOrNull() ?: 0
    val strokeSum = free + breast + back + fly + kick
    val mixedAuto = distance - strokeSum
    val strokesValid = mixedAuto >= 0
    // 1단계(기본 정보)만의 유효성 — [다음] 버튼 활성 조건.
    val basicsValid = distance in 25..30000 && duration in 1..600 &&
        (kcal == null || kcal in 1..5000) && hrValid && timeValid
    val valid = basicsValid && strokesValid

    SoodalBottomSheet(onDismiss = onDismiss) { close, dragModifier ->
        // 2단계에서 뒤로가기는 시트를 닫지 않고 1단계로 되돌린다.
        BackHandler(enabled = step == 2) { step = 1 }
        val submit = {
            close {
                onSubmit(
                    ManualEntryInput(
                        distanceM = distance, durationMin = duration,
                        calories = kcal, maxHr = maxHr, avgHr = avgHr, minHr = minHr,
                        startTime = startTime,
                        freeM = free, breastM = breast, backM = back, flyM = fly, kickM = kick,
                    ),
                )
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val forward = targetState > initialState
                (slideInHorizontally { w -> if (forward) w else -w } + fadeIn()) togetherWith
                    (slideOutHorizontally { w -> if (forward) -w else w } + fadeOut())
            },
            label = "manualEntryStep",
        ) { s ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                if (s == 1) {
                    StepBasics(
                        dragModifier = dragModifier,
                        dateLabel = dateLabel,
                        distanceText = distanceText, onDistance = { distanceText = it },
                        durationText = durationText, onDuration = { durationText = it },
                        startText = startText, onStart = { startText = it },
                        startValid = timeValid,
                        kcalText = kcalText, onKcal = { kcalText = it },
                        maxHrText = maxHrText, onMaxHr = { maxHrText = it },
                        avgHrText = avgHrText, onAvgHr = { avgHrText = it },
                        minHrText = minHrText, onMinHr = { minHrText = it },
                        hrValid = hrValid,
                        nextEnabled = basicsValid,
                        onNext = { step = 2 },
                    )
                } else {
                    StepStrokes(
                        dragModifier = dragModifier,
                        distance = distance,
                        freeText = freeText, onFree = { freeText = it },
                        breastText = breastText, onBreast = { breastText = it },
                        backText = backText, onBack = { backText = it },
                        flyText = flyText, onFly = { flyText = it },
                        kickText = kickText, onKick = { kickText = it },
                        mixedAuto = mixedAuto,
                        valid = valid,
                        onPrev = { step = 1 },
                        onRegister = submit,
                    )
                }
            }
        }
    }
}

/** 1단계 — 총 거리/운동 시간(필수), 시작 시각/칼로리/최대·평균·최소 심박(선택) 입력 후 [다음]. */
@Composable
private fun StepBasics(
    dragModifier: Modifier,
    dateLabel: String?,
    distanceText: String, onDistance: (String) -> Unit,
    durationText: String, onDuration: (String) -> Unit,
    startText: String, onStart: (String) -> Unit,
    startValid: Boolean,
    kcalText: String, onKcal: (String) -> Unit,
    maxHrText: String, onMaxHr: (String) -> Unit,
    avgHrText: String, onAvgHr: (String) -> Unit,
    minHrText: String, onMinHr: (String) -> Unit,
    hrValid: Boolean,
    nextEnabled: Boolean,
    onNext: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        // 헤더 — 첫 입력 섹션 직전까지 드래그로 닫기 가능.
        Column(Modifier.fillMaxWidth().then(dragModifier)) {
            Spacer(Modifier.height(4.dp))
            Text("기록 추가", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = FieldTxt1)
            Spacer(Modifier.height(2.dp))
            Text("${dateLabel ?: "오늘"} 수영한 기록을 입력해 주세요", fontSize = 11.sp, color = FieldTxt3)
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(Modifier.weight(1f), "총 거리", distanceText, "m", onChange = onDistance)
            NumberField(Modifier.weight(1f), "운동 시간", durationText, "분", onChange = onDuration)
        }

        Spacer(Modifier.height(12.dp))
        Text("선택 입력 — 비우면 자동 계산/생략돼요", fontSize = 11.sp, color = FieldTxt3)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimeField(Modifier.weight(1f), "시작 시각", startText, isError = !startValid, onChange = onStart)
            NumberField(Modifier.weight(1f), "칼로리", kcalText, "kcal", onChange = onKcal)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(Modifier.weight(1f), "최대 심박", maxHrText, "bpm", isError = !hrValid && maxHrText.isNotEmpty(), onChange = onMaxHr)
            NumberField(Modifier.weight(1f), "평균 심박", avgHrText, "bpm", isError = !hrValid && avgHrText.isNotEmpty(), onChange = onAvgHr)
            NumberField(Modifier.weight(1f), "최소 심박", minHrText, "bpm", isError = !hrValid && minHrText.isNotEmpty(), onChange = onMinHr)
        }

        Spacer(Modifier.height(16.dp))
        // 다음 — 영법 구성 단계로. 등록은 2단계에서만 한다 (영법 입력이 정식 플로우).
        PrimaryButton(Modifier.fillMaxWidth(), "다음", nextEnabled, onNext)
        Spacer(Modifier.height(18.dp))
    }
}

/** 2단계 — 영법별 거리(m). 미입력 잔여분은 혼영으로 자동 배정된다. */
@Composable
private fun StepStrokes(
    dragModifier: Modifier,
    distance: Int,
    freeText: String, onFree: (String) -> Unit,
    breastText: String, onBreast: (String) -> Unit,
    backText: String, onBack: (String) -> Unit,
    flyText: String, onFly: (String) -> Unit,
    kickText: String, onKick: (String) -> Unit,
    mixedAuto: Int,
    valid: Boolean,
    onPrev: () -> Unit,
    onRegister: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        // 헤더 — 첫 입력 섹션 직전까지 드래그로 닫기 가능 (뒤로 버튼 탭은 그대로 동작).
        Column(Modifier.fillMaxWidth().then(dragModifier)) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color.White)
                        .border(1.dp, FieldBorder, RoundedCornerShape(9.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPrev,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    SoodalIcon(icon = SoodalIcons.ArrowLeft, tint = FieldTxt1, size = 13.dp)
                }
                Text("영법 구성", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = FieldTxt1)
            }
            Spacer(Modifier.height(2.dp))
            Text("입력하지 않은 거리는 혼영으로 등록돼요", fontSize = 11.sp, color = FieldTxt3)
            Spacer(Modifier.height(16.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(Modifier.weight(1f), "자유형", freeText, "m", onChange = onFree)
            NumberField(Modifier.weight(1f), "평영", breastText, "m", onChange = onBreast)
            NumberField(Modifier.weight(1f), "배영", backText, "m", onChange = onBack)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NumberField(Modifier.weight(1f), "접영", flyText, "m", onChange = onFly)
            NumberField(Modifier.weight(1f), "킥판", kickText, "m", onChange = onKick)
            // 혼영 = 잔여 자동 배정 (표시 전용)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.55f))
                    .border(1.dp, FieldBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text("혼영 · 자동", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FieldTxt3)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = mixedAuto.coerceAtLeast(0).toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = JetBrainsMonoFamily,
                        color = if (mixedAuto < 0) FieldError else FieldTxt3,
                    )
                    Text("m", fontSize = 11.sp, color = FieldTxt3, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                }
            }
        }
        if (mixedAuto < 0) {
            Spacer(Modifier.height(8.dp))
            Text("영법 합계가 전체 거리(${distance}m)를 넘었어요", fontSize = 11.sp, color = FieldError)
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(Modifier.fillMaxWidth(), "인증하고 등록", valid, onRegister)
        Spacer(Modifier.height(18.dp))
    }
}

/**
 * 시트 주 액션 버튼 — 활성이면 파랑 그라데이션, 비활성이면 잉크 틴트 회색.
 * 반투명(alpha)으로 죽이면 유리 시트 위에서 버튼이 '투명해진' 것처럼 보여 solid로 처리한다.
 */
@Composable
private fun PrimaryButton(modifier: Modifier, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF2563EB)))
                else SolidColor(Color(0xFF1E3C64).copy(alpha = 0.12f)),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (enabled) Color.White else FieldTxt3,
            letterSpacing = 0.3.sp,
        )
    }
}

/** 숫자 입력 필드 — 라벨 + 모노 숫자 + 단위. */
@Composable
private fun NumberField(
    modifier: Modifier,
    label: String,
    value: String,
    unit: String,
    isError: Boolean = false,
    placeholder: String = "0",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onChange: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(FieldBg)
            .border(1.dp, if (isError) FieldError.copy(alpha = 0.6f) else FieldBorder, RoundedCornerShape(14.dp))
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
                visualTransformation = visualTransformation,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = JetBrainsMonoFamily, color = FieldTxt3)
                        }
                        inner()
                    }
                },
            )
            if (unit.isNotEmpty()) {
                Text(unit, fontSize = 11.sp, color = FieldTxt3, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
        }
    }
}

/** 시각 입력 필드 — 숫자만 받아 "930" → "9:30"으로 보여준다 (24시간제). */
@Composable
private fun TimeField(
    modifier: Modifier,
    label: String,
    value: String,
    isError: Boolean,
    onChange: (String) -> Unit,
) {
    NumberField(
        modifier = modifier,
        label = label,
        value = value,
        unit = "",
        isError = isError,
        placeholder = "-:--",
        visualTransformation = remember { TimeColonTransformation() },
        onChange = { if (it.length <= 4) onChange(it) },
    )
}

/** "1430" 숫자 입력을 "14:30"으로 표시하는 변환 — 커서 위치도 콜론만큼 보정한다. */
private class TimeColonTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val s = text.text
        if (s.length < 3) return TransformedText(text, OffsetMapping.Identity)
        val cut = s.length - 2
        val out = s.substring(0, cut) + ":" + s.substring(cut)
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = if (offset <= cut) offset else offset + 1
            override fun transformedToOriginal(offset: Int) = if (offset <= cut) offset else offset - 1
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}
