package com.soodalbbobgi.app.presentation.profile

import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.AssetImage
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.SoodalTextField

/**
 * 프로필 편집 컨트롤 바텀 시트.
 *
 * 카드 미리보기는 호출부(홈)가 그리므로 여기에는 탭/아이템 그리드/슬라이더/색상/텍스트/적용만 둔다.
 * 편집값은 아직 미저장이라 전체화면(저장값)과 어긋나므로 "크게보기"는 두지 않는다.
 * 핸들(또는 시트)을 아래로 충분히 끌면 [onDismiss], [적용] 버튼은 [onApply].
 *
 * @param state 편집 상태(미저장 현재값)
 * @param vm 편집 ViewModel(슬라이더/선택 콜백 호출용)
 * @param isOpen 시트가 열려 있는지 — 열릴 때마다 끌어내린 잔여 오프셋을 0으로 되돌린다
 * @param onApply 적용(저장) 콜백
 * @param onDismiss 아래로 끌어 닫기(취소) 콜백
 */
@Composable
fun EditorSheet(
    state: ProfileEditorUiState,
    vm: ProfileEditorViewModel,
    isOpen: Boolean,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing
    val density = LocalDensity.current
    var dragOffset by remember { mutableFloatStateOf(0f) }
    // 닫힘 애니메이션 도중 다시 열면 끌어내린 오프셋이 남아 끝까지 안 올라온다 → 열릴 때마다 리셋.
    LaunchedEffect(isOpen) { if (isOpen) dragOffset = 0f }
    // 거리(완화) 또는 아래로 빠르게 튕기는 속도 중 하나만 넘어도 닫는다.
    // 속도 임계값은 dp/s로 잡아 기기 밀도와 무관하게 같은 손맛을 낸다.
    val dismissDistancePx = with(density) { 100.dp.toPx() }
    val dismissVelocityPx = with(density) { 1000.dp.toPx() }

    val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Column(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { translationY = dragOffset }
            // 경계선 대신 그림자로 시트를 카드/홈 위에 띄운다.
            .shadow(16.dp, sheetShape, clip = false)
            .clip(sheetShape)
            .background(colors.surface2),
    ) {
        // -- 드래그 핸들 + 제목 --
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        dragOffset = (dragOffset + delta).coerceAtLeast(0f)
                    },
                    onDragStopped = { velocity ->
                        if (shouldDismissSheet(dragOffset, velocity, dismissDistancePx, dismissVelocityPx)) onDismiss()
                        else animate(dragOffset, 0f) { value, _ -> dragOffset = value }
                    },
                ),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.surface3),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.s4, vertical = spacing.s3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("프로필 편집", style = SoodalDesign.typography.lg, color = colors.textPrimary)
                }
            }
        }

        // -- 컨트롤 (스크롤): weight(1f)로 핸들과 적용 버튼 사이 남은 공간을 차지하고 내부에서 스크롤.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4),
        ) {
            // -- Tab Selector --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoodalShape.md)
                    .background(colors.surface2),
            ) {
                EditorCategory.values().forEach { tab ->
                    val isActive = tab == state.activeTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SoodalShape.md)
                            .background(if (isActive) colors.accentBlueSoft else Color.Transparent)
                            .then(
                                if (isActive) Modifier.border(1.dp, colors.accentBlue.copy(alpha = 0.3f), SoodalShape.md)
                                else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { vm.setActiveTab(tab) },
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) colors.accentBlue else colors.textTertiary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.s4))

            // -- Tab Content --
            when (state.activeTab) {
                EditorCategory.Background -> ItemGrid(
                    items = state.bgItems,
                    tabName = "배경",
                    onClick = { vm.selectItem(EditorCategory.Background, it) },
                    showNoneOption = true,
                    isNoneSelected = state.selectedBgInventoryId == null,
                    onClickNone = { vm.selectItem(EditorCategory.Background, null) },
                )
                EditorCategory.Character -> {
                    ItemGrid(
                        items = state.charItems,
                        tabName = "캐릭터",
                        onClick = { vm.selectItem(EditorCategory.Character, it) },
                    )
                    Spacer(Modifier.height(spacing.s3))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "캐릭터 위치 · 크기",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                        )
                        Text(
                            "초기화 ↺",
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                vm.setCharX(0.5f)
                                vm.setCharY(0.5f)
                                vm.setCharScale(1.0f)
                            },
                        )
                    }
                    Spacer(Modifier.height(spacing.s2))
                    SliderRow("↔ 좌우", state.charX, 0f..1f) { vm.setCharX(it) }
                    SliderRow("↕ 상하", state.charY, 0f..1f) { vm.setCharY(it) }
                    SliderRow("⊕ 크기", state.charScale, 0.3f..1f) { vm.setCharScale(it) }
                }
                EditorCategory.Frame -> ItemGrid(
                    items = state.frameItems,
                    tabName = "테두리",
                    onClick = { vm.selectItem(EditorCategory.Frame, it) },
                    showNoneOption = true,
                    isNoneSelected = state.selectedFrameInventoryId == null,
                    onClickNone = { vm.selectItem(EditorCategory.Frame, null) },
                )
                EditorCategory.Text -> {
                    // 한글 IME 조합 중 글자 누락 방지: TextField의 value를 ViewModel StateFlow에
                    // 직접 묶으면 onValueChange→VM→flow 왕복 사이 IME 조합이 끊겨 글자가 사라진다.
                    // 로컬 상태를 즉시 갱신해 조합을 끊지 않고, 같은 콜백에서 VM에도 밀어넣어
                    // Live Preview는 동기화하되 입력 표시는 로컬 값으로 유지한다.
                    var localText by remember { mutableStateOf<String?>(null) }
                    // 저장된 카드가 로드되면 로컬 값을 한 번만 시드한다 (사용자 입력 전).
                    LaunchedEffect(state.customText) {
                        if (localText == null) localText = state.customText
                    }
                    SoodalTextField(
                        value = localText ?: state.customText,
                        onValueChange = {
                            localText = it
                            vm.setCustomText(it)
                        },
                        placeholder = "카드 텍스트 입력 (최대 20자)",
                        maxLength = 20,
                    )

                    // -- 정렬 (줄 정렬: 좌/우) --
                    Spacer(Modifier.height(spacing.s3))
                    Text("정렬", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Spacer(Modifier.height(spacing.s2))
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                        listOf("LEFT" to "좌", "RIGHT" to "우").forEach { (value, label) ->
                            SegmentChip(
                                label = label,
                                isActive = state.textAlign == value,
                                onClick = { vm.setTextAlign(value) },
                            )
                        }
                    }

                    // -- 텍스트 위치 (블록 전체를 카드 위에서 이동) --
                    Spacer(Modifier.height(spacing.s3))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("텍스트 위치", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                        Text(
                            "초기화 ↺",
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                vm.setTextX(0.95f)
                                vm.setTextY(0.5f)
                            },
                        )
                    }
                    Spacer(Modifier.height(spacing.s2))
                    SliderRow("↔ 좌우", state.textX, 0f..1f) { vm.setTextX(it) }
                    SliderRow("↕ 상하", state.textY, 0f..1f) { vm.setTextY(it) }

                    // -- 크기 (5단계) --
                    Spacer(Modifier.height(spacing.s3))
                    Text("크기 단계", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Spacer(Modifier.height(spacing.s2))
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                        (1..5).forEach { step ->
                            SegmentChip(
                                label = step.toString(),
                                isActive = state.textScaleStep == step,
                                onClick = { vm.setTextScaleStep(step) },
                            )
                        }
                    }

                    // -- 기록 표시 토글 --
                    Spacer(Modifier.height(spacing.s3))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("기록 표시", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                        Switch(
                            checked = state.showStats,
                            onCheckedChange = { vm.setShowStats(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.btnPrimaryText,
                                checkedTrackColor = colors.accentBlue,
                                uncheckedTrackColor = colors.surface3,
                            ),
                        )
                    }

                    // -- 색상 (닉네임 / 소개 / 기록) --
                    Spacer(Modifier.height(spacing.s3))
                    Text("색상", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Spacer(Modifier.height(spacing.s2))
                    ColorPaletteRow("닉네임", state.nicknameColor) { vm.setNicknameColor(it) }
                    Spacer(Modifier.height(spacing.s2))
                    ColorPaletteRow("소개", state.taglineColor) { vm.setTaglineColor(it) }
                    Spacer(Modifier.height(spacing.s2))
                    ColorPaletteRow("기록", state.statsColor) { vm.setStatsColor(it) }

                    Spacer(Modifier.height(spacing.s3))
                    Text(
                        text = "폰트 스타일",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(spacing.s2))
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                        val styles = listOf("REGULAR" to "기본", "ITALIC" to "이탤릭", "BOLD" to "굵게")
                        styles.forEach { (value, label) ->
                            val isActive = value == state.textStyle
                            Box(
                                modifier = Modifier
                                    .clip(SoodalShape.md)
                                    .background(if (isActive) colors.accentBlueSoft else colors.surface2)
                                    .then(
                                        if (isActive) Modifier.border(1.dp, colors.accentBlue.copy(alpha = 0.3f), SoodalShape.md)
                                        else Modifier
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { vm.setTextStyle(value) },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) colors.accentBlue else colors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.s3))
        }

        // -- 적용 버튼 (하단 고정): 스크롤 영역 밖에 두어 항상 보이게 한다.
        if (state.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4, vertical = spacing.s3)
                    .height(52.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = colors.accentBlue,
                    strokeWidth = 2.dp,
                )
            }
        } else {
            // 저장 — 기록 수정 시트의 저장 버튼과 동일한 스타일/여백 (높이 52, 곡률 14, 좌우·하단 18dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(top = 10.dp, bottom = 18.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF2563EB))))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onApply,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text("저장", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 0.3.sp)
            }
        }
    }
}

