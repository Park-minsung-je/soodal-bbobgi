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

// 룰렛 운동 모델 — 2026-09-11. 사용자가 튜너에서 그린 속도 곡선(초, 칸/초)을 키프레임으로 그대로 쓴다.
// 점 사이는 넘침 없는 단조 3차 보간. 세 구간으로 나뉜다:
//   머리 [0, 무릎] — 매번 같다 · 중간 [무릎, 멈춤 구간 시작] — 결과 상자 칸까지 남은 거리에 맞춰 시간을
//   배율로 늘린다(속도 값은 그대로, 경사만 완만) · 멈춤 [멈춤 구간 시작, 끝] — 마지막 세 점, 늘리지 않는다.
private val SPIN_CURVE: List<Pair<Float, Float>> = listOf(
    0.00f to 0.1f, 0.40f to 9.8f, 1.20f to 12.2f, 2.06f to 10.6f, 2.54f to 5.3f,
    2.89f to 2.9f, 3.26f to 1.6f, 3.73f to 0.8f, 4.16f to 0.4f, 5.64f to 0.0f,
).map { (t, slotsPerSec) -> t to slotsPerSec * ITEM_WIDTH_WITH_GAP }
/** 무릎 — 늘어나는 구간이 시작되는 시각(초). 키프레임 사이여도 된다. */
private const val SPIN_KNEE_T = 2.63f
/** 멈춤 구간이 시작되는 키프레임 인덱스(끝에서 세 번째, 3.73초). 여기부터 끝까지는 늘리지 않는다. */
private const val SPIN_STOP_INDEX = 7
private const val SPIN_INTEGRATION_STEPS = 2000

/**
 * 단조 3차 보간(Fritsch–Carlson). 키프레임 사이에서 넘치거나 되돌아가지 않는다.
 *
 * @param xs 오름차순 시간
 * @param ys 각 시간의 값
 */
internal class MonotoneCurve(private val xs: FloatArray, private val ys: FloatArray) {
    private val n = xs.size
    private val h = FloatArray(n - 1) { xs[it + 1] - xs[it] }
    private val delta = FloatArray(n - 1) { (ys[it + 1] - ys[it]) / h[it] }
    private val m = FloatArray(n).also { m ->
        if (n == 2) { m[0] = delta[0]; m[1] = delta[0]; return@also }
        m[0] = delta[0]; m[n - 1] = delta[n - 2]
        for (i in 1 until n - 1) m[i] = if (delta[i - 1] * delta[i] <= 0f) 0f else (delta[i - 1] + delta[i]) / 2f
        for (i in 0 until n - 1) {
            if (delta[i] == 0f) { m[i] = 0f; m[i + 1] = 0f; continue }
            val a = m[i] / delta[i]; val b = m[i + 1] / delta[i]; val sq = a * a + b * b
            if (sq > 9f) { val tau = 3f / kotlin.math.sqrt(sq); m[i] = tau * a * delta[i]; m[i + 1] = tau * b * delta[i] }
        }
    }

    /** [x]에서의 값. 범위 밖은 양 끝 값으로 고정. */
    fun value(x: Float): Float {
        if (x <= xs[0]) return ys[0]
        if (x >= xs[n - 1]) return ys[n - 1]
        var i = 0
        while (i < n - 2 && x > xs[i + 1]) i++
        val u = (x - xs[i]) / h[i]; val u2 = u * u; val u3 = u2 * u
        val v = (2f * u3 - 3f * u2 + 1f) * ys[i] + (u3 - 2f * u2 + u) * h[i] * m[i] +
            (-2f * u3 + 3f * u2) * ys[i + 1] + (u3 - u2) * h[i] * m[i + 1]
        return maxOf(0f, v)
    }
}

/** 속도 곡선을 [from]~[to]에서 사다리꼴로 적분한 누적 거리표 — [distanceAt]가 시간 → 거리를 돌려준다. */
internal class DistanceTable(private val curve: MonotoneCurve, private val from: Float, private val to: Float) {
    private val table = FloatArray(SPIN_INTEGRATION_STEPS + 1).also { d ->
        for (i in 1..SPIN_INTEGRATION_STEPS) {
            val t0 = from + (to - from) * (i - 1) / SPIN_INTEGRATION_STEPS
            val t1 = from + (to - from) * i / SPIN_INTEGRATION_STEPS
            d[i] = d[i - 1] + (curve.value(t0) + curve.value(t1)) / 2f * (t1 - t0)
        }
    }
    val duration: Float get() = to - from
    val total: Float get() = table[SPIN_INTEGRATION_STEPS]

