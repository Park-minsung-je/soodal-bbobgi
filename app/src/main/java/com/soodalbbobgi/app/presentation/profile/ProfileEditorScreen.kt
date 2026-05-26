package com.soodalbbobgi.app.presentation.profile

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.SoodalTabBar
import com.soodalbbobgi.app.core.ui.SoodalTextField
import com.soodalbbobgi.app.domain.model.Grade

private data class EditorItem(
    val name: String,
    val emoji: String,
    val grade: Grade,
    val isSelected: Boolean = false,
)

private val demoBgItems = listOf(
    EditorItem("오로라", "🌅", Grade.SR, true),
    EditorItem("한밤", "🌙", Grade.N),
    EditorItem("산호초", "🪸", Grade.R),
    EditorItem("딥블루", "🌊", Grade.R),
    EditorItem("열대야", "🌴", Grade.N),
)

private val demoCharItems = listOf(
    EditorItem("수달이", "🦦", Grade.N, true),
    EditorItem("진주 수달", "🦦", Grade.SR),
    EditorItem("코랄 수달", "🦦", Grade.R),
    EditorItem("버블 수달", "🦦", Grade.N),
)

private val demoFrameItems = listOf(
    EditorItem("시안 라인", "🖼️", Grade.R, true),
    EditorItem("버블", "🖼️", Grade.N),
    EditorItem("별자리", "🖼️", Grade.R),
)

private val editorTabs = listOf("배경", "캐릭터", "테두리", "텍스트")

@Composable
fun ProfileEditorScreen(
    onBack: () -> Unit,
    onPreview: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    var activeTab by remember { mutableIntStateOf(0) }
    var cardText by remember { mutableStateOf("수달 마스터") }
    var fontStyleIndex by remember { mutableIntStateOf(0) }
    var charX by remember { mutableFloatStateOf(35f) }
    var charY by remember { mutableFloatStateOf(25f) }
    var charSize by remember { mutableFloatStateOf(70f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgDeep),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4, vertical = spacing.s4),
        ) {
            // -- Header --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surface2)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onBack,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        SoodalIcon(icon = SoodalIcons.ArrowLeft, tint = colors.textPrimary, size = 16.dp)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SoodalIcon(icon = SoodalIcons.Otter, size = 20.dp)
                        Text(
                            text = "프로필 편집",
                            style = SoodalDesign.typography.lg,
                            color = colors.textPrimary,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(SoodalShape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onPreview,
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("미리보기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                    SoodalIcon(icon = SoodalIcons.Share, tint = colors.textSecondary, size = 14.dp)
                }
            }

            Spacer(Modifier.height(spacing.s4))

            // -- Live Preview Card --
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(SoodalShape.sm)
                            .background(colors.accentCyanSoft)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "LIVE PREVIEW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentCyan,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }
                Spacer(Modifier.height(spacing.s3))
                ProfileCardComposite(
                    layers = CardLayers(
                        nickname = "Soodal",
                        tagline = cardText.ifEmpty { "수달 마스터" },
                        charX = charX / 70f,
                        charY = charY / 50f,
                        charScale = charSize / 100f,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(spacing.s2))
                Text(
                    text = "배경: 오로라 · 캐릭터: 수달이 · 테두리: 시안 라인",
                    fontSize = 10.sp,
                    color = colors.textTertiary,
                )
            }

            Spacer(Modifier.height(spacing.s4))

            // -- Tab Selector --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoodalShape.md)
                    .background(colors.surface2),
            ) {
                editorTabs.forEachIndexed { index, label ->
                    val isActive = index == activeTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SoodalShape.md)
                            .background(if (isActive) colors.accentCyanSoft else Color.Transparent)
                            .then(
                                if (isActive) Modifier.border(1.dp, colors.accentCyan.copy(alpha = 0.3f), SoodalShape.md)
                                else Modifier
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { activeTab = index },
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) colors.accentCyan else colors.textTertiary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.s4))

            // -- Tab Content --
            when (activeTab) {
                0 -> ItemGrid(items = demoBgItems, tabName = "배경")
                1 -> {
                    ItemGrid(items = demoCharItems, tabName = "캐릭터")
                    Spacer(Modifier.height(spacing.s4))
                    // Character sliders
                    Text(
                        text = "캐릭터 위치 조정",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(spacing.s2))
                    SliderRow(label = "↔ 좌우", value = charX, range = 0f..70f, onValueChange = { charX = it })
                    SliderRow(label = "↕ 상하", value = charY, range = 0f..50f, onValueChange = { charY = it })
                    SliderRow(label = "◯ 크기", value = charSize, range = 30f..100f, onValueChange = { charSize = it })
                    Spacer(Modifier.height(spacing.s2))
                    SoodalButton(
                        text = "초기화",
                        onClick = {
                            charX = 35f
                            charY = 25f
                            charSize = 70f
                        },
                        style = ButtonStyle.Ghost,
                    )
                }
                2 -> ItemGrid(items = demoFrameItems, tabName = "테두리")
                3 -> {
                    SoodalTextField(
                        value = cardText,
                        onValueChange = { cardText = it },
                        placeholder = "카드 텍스트 입력 (최대 30자)",
                        maxLength = 30,
                    )
                    Spacer(Modifier.height(spacing.s3))
                    Text(
                        text = "폰트 스타일",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(spacing.s2))
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                        val fontLabels = listOf("기본", "이탤릭", "굵게")
                        fontLabels.forEachIndexed { index, label ->
                            val isActive = index == fontStyleIndex
                            Box(
                                modifier = Modifier
                                    .clip(SoodalShape.md)
                                    .background(if (isActive) colors.accentCyanSoft else colors.surface2)
                                    .then(
                                        if (isActive) Modifier.border(1.dp, colors.accentCyan.copy(alpha = 0.3f), SoodalShape.md)
                                        else Modifier
                                    )
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = { fontStyleIndex = index },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isActive) colors.accentCyan else colors.textTertiary,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.s5))

            // -- Bottom Buttons --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                SoodalButton(
                    text = "미리보기",
                    onClick = onPreview,
                    style = ButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                )
                SoodalButton(
                    text = "저장 & 적용",
                    onClick = {},
                    style = ButtonStyle.Primary,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(spacing.s4))
        }

        // -- Tab Bar --
        SoodalTabBar(
            activeTab = "home",
            onTabSelected = {},
        )
    }
}