/**
 * 카테고리별 보유 아이템 4열 그리드.
 *
 * @param showNoneOption true면 그리드 맨 앞에 "선택안함" 칩을 추가한다 (배경/테두리 전용).
 * @param isNoneSelected "선택안함" 칩의 선택 표시 여부 (해당 슬롯이 비어 있을 때 true).
 * @param onClickNone "선택안함" 칩 클릭 콜백.
 */
@Composable
private fun ItemGrid(
    items: List<EditorItemUi>,
    tabName: String,
    onClick: (Long) -> Unit,
    showNoneOption: Boolean = false,
    isNoneSelected: Boolean = false,
    onClickNone: () -> Unit = {},
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    if (items.isEmpty() && !showNoneOption) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "아직 보유한 ${tabName}이 없어요.\n뽑기에서 새로 만나보세요!",
                fontSize = 13.sp,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // "선택안함"은 항상 첫 셀로 고정되도록 일반 아이템과 함께 슬롯 단위로 청크한다.
    val cells: List<@Composable (Modifier) -> Unit> = buildList {
        if (showNoneOption) add { m ->
            NoneGridCell(isSelected = isNoneSelected, onClick = onClickNone, modifier = m)
        }
        items.forEach { item ->
            add { m -> ItemGridCell(item = item, onClick = { onClick(item.inventoryId) }, modifier = m) }
        }
    }

    val rows = cells.chunked(4)
    rows.forEachIndexed { rowIndex, rowCells ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            rowCells.forEach { cell -> cell(Modifier.weight(1f)) }
            repeat(4 - rowCells.size) {
                Spacer(Modifier.weight(1f))
            }
        }
        if (rowIndex < rows.size - 1) Spacer(Modifier.height(spacing.s2))
    }
}

