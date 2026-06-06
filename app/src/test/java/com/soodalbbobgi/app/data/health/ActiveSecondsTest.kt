package com.soodalbbobgi.app.data.health

import androidx.health.connect.client.records.ExerciseLap
import androidx.health.connect.client.records.ExerciseSegment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/**
 * 실제 운동 시간 계산 검증.
 * 세그먼트(휴식·일시정지 제외) → 랩 합산 → 없으면 null 순으로 폴백한다.
 */
class ActiveSecondsTest {

    private fun t(sec: Long): Instant = Instant.ofEpochSecond(sec)

    @Test
    fun `세그먼트가 있으면 휴식과 일시정지를 제외하고 합산한다`() {
        val segments = listOf(
            ExerciseSegment(t(0), t(600), ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_FREESTYLE),
            ExerciseSegment(t(600), t(660), ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST),
            ExerciseSegment(t(660), t(1260), ExerciseSegment.EXERCISE_SEGMENT_TYPE_SWIMMING_BREASTSTROKE),
            ExerciseSegment(t(1260), t(1320), ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE),
        )
        assertEquals(1200, computeActiveSeconds(segments, emptyList()))
    }

    @Test
    fun `세그먼트가 없으면 랩 시간을 합산한다`() {
        val laps = listOf(
            ExerciseLap(t(0), t(300)),
            ExerciseLap(t(360), t(660)),
        )
        assertEquals(600, computeActiveSeconds(emptyList(), laps))
    }

    @Test
    fun `세그먼트와 랩이 모두 없으면 null이다`() {
        assertNull(computeActiveSeconds(emptyList(), emptyList()))
    }

    @Test
    fun `속도 기반 실운동시간은 거리 나누기 평균속도다`() {
        assertEquals(1800, speedBasedActiveSeconds(1500, 1500.0 / 1800.0))
        assertNull(speedBasedActiveSeconds(0, 1.0)) // 거리 없음
        assertNull(speedBasedActiveSeconds(1500, 0.0)) // 속도 없음
    }

    /** 수영 구간용 — 150/152 교대로 출렁여서 휴식 시작점 역확장이 수영 구간을 침범하지 않게 한다. */
    private fun swimBpm(sec: Int): Long = if (sec % 2 == 0) 150L else 152L

    @Test
    fun `휴식은 하락 시작점부터 반등 시작점까지다`() {
        // 수영 600초 + 하강 61초(150→90) + 바닥 119초(90/91) + 상승 60초(90→149) + 수영 600초
        val samples = (0 until 600).map { t(it.toLong()) to swimBpm(it) } +
            (600 until 661).map { t(it.toLong()) to (150L - (it - 600)) } +
            (661 until 780).map { t(it.toLong()) to if (it % 2 == 1) 91L else 90L } +
            (780 until 840).map { t(it.toLong()) to (90L + (it - 780)) } +
            (840 until 1440).map { t(it.toLong()) to swimBpm(it) }
        // 바닥 90 → 임계 118. 휴식 = 하락 시작(599) ~ 반등 시작(바닥+9 초과 = bpm 100, 790) 직전
        // 총 1,439초 - 휴식 191초 = 1,248초
        assertEquals(1248, hrActiveSeconds(samples))
    }

    @Test
    fun `벽 휴식 코어는 꼭대기까지 확장돼 차감된다`() {
        // 수영 600초 + 벽 휴식 300초(95bpm, 반등 없음) → 휴식이 599초 지점(꼭대기)부터 끝까지
        val samples = (0 until 600).map { t(it.toLong()) to swimBpm(it) } +
            (600 until 900).map { t(it.toLong()) to 95L }
        assertEquals(599, hrActiveSeconds(samples))
    }

    @Test
    fun `일시정지 공백 너머로는 휴식이 확장되지 않는다`() {
        // 수영 600초 + (일시정지 300초: 샘플 없음) + 벽 휴식 300초
        val samples = (0 until 600).map { t(it.toLong()) to swimBpm(it) } +
            (900 until 1200).map { t(it.toLong()) to 95L }
        assertEquals(599, hrActiveSeconds(samples))
    }

    @Test
    fun `휴식 없이 수영한 세션은 차감하지 않는다`() {
        // 바닥이 125bpm(>110) — 쉼 없이 수영한 날은 임계 분류 없이 전체가 운동
        val samples = (0 until 600).map { t(it.toLong()) to (125L + (it % 20)) }
        assertEquals(599, hrActiveSeconds(samples))
    }

    @Test
    fun `순간 글리치는 휴식 바닥을 끌어내리지 않는다`() {
        // 40bpm짜리 글리치 3개 — 하위 0.5% 바닥은 여전히 95 부근이라 임계가 흔들리지 않는다
        val samples = (0 until 600).map { t(it.toLong()) to swimBpm(it) } +
            (600 until 900).map { t(it.toLong()) to 95L } +
            (900 until 903).map { t(it.toLong()) to 40L }
        assertEquals(599, hrActiveSeconds(samples))
    }

    @Test
    fun `심박 샘플이 너무 적으면 추정하지 않는다`() {
        val samples = (0 until 30).map { t(it.toLong()) to 150L }
        assertNull(hrActiveSeconds(samples))
    }

    @Test
    fun `심박 다운샘플은 포인트 수를 제한하고 구간 평균을 쓴다`() {
        // 1시간(3600초) 1초 간격 샘플 → 최대 120포인트로 압축
        val samples = (0 until 3600).map { t(it.toLong()) to (100L + (it % 40)) }
        val points = downsampleHr(samples, maxPoints = 120)
        org.junit.Assert.assertTrue("points=${points.size}", points.size in 60..120)
        // 오프셋은 0에서 시작해 증가
        org.junit.Assert.assertTrue(points.first().first < points.last().first)
        // bpm은 원본 범위(100~139) 안의 평균값
        org.junit.Assert.assertTrue(points.all { it.second in 100..139 })
    }

    @Test
    fun `심박 다운샘플은 빈 입력에 빈 결과를 준다`() {
        org.junit.Assert.assertTrue(downsampleHr(emptyList()).isEmpty())
    }

    @Test
    fun `휴식뿐인 세그먼트는 무시하고 랩으로 폴백한다`() {
        val segments = listOf(
            ExerciseSegment(t(0), t(120), ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST),
        )
        val laps = listOf(ExerciseLap(t(0), t(500)))
        assertEquals(500, computeActiveSeconds(segments, laps))
    }
}
