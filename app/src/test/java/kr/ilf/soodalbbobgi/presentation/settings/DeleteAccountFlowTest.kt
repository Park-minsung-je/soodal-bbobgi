package kr.ilf.soodalbbobgi.presentation.settings

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 계정 탈퇴 2단계 확인 흐름 — 최종 확인에서만 실행되고, 취소는 어느 단계에서든 완전히 닫는다 (R13). */
class DeleteAccountFlowTest {

    @Test
    fun `처음엔 닫혀 있고 확인해도 실행되지 않는다`() {
        val flow = DeleteAccountFlow()
        assertThat(flow.step).isNull()
        assertThat(flow.proceed()).isFalse()
        assertThat(flow.step).isNull()
    }

    @Test
    fun `시작하면 1차 경고부터 연다`() {
        val flow = DeleteAccountFlow()
        flow.start()
        assertThat(flow.step).isEqualTo(DeleteAccountFlow.Step.Warn)
    }

    @Test
    fun `1차에서 확인하면 최종 확인으로 넘어가고 아직 실행하지 않는다`() {
        val flow = DeleteAccountFlow()
        flow.start()
        assertThat(flow.proceed()).isFalse()
        assertThat(flow.step).isEqualTo(DeleteAccountFlow.Step.Final)
    }

    @Test
    fun `최종 확인에서 확인해야 실행되고 팝업은 남는다`() {
        // 서버 실패 메시지·처리 중 표시를 같은 팝업에 띄워야 하므로 닫지 않는다.
        val flow = DeleteAccountFlow()
        flow.start(); flow.proceed()
        assertThat(flow.proceed()).isTrue()
        assertThat(flow.step).isEqualTo(DeleteAccountFlow.Step.Final)
    }

    @Test
    fun `최종 확인에서 취소하면 1차로 돌아가지 않고 완전히 닫힌다`() {
        val flow = DeleteAccountFlow()
        flow.start(); flow.proceed()
        flow.cancel()
        assertThat(flow.step).isNull()
    }

    @Test
    fun `닫힌 뒤 다시 시작하면 1차부터 다시 묻는다`() {
        val flow = DeleteAccountFlow()
        flow.start(); flow.proceed(); flow.cancel()
        flow.start()
        assertThat(flow.step).isEqualTo(DeleteAccountFlow.Step.Warn)
    }
}
