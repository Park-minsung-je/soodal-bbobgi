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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.AssetImage
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.motion.rememberPopupEnter
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.presentation.gacha.GachaResultOverlay

@Composable
fun ShopScreen(
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
                    // 상태바 인셋을 스크롤되는 콘텐츠 패딩으로 — 전역 페이드 스크림과 함께
                    // 콘텐츠가 상태바 밑으로 자연스럽게 사라진다.
                    .statusBarsPadding()
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SoodalIcon(icon = SoodalIcons.Shop, size = 22.dp)
                        Text(
                            text = "상점",
                            style = SoodalDesign.typography.lg,
                            color = colors.textPrimary,
                        )
                    }
                    SoodalChip(
                        text = state.pearls.toString(),
                        color = ChipColor.Purple,
                        iconType = SoodalIcons.Pearl,
                        label = "진주",
                    )
                }

                Spacer(Modifier.height(spacing.s4))

                val boxListings = state.listings.filter { it.productType == "box" }
                val itemListings = state.listings.filter { it.productType == "item" }

                // -- Boxes Section --
                if (boxListings.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SoodalIcon(icon = SoodalIcons.Box, size = 18.dp)
                        Text(
                            text = "상자",
                            style = SoodalDesign.typography.md,
                            color = colors.textPrimary,
                        )
                    }
                    Spacer(Modifier.height(spacing.s3))

                    val boxRows = boxListings.chunked(2)
                    boxRows.forEachIndexed { rowIndex, rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                        ) {
                            rowItems.forEach { item ->
                                BoxGridItem(
                                    item = item,
                                    onClick = { viewModel.selectForPurchase(item) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                        if (rowIndex < boxRows.size - 1) Spacer(Modifier.height(spacing.s3))
                    }
                    Spacer(Modifier.height(spacing.s5))
                }

                // -- Direct Items Section --
                if (itemListings.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SoodalIcon(icon = SoodalIcons.Gift, size = 18.dp)
                        Text(
                            text = "아이템 직접 구매",
                            style = SoodalDesign.typography.md,
                            color = colors.textPrimary,
                        )
                    }
                    Spacer(Modifier.height(spacing.s3))

                    val itemRows = itemListings.chunked(2)
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
                            if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                        }
                        if (rowIndex < itemRows.size - 1) Spacer(Modifier.height(spacing.s3))
                    }
                    Spacer(Modifier.height(spacing.s5))
                }
            }
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

        // -- Box Purchase Result Overlay (뽑기 결과 모달 재사용) --
        if (state.boxResults.isNotEmpty()) {
            GachaResultOverlay(
                results = state.boxResults,
                onClose = { viewModel.dismissBoxResults() },
            )
        }
    }
}

/**
 * 구매 한도 라벨. 기간 한도가 있으면 "오늘 0/1"처럼, 1인 한도가 있으면 "구매 0/1"처럼 표시.
 *
 * @return 표시할 라벨, 한도가 없으면 null
 */
private fun limitLabel(item: ShopItem): String? {
    item.maxPerPeriod?.let { max ->
        val periodName = when (item.periodType) {
            "daily" -> "오늘"
            "weekly" -> "이번 주"
            "monthly" -> "이번 달"
            else -> "기간"
        }
        return "$periodName ${item.purchasedThisPeriod}/$max"
    }
    item.maxPerUser?.let { max ->
        return "구매 ${item.purchasedTotal}/$max"
    }
    return null
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
            val boxTint = when (item.icon) {
                SoodalIcons.Aurora -> colors.accentBlue
                SoodalIcons.Frame -> colors.accentPurple
                else -> colors.accentGold
            }
            // 상자는 배경 없이 일러스트만 (아이템과 달리 티어 배경 미적용)
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.imageAsset.isNullOrBlank()) {
                    AssetImage(
                        imageAsset = item.imageAsset,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SoodalIcon(icon = item.icon, tint = boxTint, size = 28.dp)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = item.description,
                fontSize = 10.sp,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                SoodalIcon(icon = SoodalIcons.Diamond, tint = colors.accentPurple, size = 12.dp)
                Text(
                    text = "${item.price}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPurple,
                )
            }
            limitLabel(item)?.let { label ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.canBuy) colors.textTertiary else colors.warn,
                )
            }
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
    val bgAlpha = if (!item.canBuy) 0.5f else 1f

    SoodalCard(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                enabled = item.canBuy,
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val (itemBg, itemTint) = when (item.grade) {
                Grade.SSR -> colors.accentGoldSoft to colors.accentGold
                Grade.SR -> colors.accentPurpleSoft to colors.accentPurple
                Grade.R -> colors.accentBlueSoft to colors.accentBlue
                else -> colors.surface2 to colors.textTertiary
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(SoodalShape.md)
                    .background(itemBg),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.imageAsset.isNullOrBlank()) {
                    AssetImage(
                        imageAsset = item.imageAsset,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    SoodalIcon(icon = item.icon, tint = itemTint.copy(alpha = bgAlpha), size = 24.dp)
                }
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
            if (!item.canBuy) {
                Text(
                    text = "보유 중",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.success,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    SoodalIcon(icon = SoodalIcons.Diamond, tint = colors.accentPurple, size = 11.dp)
                    Text(
                        text = "${item.price}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentPurple,
                    )
                }
                limitLabel(item)?.let { label ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textTertiary,
                    )
                }
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
    val p = rememberPopupEnter()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f * p))
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
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = 0.9f + 0.1f * p
                    scaleY = 0.9f + 0.1f * p
                    alpha = p
                },
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
                    // 목록 셀과 동일하게 에셋 우선, 없으면 아이콘 폴백
                    if (!item.imageAsset.isNullOrBlank()) {
                        AssetImage(
                            imageAsset = item.imageAsset,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize().padding(6.dp),
                        )
                    } else {
                        SoodalIcon(icon = item.icon, tint = colors.textTertiary, size = 32.dp)
                    }
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Diamond, tint = colors.accentPurple, size = 16.dp)
                    Text(
                        text = "${item.price} 진주",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentPurple,
                    )
                }

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
