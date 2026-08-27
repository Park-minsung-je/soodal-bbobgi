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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.soodalbbobgi.app.core.ui.GlassInfoGroup
import com.soodalbbobgi.app.core.ui.GlassInfoSegment
import com.soodalbbobgi.app.core.ui.AppOverlay
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.GlassSheen
import com.soodalbbobgi.app.core.ui.LocalHazeContent
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.topFadeEdge
import com.soodalbbobgi.app.core.ui.glassFrost
import com.soodalbbobgi.app.core.ui.glassShadow
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.TabBarClearance
import com.soodalbbobgi.app.core.ui.motion.rememberPopupEnter
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.presentation.gacha.GachaResultOverlay

/** 헤더 ~ 첫 섹션 간격. 홈과 같이 카드끼리의 간격(14dp)에 맞추고, 경계 페이드도 같은 길이를 쓴다. */
private val ShopTopGap = 14.dp

/**
 * 구매 확인 팝업의 정사각 그림 영역 한 변.
 *
 * 제목("구매 확인")을 빼고, 이름·설명·가격 글자를 줄이고, 요소 사이 상하 공백까지 압축해
 * 확보한 세로 공간(≈74dp)을 전부 그림에 넘긴 값이다 — 팝업 높이는 그대로면서 아이템만 커진다.
 */
private val PurchaseArtSize = 132.dp

/**
 * 구매 팝업에서 아이템 그림을 담는 방식 — 에셋이 자기 여백을 갖고 있는지에 따라 갈린다.
 */
private enum class PurchaseArtStyle {
    /**
     * 정사각 영역을 꽉 채우고 아래 여백은 두지 않는다.
     * 캐릭터·상자처럼 **에셋 안에 이미 투명 여백이 들어 있는** 그림용.
     */
    Padded,

    /**
     * 정사각에 가두지 않고 팝업 안쪽 폭(상하 여백과 같은 좌우 여백을 남긴 폭)을 다 쓴 뒤
     * 비율대로 눕히고, 아래에만 여백을 준다.
     * 배경처럼 **여백 없이 꽉 찬** 그림용 — 정사각에 넣으면 위아래가 비어 작아 보인다.
     */
    Wide,
}

/**
 * 상품에 맞는 그림 표현 방식을 고른다.
 *
 * 여백 없이 꽉 찬 가로형 에셋만 [PurchaseArtStyle.Wide]를 쓴다 — 지금은 배경 아이템뿐이다.
 * **상자는 내용물이 배경이라 `category`가 `bg`여도 그림 자체는 상자**이고 투명 여백이
 * 있으므로 [PurchaseArtStyle.Padded]다. 그래서 카테고리만 보지 않고 상품 종류를 함께 본다.
 * 액자가 같은 성격이면 여기에 추가하면 된다.
 */
