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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.SoodalTabBar
import com.soodalbbobgi.app.core.ui.SoodalTextField

@Composable
fun ProfileEditorScreen(
    onBack: () -> Unit,
    onPreview: () -> Unit,
    viewModel: ProfileEditorViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // 저장 결과 토스트
    LaunchedEffect(state.saveSuccess, state.saveError) {
        if (state.saveSuccess) {
            Toast.makeText(context, "프로필 카드가 저장됐어요", Toast.LENGTH_SHORT).show()
            viewModel.clearSaveResult()
            onBack()
        }
        state.saveError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveResult()
        }
    }

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
                        tagline = state.customText.ifEmpty { "수달 마스터" },
                        charX = state.charX,
                        charY = state.charY,
                        charScale = state.charScale,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(spacing.s2))
                val bgName = state.bgItems.firstOrNull { it.isSelected }?.name ?: "미선택"
                val charName = state.charItems.firstOrNull { it.isSelected }?.name ?: "미선택"
                val frameName = state.frameItems.firstOrNull { it.isSelected }?.name ?: "미선택"
                Text(
                    text = "배경: $bgName · 캐릭터: $charName · 테두리: $frameName",
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
                EditorCategory.values().forEach { tab ->
                    val isActive = tab == state.activeTab
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
                                onClick = { viewModel.setActiveTab(tab) },
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = tab.label,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isActive) colors.accentCyan else colors.textTertiary,
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
                    onClick = { viewModel.selectItem(EditorCategory.Background, it) },
                )
                EditorCategory.Character -> {
                    ItemGrid(
                        items = state.charItems,
                        tabName = "캐릭터",
                        onClick = { viewModel.selectItem(EditorCategory.Character, it) },
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
                                viewModel.setCharX(0.16f)
                                viewModel.setCharY(0.06f)
                                viewModel.setCharScale(0.70f)
                            },
                        )
                    }
                    Spacer(Modifier.height(spacing.s2))
                    SliderRow("↔ 좌우", state.charX, 0f..1f) { viewModel.setCharX(it) }
                    SliderRow("↕ 상하", state.charY, 0f..1f) { viewModel.setCharY(it) }
                    SliderRow("⊕ 크기", state.charScale, 0.3f..1f) { viewModel.setCharScale(it) }
                }
                EditorCategory.Frame -> ItemGrid(
                    items = state.frameItems,
                    tabName = "테두리",
                    onClick = { viewModel.selectItem(EditorCategory.Frame, it) },
                )
                EditorCategory.Text -> {
                    SoodalTextField(
                        value = state.customText,
                        onValueChange = { viewModel.setCustomText(it) },
                        placeholder = "카드 텍스트 입력 (최대 20자)",
                        maxLength = 20,
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
                        val styles = listOf("REGULAR" to "기본", "ITALIC" to "이탤릭", "BOLD" to "굵게")
                        styles.forEach { (value, label) ->
                            val isActive = value == state.textStyle
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
                                        onClick = { viewModel.setTextStyle(value) },
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
                if (state.isSaving) {
                    Box(
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = colors.accentCyan,
                            strokeWidth = 2.dp,
                        )
                    }
                } else {
                    SoodalButton(
                        text = "저장 & 적용",
                        onClick = { viewModel.save() },
                        style = ButtonStyle.Primary,
                        modifier = Modifier.weight(1f),
                    )
                }
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
private fun ItemGrid(
    items: List<EditorItemUi>,
    tabName: String,
    onClick: (Long) -> Unit,
) {
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

    val rows = items.chunked(4)
    rows.forEachIndexed { rowIndex, rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s2),
        ) {
            rowItems.forEach { item ->
                ItemGridCell(
                    item = item,
                    onClick = { onClick(item.inventoryId) },
                    modifier = Modifier.weight(1f),
                )
            }
            repeat(4 - rowItems.size) {
                Spacer(Modifier.weight(1f))
            }
        }
        if (rowIndex < rows.size - 1) Spacer(Modifier.height(spacing.s2))
    }
}

@Composable
private fun ItemGridCell(
    item: EditorItemUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    val borderColor = if (item.isSelected) colors.accentCyan else colors.cardBorder
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
            // 에셋 다운로드 전 임시 플레이스홀더 (등급별 색)
            val placeholderColor = when (item.grade) {
                com.soodalbbobgi.app.domain.model.Grade.SSR -> colors.accentGold
                com.soodalbbobgi.app.domain.model.Grade.SR -> colors.accentPurple
                com.soodalbbobgi.app.domain.model.Grade.R -> colors.accentCyan
                com.soodalbbobgi.app.domain.model.Grade.N -> colors.textTertiary
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(SoodalShape.sm)
                    .background(placeholderColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                SoodalIcon(icon = SoodalIcons.Box, tint = placeholderColor, size = 24.dp)
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
                thumbColor = colors.accentCyan,
                activeTrackColor = colors.accentCyan,
                inactiveTrackColor = colors.surface3,
            ),
        )
        val pct = ((value - range.start) / (range.endInclusive - range.start) * 100).toInt()
        Text("$pct%", fontSize = 11.sp, color = colors.textTertiary, modifier = Modifier.width(36.dp))
    }
}
