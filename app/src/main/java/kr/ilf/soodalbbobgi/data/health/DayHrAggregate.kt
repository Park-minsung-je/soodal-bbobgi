package kr.ilf.soodalbbobgi.data.health

import kr.ilf.soodalbbobgi.core.util.averageHr
import kr.ilf.soodalbbobgi.core.util.decodeHrRestRanges
import kr.ilf.soodalbbobgi.core.util.decodeHrSeries
import kr.ilf.soodalbbobgi.core.util.encodeHrSeries
import kr.ilf.soodalbbobgi.domain.model.SwimLog

/**
 * 하루치로 합친 심박 값 — 서버에 올릴 형태.
 *
 * @property maxHr 하루 전체 최대 심박(bpm)
 * @property minHr 하루 전체 최소 심박(bpm)
 * @property avgHr 하루 전체 평균 심박(bpm)
 * @property hrSeries 하루 첫 세션 기준으로 이어붙인 시계열. 없으면 null
 */
data class DayHrAggregate(
    val maxHr: Int? = null,
    val minHr: Int? = null,
    val avgHr: Int? = null,
    val hrSeries: String? = null,
)

/**
 * 같은 날짜의 세션들을 서버 1행에 담을 심박 값으로 합친다.
 *
 * 서버는 날짜당 한 행이라 세션별 시계열을 그대로 둘 수 없다. 각 세션의 오프셋을
 * 하루 첫 세션 시작 기준으로 다시 잡아 하나의 곡선으로 잇고, 세션 사이의 빈 시간은
 * 휴식 구간으로 넣어 차트가 끊긴 구간을 운동으로 오해하지 않게 한다.
 *
 * @param rows 같은 날짜의 로컬 기록들 (순서 무관)
 * @return 합쳐진 심박 값. 심박이 하나도 없으면 모든 필드가 null
 */
fun aggregateDayHr(rows: List<SwimLog>): DayHrAggregate {
    if (rows.isEmpty()) return DayHrAggregate()

    // 시작 시각이 있는 행이 먼저, 그 안에서는 시간순. 시각이 없으면 들어온 순서를 지킨다.
    val ordered = rows.sortedBy { it.startEpochSec ?: Long.MAX_VALUE }
    val base = ordered.firstNotNullOfOrNull { it.startEpochSec }

    val points = mutableListOf<Pair<Int, Int>>()
    val restRanges = mutableListOf<IntRange>()
    // 시작 시각을 모르는 행은 앞 세션 끝에 바로 잇기 위해 마지막 오프셋을 들고 다닌다.
    var cursor = 0

    for (row in ordered) {
        val sessionPoints = decodeHrSeries(row.hrSeries)
        if (sessionPoints.isEmpty()) continue

        val shift = row.startEpochSec?.let { start ->
            base?.let { (start - it).toInt() } ?: 0
        } ?: cursor

        val lastOffset = points.lastOrNull()?.first
        val firstShifted = sessionPoints.first().first + shift
        // 세션 사이의 공백 = 물 밖에 있던 시간
        if (lastOffset != null && firstShifted > lastOffset) {
            restRanges += lastOffset..firstShifted
        }

        sessionPoints.mapTo(points) { (offset, bpm) -> (offset + shift) to bpm }
        decodeHrRestRanges(row.hrSeries).mapTo(restRanges) { (it.first + shift)..(it.last + shift) }
        cursor = points.last().first
    }

    // 시계열이 있으면 그걸로, 없으면 수동 입력 평균을 살린다.
    val avg = averageHr(points) ?: ordered.firstNotNullOfOrNull { it.avgHr }

    return DayHrAggregate(
        maxHr = ordered.mapNotNull { it.maxHr }.maxOrNull(),
        minHr = ordered.mapNotNull { it.minHr }.minOrNull(),
        avgHr = avg,
        hrSeries = points.takeIf { it.isNotEmpty() }
            ?.let { encodeHrSeries(it, restRanges.sortedBy { r -> r.first }) },
    )
}
