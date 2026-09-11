package kr.ilf.soodalbbobgi.presentation.gacha

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.GachaPullRequest
import kr.ilf.soodalbbobgi.data.remote.dto.ServerGachaResult
import kr.ilf.soodalbbobgi.domain.model.GachaBoxWithDrops
import kr.ilf.soodalbbobgi.domain.model.Grade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

enum class GachaPhase { Idle, Spinning, Reeling, Celebrating, Result }

data class BoxInfo(
    val id: String,
    val icon: SoodalIcons,
    val label: String,
    val color: Color,
    /** 관리자에서 등록한 상자 일러스트 에셋 경로. 없으면 아이콘 폴백. */
    val iconAsset: String? = null,
)

data class GachaResultItem(
    val name: String,
    val grade: Grade,
    val kind: String,
    val isNew: Boolean,
    val pearlsEarned: Int,
    val imageAsset: String? = null,
)

data class GachaUiState(
    val shells: Int = 0,
    val pearls: Int = 0,
    val phase: GachaPhase = GachaPhase.Idle,
    val offset: Float = 0f,
    val results: List<GachaResultItem> = emptyList(),
    /** Reeling 단계에서 로프를 타고 올라오는 상자 (뽑힌 박스). */
    val risingBox: BoxInfo? = null,
)

val GACHA_BOXES = listOf(
    BoxInfo("char", SoodalIcons.Box, "캐릭터 상자", Color(0xFFFFD60A)),
    BoxInfo("bg", SoodalIcons.Wave, "배경 상자", Color(0xFF00F5FF)),
    BoxInfo("frame", SoodalIcons.Frame, "테두리 상자", Color(0xFFBF5AF2)),
    BoxInfo("mystery", SoodalIcons.Gift, "랜덤 상자", Color(0xFFE0B0FF)),
)

/**
 * 룰렛이 멈출 상자를 서버 결과로 정한다 — 첫 결과가 나온 박스(boxId), 그게 없으면
 * 첫 아이템 카테고리와 같은 박스. 둘 다 못 찾으면 null(호출부가 임의 상자로 연출).
 * 혼합 뽑기는 서버가 박스를 고르므로 앱이 먼저 고르면 상자와 아이템이 어긋난다.
 *
 * @param boxes 뽑기 화면의 활성 박스 목록(룰렛 순서)
 * @param results 서버 뽑기 결과 (10연은 첫 결과 기준)
 */
internal fun resolveStopBox(boxes: List<GachaBoxWithDrops>, results: List<ServerGachaResult>): GachaBoxWithDrops? {
    val first = results.firstOrNull() ?: return null
    return boxes.firstOrNull { it.id == first.boxId }
        ?: first.item.category?.let { cat -> boxes.firstOrNull { it.category == cat } }
}

/** 인양 장면의 상자 슬롯 폭 (상자 92dp + 간격 26dp). */
const val ITEM_WIDTH_WITH_GAP = 118f

private const val SPIN_ACCEL_PHASE = 0.25f
private const val SPIN_ACCEL_PROGRESS = 0.4f
private const val SPIN_DECEL_POWER = 4
private const val SPIN_DURATION_MS = 4500L

/** 인양 연출 시간 — 갈고리 내리기 + 상자를 걸어 수면까지 감아올리기. */
const val REEL_DURATION_MS = 1600L

/** 인양 직후 수달이 "우와! ~상자야!" 외치는 시간 — 이 뒤에 결과 팝업이 뜬다. */
private const val CELEBRATE_MS = 900L

/**
 * 뽑기 화면 ViewModel.
 *
 * - currency는 [AppState.currency]에서 관찰
 * - 박스 목록은 [AppState.gachaBoxes]에서 관찰 (진입 시 [AppStateLoader.refreshGachaBoxes]로 새로고침)
 * - 뽑기 pull 결과는 [AppStateLoader.applyGachaResults]로 메모리에 즉시 반영
 */
