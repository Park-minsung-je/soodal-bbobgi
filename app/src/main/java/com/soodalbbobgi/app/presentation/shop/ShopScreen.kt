package com.soodalbbobgi.app.presentation.shop

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalTabBar
import com.soodalbbobgi.app.domain.model.Grade

@Composable
fun ShopScreen(
    onNavigateToTab: (String) -> Unit,
    viewModel: ShopViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    Box(modifier = Modifier.fillMaxSize()) {
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
                    Text(
                        text = "🛒 상점",
                        style = SoodalDesign.typography.lg,
                        color = colors.textPrimary,
                    )
                    SoodalChip(
                        text = state.pearls.toString(),
                        color = ChipColor.Purple,
                        icon = "🔮",
                    )
                }

                Spacer(Modifier.height(spacing.s4))

                // -- Featured Item --
                SoodalCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { viewModel.selectForPurchase(state.featured) },
                        ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(SoodalShape.lg)
                                .background(colors.accentGoldSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(text = state.featured.emoji, fontSize = 36.sp)
                        }
                        Spacer(Modifier.width(spacing.s3))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
                            ) {
                                GradeBadge(grade = Grade.SSR)
                                Text(
                                    text = "한정 출시",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accentGold,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = state.featured.name,
                                style = SoodalDesign.typography.md,
                                color = colors.textPrimary,
                            )
                            Text(
                                text = state.featured.desc,
                                fontSize = 12.sp,
                                color = colors.textTertiary,
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🔮", fontSize = 18.sp)
                            Text(
                                text = "${state.featured.price}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentPurple,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(spacing.s5))

                // -- Boxes Section --
                Text(
                    text = "📦 상자",
                    style = SoodalDesign.typography.md,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(spacing.s3))

                // 2x2 grid
                for (row in 0 until 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                    ) {
                        for (col in 0 until 2) {
                            val index = row * 2 + col
                            if (index < state.boxes.size) {
                                BoxGridItem(
                                    item = state.boxes[index],
                                    onClick = { viewModel.selectForPurchase(state.boxes[index]) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    if (row == 0) Spacer(Modifier.height(spacing.s3))
                }

                Spacer(Modifier.height(spacing.s5))

                // -- Direct Items Section --
                Text(
                    text = "🎁 아이템 직접 구매",
                    style = SoodalDesign.typography.md,
                    color = colors.textPrimary,
                )
                Spacer(Modifier.height(spacing.s3))

                // 2-column grid
                val itemRows = state.directItems.chunked(2)
                itemRows.forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                    ) {
                        rowItems.forEach { item ->
                            DirectItemCard(
                                item = item,
                                onClick = { viewModel.selectForPurchase(item) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        // Fill empty space if odd number
                        if (rowItems.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                    if (rowIndex < itemRows.size - 1) Spacer(Modifier.height(spacing.s3))
                }

                Spacer(Modifier.height(spacing.s5))
            }

            // -- Tab Bar --
            SoodalTabBar(activeTab = "shop", onTabSelected = onNavigateToTab)
        }

        // -- Purchase Confirm Modal --
        if (state.confirmItem != null) {
            PurchaseConfirmOverlay(
                item = state.confirmItem!!,
                pearls = state.pearls,
                onCancel = { viewModel.cancelPurchase() },
                onConfirm = { viewModel.confirmPurchase() },
            )
        }
    }
}

@Composable
private fun BoxGridItem(
    item: ShopItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    SoodalCard(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = item.emoji, fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = item.desc,
                fontSize = 10.sp,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "💎 ${item.price}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.accentPurple,
            )
        }
    }
}

@Composable
private fun DirectItemCard(
    item: ShopItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = SoodalDesign.colors
    val bgAlpha = if (item.isOwned) 0.5f else 1f

    SoodalCard(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                enabled = !item.isOwned,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(SoodalShape.md)
                    .background(colors.surface2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 24.sp,
                    color = Color.Unspecified.copy(alpha = bgAlpha),
                )
            }
            Spacer(Modifier.height(4.dp))
            if (item.grade != null) {
                GradeBadge(grade = item.grade)
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = item.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary.copy(alpha = bgAlpha),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.isOwned) {
                Text(
                    text = "보유 중",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.success,
                )
            } else {
                Text(
                    text = "💎 ${item.price}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPurple,
                )
            }
        }
    }
}

@Composable
private fun PurchaseConfirmOverlay(
    item: ShopItem,
    pearls: Int,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing
    val canAfford = pearls >= item.price

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        SoodalCard(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "구매 확인",
                    style = SoodalDesign.typography.md,
                    color = colors.textPrimary,
                )

                Spacer(Modifier.height(spacing.s4))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(SoodalShape.lg)
                        .background(colors.surface2),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = item.emoji, fontSize = 32.sp)
                }

                Spacer(Modifier.height(spacing.s3))

                if (item.grade != null) {
                    GradeBadge(grade = item.grade)
                    Spacer(Modifier.height(spacing.s2))
                }

                Text(
                    text = item.name,
                    style = SoodalDesign.typography.md,
                    color = colors.textPrimary,
                )

                Spacer(Modifier.height(spacing.s2))

                Text(
                    text = "💎 ${item.price} 진주",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accentPurple,
                )

                if (!canAfford) {
                    Spacer(Modifier.height(spacing.s2))
                    Text(
                        text = "진주가 부족합니다 (보유: ${pearls}개)",
                        fontSize = 12.sp,
                        color = colors.warn,
                    )
                }

                Spacer(Modifier.height(spacing.s5))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                ) {
                    SoodalButton(
                        text = "취소",
                        onClick = onCancel,
                        style = ButtonStyle.Secondary,
                        modifier = Modifier.weight(1f),
                    )
                    SoodalButton(
                        text = "구매",
                        onClick = onConfirm,
                        style = ButtonStyle.Purple,
                        enabled = canAfford,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
