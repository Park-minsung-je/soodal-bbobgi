package com.soodalbbobgi.app.core.state

import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ServerCurrency
import com.soodalbbobgi.app.data.remote.dto.ServerGachaBox
import com.soodalbbobgi.app.data.remote.dto.ServerGachaResult
import com.soodalbbobgi.app.data.remote.dto.ServerInventoryItem
import com.soodalbbobgi.app.data.remote.dto.ServerItem
import com.soodalbbobgi.app.data.remote.dto.ServerProfileCard
import com.soodalbbobgi.app.data.remote.dto.ServerShopListing
import com.soodalbbobgi.app.data.remote.dto.UserData
import com.soodalbbobgi.app.domain.model.Currency
import com.soodalbbobgi.app.domain.model.GachaBoxWithDrops
import com.soodalbbobgi.app.domain.model.GachaDrop
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.model.Item
import com.soodalbbobgi.app.domain.model.ProfileCard
import com.soodalbbobgi.app.domain.model.ShopListingDomain
import com.soodalbbobgi.app.domain.model.ShopProduct
import com.soodalbbobgi.app.domain.model.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 서버 API를 호출해서 [AppState]를 갱신하는 단일 책임 컴포넌트.
 *
 * 각 trigger마다 호출할 메서드:
 * - 앱 시작 (Splash) → [loadAll]
 * - Gacha/ProfileEditor 진입 → [refreshGachaBoxes] (+ [refreshInventory])
 * - Shop 진입 → [refreshShop] (+ [refreshCurrency])
 * - 가챠 pull 후 → [applyGachaPullResult]
 * - 상점 구매 후 → [applyShopPurchaseResult]
 * - 수영 기록 POST 후 → [applyShellReward]
 */