    /** 구간 시작 후 [u]초 때 거리. */
    fun distanceAt(u: Float): Float {
        if (u <= 0f) return 0f
        if (u >= duration) return total
        val k = u / duration * SPIN_INTEGRATION_STEPS
        val i = k.toInt().coerceIn(0, SPIN_INTEGRATION_STEPS - 1)
        return table[i] + (table[i + 1] - table[i]) * (k - i)
    }
}

/** 곡선·거리표는 키프레임이 상수라 한 번만 만든다. */
private val SPIN_CURVE_FN = MonotoneCurve(SPIN_CURVE.map { it.first }.toFloatArray(), SPIN_CURVE.map { it.second }.toFloatArray())
private val SPIN_STOP_T = SPIN_CURVE[SPIN_STOP_INDEX].first
private val SPIN_END_T = SPIN_CURVE.last().first
private val SPIN_HEAD = DistanceTable(SPIN_CURVE_FN, 0f, SPIN_KNEE_T)
private val SPIN_MID = DistanceTable(SPIN_CURVE_FN, SPIN_KNEE_T, SPIN_STOP_T)
private val SPIN_STOP = DistanceTable(SPIN_CURVE_FN, SPIN_STOP_T, SPIN_END_T)

/** 시간 왜곡 램프 길이(초) — 곡선 시간의 진행 속도가 1에서 평지 속도로, 다시 1로 돌아오는 데 쓰는 시간. */
private const val SPIN_WARP_RAMP_S = 0.25f
/** 평지 진행 속도 하한 — 이보다 느리게는 늘리지 않는다(사실상 무제한). */
private const val SPIN_WARP_MIN_RATE = 0.004f
private fun smoothstep(k: Float): Float { val c = k.coerceIn(0f, 1f); return c * c * (3f - 2f * c) }

/**
 * 한 번의 룰렛 연출 계획. 머리와 멈춤 구간은 그린 곡선 그대로이고, 중간은 시간 왜곡으로 늘린다:
 * 곡선 시간 τ의 진행 속도 r(t)가 무릎에서 1로 시작해 [SPIN_WARP_RAMP_S] 동안 부드럽게 [rate]까지 내려가
 * 평지를 지난 뒤, 멈춤 구간 앞 [SPIN_WARP_RAMP_S] 동안 다시 1로 돌아온다. 그래서 늘림 구간에 들어가고
 * 나올 때 속도뿐 아니라 가속도도 이어진다. 속도 값은 원곡선 그대로(v(τ))라 "속도 유지 구간"은 생기지
 * 않고 경사만 완만해진다. 평지 길이는 ∫r = 원곡선 중간 길이가 되도록 정해진다.
 *
 * @param rate 평지에서 곡선 시간이 흐르는 속도 — 1이면 원곡선 그대로, 작을수록 더 길게 늘어난다
 */
internal class SpinPlan(val rate: Float) {
    val headDuration = SPIN_HEAD.duration
    val stopDuration = SPIN_STOP.duration
    private val ramp = minOf(SPIN_WARP_RAMP_S, SPIN_MID.duration / 3f)
    private val plateau = (SPIN_MID.duration - ramp * (1f + rate)) / rate
    val midDuration = 2f * ramp + plateau
    val totalDuration = headDuration + midDuration + stopDuration
    val headDistance = SPIN_HEAD.total
    val stopDistance = SPIN_STOP.total

    /** 중간 구간 시작 후 [u]초 때 곡선 시간의 진행 속도. */
    private fun warpRate(u: Float): Float = when {
        u < ramp -> 1f - (1f - rate) * smoothstep(u / ramp)
        u < ramp + plateau -> rate
        else -> rate + (1f - rate) * smoothstep((u - ramp - plateau) / ramp)
    }

