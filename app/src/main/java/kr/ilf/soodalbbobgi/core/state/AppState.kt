package kr.ilf.soodalbbobgi.core.state

import kr.ilf.soodalbbobgi.domain.model.Currency
import kr.ilf.soodalbbobgi.domain.model.GachaBoxWithDrops
import kr.ilf.soodalbbobgi.domain.model.InventoryItem
import kr.ilf.soodalbbobgi.domain.model.Item
import kr.ilf.soodalbbobgi.domain.model.ProfileCard
import kr.ilf.soodalbbobgi.domain.model.ShopListingDomain
import kr.ilf.soodalbbobgi.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application 스코프 in-memory 상태.
 *
 * 서버가 진실의 소스이고, 이 클래스는 단지 서버 응답을 메모리에 두고 ViewModel이
 * StateFlow로 관찰할 수 있도록 제공한다. 영속화는 swim_logs(Room)만 별도이며
 * 그 외 모든 상태는 앱 종료 시 사라진다.
 *
 * 갱신 트리거:
 * - 앱 시작 (Splash) → loadAll() — 전체 메모리 채우기
 * - 화면 진입 (Gacha/Shop/ProfileEditor) → 해당 부분 refresh
 * - 가챠 pull / 상점 구매 / 수영 기록 POST 응답 → applyXxx() 즉시 반영
 * - 사용자 명시 저장 (프로필 카드 / 닉네임 등) → 서버 + 메모리 동시
 */
@Singleton
class AppState @Inject constructor() {

    // ─── 프로필 (앱 시작 + 수정 시) ─────────────────────────
    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile

    // ─── 재화·천장 (자주 변동) ───────────────────────────────
    private val _currency = MutableStateFlow(Currency())
    val currency: StateFlow<Currency> = _currency

    // ─── 인벤토리 ────────────────────────────────────────────
    private val _inventory = MutableStateFlow<List<InventoryItem>>(emptyList())
    val inventory: StateFlow<List<InventoryItem>> = _inventory

    // ─── 아이템 마스터 카탈로그 (가챠 박스 + 상점 응답에서 모음) ──
    private val _items = MutableStateFlow<Map<Long, Item>>(emptyMap())
    val items: StateFlow<Map<Long, Item>> = _items

    // ─── 가챠 박스 (화면 진입 시 새로고침) ───────────────────
    private val _gachaBoxes = MutableStateFlow<List<GachaBoxWithDrops>>(emptyList())
    val gachaBoxes: StateFlow<List<GachaBoxWithDrops>> = _gachaBoxes

    // ─── 상점 진열 (Shop 진입 시 새로고침) ────────────────────
    private val _shopListings = MutableStateFlow<List<ShopListingDomain>>(emptyList())
    val shopListings: StateFlow<List<ShopListingDomain>> = _shopListings

    // ─── 프로필 카드 설정 ────────────────────────────────────
    private val _profileCard = MutableStateFlow<ProfileCard?>(null)
    val profileCard: StateFlow<ProfileCard?> = _profileCard

    // ─── 조개 보상 팝업 (Splash → Home 전달) ──────────────────
    private val _pendingShellReward = MutableStateFlow(0)
    val pendingShellReward: StateFlow<Int> = _pendingShellReward

    // ─── Apply 메서드들 ──────────────────────────────────────

    fun applyProfile(p: UserProfile) { _profile.value = p }

    fun applyCurrency(c: Currency) { _currency.value = c }

    fun applyInventory(items: List<InventoryItem>) { _inventory.value = items }

    /** 가챠 pull 결과로 인벤토리에 새 아이템들을 추가하고 currency 갱신. */
    fun applyNewInventoryItems(newItems: List<InventoryItem>) {
        _inventory.update { it + newItems }
    }

    /** 아이템 마스터 캐시에 신규 항목들을 병합 (덮어쓰기). */
    fun mergeItems(newItems: List<Item>) {
        _items.update { existing -> existing + newItems.associateBy { it.id } }
    }

    fun applyGachaBoxes(boxes: List<GachaBoxWithDrops>) {
        _gachaBoxes.value = boxes
        // 박스 안에 포함된 아이템 메타들을 마스터 캐시에 병합
        mergeItems(boxes.flatMap { it.drops }.map { it.item })
    }

    fun applyShopListings(listings: List<ShopListingDomain>) {
        _shopListings.value = listings
    }

    fun applyProfileCard(c: ProfileCard) { _profileCard.value = c }

    /** Splash 동기화 중 지급된 조개 누적 (Home에서 팝업 후 consumePendingShellReward). */
    fun addPendingShellReward(amount: Int) {
        _pendingShellReward.update { it + amount }
    }

    fun consumePendingShellReward(): Int {
        val v = _pendingShellReward.value
        _pendingShellReward.value = 0
        return v
    }

    /** 로그아웃 시 모든 메모리 상태 초기화. */
    fun clear() {
        _profile.value = null
        _currency.value = Currency()
        _inventory.value = emptyList()
        _items.value = emptyMap()
        _gachaBoxes.value = emptyList()
        _shopListings.value = emptyList()
        _profileCard.value = null
        _pendingShellReward.value = 0
    }
}
