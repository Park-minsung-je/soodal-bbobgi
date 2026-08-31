package kr.ilf.soodalbbobgi.presentation.calendar

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ilf.soodalbbobgi.core.theme.JetBrainsMonoFamily
import kr.ilf.soodalbbobgi.core.theme.StrokePalette
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.SoodalBottomSheet

// 시트는 항상 화이트(라이트) — 앱 테마와 무관하게 디자인 고정값 사용.
// 프로스트(블러) 표면 위에서 인풋이 죽어 보이지 않도록 디자인 `.input`처럼 흰 배경 + 또렷한 테두리.
private val SheetTxt1 = Color(0xFF1A2438)
private val SheetTxt2 = Color(0xFF5C6B7E)
private val SheetTxt3 = Color(0xFFA7B0BF)
private val SheetFieldBg = Color.White
private val SheetFieldBorder = Color(0xFF1E3C64).copy(alpha = 0.12f)
private val SheetBlue = Color(0xFF2563EB)

// 슬라이더/버튼 조절 단위(m) — 기록에 수영장 길이 정보가 없어 일반적인 25m 고정.
private const val METER_STEP = 25

/** 수정 시트의 6개 영법 — 표시 라벨/색/기본 여부. */
private data class StrokeSpec(val label: String, val color: Color, val isDefault: Boolean = false)

// 디자인 순서: 혼영(기본)이 맨 위, 이후 자유형~킥판. 막대 그래프 순서와도 동일.
private val EDIT_STROKES = listOf(
    StrokeSpec("혼영", StrokePalette.Medley, isDefault = true),
    StrokeSpec("자유형", StrokePalette.Free),
    StrokeSpec("평영", StrokePalette.Breast),
    StrokeSpec("배영", StrokePalette.Back),
    StrokeSpec("접영", StrokePalette.Fly),
    StrokeSpec("킥판", StrokePalette.Kick),
)

/**
 * 영법 비율 수정 바텀시트 (디자인 06b).
 * 거리·시간은 Health Connect 표시 전용 — 수정은 입력된 기록(총 거리)의 영법별 재분배만 허용한다.
 * 자유형~킥판 5개만 직접 조절하고, 남은 거리는 혼영(기본)이 자동으로 가져간다.
 *
 * @param data 수정 대상 세션의 수영 데이터 (원본 영법 미터 포함)
 * @param onSave (free, breast, back, fly, kick, mixed) 순서의 보정값 콜백 — 합계는 항상 기록 거리와 같다
 */