@Singleton
class AppStateLoader @Inject constructor(
    private val api: SoodalApi,
    private val appState: AppState,
    private val userSession: UserSession,
) {

    // ensureHydrated의 동시 호출 직렬화 (탭 진입이 겹쳐도 중복 loadAll 방지)
    private val hydrationMutex = Mutex()

    /**
     * 메모리 상태가 비어 있으면(=프로세스 사망 후 Splash를 거치지 않고 복원된 경우)
     * 서버에서 전체 상태를 다시 채운다. 이미 채워져 있으면 아무것도 하지 않는다.
     *
     * 토큰은 EncryptedSharedPreferences에 영속되므로 프로세스가 죽었다 살아나도
     * `getMe()`로 재인증·재수화가 가능하다. 정상 콜드스타트(Splash→loadAll→Home)에선
     * 이미 profile이 채워져 있어 no-op이다.
     */
    suspend fun ensureHydrated() {
        if (appState.profile.value != null) return
        hydrationMutex.withLock {
            // 락 획득 사이에 다른 호출이 먼저 채웠을 수 있으니 재확인
            if (appState.profile.value != null) return
            loadAll()
        }
    }

    /** Splash에서 호출. 로그인된 사용자에 대해 전체 메모리 상태를 채운다. */
    suspend fun loadAll(): Result<Unit> = runCatching {
        coroutineScope {
            // 병렬 호출
            val meDef = async { api.getMe() }
            val itemsDef = async { api.getItems() }
            val inventoryDef = async { api.getInventory() }
            val gachaDef = async { api.getGachaBoxes() }
            val profileCardDef = async { api.getProfileCard() }

            val meRes = meDef.await()
            val itemsRes = itemsDef.await()
            val inventoryRes = inventoryDef.await()
            val gachaRes = gachaDef.await()
            val profileCardRes = profileCardDef.await()

            if (meRes.success && meRes.data != null) {
                userSession.setAuthenticatedUser(meRes.data.id)
                applyUserData(meRes.data)
            }
            // items 마스터 먼저 채우면 inventory/gacha/shop이 메타 룩업 가능
            if (itemsRes.success && itemsRes.data != null) {
                appState.mergeItems(itemsRes.data.items.map { it.toDomain() })
            }
            if (inventoryRes.success && inventoryRes.data != null) {
                appState.applyInventory(inventoryRes.data.items.map { it.toDomain() })
            }
            if (gachaRes.success && gachaRes.data != null) {
                appState.applyGachaBoxes(gachaRes.data.boxes.map { it.toDomain() })
            }
            if (profileCardRes.success && profileCardRes.data != null) {
                appState.applyProfileCard(profileCardRes.data.toDomain(userSession.userId))
            }
        }
    }.onFailure { Timber.w(it, "AppStateLoader.loadAll 실패") }

    /** items 마스터 카탈로그 단독 새로고침. */
    suspend fun refreshItems(): Result<Unit> = runCatching {
        val res = api.getItems()
        if (res.success && res.data != null) {
            appState.mergeItems(res.data.items.map { it.toDomain() })
        }
    }

    suspend fun refreshCurrency(): Result<Unit> = runCatching {
        val res = api.getMe()
        if (res.success && res.data != null) applyUserData(res.data)
    }

    suspend fun refreshInventory(): Result<Unit> = runCatching {
        val res = api.getInventory()
        if (res.success && res.data != null) {
            appState.applyInventory(res.data.items.map { it.toDomain() })
        }
    }

    suspend fun refreshGachaBoxes(): Result<Unit> = runCatching {
        val res = api.getGachaBoxes()
        if (res.success && res.data != null) {
            appState.applyGachaBoxes(res.data.boxes.map { it.toDomain() })
        }
    }

    suspend fun refreshShop(): Result<Unit> = runCatching {
        val res = api.getShop()
        if (res.success && res.data != null) {
            appState.applyShopListings(res.data.listings.map { it.toDomain() })
            // 상점 product가 item인 경우 마스터 캐시에도 병합
            val itemsFromShop = res.data.listings.mapNotNull { l ->
                if (l.productType != "item") return@mapNotNull null
                val p = l.product
                Item(
                    id = p.id, itemKey = p.itemKey ?: "", name = p.name,
                    grade = Grade.fromString(p.grade ?: "N"),
                    category = p.category ?: "", imageAsset = p.imageAsset,
                    isLimited = p.isLimited == true,
                )
            }
            appState.mergeItems(itemsFromShop)
        }
    }

    suspend fun refreshProfileCard(): Result<Unit> = runCatching {
        val res = api.getProfileCard()
        if (res.success && res.data != null) {
            appState.applyProfileCard(res.data.toDomain(userSession.userId))
        }
    }

    // ─── Write-side 즉시 반영 ─────────────────────────────────

    fun applyServerCurrency(c: ServerCurrency) {
        appState.applyCurrency(Currency(
            shellBalance = c.shellBalance,
            pearlBalance = c.pearlBalance,
            pityCounter = c.pityCounter,
            lastShellGrantDate = appState.currency.value.lastShellGrantDate,
        ))
    }

    /** 가챠 pull 결과를 메모리에 반영 (currency + 인벤토리 신규 아이템). */
    fun applyGachaResults(results: List<ServerGachaResult>, currency: ServerCurrency) {
        applyServerCurrency(currency)
        val newItems = results.filter { it.wasNew }.mapNotNull { r ->
            val invId = r.inventoryItemId ?: return@mapNotNull null
            InventoryItem(
                id = invId,
                userId = userSession.userId,
                itemId = r.item.id,
                grade = Grade.fromString(r.item.grade),
                category = r.item.category ?: "",
                acquiredAt = System.currentTimeMillis(),
            )
        }
        appState.applyNewInventoryItems(newItems)
        appState.mergeItems(results.map { r ->
            Item(
                id = r.item.id, itemKey = r.item.itemKey, name = r.item.name,
                grade = Grade.fromString(r.item.grade),
                category = r.item.category ?: "",
                imageAsset = r.item.imageAsset,
                isLimited = r.item.isLimited == true,
            )
        })
    }

    /** 조개 지급 (수영 기록 POST 응답)만 currency에 반영. */
    fun applyShellReward(newBalance: Int) {
        appState.applyCurrency(appState.currency.value.copy(shellBalance = newBalance))
    }

    /** 프로필 정보 수정 후 메모리 갱신. */
    fun applyProfileUpdate(data: UserData) {
        applyUserData(data)
    }

    fun applyProfileCardSaved(card: ProfileCard) {
        appState.applyProfileCard(card)
    }

    // ─── 매핑 헬퍼 ────────────────────────────────────────────

    private fun applyUserData(u: UserData) {
        appState.applyProfile(UserProfile(
            id = u.id,
            nickname = u.nickname,
            gender = u.gender,
            ageRange = u.ageRange,
            authProvider = u.authProvider,
        ))
        appState.applyCurrency(Currency(
            shellBalance = u.shellBalance,
            pearlBalance = u.pearlBalance,
            pityCounter = u.pityCounter,
            lastShellGrantDate = u.lastShellGrantDate,
        ))
    }
}

// ─── DTO → 도메인 매핑 (파일 내 private) ──────────────────────