private fun purchaseArtStyle(item: ShopItem): PurchaseArtStyle =
    if (item.productType == "item" && item.category == "bg") {
        PurchaseArtStyle.Wide
    } else {
        PurchaseArtStyle.Padded
    }

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
                // 헤더가 고정인 화면 — 루트에서 상태바 인셋 처리.
                .statusBarsPadding(),
        ) {
            // -- 고정 헤더: 상점 타이틀 + 진주 보유량 --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.s4)
                    .padding(top = spacing.s4),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "상점",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                )
                // 통화 패널 — 홈/인양소와 동일하게 한 유리 바에 묶는다.
                GlassInfoGroup {
                    GlassInfoSegment(SoodalIcons.Shell, state.shells.toString(), colors.accentGold)
                    GlassInfoSegment(SoodalIcons.Pearl, state.pearls.toString(), colors.accentPurple)
                }
            }

            // 하단 스크롤 + 헤더 경계 페이드
            Box(Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // 홈과 같은 처리 — 고정 헤더 경계에서 콘텐츠를 알파 마스크로 사라지게 한다.
                    // 길이는 헤더 아래 여백과 같아야 정지 상태에서 첫 카드가 마스크에 걸리지 않는다.
                    .topFadeEdge(ShopTopGap)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.s4),
            ) {
                Spacer(Modifier.height(ShopTopGap))

                val boxListings = state.listings.filter { it.productType == "box" }
                // 아이템은 종류(캐릭터/배경/액자)별 섹션으로 나눠 아래로 이어 보여주고,
                // 각 섹션 안의 순서는 관리자가 지정한 서버 배치(sortOrder) 그대로 유지한다.
                val itemListings = state.listings.filter { it.productType == "item" }
                val knownCategories = setOf("char", "bg", "frame")
                val itemSections = listOf(
                    "캐릭터" to itemListings.filter { it.category == "char" },
                    "배경" to itemListings.filter { it.category == "bg" },
                    "액자" to itemListings.filter { it.category == "frame" },
                    "아이템" to itemListings.filter { it.category !in knownCategories },
                ).filter { it.second.isNotEmpty() }

                // -- Boxes Section --
                if (boxListings.isNotEmpty()) {
                    // 아이템 섹션들과 마찬가지로 제목만 둔다 — 아이콘은 붙이지 않는다.
                    Text(
                        text = "상자",
                        style = SoodalDesign.typography.md,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(spacing.s3))

                    val boxRows = boxListings.chunked(3)
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
                            // 마지막 줄이 3개 미만이면 빈칸으로 채워 셀 폭을 일정하게 유지
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                        if (rowIndex < boxRows.size - 1) Spacer(Modifier.height(spacing.s3))
                    }
                    Spacer(Modifier.height(spacing.s5))
                }

                // -- Direct Items Sections (종류별: 캐릭터/배경/액자/기타, 아래로 이어짐) --
                itemSections.forEachIndexed { sectionIndex, (title, sectionItems) ->
                    Text(
                        text = title,
                        style = SoodalDesign.typography.md,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(spacing.s3))

                    val itemRows = sectionItems.chunked(3)
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
                            // 마지막 줄이 3개 미만이면 빈칸으로 채워 셀 폭을 일정하게 유지
                            repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                        }
                        if (rowIndex < itemRows.size - 1) Spacer(Modifier.height(spacing.s3))
                    }
                    if (sectionIndex < itemSections.size - 1) Spacer(Modifier.height(spacing.s5))
                }
                Spacer(Modifier.height(TabBarClearance))
            }

            // (헤더 경계 페이드 제거 — 이미지 배경 위에서 흰 그림자처럼 떠 보임. 홈과 동일 처리)
            }
        }

        // -- Purchase Confirm Modal (오버레이 레이어로 호이스팅) --
        // 호스트 렌더가 화면 리컴포지션보다 먼저 돌 수 있으므로 !! 금지 — null이면 그리지 않는다.
        if (state.confirmItem != null) {
            AppOverlay {
                state.confirmItem?.let { item ->
                    PurchaseConfirmOverlay(
                        item = item,
                        pearls = state.pearls,
                        onCancel = { viewModel.cancelPurchase() },
                        onConfirm = { viewModel.confirmPurchase() },
                    )
                }
            }
        }

        // -- Box Purchase Result Overlay (뽑기 결과 모달 재사용, 오버레이 레이어) --
        if (state.boxResults.isNotEmpty()) {
            AppOverlay {
                GachaResultOverlay(
                    results = state.boxResults,
                    onClose = { viewModel.dismissBoxResults() },
                )
            }
        }

        // ── 최신 정보 로딩 오버레이 — 진열/잔액 새로고침 동안 화면을 딤 처리 (홈 동기화와 동일 패턴) ──
        // 구매 확인/결과 팝업이 떠 있을 땐 로딩 딤을 겹치지 않는다. 구매 직후 refresh()의 로딩 딤이
        // 결과 팝업 딤 위에 얹혀 잠깐 더 진해졌다 연해지는 이중 딤을 방지한다.
        if (state.isLoading && state.boxResults.isEmpty() && state.confirmItem == null) {
            com.soodalbbobgi.app.core.ui.SyncLoadingOverlay("최신 상점 정보를 불러오는 중이에요...")
        }
    }
}

