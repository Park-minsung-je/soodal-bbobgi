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
    fun `심박 추정 실운동시간은 임계 이상 구간만 합산한다`() {
        // 0~599초 고심박(수영) + 600~899초 저심박(휴식), 1초 간격 샘플
        val samples = (0 until 900).map { sec ->
            t(sec.toLong()) to if (sec < 600) 150L else 95L
        }
        assertEquals(600, hrActiveSeconds(samples))
    }

    @Test
    fun `심박 샘플이 너무 적으면 추정하지 않는다`() {
        val samples = (0 until 30).map { t(it.toLong()) to 150L }
        assertNull(hrActiveSeconds(samples))
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