@Composable
fun StrokeEditSheet(
    dateLabel: String,
    data: SwimSessionData,
    onDismiss: () -> Unit,
    onSave: (free: Int, breast: Int, back: Int, fly: Int, kick: Int, mixed: Int) -> Unit,
) {
    val distance = data.distanceM.coerceAtLeast(0)

    // 저장값이 거리를 넘는 비정상 데이터여도 앞에서부터 잔여 안으로 눌러 담는다.
    val initial = remember(data) {
        val stored = listOf(data.freeM, data.breastM, data.backM, data.flyM, data.kickM)
        buildList {
            var acc = 0
            for (m in stored) {
                val v = clampStrokeMeters(m, acc, distance)
                add(v)
                acc += v
            }
        }
    }
    // 직접 조절하는 5개 영법 (m). 혼영은 잔여로 파생된다.
    var free by remember { mutableIntStateOf(initial[0]) }
    var breast by remember { mutableIntStateOf(initial[1]) }
    var back by remember { mutableIntStateOf(initial[2]) }
    var fly by remember { mutableIntStateOf(initial[3]) }
    var kick by remember { mutableIntStateOf(initial[4]) }

    val medley = (distance - (free + breast + back + fly + kick)).coerceAtLeast(0)
    // EDIT_STROKES 순서와 동일: 혼영(잔여)이 맨 앞.
    val values = listOf(medley, free, breast, back, fly, kick)

    // 글래스+블러+예측 뒤로가기(슬라이드-다운)는 공용 시트가 담당한다.
    SoodalBottomSheet(onDismiss = onDismiss) { close, dragModifier ->
        // 콘텐츠가 길어도 화면을 넘지 않게 제한 — 넘치면 내부 스크롤.
        val maxSheetHeight = LocalConfiguration.current.screenHeightDp.dp * 0.85f
        Column(Modifier.fillMaxWidth().heightIn(max = maxSheetHeight)) {
            // 헤더 — 스크롤 밖 고정. 거리/시간 섹션 직전까지 드래그로 닫기 가능.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(dragModifier)
                    .padding(horizontal = 18.dp),
            ) {
                Text("기록 수정", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SheetTxt1)
                Spacer(Modifier.height(2.dp))
                Text(dateLabel, fontSize = 11.sp, color = SheetTxt3)
                Spacer(Modifier.height(14.dp))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 18.dp),
            ) {
            // 거리 / 시간 — 표시 전용
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ReadonlyField(Modifier.weight(1f), "거리", formatNumberLocal(data.distanceM), "m", SheetBlue)
                ReadonlyField(Modifier.weight(1f), "시간", data.durationMin.toString(), "분", SheetTxt2)
            }

            Spacer(Modifier.height(22.dp))

            // 영법별 비율
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("영법별 비율", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SheetTxt2, letterSpacing = 0.5.sp)
                Text("남은 거리는 혼영으로 자동 배정", fontSize = 11.sp, color = SheetTxt3, fontFamily = JetBrainsMonoFamily)
            }
            Spacer(Modifier.height(10.dp))
            EditStrokeBar(values)

            Spacer(Modifier.height(18.dp))

            // 슬라이더: 혼영(기본)은 잔여 표시 전용, 나머지 5개는 잔여까지만 직접 조절.
            val setters = listOf<((Int) -> Unit)?>(
                null, // 혼영 — 남은 거리가 자동 배정된다
                { free = clampStrokeMeters(it, breast + back + fly + kick, distance) },
                { breast = clampStrokeMeters(it, free + back + fly + kick, distance) },
                { back = clampStrokeMeters(it, free + breast + fly + kick, distance) },
                { fly = clampStrokeMeters(it, free + breast + back + kick, distance) },
                { kick = clampStrokeMeters(it, free + breast + back + fly, distance) },
            )
            EDIT_STROKES.forEachIndexed { i, spec ->
                if (i > 0) Spacer(Modifier.height(16.dp))
                val setter = setters[i]
                StrokeSliderRow(
                    spec = spec,
                    value = values[i],
                    pct = strokePercent(values[i], distance),
                    maxMeters = distance.coerceAtLeast(1),
                    enabled = setter != null,
                    onChange = setter ?: { },
                )
            }

            Spacer(Modifier.height(22.dp))

            // 저장 — clickable로 매 재구성마다 최신 영법 값을 캡처한다.
            // (pointerInput(Unit)은 첫 컴포지션의 medley를 영구 캡처해 그래프 반영이 어긋났다)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF2563EB))))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = LocalIndication.current,
                    ) {
                        // 저장 시 시트를 먼저 내린 뒤 콜백 (부드러운 닫힘).
                        close { onSave(free, breast, back, fly, kick, medley) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("저장", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.3.sp)
            }
            } // 스크롤 콘텐츠 Column
        }
    }
}

@Composable
private fun ReadonlyField(modifier: Modifier, label: String, value: String, unit: String, valueColor: Color) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(SheetFieldBg)
            .border(1.dp, SheetFieldBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, fontSize = 11.sp, color = SheetTxt3, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = valueColor, fontFamily = JetBrainsMonoFamily)
            Text(unit, fontSize = 11.sp, color = SheetTxt3, modifier = Modifier.padding(start = 3.dp, bottom = 2.dp))
        }
    }
}

/** 6개 영법 비율 막대 — 합계 기준, 많이 한 영법이 왼쪽. */
@Composable
private fun EditStrokeBar(values: List<Int>) {
    val total = values.sum().coerceAtLeast(1)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF12263F).copy(alpha = 0.06f)),
    ) {
        sortedStrokeSegments(EDIT_STROKES.map { it.color }.zip(values)).forEach { (color, m) ->
            if (m > 0) {
                Box(
                    modifier = Modifier
                        .weight(m.toFloat() / total)
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }
    }
}

@Composable
private fun StrokeSliderRow(
    spec: StrokeSpec,
    value: Int,
    pct: Int,
    maxMeters: Int,
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(spec.color))
                Text(spec.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SheetTxt1)
                if (spec.isDefault) {
                    Text("기본", fontSize = 10.sp, color = SheetTxt3, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$pct%", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = spec.color, fontFamily = JetBrainsMonoFamily)
                Spacer(Modifier.size(6.dp))
                if (enabled) {
                    MeterInputChip(value = value, onCommit = onChange)
                } else {
                    Text("${value}m", fontSize = 11.sp, color = SheetTxt3, fontFamily = JetBrainsMonoFamily)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        // 긴 거리에선 슬라이더 정밀 조작이 어려워 ±step 버튼을 함께 둔다.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StepButton("−", enabled) {
                onChange(stepStrokeMeters(value, METER_STEP, up = false))
            }
            StrokeSlider(
                value = value,
                maxMeters = maxMeters,
                step = METER_STEP,
                color = spec.color,
                enabled = enabled,
                onChange = onChange,
                modifier = Modifier.weight(1f),
            )
            StepButton("+", enabled) {
                onChange(stepStrokeMeters(value, METER_STEP, up = true))
            }
        }
    }
}