/**
 * 슬롯을 비우는 "선택안함" 그리드 칩. 일반 셀과 동일한 틀이지만 X 아이콘으로 구분된다.
 */
@Composable
private fun NoneGridCell(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    val borderColor = if (isSelected) colors.accentBlue else colors.cardBorder
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SoodalShape.md)
                .background(colors.cardBg)
                .border(borderWidth, borderColor, SoodalShape.md)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(SoodalShape.sm)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                SoodalIcon(icon = SoodalIcons.Close, tint = colors.textTertiary, size = 20.dp)
            }
            Spacer(Modifier.height(2.dp))
            // GradeBadge와 높이를 맞추기 위한 빈 자리
            Spacer(Modifier.height(14.dp))
            Spacer(Modifier.height(2.dp))
            Text(
                text = "선택안함",
                fontSize = 10.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ItemGridCell(
    item: EditorItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    val borderColor = if (item.isSelected) colors.accentBlue else colors.cardBorder
    val borderWidth = if (item.isSelected) 2.dp else 1.dp

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SoodalShape.md)
                .background(colors.cardBg)
                .border(borderWidth, borderColor, SoodalShape.md)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val gradeColor = when (item.grade) {
                com.soodalbbobgi.app.domain.model.Grade.SSR -> colors.accentGold
                com.soodalbbobgi.app.domain.model.Grade.SR -> colors.accentPurple
                com.soodalbbobgi.app.domain.model.Grade.R -> colors.accentBlue
                com.soodalbbobgi.app.domain.model.Grade.N -> colors.textTertiary
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(SoodalShape.sm)
                    .background(gradeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                AssetImage(
                    imageAsset = item.imageAsset,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                )
            }
            Spacer(Modifier.height(2.dp))
            GradeBadge(grade = item.grade)
            Spacer(Modifier.height(2.dp))
            Text(
                text = item.name,
                fontSize = 10.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    val colors = SoodalDesign.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.width(60.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = colors.accentBlue,
                activeTrackColor = colors.accentBlue,
                inactiveTrackColor = colors.surface3,
            ),
        )
        val pct = ((value - range.start) / (range.endInclusive - range.start) * 100).toInt()
        Text("$pct%", fontSize = 11.sp, color = colors.textTertiary, modifier = Modifier.width(36.dp))
    }
}

