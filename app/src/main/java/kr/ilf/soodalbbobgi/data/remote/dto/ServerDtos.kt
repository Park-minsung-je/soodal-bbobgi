package kr.ilf.soodalbbobgi.data.remote.dto

// ── Profile Card ──

/** GET /profile-card 응답, PUT /profile-card 요청 (공용) */
data class ServerProfileCard(
    val backgroundItemId: Long? = null,
    val characterItemId: Long? = null,
    val borderItemId: Long? = null,
    val characterX: Float = 0.5f,
    val characterY: Float = 0.5f,
    val characterScale: Float = 1.0f,
    /** 캐릭터 부유 그림자 표시 여부 — 구서버 응답엔 없을 수 있어 nullable (없으면 true). */
    val characterShadow: Boolean? = null,
    val customText: String? = null,
    val textStyle: String = "REGULAR",
    val textAlign: String = "RIGHT",
    val textX: Float = 0.95f,
    val textY: Float = 0.5f,
    val textScaleStep: Int = 3,
    val showStats: Boolean = true,
    /** 이름표 표시 여부 — 구서버 응답엔 없을 수 있어 nullable (없으면 true로 매핑). */
    val showText: Boolean? = null,
    /** 요소별 커스텀 — 구서버 응답엔 없을 수 있어 전부 nullable (없으면 구 필드에서 유도). */
    val showNickname: Boolean? = null,
    val nicknameX: Float? = null,
    val nicknameY: Float? = null,
    val nicknameScaleStep: Int? = null,
    val showTagline: Boolean? = null,
    val taglineX: Float? = null,
    val taglineY: Float? = null,
    val taglineScaleStep: Int? = null,
    val statsX: Float? = null,
    val statsY: Float? = null,
    val statsScaleStep: Int? = null,
    /** 알약 스타일 3종 — 구서버 응답엔 없을 수 있어 nullable (없으면 기본값 매핑). */
    val nicknamePill: String? = null,
    val taglinePill: String? = null,
    val statsPill: String? = null,
    val nicknameColor: String = "#000000",
    val taglineColor: String = "#000000",
    val statsColor: String = "#000000",
    /** 요소별 글꼴/외곽선 — 구서버 응답엔 없을 수 있어 nullable (없으면 전역 값에서 유도). */
    val nicknameStyle: String? = null,
    val taglineStyle: String? = null,
    val statsStyle: String? = null,
    val nicknameOutline: Boolean? = null,
    val taglineOutline: Boolean? = null,
    val statsOutline: Boolean? = null,
    val textOutline: Boolean = false,
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
    val category: String? = null,
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

/** POST /gacha/pull 요청. mixed=true면 매 뽑기마다 서버가 랜덤 박스를 고른다(박스 혼합). */
data class GachaPullRequest(
    val boxId: Long? = null,
    val count: Int,
    val mixed: Boolean = false,
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
    /** 하루치로 합친 심박. 기록에 심박이 없으면 null — 서버는 null 필드를 건너뛴다. */
    val maxHr: Int? = null,
    val minHr: Int? = null,
    val avgHr: Int? = null,
    val hrSeries: String? = null,
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
    val maxHr: Int? = null,
    val minHr: Int? = null,
    val avgHr: Int? = null,
    val hrSeries: String? = null,
    val shellsEarned: Int,
    val createdAt: Long,
)

/**
 * PATCH /swim-logs/by-date/:date/vitals 요청 — 이미 서버에 있는 기록의 빈 심박을 채운다.
 * 서버는 값이 있는 필드를 덮어쓰지 않으므로 여러 번 보내도 안전하다.
 */
data class UpdateVitalsRequest(
    val maxHr: Int? = null,
    val minHr: Int? = null,
    val avgHr: Int? = null,
    val hrSeries: String? = null,
)

/** PATCH /swim-logs/by-date/:date/strokes 요청 — 영법별 거리(m), 합계는 총 거리와 일치해야 함 */
data class UpdateStrokesRequest(
    val strokeFreestyleM: Int,
    val strokeBreastM: Int,
    val strokeBackM: Int,
    val strokeFlyM: Int,
    val strokeMixedM: Int,
    val strokeKickM: Int,
)

/** PATCH /swim-logs/by-date/:date/strokes 응답 */
data class UpdateStrokesData(
    val swimLog: ServerSwimLog,
    /** 영법을 처음 채운 보상 — 지급됐을 때만 실린다 (기록당 1회, 오늘·새벽 유예 한정). */
    val shellReward: ShellRewardData? = null,
)

/** DELETE /swim-logs/by-date/:date 응답 */
data class DeleteSwimLogData(
    val date: String,
    val deleted: Boolean,
)

/** POST /dev/reset-swim-logs 요청 — 오늘부터 거슬러 되돌릴 일수. */
data class DevResetRequest(val days: Int)

/** POST /dev/reset-swim-logs 응답 — 지운 기록 수와 회수한 조개. */
data class DevResetData(
    val from: String,
    val to: String,
    val deletedLogs: Int,
    val revokedShells: Int,
    val newBalance: Int?,
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