private fun ServerInventoryItem.toDomain() = InventoryItem(
    id = id, userId = "", // 호출자 컨텍스트 미사용. 필요하면 별도 전달.
    itemId = itemId,
    grade = Grade.fromString(grade),
    category = category,
    isEquippedAs = isEquippedAs,
    acquiredAt = acquiredAt,
)

private fun ServerItem.toDomain() = Item(
    id = id, itemKey = itemKey, name = name,
    grade = Grade.fromString(grade), category = category,
    imageAsset = imageAsset, isLimited = isLimited, isDefault = isDefault,
)

private fun ServerGachaBox.toDomain(): GachaBoxWithDrops {
    val drops = items.map { boxItem ->
        GachaDrop(
            item = Item(
                id = boxItem.id,
                itemKey = boxItem.itemKey,
                name = boxItem.name,
                grade = Grade.fromString(boxItem.grade),
                category = boxItem.category ?: category ?: "",
                imageAsset = boxItem.imageAsset,
                isLimited = boxItem.isLimited == true,
            ),
            weight = boxItem.weight,
        )
    }
    return GachaBoxWithDrops(
        id = id, name = name, description = description, category = category,
        iconAsset = iconAsset, shellCost = shellCost, tenPullCost = tenPullCost,
        drops = drops,
    )
}

private fun ServerShopListing.toDomain() = ShopListingDomain(
    id = id, productType = productType,
    product = ShopProduct(
        id = product.id, name = product.name,
        description = product.description,
        grade = product.grade?.let { Grade.fromString(it) },
        category = product.category,
        imageAsset = product.imageAsset,
        iconAsset = product.iconAsset,
        isLimited = product.isLimited == true,
    ),
    pearlPrice = pearlPrice,
    maxPerUser = maxPerUser, purchasedTotal = purchasedTotal,
    maxPerPeriod = maxPerPeriod, periodType = periodType,
    purchasedThisPeriod = purchasedThisPeriod,
    periodResetAt = periodResetAt,
    startAt = startAt, endAt = endAt, canBuy = canBuy,
)

private fun ServerProfileCard.toDomain(userId: String): ProfileCard {
    // 요소별 필드가 없는 구서버 응답이면 구 블록 필드(textX/Y·showText)에서 근사 유도한다.
    // 구 앵커는 정렬 기준 모서리였으므로 중심 앵커로 살짝 안쪽으로 당긴다.
    val legacyCx = (if (textAlign == "LEFT") textX + 0.12f else textX - 0.12f).coerceIn(0f, 1f)
    return ProfileCard(
        userId = userId,
        backgroundItemId = backgroundItemId,
        characterItemId = characterItemId,
        borderItemId = borderItemId,
        characterX = characterX,
        characterY = characterY,
        characterScale = characterScale,
        characterShadow = characterShadow ?: true,
        customText = customText ?: "",
        textStyle = textStyle,
        textAlign = textAlign,
        textX = textX,
        textY = textY,
        textScaleStep = textScaleStep,
        showStats = showStats,
        showText = showText ?: true,
        showNickname = showNickname ?: showText ?: true,
        nicknameX = nicknameX ?: legacyCx,
        nicknameY = nicknameY ?: (textY - 0.09f).coerceIn(0f, 1f),
        nicknameScaleStep = nicknameScaleStep ?: textScaleStep,
        showTagline = showTagline ?: showText ?: true,
        taglineX = taglineX ?: legacyCx,
        taglineY = taglineY ?: (textY + 0.08f).coerceIn(0f, 1f),
        taglineScaleStep = taglineScaleStep ?: textScaleStep,
        statsX = statsX ?: 0.16f,
        statsY = statsY ?: 0.90f,
        statsScaleStep = statsScaleStep ?: 3,
        nicknamePill = nicknamePill ?: "WHITE",
        taglinePill = taglinePill ?: "NONE",
        statsPill = statsPill ?: "BLUR",
        nicknameColor = nicknameColor,
        taglineColor = taglineColor,
        statsColor = statsColor,
        // 요소별 글꼴/외곽선이 없는 구서버 응답이면 전역 값을 모든 요소에 적용한다
        nicknameStyle = nicknameStyle ?: textStyle,
        taglineStyle = taglineStyle ?: textStyle,
        statsStyle = statsStyle ?: textStyle,
        nicknameOutline = nicknameOutline ?: textOutline,
        taglineOutline = taglineOutline ?: textOutline,
        statsOutline = statsOutline ?: textOutline,
        textOutline = textOutline,
    )
}
