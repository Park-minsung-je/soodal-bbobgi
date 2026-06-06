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

    @Test
    fun `Otsu 임계값은 이중봉 심박 분포를 두 무리 사이에서 가른다`() {
        val bpm = List(600) { 150L } + List(300) { 95L }
        val threshold = otsuThreshold(bpm)!!
        org.junit.Assert.assertTrue("threshold=$threshold", threshold in 96..150)
    }

    @Test
    fun `변별력 없는 단봉 분포는 임계값을 만들지 않는다`() {
        val bpm = List(500) { 100L + (it % 6) } // 100~105 좁은 범위
        assertNull(otsuThreshold(bpm))
    }

    @Test
    fun `심박 샘플 공백을 일시정지로 보고 운동시간에서 뺀다`() {
        // 0~599초 연속 기록 + (600~899초 일시정지: 샘플 없음) + 900~1199초 연속 기록
        val samples = (0 until 600).map { t(it.toLong()) to 130L } +
            (900 until 1200).map { t(it.toLong()) to 130L }
        // 연속 구간 내 간격 합: 599 + 299 = 898초 (공백 300초 제외, 단봉이라 휴식 차감 없음)
        assertEquals(898, hrActiveSeconds(samples))
    }

    @Test
    fun `지속된 저심박 구간은 벽 휴식으로 보고 추가 차감한다`() {
        // 수영 600초 + 벽 휴식 120초(저심박, 기록은 계속) + 수영 600초
        val samples = (0 until 600).map { t(it.toLong()) to 150L } +
            (600 until 720).map { t(it.toLong()) to 95L } +
            (720 until 1320).map { t(it.toLong()) to 150L }
        // 공백 기반 1319초 - 지속 저심박 120초 = 1199초
        assertEquals(1199, hrActiveSeconds(samples))
    }

    @Test
    fun `짧은 심박 출렁임은 휴식으로 치지 않는다`() {
        // 저심박이 20초만 지속 — 45초 미만이라 차감하지 않는다
        val samples = (0 until 600).map { t(it.toLong()) to 150L } +
            (600 until 620).map { t(it.toLong()) to 95L } +
            (620 until 1220).map { t(it.toLong()) to 150L }
        assertEquals(1219, hrActiveSeconds(samples))
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
