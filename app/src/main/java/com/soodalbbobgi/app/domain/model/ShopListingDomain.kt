package com.soodalbbobgi.app.domain.model

/**
 * 상점 진열 상품. 서버 GET /shop 응답을 도메인으로 표현.
 *
 * @property productType "item" 또는 "box"
 * @property product productType에 따라 [Item] 또는 [GachaBoxWithDrops]의 메타
 * @property maxPerUser 평생 누적 구매 제한 (null=무제한)
 * @property maxPerPeriod 기간당 구매 제한 (null=기간 제한 없음)
 * @property periodType "daily" / "weekly" / "monthly" (null=영구)
 */
data class ShopListingDomain(
    val id: Long,
    val productType: String,
    val product: ShopProduct,
    val pearlPrice: Int,
    val maxPerUser: Int?,
    val purchasedTotal: Int,
    val maxPerPeriod: Int?,
    val periodType: String?,
    val purchasedThisPeriod: Int,
    val periodResetAt: Long?,
    val startAt: Long?,
    val endAt: Long?,
    val canBuy: Boolean,
)

/** productType에 따라 다른 product 정보를 담는 sealed (지금은 메타만 평면 보관). */
data class ShopProduct(
    val id: Long,
    val name: String,
    val description: String? = null,
    val grade: Grade? = null,
    val category: String? = null,
    val imageAsset: String? = null,
    val iconAsset: String? = null,
    val isLimited: Boolean = false,
)
