package com.soodalbbobgi.app.data.remote.dto

// ── Profile Card ──

/** GET /profile-card 응답, PUT /profile-card 요청 (공용) */
data class ServerProfileCard(
    val backgroundItemId: Long? = null,
    val characterItemId: Long? = null,
    val borderItemId: Long? = null,
    val characterX: Float = 0.16f,
    val characterY: Float = 0.06f,
    val characterScale: Float = 0.70f,
    val customText: String? = null,
    val textStyle: String = "REGULAR",
    val textAlign: String = "RIGHT",
    val textX: Float = 0.95f,
    val textY: Float = 0.5f,
    val textScaleStep: Int = 3,
    val showStats: Boolean = true,
    val nicknameColor: String = "#FFFFFF",
    val taglineColor: String = "#FFFFFF",
    val statsColor: String = "#00F5FF",
    val lastEditedAt: Long? = null,
)

/** PUT /profile-card 응답 */
data class ProfileCardUpdateData(
    val lastEditedAt: Long,
)

// ── Shop ──

/** POST /shop/purchase 요청 */
data class ShopPurchaseRequest(
    val shopListingId: Long,
)

/** POST /shop/purchase 응답 */
data class ShopPurchaseData(
    val shopListingId: Long,
    val productType: String,
    val inventoryItemId: Long?,
    val gachaHistoryId: Long?,
    val acquiredItems: List<ServerGachaResult>?,
    val currency: ServerCurrency,
)

/** GET /shop 응답 */
data class ShopListingsData(
    val listings: List<ServerShopListing>,
)

data class ServerShopListing(
    val id: Long,
    val productType: String,                  // 'item' | 'box'
    val product: ServerShopProduct,
    val pearlPrice: Int,
    val maxPerUser: Int?,
    val purchasedTotal: Int,
    val maxPerPeriod: Int?,
    val periodType: String?,                  // 'daily' | 'weekly' | 'monthly'
    val purchasedThisPeriod: Int,
    val periodResetAt: Long?,
    val startAt: Long?,
    val endAt: Long?,
    val canBuy: Boolean,
)

/** product 필드는 item이면 ServerItemProduct, box면 ServerBoxProduct 형태 */
data class ServerShopProduct(
    val id: Long,
    val itemKey: String? = null,        // item만
    val name: String,
    val grade: String? = null,          // item만
    val category: String? = null,
    val imageAsset: String? = null,
    val isLimited: Boolean? = null,     // item만
    val description: String? = null,    // box만
    val iconAsset: String? = null,      // box만
)

// ── User ──

/** PATCH /users/me 요청 */
data class UpdateUserRequest(
    val nickname: String? = null,
    val gender: String? = null,
    val ageRange: String? = null,
)

// ── Gacha ──

/** GET /gacha/boxes 응답 */
data class GachaBoxesData(
    val boxes: List<ServerGachaBox>,
)

data class ServerGachaBox(
    val id: Long,
    val name: String,
    val description: String,
    val category: String,
    val iconAsset: String?,
    val shellCost: Int,
    val tenPullCost: Int,
    val items: List<ServerGachaBoxItem>,
)

data class ServerGachaBoxItem(
    val id: Long,
    val itemKey: String,
    val name: String,
    val grade: String,
    val category: String? = null,
    val weight: Int,
    val imageAsset: String?,
    val isLimited: Boolean? = null,
)

/** POST /gacha/pull 요청 */
data class GachaPullRequest(
    val boxId: Long,
    val count: Int,
)

/** POST /gacha/pull 응답 */
data class GachaPullData(
    val results: List<ServerGachaResult>,
    val currency: ServerCurrency,
)

data class ServerGachaResult(
    val item: ServerGachaBoxItem,
    val wasNew: Boolean,
    val pearlsEarned: Int,
    val shellsSpent: Int,
    val pityCountAtPull: Int,
    val historyId: Long? = null,
    val inventoryItemId: Long? = null,
)

data class ServerCurrency(
    val shellBalance: Int,
    val pearlBalance: Int,
    val pityCounter: Int,
)

// ── Inventory ──

/** GET /inventory 응답 */
data class InventoryData(
    val items: List<ServerInventoryItem>,
)

data class ServerInventoryItem(
    val id: Long,
    val itemId: Long,
    val grade: String,
    val category: String,
    val isEquippedAs: String,
    val acquiredAt: Long,
)

// ── Items 마스터 카탈로그 ──

/** GET /items 응답 */
data class ItemsData(
    val items: List<ServerItem>,
)

data class ServerItem(
    val id: Long,
    val itemKey: String,
    val name: String,
    val grade: String,
    val category: String,
    val imageAsset: String?,
    val isLimited: Boolean = false,
    val isDefault: Boolean = false,
)

// ── Swim Logs ──

/** POST /swim-logs 요청 */
data class SwimLogRequest(
    val date: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
    val strokeFreestyleM: Int,
    val strokeBreastM: Int,
    val strokeBackM: Int,
    val strokeFlyM: Int,
    val strokeMixedM: Int,
    val strokeKickM: Int,
    val source: String,
)

/** POST /swim-logs 응답 */
data class SwimLogResponseData(
    val swimLog: ServerSwimLog,
    val shellReward: ShellRewardData?,
)

data class ShellRewardData(
    val earned: Int,
    val newBalance: Int,
)

/** GET /swim-logs 응답 */
data class SwimLogsData(
    val items: List<ServerSwimLog>,
)

data class ServerSwimLog(
    val id: String,
    val date: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
    val strokeFreestyleM: Int,
    val strokeBreastM: Int,
    val strokeBackM: Int,
    val strokeFlyM: Int,
    val strokeMixedM: Int,
    val strokeKickM: Int,
    val source: String,
    val shellsEarned: Int,
    val createdAt: Long,
)

/** DELETE /swim-logs/by-date/:date 응답 */
data class DeleteSwimLogData(
    val date: String,
    val deleted: Boolean,
)

/** GET /swim-logs/stats 응답 */
data class SwimStatsData(
    val totalDistanceMeters: Int,
    val swimCount: Int,
    val totalCalories: Int,
    val totalShellsEarned: Int,
)

// ── Assets ──

/** GET /assets/version 응답 */
data class AssetManifestData(
    val version: String,
    val updatedAt: Long,
    val files: List<ServerAssetFile>,
)

data class ServerAssetFile(
    val path: String,
    val hash: String,
    val size: Long,
)
