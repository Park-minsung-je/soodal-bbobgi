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
    fun `겹치는 레코드의 중복 샘플은 오프셋 기준으로 제거된다`() {
        // 같은 초의 중복 샘플은 dt=0 경계로 세그먼트를 파편화시키므로 첫 값만 남긴다
        val samples = listOf(t(1000) to 150L, t(1001) to 151L, t(1001) to 153L, t(1002) to 152L)
        assertEquals(listOf(0 to 150, 1 to 151, 2 to 152), hrPoints(samples))
    }

    @Test
    fun `칼로리가 없으면 골짜기 추정을 그대로 쓴다`() {
        // 보정식의 입력(칼로리)이 없으면 보정 없이 Tv — 동기화와 차트가 같은 분류 공유
        val points = valleyPoints()
        val rest = com.soodalbbobgi.app.core.util.hrRestMask(points)
        val tv = (0 until points.size - 1).count { !rest[it] }
        val est = estimateActive(points, distanceM = 500, calories = 0)!!
        assertEquals(tv, est.activeSeconds)
    }

    @Test
    fun `칼로리 보정 결과는 골짜기 추정의 35퍼 한도 안에 있다`() {
        val points = valleyPoints()
        val rest = com.soodalbbobgi.app.core.util.hrRestMask(points)
        val tv = (0 until points.size - 1).count { !rest[it] }
        // 칼로리가 커도 보정량은 Tv의 ±35%로 제한된다
        val est = estimateActive(points, distanceM = 500, calories = 2000)!!
        org.junit.Assert.assertTrue(
            "act=${est.activeSeconds}, tv=$tv",
            est.activeSeconds in (tv * 0.65).toInt() - 1..(tv * 1.35).toInt() + 1,
        )
        org.junit.Assert.assertTrue(est.activeSeconds <= 1439)
    }

    @Test
    fun `연속 수영 세션은 기록 시간 전체가 운동이다`() {
        // 휴식 골짜기가 없으면 전체가 운동 시간
        val points = (0 until 1200).map { it to swimBpm(it) }
        assertEquals(1199, estimateActive(points, distanceM = 0, calories = 0)!!.activeSeconds)
    }

    @Test
    fun `페이스 하한으로 비현실적으로 빠른 추정을 클램프한다`() {
        // 거리 1,700m → 하한 1,360초(1'20"/100m). 추정이 그보다 빠르면 하한으로 올린다
        assertEquals(1360, estimateActive(valleyPoints(), distanceM = 1700, calories = 0)!!.activeSeconds)
    }

    @Test
    fun `하한은 기록된 시간을 넘지 않는다`() {
        // 거리 3,000m → 하한 2,400초 > 기록 1,439초 — 기록된 시간으로 캡
        assertEquals(1439, estimateActive(valleyPoints(), distanceM = 3000, calories = 0)!!.activeSeconds)
    }

    @Test
    fun `일시정지 공백은 합산에서 빠진다`() {
        // 수영 600초 + (공백 300초) + 수영 300초 — 공백은 기록이 없으므로 599+299
        val points = (0 until 600).map { it to swimBpm(it) } +
            (900 until 1200).map { it to swimBpm(it) }
        assertEquals(898, estimateActive(points, distanceM = 0, calories = 0)!!.activeSeconds)
    }

    @Test
    fun `차트 휴식 구간은 골짜기 위치 그대로다 - 예산이 충분하면 전부 유지`() {
        // 휴식 위치·크기는 변형하지 않는다 (스케일/병합하면 차트가 심박 모양과 어긋난다)
        val points = valleyPoints()
        val est = estimateActive(points, distanceM = 500, calories = 0)!!
        assertEquals(com.soodalbbobgi.app.core.util.hrRestRanges(points), est.restRanges)
    }

    @Test
    fun `심박 샘플이 너무 적으면 추정하지 않는다`() {
        val points = (0 until 30).map { it to 150 }
        assertNull(estimateActive(points, distanceM = 500, calories = 100))
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