/**
 * 거리 직접 입력 칩 — 탭하면 숫자 키패드로 m 값을 입력한다.
 * 빈 입력은 무시하고, 범위 제한은 호출자(onCommit의 clamp)에 맡긴다.
 */
@Composable
private fun MeterInputChip(value: Int, onCommit: (Int) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var hadFocus by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    fun commit() {
        text.text.toIntOrNull()?.let(onCommit)
        editing = false
        hadFocus = false
    }

    val chipShape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier
            .clip(chipShape)
            .background(SheetFieldBg)
            .border(1.dp, SheetFieldBorder, chipShape)
            .then(
                if (editing) Modifier else Modifier.pointerInput(value) {
                    detectTapGestures {
                        // 커서를 맨 뒤에 놓고 시작 — 바로 이어서 지우거나 덧붙일 수 있게
                        val s = value.toString()
                        text = TextFieldValue(s, selection = TextRange(s.length))
                        editing = true
                    }
                },
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (editing) {
            BasicTextField(
                value = text,
                onValueChange = { input ->
                    val filtered = input.text.filter { it.isDigit() }.take(5)
                    text = if (filtered == input.text) {
                        input
                    } else {
                        TextFieldValue(filtered, selection = TextRange(filtered.length))
                    }
                },
                modifier = Modifier
                    .width(40.dp)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        if (state.isFocused) hadFocus = true
                        else if (hadFocus) commit() // 키보드 닫힘 등 포커스 이탈 시에도 반영
                    },
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SheetTxt1,
                    fontFamily = JetBrainsMonoFamily,
                    textAlign = TextAlign.End,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commit() }),
                singleLine = true,
                cursorBrush = SolidColor(SheetBlue),
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        } else {
            Text("$value", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SheetTxt1, fontFamily = JetBrainsMonoFamily)
        }
        Text("m", fontSize = 10.sp, color = SheetTxt3, modifier = Modifier.padding(start = 2.dp))
    }
}

/** 슬라이더 양옆 ± 조절 버튼 — 동그란 칩. enabled=false면 흐리게 표시만 한다. */
@Composable
private fun StepButton(glyph: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.35f)
            .size(26.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(SheetFieldBg)
            .border(1.dp, SheetFieldBorder, RoundedCornerShape(999.dp))
            // clickable은 재구성마다 최신 onClick을 쓴다 — pointerInput(Unit)은 첫 value를 영구
            // 캡처해 버튼이 한 번만 먹히는 버그가 있었다.
            .then(
                if (enabled) {
                    Modifier.pressable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SheetTxt2, fontFamily = JetBrainsMonoFamily)
    }
}

/** 트랙 + 썸 커스텀 슬라이더. 탭/드래그로 0~max(m) 사이 step 단위 선택. enabled=false면 표시 전용. */
@Composable
private fun StrokeSlider(
    value: Int,
    maxMeters: Int,
    step: Int,
    color: Color,
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(30.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val frac = (value.toFloat() / maxMeters).coerceIn(0f, 1f)

        fun valueAt(x: Float): Int {
            val ratio = (x / widthPx).coerceIn(0f, 1f)
            return (Math.round(ratio * maxMeters / step) * step).coerceIn(0, maxMeters)
        }

        // 입력 영역 — 표시 전용(혼영=잔여)일 때는 제스처를 붙이지 않는다.
        val inputModifier = if (enabled) {
            Modifier
                .pointerInput(maxMeters) { detectTapGestures { onChange(valueAt(it.x)) } }
                .pointerInput(maxMeters) {
                    detectHorizontalDragGestures { change, _ -> onChange(valueAt(change.position.x)) }
                }
        } else {
            Modifier
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
                .then(inputModifier),
            contentAlignment = Alignment.CenterStart,
        ) {
            // 트랙
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFF12263F).copy(alpha = 0.08f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(frac)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.4f), color))),
                )
            }
            // 썸
            val thumbXDp = with(androidx.compose.ui.platform.LocalDensity.current) { (frac * widthPx).toDp() } - 10.dp
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbXDp.roundToPx().coerceAtLeast(0), 0) }
                    .size(20.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White)
                    .border(2.5.dp, color, RoundedCornerShape(999.dp)),
            )
        }
    }
}

private fun formatNumberLocal(n: Int): String {
    if (n < 1000) return n.toString()
    val s = n.toString()
    return buildString {
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) append(',')
            append(c)
        }
    }
}