    // 중간 구간을 시간 순으로 적분한 표: 곡선 시간 τ와 이동 거리
    private val midTau = FloatArray(SPIN_INTEGRATION_STEPS + 1)
    private val midDist = FloatArray(SPIN_INTEGRATION_STEPS + 1)
    init {
        var tau = SPIN_KNEE_T
        var dist = 0f
        midTau[0] = tau
        val dt = midDuration / SPIN_INTEGRATION_STEPS
        for (i in 1..SPIN_INTEGRATION_STEPS) {
            val r = (warpRate((i - 1) * dt) + warpRate(i * dt)) / 2f
            val v0 = SPIN_CURVE_FN.value(tau)
            tau += r * dt
            dist += (v0 + SPIN_CURVE_FN.value(tau)) / 2f * dt
            midTau[i] = tau
            midDist[i] = dist
        }
    }
    val midDistance = midDist[SPIN_INTEGRATION_STEPS]
    val totalDistance = headDistance + midDistance + stopDistance

    private fun midLookup(table: FloatArray, u: Float): Float {
        val k = (u / midDuration * SPIN_INTEGRATION_STEPS).coerceIn(0f, SPIN_INTEGRATION_STEPS.toFloat())
        val i = k.toInt().coerceIn(0, SPIN_INTEGRATION_STEPS - 1)
        return table[i] + (table[i + 1] - table[i]) * (k - i)
    }

    /** 연출 시작 후 [t]초 때 시작점 기준 이동 거리. 끝 이후는 [totalDistance]에 고정. */
    fun distance(t: Float): Float = when {
        t <= 0f -> 0f
        t < headDuration -> SPIN_HEAD.distanceAt(t)
        t < headDuration + midDuration -> headDistance + midLookup(midDist, t - headDuration)
        t < totalDuration -> headDistance + midDistance + SPIN_STOP.distanceAt(t - headDuration - midDuration)
        else -> totalDistance
    }

    /** [t]초 때 속도(px/s) — 중간은 곡선 시간 τ(t)에서의 원곡선 값. */
    fun velocity(t: Float): Float = when {
        t < headDuration -> SPIN_CURVE_FN.value(t)
        t < headDuration + midDuration -> SPIN_CURVE_FN.value(midLookup(midTau, t - headDuration))
        t < totalDuration -> SPIN_CURVE_FN.value(SPIN_STOP_T + (t - headDuration - midDuration))
        else -> 0f
    }
}

/**
 * 시작 위치와 결과 상자 인덱스로 연출 계획을 세운다.
 * 곡선을 그대로 돌렸을 때 멈출 최소 위치 뒤의 첫 결과 상자 칸을 목표로 잡고, 거기까지 남은 거리
 * (0~박스 수-1칸)가 나오도록 평지 속도를 이분 탐색한다(느릴수록 거리가 단조 증가).
 *
 * @param startOffset 현재 룰렛 오프셋(px)
 * @param boxCount 룰렛에 있는 박스 수
 * @param resultBoxIndex 멈춰야 할 박스 인덱스
 * @return 계획과 최종 오프셋
 */
internal fun planSpin(startOffset: Float, boxCount: Int, resultBoxIndex: Int): Pair<SpinPlan, Float> {
    val base = SpinPlan(1f)
    val minEnd = startOffset + base.totalDistance
    var targetSlot = kotlin.math.ceil(minEnd / ITEM_WIDTH_WITH_GAP).toInt()
    while (((targetSlot % boxCount) + boxCount) % boxCount != resultBoxIndex) targetSlot++
    val targetOffset = targetSlot * ITEM_WIDTH_WITH_GAP
    val need = targetOffset - startOffset
    var plan = base
    if (base.totalDistance < need) {
        var fast = 1f          // 거리가 부족한 쪽
        var slow = SPIN_WARP_MIN_RATE  // 거리가 충분한 쪽
        repeat(30) {
            val mid = (fast + slow) / 2f
            val candidate = SpinPlan(mid)
            if (candidate.totalDistance < need) fast = mid else { slow = mid; plan = candidate }
        }
        if (plan === base) plan = SpinPlan(SPIN_WARP_MIN_RATE)
    }
    return plan to targetOffset
}

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

            // 연출 계획: 그린 속도 곡선 그대로, 무릎~멈춤 구간 사이만 늘려 결과 상자 칸에 정지. 위치는 거리표 조회.
            val (plan, targetOffset) = planSpin(startOffset, boxCount, resultBoxIndex)
            val startTime = System.currentTimeMillis()
            while (true) {
                val t = (System.currentTimeMillis() - startTime) / 1000f
                if (t >= plan.totalDuration) break
                _localState.update { it.copy(offset = startOffset + plan.distance(t)) }
                delay(8)
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
