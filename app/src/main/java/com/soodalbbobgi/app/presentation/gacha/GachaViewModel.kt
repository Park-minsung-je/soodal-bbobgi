package com.soodalbbobgi.app.presentation.gacha

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.repository.GachaRepository
import com.soodalbbobgi.app.domain.repository.UserRepository
import com.soodalbbobgi.app.domain.usecase.GachaUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class GachaPhase { Idle, Spinning, Result }

data class BoxInfo(
    val id: String,
    val icon: SoodalIcons,
    val label: String,
    val color: Color,
)

data class GachaResultItem(
    val name: String,
    val grade: Grade,
    val kind: String,
    val isNew: Boolean,
    val pearlsEarned: Int,
)

data class GachaUiState(
    val shells: Int = 0,
    val phase: GachaPhase = GachaPhase.Idle,
    val offset: Float = 0f,
    val results: List<GachaResultItem> = emptyList(),
    val resultIndex: Int = 0,
    val pityRemaining: Int = 90,
)

val GACHA_BOXES = listOf(
    BoxInfo("char", SoodalIcons.Box, "캐릭터 상자", Color(0xFFFFD60A)),
    BoxInfo("bg", SoodalIcons.Wave, "배경 상자", Color(0xFF00F5FF)),
    BoxInfo("frame", SoodalIcons.Frame, "테두리 상자", Color(0xFFBF5AF2)),
    BoxInfo("mystery", SoodalIcons.Gift, "랜덤 상자", Color(0xFFE0B0FF)),
)

const val ITEM_WIDTH_WITH_GAP = 140f

/** idle 속도에서 서서히 가속하는 구간 비율 (0.25 = 전체의 25%) */
private const val SPIN_ACCEL_PHASE = 0.25f
/** 가속 구간에서 소화하는 거리 비율 (0.4 = 전체 거리의 40%) */
private const val SPIN_ACCEL_PROGRESS = 0.4f
/** 감속 강도 — 높을수록 끝에서 더 천천히 멈춤 (긴장감 조절) */
private const val SPIN_DECEL_POWER = 4
/** 스핀 총 시간 (ms) */
private const val SPIN_DURATION_MS = 4500L
/** 정지 후 결과 모달까지 대기 시간 (ms) */
private const val SPIN_PAUSE_MS = 800L

/**
 * 뽑기 화면의 ViewModel.
 *
 * Room DB에서 사용자 화폐(조개/천장)와 상자 목록을 관찰하고,
 * [GachaUseCase]를 통해 실제 뽑기를 수행한다.
 */