@Composable
private fun ItemGrid(items: List<EditorItem>, tabName: String) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    if (items.isEmpty()) {
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

    // Since we can't use LazyVerticalGrid inside verticalScroll, manually lay out
    val rows = items.chunked(4)
    rows.forEachIndexed { rowIndex, rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            rowItems.forEach { item ->
                ItemGridCell(
                    item = item,
                    modifier = Modifier.weight(1f),
                )
            }
            // Fill remaining cells if row is not full
            repeat(4 - rowItems.size) {
                Spacer(Modifier.weight(1f))
            }
        }
        if (rowIndex < rows.size - 1) Spacer(Modifier.height(spacing.s2))
    }
}

@Composable
private fun ItemGridCell(
    item: EditorItem,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    val borderColor = when {
        item.isSelected -> colors.accentCyan
        else -> Color.Transparent
    }
    val borderWidth = if (item.isSelected) 2.dp else 0.dp

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SoodalShape.md)
                .background(colors.cardBg)
                .then(
                    if (item.isSelected) Modifier.border(borderWidth, borderColor, SoodalShape.md)
                    else Modifier.border(1.dp, colors.cardBorder, SoodalShape.md)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = item.emoji,
                fontSize = 24.sp,
            )
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
    val spacing = SoodalDesign.spacing
    val fraction = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.width(56.dp),
        )
        Spacer(Modifier.width(spacing.s2))
        // Simplified slider track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .clip(SoodalShape.sm)
                .background(colors.surface2)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(24.dp)
                    .clip(SoodalShape.sm)
                    .background(colors.accentCyan.copy(alpha = 0.3f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value.toInt().toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentCyan,
                )
            }
        }
    }
}
