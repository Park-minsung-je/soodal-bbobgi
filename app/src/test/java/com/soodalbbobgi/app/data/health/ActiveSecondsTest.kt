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
    fun `휴식뿐인 세그먼트는 무시하고 랩으로 폴백한다`() {
        val segments = listOf(
            ExerciseSegment(t(0), t(120), ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST),
        )
        val laps = listOf(ExerciseLap(t(0), t(500)))
        assertEquals(500, computeActiveSeconds(segments, laps))
    }
}
