package kr.ilf.soodalbbobgi.presentation.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 계정 탈퇴 2단계 확인 흐름 — 경고(1차) → 최종 확인(2차) → 실행.
 * 탭 한 번의 실수로 계정이 지워지지 않도록 최종 확인에서만 [proceed]가 true를 돌려준다.
 */
class DeleteAccountFlow {
    /** 탈퇴 확인 단계. */
    enum class Step { Warn, Final }

    /** 현재 단계 — null이면 팝업이 닫혀 있다. */
    var step: Step? by mutableStateOf(null)
        private set

    /** 1차 경고 팝업을 연다. */
    fun start() { step = Step.Warn }

    /**
     * 현재 팝업의 확인 버튼 처리.
     *
     * @return true면 실제 탈퇴를 실행해야 한다 (최종 확인에서만). 팝업은 그대로 둔다 — 진행/실패 표시용.
     */
    fun proceed(): Boolean = when (step) {
        Step.Warn -> { step = Step.Final; false }
        Step.Final -> true
        null -> false
    }

    /** 어느 단계에서든 취소 — 1차로 되돌리지 않고 완전히 닫는다 (다시 처음부터 확인). */
    fun cancel() { step = null }
}
