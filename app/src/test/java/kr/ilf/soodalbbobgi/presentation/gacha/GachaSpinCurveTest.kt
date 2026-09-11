package kr.ilf.soodalbbobgi.presentation.gacha

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * 룰렛 운동 모델 — 사용자가 그린 속도 키프레임을 단조 3차 보간으로 재생. 머리와 멈춤 구간(마지막 세 점)은
 * 그대로 두고 무릎~멈춤 사이만 시간을 늘려 결과 상자 칸에 정확히 멈춘다. 속도 값은 늘려도 그대로.
 */
class GachaSpinCurveTest {

    private val w = ITEM_WIDTH_WITH_GAP

    @Test
    fun `계획은 정확히 결과 상자 칸에서 끝난다`() {
        for (idx in 0 until 3) {
            val (plan, target) = planSpin(startOffset = 37f, boxCount = 3, resultBoxIndex = idx)
            assertThat(37f + plan.distance(plan.totalDuration)).isWithin(0.5f).of(target)
            assertThat(((target / w).toInt() % 3)).isEqualTo(idx)
        }
    }

    @Test
    fun `늘어나는 건 중간 구간뿐이다 - 머리와 멈춤 구간은 결과 상자와 무관하게 같다`() {
        val plans = (0 until 3).map { planSpin(0f, 3, it).first }
        assertThat(plans.map { it.headDuration }.toSet()).hasSize(1)
        assertThat(plans.map { it.stopDuration }.toSet()).hasSize(1)
        assertThat(plans.map { it.stopDistance }.toSet()).hasSize(1)
        assertThat(plans.map { it.rate }.toSet().size).isAtLeast(2)
        assertThat(plans.maxOf { it.rate }).isAtMost(1f)
        // 멈춤 구간은 그린 그대로 3.73 → 5.64초, 1.91초
        assertThat(plans[0].stopDuration).isWithin(0.01f).of(1.91f)
    }

    @Test
    fun `중간을 늘려도 속도 값은 그대로다 - 무릎 4점6칸, 멈춤 시작 0점8칸, 끝 0`() {
        val plan = (0 until 3).map { planSpin(0f, 3, it).first }.minBy { it.rate }
        val knee = plan.headDuration
        val stopStart = plan.headDuration + plan.midDuration
        assertThat(plan.velocity(knee - 0.001f)).isWithin(0.3f * w).of(4.6f * w)
        assertThat(plan.velocity(knee + 0.001f)).isWithin(0.3f * w).of(4.6f * w)
        assertThat(plan.velocity(stopStart - 0.001f)).isWithin(0.1f * w).of(0.8f * w)
        assertThat(plan.velocity(stopStart + 0.001f)).isWithin(0.1f * w).of(0.8f * w)
        assertThat(plan.velocity(plan.totalDuration - 0.01f)).isLessThan(0.05f * w)
    }

    @Test
    fun `늘림 구간에 들어갈 때 가속도가 끊기지 않는다 - 무릎 앞뒤 기울기가 같다`() {
        val plan = (0 until 3).map { planSpin(0f, 3, it).first }.minBy { it.rate }
        val knee = plan.headDuration
        val eps = 0.02f
        val slopeBefore = (plan.velocity(knee) - plan.velocity(knee - eps)) / eps
        val slopeAfter = (plan.velocity(knee + eps) - plan.velocity(knee)) / eps
        // 하드 늘림이었다면 뒤 기울기가 앞의 1/배율로 뚝 떨어진다. 왜곡은 r=1로 시작해 서서히 느려진다.
        assertThat(slopeAfter).isWithin(kotlin.math.abs(slopeBefore) * 0.25f + 5f).of(slopeBefore)
        // 멈춤 구간으로 나올 때도 마찬가지
        val stopStart = knee + plan.midDuration
        val sb = (plan.velocity(stopStart) - plan.velocity(stopStart - eps)) / eps
        val sa = (plan.velocity(stopStart + eps) - plan.velocity(stopStart)) / eps
        assertThat(sa).isWithin(kotlin.math.abs(sb) * 0.35f + 5f).of(sb)
    }

    @Test
    fun `위치는 단조 증가하고 봉우리 이후 속도는 다시 빨라지지 않는다`() {
        val (plan, _) = planSpin(0f, 3, 2)
        var prevX = -1f
        var prevV = Float.MAX_VALUE
        var t = 1.20f
        while (t < plan.totalDuration) {
            assertThat(plan.distance(t)).isAtLeast(prevX)
            val v = plan.velocity(t)
            assertThat(v).isAtMost(prevV + 0.5f)
            prevX = plan.distance(t); prevV = v
            t += 0.01f
        }
    }

    @Test
    fun `봉우리는 둥글다 - 0점40초와 2점06초 사이에서 완만하게 넘어간다`() {
        val (plan, _) = planSpin(0f, 3, 2)
        assertThat(plan.velocity(1.20f)).isWithin(0.3f * w).of(12.2f * w)
        assertThat(plan.velocity(1.63f)).isWithin(1.0f * w).of(11.6f * w)
        assertThat(plan.velocity(2.06f)).isWithin(0.3f * w).of(10.6f * w)
    }

    @Test
    fun `전체 시간은 그린 곡선의 5점6초에서 최대 2칸만큼 늘어난다`() {
        for (idx in 0 until 3) {
            val (plan, _) = planSpin(0f, 3, idx)
            assertThat(plan.totalDuration).isAtLeast(5.64f)
            assertThat(plan.totalDuration).isAtMost(7.2f)
        }
    }
}