@HiltViewModel
class GachaViewModel @Inject constructor(
    private val userSession: UserSession,
    private val userRepository: UserRepository,
    private val gachaUseCase: GachaUseCase,
    private val gachaRepository: GachaRepository,
) : ViewModel() {

    private val userId get() = userSession.userId

    /** User의 shells/pity를 Room Flow에서 자동 관찰 */
    private val userFlow = userRepository.getUser(userId).filterNotNull()

    /** 로컬 UI 상태 (애니메이션/결과 관련) */
    private val _localState = MutableStateFlow(LocalGachaState())

    /**
     * 애니메이션과 결과 표시에 필요한 로컬 전용 상태.
     * DB에서 오는 화폐/천장 정보와 결합되어 [GachaUiState]를 구성한다.
     */
    data class LocalGachaState(
        val phase: GachaPhase = GachaPhase.Idle,
        val offset: Float = 0f,
        val results: List<GachaResultItem> = emptyList(),
        val resultIndex: Int = 0,
    )

    /** User DB + 로컬 상태를 결합한 최종 UI 상태 */
    val uiState: StateFlow<GachaUiState> = combine(userFlow, _localState) { user, local ->
        GachaUiState(
            shells = user.shellBalance,
            phase = local.phase,
            offset = local.offset,
            results = local.results,
            resultIndex = local.resultIndex,
            pityRemaining = 90 - user.pityCounter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GachaUiState())

    /** DB에서 활성 상자 목록을 관찰하여 룰렛 아이템으로 변환 */
    val boxes: StateFlow<List<BoxInfo>> = gachaRepository.getAllActiveBoxes().map { list ->
        list.map { box ->
            BoxInfo(
                id = box.id.toString(),
                icon = when (box.category) {
                    "char" -> SoodalIcons.Box
                    "bg" -> SoodalIcons.Wave
                    "frame" -> SoodalIcons.Frame
                    else -> SoodalIcons.Gift
                },
                label = box.name,
                color = when (box.category) {
                    "char" -> Color(0xFFFFD60A)
                    "bg" -> Color(0xFF00F5FF)
                    "frame" -> Color(0xFFBF5AF2)
                    else -> Color(0xFFE0B0FF)
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GACHA_BOXES)

    init {
        // Idle 상태에서 룰렛을 느리게 자동 회전시킨다
        viewModelScope.launch {
            while (true) {
                delay(16)
                if (_localState.value.phase == GachaPhase.Idle) {
                    _localState.update { it.copy(offset = it.offset + 0.25f) }
                }
            }
        }
    }

    /**
     * 뽑기를 실행한다. 결과 상자를 미리 결정한 뒤 룰렛을 자연스럽게 감속시켜 해당 상자에 정지시킨다.
     *
     * 비용은 단발 1개, 10연 9개(할인). 조개가 부족하거나 이미 회전 중이면 무시한다.
     * 내부적으로 결과가 먼저 정해지고, 룰렛 애니메이션은 연출일 뿐이다.
     *
     * @param count 뽑기 횟수 (1 = 단발, 10 = 10연)
     */
    fun spin(count: Int) {
        val cost = if (count >= 10) 9 else count
        val s = uiState.value
        if (s.shells < cost || s.phase != GachaPhase.Idle) return

        _localState.update { it.copy(phase = GachaPhase.Spinning) }

        viewModelScope.launch {
            val startOffset = _localState.value.offset

            // DB에서 활성 상자 목록을 가져와 랜덤 선택
            val activeBoxes = gachaRepository.getAllActiveBoxes().first()
            if (activeBoxes.isEmpty()) return@launch
            val selectedBox = activeBoxes.random()
            val resultBoxIndex = activeBoxes.indexOf(selectedBox)

            // 애니메이션과 병행: 실제 뽑기 결과를 UseCase에서 받는다
            val pullDeferred = async(Dispatchers.IO) {
                gachaUseCase.pull(userId, selectedBox.id, count)
            }

            // 현재 위치에서 6~8바퀴 + 결과 상자까지의 잔여 슬롯으로 목표 offset 산출
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

                // 커스텀 이징: 서서히 가속 → 피크 → 끝에서 쫄깃하게 감속
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
            delay(SPIN_PAUSE_MS)

            // 애니메이션 완료 후 결과 수집
            val gachaResults = pullDeferred.await()
            val batch = gachaResults.map { r ->
                GachaResultItem(
                    name = r.item.name,
                    grade = r.item.grade,
                    kind = selectedBox.category,
                    isNew = r.wasNew,
                    pearlsEarned = r.pearlsEarned,
                )
            }
            _localState.update {
                it.copy(phase = GachaPhase.Result, results = batch, resultIndex = 0)
            }
        }
    }

    /** 결과 목록에서 다음 아이템을 표시한다. */
    fun nextResult() {
        val s = _localState.value
        if (s.resultIndex < s.results.size - 1) {
            _localState.update { it.copy(resultIndex = s.resultIndex + 1) }
        }
    }

    /** 결과 목록의 마지막 아이템으로 건너뛴다. */
    fun skipToLastResult() {
        _localState.update { it.copy(resultIndex = it.results.size - 1) }
    }

    /** 결과 모달을 닫고 Idle 상태로 복귀한다. 천장 카운터는 GachaUseCase에서 이미 처리됨. */
    fun closeResults() {
        _localState.update {
            it.copy(phase = GachaPhase.Idle, results = emptyList(), resultIndex = 0)
        }
    }
}
