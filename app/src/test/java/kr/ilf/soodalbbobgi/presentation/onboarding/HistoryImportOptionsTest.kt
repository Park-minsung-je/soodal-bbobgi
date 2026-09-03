package kr.ilf.soodalbbobgi.presentation.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 지난 기록 가져오기 기간 선택지 — 값과 라벨이 기획(1·3·6개월·1년)과 일치하는지 고정한다. */
class HistoryImportOptionsTest {

    @Test
    fun `선택지는 1 3 6 12개월 네 가지다`() {
        assertThat(HISTORY_MONTH_OPTIONS.map { it.first }).containsExactly(1, 3, 6, 12).inOrder()
    }

    @Test
    fun `12개월은 1년으로 표시한다`() {
        assertThat(HISTORY_MONTH_OPTIONS.map { it.second })
            .containsExactly("1개월", "3개월", "6개월", "1년").inOrder()
    }

    @Test
    fun `모든 선택지는 상한 이내의 양수다`() {
        // HcSyncPreferences.getPendingInitialMonths()는 0 이하를 '없음'으로 본다
        HISTORY_MONTH_OPTIONS.forEach { (months, _) ->
            assertThat(months).isGreaterThan(0)
            assertThat(months).isAtMost(OnboardingPermissionViewModel.MAX_INITIAL_MONTHS)
        }
    }
}