@HiltViewModel
class GachaViewModel @Inject constructor(
    private val userSession: UserSession,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val soodalApi: SoodalApi,
) : ViewModel() {

    private val _localState = MutableStateFlow(LocalGachaState())

    data class LocalGachaState(
        val phase: GachaPhase = GachaPhase.Idle,
        /** 서버 뽑기 응답을 기다리는 중 — 룰렛은 계속 천천히 돌고 버튼만 잠근다. */
        val awaitingServer: Boolean = false,
        val offset: Float = 0f,
        val results: List<GachaResultItem> = emptyList(),
        val risingBox: BoxInfo? = null,
    )

    val uiState: StateFlow<GachaUiState> = combine(
        appState.currency, _localState,
    ) { currency, local ->
        GachaUiState(
            shells = currency.shellBalance,
            pearls = currency.pearlBalance,
            phase = local.phase,
            offset = local.offset,
            results = local.results,
            risingBox = local.risingBox,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GachaUiState())

    val boxes: StateFlow<List<BoxInfo>> = appState.gachaBoxes.map { list ->
        list.map { it.toBoxInfo() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GACHA_BOXES)

    init {
        // 화면 진입 시 박스 목록 + 인벤토리 + 잔액 새로고침
        viewModelScope.launch {
            // 프로세스 사망 후 이 탭으로 복원됐을 수 있으니 먼저 전체 재수화
            appStateLoader.ensureHydrated()
            appStateLoader.refreshGachaBoxes()
            appStateLoader.refreshInventory()
            appStateLoader.refreshCurrency()
        }
        // Idle 룰렛 자동 회전 (코루틴 cancel 시 종료되도록 isActive 체크)
        viewModelScope.launch {
            while (isActive) {
                delay(16)
                // 서버 응답을 기다리는 동안에도 멈추지 않게 — 정지 상자가 정해진 뒤 감속이 이어진다.
                val local = _localState.value
                if (local.phase == GachaPhase.Idle || local.awaitingServer) {
                    _localState.update { it.copy(offset = it.offset + 0.25f) }
                }
            }
        }
    }

    /**
     * 서버 뽑기(매 뽑기마다 랜덤 박스)를 먼저 끝내고, 그 결과가 나온 상자에서 룰렛이 멈추도록
     * 감속 연출을 돌린다. 응답을 기다리는 동안 룰렛은 계속 천천히 돈다.
     */
    fun spin(count: Int) {
        // 할인 없음: 단발 1, 10연 10 (1회당 조개 1개)
        val cost = count
        val s = uiState.value
        if (s.shells < cost || s.phase != GachaPhase.Idle) return

        _localState.update { it.copy(phase = GachaPhase.Spinning, awaitingServer = true) }

        viewModelScope.launch {
            val activeBoxes: List<GachaBoxWithDrops> = appState.gachaBoxes.value
            if (activeBoxes.isEmpty()) {
                _localState.update { it.copy(phase = GachaPhase.Idle, awaitingServer = false) }
                return@launch
            }

            // 정지 상자를 알아야 감속 목표를 잡을 수 있으므로 서버 결과부터 받는다.
            val response = try {
                withContext(Dispatchers.IO) { soodalApi.gachaPull(GachaPullRequest(count = count, mixed = true)) }
            } catch (e: Exception) {
                Timber.w(e, "뽑기 요청 실패")
                null
            }
            val data = response?.takeIf { it.success }?.data
            val results = data?.results ?: run {
                _localState.update { it.copy(phase = GachaPhase.Idle, awaitingServer = false, risingBox = null) }
                return@launch
            }
            // 서버가 고른 상자에서 멈춘다 — 못 찾으면(구서버 응답) 임의 상자로 연출만 한다.
            val selectedBox = resolveStopBox(activeBoxes, results) ?: activeBoxes.random()
            val resultBoxIndex = activeBoxes.indexOf(selectedBox)
            _localState.update { it.copy(awaitingServer = false) }
            val startOffset = _localState.value.offset

            val boxCount = activeBoxes.size.coerceAtLeast(1)
            val currentSlot = (startOffset / ITEM_WIDTH_WITH_GAP).toInt()
            val extraSlots = (6 + (0..2).random()) * boxCount
            var targetSlot = currentSlot + extraSlots
            while (((targetSlot % boxCount) + boxCount) % boxCount != resultBoxIndex) {
                targetSlot++
            }
            val targetOffset = targetSlot * ITEM_WIDTH_WITH_GAP
            val totalDistance = targetOffset - startOffset

            val startTime = System.currentTimeMillis()
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed >= SPIN_DURATION_MS) break
                val k = (elapsed.toFloat() / SPIN_DURATION_MS).coerceIn(0f, 1f)
                val eased = if (k < SPIN_ACCEL_PHASE) {
                    val t = k / SPIN_ACCEL_PHASE
                    SPIN_ACCEL_PROGRESS * t * t
                } else {
                    val t = (k - SPIN_ACCEL_PHASE) / (1f - SPIN_ACCEL_PHASE)
                    val oneMinusT = 1f - t
                    var decel = oneMinusT
                    repeat(SPIN_DECEL_POWER - 1) { decel *= oneMinusT }
                    SPIN_ACCEL_PROGRESS + (1f - SPIN_ACCEL_PROGRESS) * (1f - decel)
                }
                _localState.update { it.copy(offset = startOffset + totalDistance * eased) }
                delay(16)
            }
            _localState.update { it.copy(offset = targetOffset) }

            // 인양 연출 — 멈춘 상자가 로프를 타고 수면으로 끌려 올라간다
            _localState.update { it.copy(phase = GachaPhase.Reeling, risingBox = selectedBox.toBoxInfo()) }
            delay(REEL_DURATION_MS)

            run {
                val batch = results.map { r ->
                    GachaResultItem(
                        name = r.item.name,
                        grade = Grade.fromString(r.item.grade),
                        // 혼합 뽑기: 아이템별 실제 출처 박스 카테고리 사용
                        kind = r.item.category ?: selectedBox.category ?: "",
                        isNew = r.wasNew,
                        pearlsEarned = r.pearlsEarned,
                        imageAsset = r.item.imageAsset,
                    )
                }
                // 서버 응답으로 메모리 즉시 반영 (currency + 신규 인벤토리)
                appStateLoader.applyGachaResults(results, data.currency)

                // 수달이 건진 상자를 자랑할 시간을 준 뒤 결과 팝업을 띄운다
                _localState.update {
                    it.copy(phase = GachaPhase.Celebrating, results = batch)
                }
                delay(CELEBRATE_MS)
                _localState.update { it.copy(phase = GachaPhase.Result) }
            }
        }
    }

    fun closeResults() {
        _localState.update { it.copy(phase = GachaPhase.Idle, results = emptyList(), risingBox = null) }
    }

    /** 도메인 박스 → 룰렛/인양 연출용 UI 모델. */
    private fun GachaBoxWithDrops.toBoxInfo() = BoxInfo(
        id = id.toString(),
        icon = iconFor(category),
        label = name,
        color = colorFor(category),
        iconAsset = iconAsset,
    )

    // category가 null이면 (서버에서 필드 제거) else 분기 기본값으로 떨어진다
    private fun iconFor(category: String?): SoodalIcons = when (category) {
        "char" -> SoodalIcons.Box
        "bg" -> SoodalIcons.Wave
        "frame" -> SoodalIcons.Frame
        else -> SoodalIcons.Gift
    }

    // 상자 일러스트 톤에 맞춘 카테고리 색 (디자인 확정값), null이면 else 색
    private fun colorFor(category: String?): Color = when (category) {
        "char" -> Color(0xFFC98A4B)
        "bg" -> Color(0xFF6FB04A)
        "frame" -> Color(0xFFE0B24A)
        else -> Color(0xFF7C5BD0)
    }
}