/**
 * 텍스트 커스터마이즈용 프리셋 색상 팔레트.
 * SoodalDesign 라이트/네온 포인트 컬러 + 무채색 + 보조 액센트로 구성한다.
 */
private val TextColorPalette = listOf(
    "#FFFFFF", "#000000", "#9CA3AF",
    "#00F5FF", "#00A8B8",
    "#BF5AF2", "#8B3DDB",
    "#FFD60A", "#D99500",
    "#FF6B6B", "#30D158", "#4FB8FF",
)

/**
 * 좌/우 정렬·크기 단계 등 단일 선택용 작은 칩.
 *
 * @param label 칩에 표시할 짧은 라벨
 * @param isActive 선택 상태 여부 (선택 시 강조 배경/테두리)
 */
@Composable
private fun SegmentChip(
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val colors = SoodalDesign.colors
    Box(
        modifier = Modifier
            .clip(SoodalShape.md)
            .background(if (isActive) colors.accentBlueSoft else colors.surface2)
            .then(
                if (isActive) Modifier.border(1.dp, colors.accentBlue.copy(alpha = 0.3f), SoodalShape.md)
                else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) colors.accentBlue else colors.textTertiary,
        )
    }
}

/**
 * 한 요소(닉네임/소개/기록)의 색상을 프리셋 스와치 가로 줄에서 고른다.
 * 선택된 스와치에는 강조 링을 둘러 표시한다.
 *
 * @param label 좌측 요소 라벨
 * @param selectedColor 현재 선택된 "#RRGGBB" 색상
 * @param onSelect 스와치 클릭 시 선택 색상 전달 콜백
 */
@Composable
private fun ColorPaletteRow(
    label: String,
    selectedColor: String,
    onSelect: (String) -> Unit,
) {
    val colors = SoodalDesign.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 11.sp, color = colors.textSecondary, modifier = Modifier.width(48.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextColorPalette.forEach { hex ->
                val isSelected = hex.equals(selectedColor, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(parseSwatchColor(hex))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) colors.accentBlue else colors.cardBorder,
                            shape = CircleShape,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(hex) },
                        ),
                )
            }
        }
    }
}

/** "#RRGGBB" 문자열을 Compose Color로 변환 (실패 시 회색 폴백). */
private fun parseSwatchColor(hex: String): Color =
    try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: IllegalArgumentException) {
        Color.Gray
    }

/**
 * 드래그를 놓았을 때 편집 시트를 닫을지 결정한다.
 * 충분히 내렸거나(거리) 아래로 빠르게 튕겼으면(속도) 닫는다 — 둘 중 하나만 넘으면 충분.
 *
 * @param dragOffsetPx 현재까지 내려간 거리(px, 0 이상)
 * @param velocityPx 드래그를 놓은 순간 속도(px/s, 양수 = 아래 방향)
 * @param distanceThresholdPx 닫힘 거리 임계값(px)
 * @param velocityThresholdPx 닫힘 속도 임계값(px/s)
 * @return 닫아야 하면 true
 */
internal fun shouldDismissSheet(
    dragOffsetPx: Float,
    velocityPx: Float,
    distanceThresholdPx: Float,
    velocityThresholdPx: Float,
): Boolean =
    dragOffsetPx > distanceThresholdPx || velocityPx > velocityThresholdPx
