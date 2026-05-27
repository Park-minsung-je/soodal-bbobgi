package com.soodalbbobgi.app.data.remote.dto

// ── Shop ──

/** POST /shop/purchase 요청 */
data class ShopPurchaseRequest(
    val boxItemId: Long,
    val price: Int,
)

/** POST /shop/purchase 응답 */
data class ShopPurchaseData(
    val inventoryItem: ServerInventoryItem,
    val currency: ServerCurrency,
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
    val weight: Int,
    val imageAsset: String,
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
    val boxItemId: Long,
    val grade: String,
    val category: String,
    val isEquippedAs: String,
    val acquiredAt: Long,
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

/** GET /swim-logs/stats 응답 */
data class SwimStatsData(
    val totalDistanceMeters: Int,
    val swimCount: Int,
    val totalCalories: Int,
    val totalShellsEarned: Int,
)