/**
 * 구매 한도 라벨 — 산 개수가 아니라 **남은 재고**로 보여준다.
 * 기간 한도는 "오늘 재고 1개", 1인 한도는 "재고 1개", 소진되면 "품절".
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
        val remaining = (max - item.purchasedThisPeriod).coerceAtLeast(0)
        return if (remaining == 0) "$periodName 품절" else "$periodName 재고 ${remaining}개"
    }
    item.maxPerUser?.let { max ->
        val remaining = (max - item.purchasedTotal).coerceAtLeast(0)
        return if (remaining == 0) "품절" else "재고 ${remaining}개"
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
        contentPadding = 10.dp,
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
                modifier = Modifier
                    .fillMaxWidth(0.66f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.imageAsset.isNullOrBlank()) {
                    AssetImage(
                        imageAsset = item.imageAsset,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SoodalIcon(icon = item.icon, tint = boxTint, size = 48.dp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = item.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                SoodalIcon(icon = SoodalIcons.Pearl, tint = colors.accentPurple, size = 12.dp)
                Text(
                    text = "${item.price}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.accentPurple,
                )
            }
            limitLabel(item)?.let { label ->
                Spacer(Modifier.height(2.dp))
                // 살 수 있는 상품(재고 있음)은 눈에 띄게 — 3차 회색이면 품절보다도 덜 보였다.
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = if (item.canBuy) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (item.canBuy) colors.success else colors.warn,
                )
            }
            saleRemainingLabel(item.endAt)?.let { label ->
                Spacer(Modifier.height(2.dp))
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.warn,
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
    val bgAlpha = if (!item.canBuy || item.owned) 0.5f else 1f

    SoodalCard(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                enabled = item.canBuy && !item.owned,
            ),
        contentPadding = 10.dp,
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
                    .fillMaxWidth(0.66f)
                    .aspectRatio(1f)
                    .clip(SoodalShape.md)
                    .background(itemBg),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.imageAsset.isNullOrBlank()) {
                    AssetImage(
                        imageAsset = item.imageAsset,
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    SoodalIcon(icon = item.icon, tint = itemTint.copy(alpha = bgAlpha), size = 40.dp)
                }
            }
            Spacer(Modifier.height(6.dp))
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
            // 보유 판정은 인벤토리 기준(owned) — 뽑기로 얻은 아이템도 보유 중으로 표시된다.
            // 아이템은 1인 1개라 재고 라벨은 두지 않고, 보유 중 표시를 가격 행과 같은 높이로
            // 맞춰 카드 높이가 보유 여부와 무관하게 일정하도록 한다.
            Row(
                modifier = Modifier.height(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                if (item.owned) {
                    Text(
                        text = "보유 중",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.success,
                    )
                } else {
                    SoodalIcon(icon = SoodalIcons.Pearl, tint = colors.accentPurple, size = 11.dp)
                    Text(
                        text = "${item.price}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.accentPurple,
                    )
                }
            }
            if (!item.owned) {
                saleRemainingLabel(item.endAt)?.let { label ->
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.warn,
                    )
                }
            }
        }
    }
}

/**
 * 판매 종료까지 남은 기간 라벨 — 기간 한정 상품에만 표시.
 *
 * @return endAt이 없으면 null, 있으면 "기간 한정 · N일 남음" 형식
 */
