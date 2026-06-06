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

    /** 수영 구간용 합성 심박 — 주기 60초 ±12bpm 사인 출렁임 (실측처럼 평활 후에도 굴곡이 남는다). */
    private fun swimBpm(sec: Int): Int =
        (150 + 12 * kotlin.math.sin(2 * Math.PI * sec / 60)).toInt()

    /** 수영 600초 → 하강 60초 → 바닥 120초 → 상승 60초 → 수영 600초 (1초 간격). */
    private fun valleyPoints(): List<Pair<Int, Int>> =
        (0 until 600).map { it to swimBpm(it) } +
            (600 until 660).map { it to (150 - (it - 600)) } +
            (660 until 780).map { it to (90 + it % 2) } +
            (780 until 840).map { it to (90 + (it - 780)) } +
            (840 until 1440).map { it to swimBpm(it) }

    @Test
    fun `심박 샘플은 첫 샘플 기준 오프셋초 포인트로 변환된다`() {
        // 정렬 안 된 입력도 시간순으로 정렬된다
        val samples = listOf(t(1000) to 150L, t(1005) to 152L, t(1002) to 151L)
        assertEquals(listOf(0 to 150, 2 to 151, 5 to 152), hrPoints(samples))
        assertEquals(emptyList<Pair<Int, Int>>(), hrPoints(emptyList()))
    }

    @Test
    fun `심박 추정은 휴식 마스크와 일치하게 비휴식 간격만 합산한다`() {
        val points = valleyPoints()
        val rest = com.soodalbbobgi.app.core.util.hrRestMask(points)
        // 마스크 기준 기대값 — 동기화와 차트가 같은 분류를 공유해야 한다
        val expected = (0 until points.size - 1).count { !rest[it] }
        assertEquals(expected, hrActiveSeconds(points, distanceM = 500))
        // 골짜기(~195초)를 뺀 값 부근인지 타당성 확인
        org.junit.Assert.assertTrue("active=$expected", expected in 1190..1280)
    }

    @Test
    fun `페이스 하한으로 비현실적으로 빠른 추정을 클램프한다`() {
        // 거리 1,700m → 하한 1,360초(1'20"/100m). 추정(~1,240초)이 그보다 빠르면 하한으로 올린다
        assertEquals(1360, hrActiveSeconds(valleyPoints(), distanceM = 1700))
    }

    @Test
    fun `하한은 기록된 시간을 넘지 않는다`() {
        // 거리 3,000m → 하한 2,400초 > 기록 1,439초 — 기록된 시간으로 캡
        assertEquals(1439, hrActiveSeconds(valleyPoints(), distanceM = 3000))
    }

    @Test
    fun `일시정지 공백은 합산에서 빠진다`() {
        // 수영 600초 + (공백 300초) + 수영 300초 — 공백은 기록이 없으므로 599+299
        val points = (0 until 600).map { it to swimBpm(it) } +
            (900 until 1200).map { it to swimBpm(it) }
        assertEquals(898, hrActiveSeconds(points, distanceM = 0))
    }

    @Test
    fun `심박 샘플이 너무 적으면 추정하지 않는다`() {
        val points = (0 until 30).map { it to 150 }
        assertNull(hrActiveSeconds(points, distanceM = 500))
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
