package com.soodalbbobgi.app.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.JetBrainsMonoFamily
import com.soodalbbobgi.app.core.theme.StrokePalette
import kotlinx.coroutines.launch

// 시트는 항상 화이트(라이트) — 앱 테마와 무관하게 디자인 고정값 사용.
private val SheetTxt1 = Color(0xFF1A2438)
private val SheetTxt2 = Color(0xFF5C6B7E)
private val SheetTxt3 = Color(0xFFA7B0BF)
private val SheetFieldBg = Color(0xFF12263F).copy(alpha = 0.03f)
private val SheetFieldBorder = Color(0xFF12263F).copy(alpha = 0.08f)
private val SheetBlue = Color(0xFF2563EB)

/** 수정 시트의 6개 영법 — 표시 라벨/색/기본 여부. */
private data class StrokeSpec(val label: String, val color: Color, val isDefault: Boolean = false)

private val EDIT_STROKES = listOf(
    StrokeSpec("자유형", StrokePalette.Free),
    StrokeSpec("평영", StrokePalette.Breast),
    StrokeSpec("배영", StrokePalette.Back),
    StrokeSpec("접영", StrokePalette.Fly),
    StrokeSpec("킥판", StrokePalette.Kick),
    StrokeSpec("혼영", StrokePalette.Medley, isDefault = true),
)

/**
 * 영법 비율 수정 바텀시트 (디자인 06b).
 * 거리·시간은 Health Connect 표시 전용 — 수정은 입력된 기록(총 거리)의 영법별 재분배만 허용한다.
 * 자유형~킥판 5개만 직접 조절하고, 남은 거리는 혼영(기본)이 자동으로 가져간다.
 *
 * @param data 현재 선택한 날의 수영 데이터 (원본 영법 미터 포함)
 * @param onSave (free, breast, back, fly, kick, mixed) 순서의 보정값 콜백 — 합계는 항상 기록 거리와 같다
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrokeEditSheet(
    dateLabel: String,
    data: SwimDayData,
    onDismiss: () -> Unit,
    onSave: (free: Int, breast: Int, back: Int, fly: Int, kick: Int, mixed: Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
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
    val values = listOf(free, breast, back, fly, kick, medley)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(bottom = 18.dp),
        ) {
            // 헤더
            Text("기록 수정", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = SheetTxt1)
            Spacer(Modifier.height(2.dp))
            Text(dateLabel, fontSize = 11.sp, color = SheetTxt3)

            Spacer(Modifier.height(14.dp))

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

            // 슬라이더: 5개는 직접 조절(잔여까지만), 혼영(기본)은 잔여 표시 전용.
            val setters = listOf<(Int) -> Unit>(
                { free = clampStrokeMeters(it, breast + back + fly + kick, distance) },
                { breast = clampStrokeMeters(it, free + back + fly + kick, distance) },
                { back = clampStrokeMeters(it, free + breast + fly + kick, distance) },
                { fly = clampStrokeMeters(it, free + breast + back + kick, distance) },
                { kick = clampStrokeMeters(it, free + breast + back + fly, distance) },
            )
            EDIT_STROKES.forEachIndexed { i, spec ->
                if (i > 0) Spacer(Modifier.height(16.dp))
                val editable = i < setters.size
                StrokeSliderRow(
                    spec = spec,
                    value = values[i],
                    pct = strokePercent(values[i], distance),
                    maxMeters = distance.coerceAtLeast(1),
                    enabled = editable,
                    onChange = if (editable) setters[i] else ({ }),
                )
            }

            Spacer(Modifier.height(22.dp))

            // 저장
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF2563EB))))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            // 저장 시 시트를 먼저 내린 뒤 콜백 (부드러운 닫힘).
                            scope.launch {
                                sheetState.hide()
                                onSave(free, breast, back, fly, kick, medley)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("저장", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.3.sp)
            }
        }
    }
}

@Composable
private fun SheetHandle() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF12263F).copy(alpha = 0.16f)),
        )
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
        Spacer(Modifier.height(4.dp))
        Text("Health Connect에서 가져옴", fontSize = 10.sp, color = SheetTxt3, letterSpacing = 0.2.sp)
    }
}

/** 6개 영법 비율 막대 — 합계 기준. */
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
        EDIT_STROKES.forEachIndexed { i, spec ->
            if (values[i] > 0) {
                Box(
                    modifier = Modifier
                        .weight(values[i].toFloat() / total)
                        .fillMaxHeight()
                        .background(spec.color),
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
            Row(verticalAlignment = Alignment.Bottom) {
                Text("$pct%", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = spec.color, fontFamily = JetBrainsMonoFamily)
                Text("${value}m", fontSize = 11.sp, color = SheetTxt3, modifier = Modifier.padding(start = 6.dp, bottom = 1.dp), fontFamily = JetBrainsMonoFamily)
            }
        }
        Spacer(Modifier.height(6.dp))
        StrokeSlider(value = value, maxMeters = maxMeters, step = 25, color = spec.color, enabled = enabled, onChange = onChange)
    }
}

/** 트랙 + 썸 커스텀 슬라이더. 탭/드래그로 0~max(m) 사이 step 단위 선택. enabled=false면 표시 전용. */
@Composable
private fun StrokeSlider(value: Int, maxMeters: Int, step: Int, color: Color, enabled: Boolean = true, onChange: (Int) -> Unit) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().height(30.dp),
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