private fun saleRemainingLabel(endAt: Long?): String? {
    if (endAt == null) return null
    val remainMs = endAt - System.currentTimeMillis()
    val label = when {
        remainMs <= 0 -> "판매 종료"
        remainMs >= 24 * 60 * 60 * 1000L -> "${remainMs / (24 * 60 * 60 * 1000L)}일 남음"
        remainMs >= 60 * 60 * 1000L -> "${remainMs / (60 * 60 * 1000L)}시간 남음"
        else -> "곧 종료"
    }
    return "기간 한정 · $label"
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
    // (탭바 dim 불필요 — 오버레이 레이어로 호이스팅되어 스크림이 탭바까지 직접 덮는다)

    // 백키 = 구매 확인 닫기(취소) 우선.
    androidx.activity.compose.BackHandler { onCancel() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = com.soodalbbobgi.app.core.ui.SoodalDimAlpha * p.coerceIn(0f, 1f)))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        // 글래스 패널 — 다른 팝업과 동일: 콘텐츠 프로스트 + 보더 + sheen.
        // 등장: 뽑기 팝업과 동일 (graphicsLayer scale 0.9→1 + alpha).
        val panelShape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    // 첫 프레임은 스케일 1로 배치 — 축소 상태 배치는 블러 위치가 어긋난 채 굳는다.
                    val s = com.soodalbbobgi.app.core.ui.motion.popupEnterScale(p)
                    scaleX = s
                    scaleY = s
                    alpha = p.coerceIn(0f, 1f)
                    // alpha<1일 때 오프스크린 합성이 경계 밖 그림자를 잘라 스프링 정착 중
                    // 그림자가 깜빡인다 — 클립 없는 알파 변조로 그린다.
                    compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                }
                .glassShadow(24.dp, colors)
                .glassFrost(colors, panelShape, LocalHazeContent.current)
                .border(1.dp, colors.glassBorder, panelShape),
        ) {
            GlassSheen(panelShape)
            Column(
                // 사방 12dp — 가로형 그림이 상하 여백과 같은 좌우 여백을 갖게 한다.
                // 요소 사이 간격은 바짝 붙여 남은 세로 공간을 전부 아이템 그림에 넘긴다.
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 제목("구매 확인") 없음 — 무엇을 사는지는 아래 아이템 자체가 말해준다.
                // 대신 그만큼 그림을 키워 팝업 높이는 그대로 두고 아이템을 크게 보여준다.
                // 배경 타일도 없앤다 — 에셋이 타일 안에 갇히지 않고 영역을 다 쓴다.
                val artStyle = purchaseArtStyle(item)
                if (!item.imageAsset.isNullOrBlank()) {
                    AssetImage(
                        imageAsset = item.imageAsset,
                        contentDescription = item.name,
                        modifier = when (artStyle) {
                            PurchaseArtStyle.Padded -> Modifier.size(PurchaseArtSize)
                            // 정사각에 가두지 않는다 — 팝업 안쪽 폭을 다 쓰고 높이는 비율대로 따라온다.
                            PurchaseArtStyle.Wide -> Modifier.fillMaxWidth()
                        },
                    )
                } else {
                    // 에셋이 없으면 아이콘 폴백 — 표현 방식과 무관하게 정사각 중앙.
                    Box(
                        modifier = Modifier.size(PurchaseArtSize),
                        contentAlignment = Alignment.Center,
                    ) {
                        SoodalIcon(icon = item.icon, tint = colors.textTertiary, size = PurchaseArtSize / 2)
                    }
                }

                // 여백 없는 그림만 글자와 띄운다. 캐릭터 에셋은 자기 투명 여백이 있어 붙여도 된다.
                if (artStyle == PurchaseArtStyle.Wide) Spacer(Modifier.height(8.dp))

                Column(
                    // 바깥 12dp에 더해 글자·버튼만 예전과 같은 좌우 16dp를 갖는다.
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                if (item.grade != null) {
                    GradeBadge(grade = item.grade)
                    Spacer(Modifier.height(5.dp))
                }

                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary,
                )

                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.5.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                Spacer(Modifier.height(5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Pearl, tint = colors.accentPurple, size = 14.dp)
                    Text(
                        text = "${item.price} 진주",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accentPurple,
                    )
                }

                if (!canAfford) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = "진주가 부족합니다 (보유: ${pearls}개)",
                        fontSize = 11.sp,
                        color = colors.warn,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing.s3),
                ) {
                    SoodalButton(
                        text = "취소",
                        onClick = onCancel,
                        style = ButtonStyle.Secondary,
                        // 흰 프로스트 패널 위 대비 — 불투명 흰색 + 잉크 테두리 (뽑기 '계속'과 같은 룩).
                        backgroundOverride = androidx.compose.ui.graphics.SolidColor(Color.White),
                        modifier = Modifier.weight(1f),
                        // 구매 버튼(Purple 기본 52dp)과 시각적 높이를 맞춘다
                        heightOverride = 52.dp,
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
}
