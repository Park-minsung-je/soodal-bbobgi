package kr.ilf.soodalbbobgi.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kr.ilf.soodalbbobgi.core.util.encodeHrSeries
import kr.ilf.soodalbbobgi.core.util.hrSmoothed
import kr.ilf.soodalbbobgi.core.util.intuitiveRestRanges
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Health Connect에서 읽어온 수영 세션 데이터.
 *
 * @property date 수영한 날짜 (yyyy-MM-dd 형식)
 * @property distanceMeters 수영 거리 (미터)
 * @property durationSeconds 수영 시간 (초)
 * @property calories 소모 칼로리
 * @property hcRecordId Health Connect 레코드 UID (삭제 추적용)
 * @property maxHr 세션 중 최대 심박(bpm). 심박 기록이 없으면 null
 * @property minHr 세션 중 최소 심박(bpm). 심박 기록이 없으면 null
 * @property activeSeconds 실제 운동 시간(초) — 세그먼트/랩 → 속도 → 심박추정 순으로 계산. 모두 없으면 null
 * @property hrSeries 차트용 다운샘플 심박 시계열 + 휴식 구간 ("오프셋초:bpm,...|시작-끝,..." 직렬화). 없으면 null
 */
data class SwimSession(
    val date: String,
    /** 세션 시작 시각(epoch 초) — 같은 날 여러 세션의 정렬·시간대 표시용. */
    val startEpochSec: Long? = null,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
    val hcRecordId: String? = null,
    val maxHr: Int? = null,
    val minHr: Int? = null,
    val activeSeconds: Int? = null,
    val hrSeries: String? = null,
)

/**
 * 세션의 실제 운동 시간(초)을 계산한다.
 * 1순위: 세그먼트에서 휴식·일시정지 구간을 제외한 합. 2순위: 랩 시간 합산. 둘 다 없으면 null
 * (호출자는 경과 시간으로 폴백한다).
 */
internal fun computeActiveSeconds(
    segments: List<androidx.health.connect.client.records.ExerciseSegment>,
    laps: List<androidx.health.connect.client.records.ExerciseLap>,
): Int? {
    if (segments.isNotEmpty()) {
        val active = segments
            .filter {
                it.segmentType != androidx.health.connect.client.records.ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST &&
                    it.segmentType != androidx.health.connect.client.records.ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE
            }
            .sumOf { Duration.between(it.startTime, it.endTime).seconds }
        if (active > 0) return active.toInt()
    }
    if (laps.isNotEmpty()) {
        val active = laps.sumOf { Duration.between(it.startTime, it.endTime).seconds }
        if (active > 0) return active.toInt()
    }
    return null
}

/**
 * 시간 구간이 겹치는 레코드를 한 벌만 남기고 값을 합산한다 — 거리/칼로리 이중 기록 방어.
 *
 * HC에는 같은 수영의 거리/칼로리가 총계 레코드 + 랩별 레코드로 함께 저장되거나
 * 복수 소스(워치/폰)가 각각 기록해 이중으로 존재할 수 있다 — 단순 합산은 값이 두 배가 된다.
 * 시작시각 순으로 훑으며 직전 채택 구간과 겹치는 레코드는 이중 기록으로 보고 건너뛴다.
 * 같은 시작이면 긴 구간 우선(총계+랩 공존 시 총계 한 벌만 남음),
 * 랩 경계의 1초 이하 겹침은 기록 노이즈로 보고 겹침으로 치지 않는다.
 *
 * @param intervals (시작, 끝, 값) 목록
 * @return 겹침 제거 후 값 합계
 */
internal fun sumNonOverlapping(intervals: List<Triple<Instant, Instant, Double>>): Double {
    val sorted = intervals.sortedWith(
        compareBy<Triple<Instant, Instant, Double>> { it.first }.thenByDescending { it.second },
    )
    var sum = 0.0
    var lastEnd: Instant? = null
    for ((start, end, value) in sorted) {
        if (lastEnd != null && start < lastEnd.minusSeconds(1)) continue
        sum += value
        if (lastEnd == null || end > lastEnd) lastEnd = end
    }
    return sum
}

/**
 * 심박 샘플을 차트용으로 다운샘플한다 — 구간 평균으로 최대 [maxPoints]개의 (오프셋초, bpm) 포인트.
 */
internal fun downsampleHr(samples: List<Pair<Instant, Long>>, maxPoints: Int = 120): List<Pair<Int, Int>> {
    if (samples.isEmpty()) return emptyList()
    val sorted = samples.sortedBy { it.first }
    val start = sorted.first().first
    val totalSec = Duration.between(start, sorted.last().first).seconds.toInt().coerceAtLeast(1)
    val bucketSec = totalSec / maxPoints + 1
    return sorted
        .groupBy { Duration.between(start, it.first).seconds.toInt() / bucketSec }
        .toSortedMap()
        .map { (_, group) ->
            val avgOffset = group.map { Duration.between(start, it.first).seconds.toDouble() }.average().toInt()
            val avgBpm = Math.round(group.map { it.second.toDouble() }.average()).toInt()
            avgOffset to avgBpm
        }
}

/**
 * (시각, bpm) 심박 샘플을 첫 샘플 기준 (오프셋초, bpm) 포인트로 변환한다.
 * 시간순으로 정렬하고, 겹치는 레코드에서 온 같은 초의 중복 샘플은 첫 값만 남긴다
 * (중복은 dt=0 경계로 휴식 마스크의 세그먼트를 파편화시킨다).
 */
internal fun hrPoints(samples: List<Pair<Instant, Long>>): List<Pair<Int, Int>> {
    if (samples.isEmpty()) return emptyList()
    val sorted = samples.sortedBy { it.first }
    val base = sorted.first().first
    val points = ArrayList<Pair<Int, Int>>(sorted.size)
    var lastOffset = -1
    for ((time, bpm) in sorted) {
        val offset = Duration.between(base, time).seconds.toInt()
        if (offset == lastOffset) continue
        lastOffset = offset
        points.add(offset to bpm.toInt())
    }
    return points
}

/**
 * 심박 기반 추정 결과.
 *
 * @property activeSeconds 실운동시간(초) — 기록된 시간에서 휴식초를 뺀 값
 * @property restRanges 차트용 휴식 구간 (오프셋초) — intuitiveRestRanges 단일 소스
 */
internal data class HrEstimate(
    val activeSeconds: Int,
    val restRanges: List<IntRange>,
)

// 페이스 보정식 act = W_K·kcal + W_I·I + W_TV·Tv 의 계수 (I = 심박 적분, Tv = 직관 운동시간).
// 2026-06-09 삼성헬스 실측 4세션(05-30/31, 06-04/05)에 적합 — 적합오차 ±12.6s/100m.
// 데이터가 4개뿐이라 적합 범위 밖 세션은 불안정 → estimateActive에서 Tv ±30%로 클램프.
private const val HR_W_K = 3.2268
private const val HR_W_I = -0.01849
private const val HR_W_TV = 1.1482

/**
 * 직관 모델 기반 심박 실운동시간 추정 — 페이스 계산과 차트 휴식 표시의 단일 소스.
 *
 * 차트 휴식 구간(restRanges)은 직관 모델 그대로 — 수영 수준(양방향 120초 트레일링 최대)
 * 대비 22bpm 이상 낮게 깔린 골짜기, 25초 미만은 무시.
 * 페이스(activeSeconds)는 칼로리 보정식으로 산출한다 — 심박 그래프만으로는 느린 연속 수영과
 * 휴식 위주를 구분 못 하므로(5/30 vs 5/31), 세션 칼로리로 실제 운동시간을 보정한다.
 * 차트 회색 총량과 페이스가 정확히 일치하진 않지만, 둘 다 의미 있게 맞춘 절충이다.
 * 보정량은 직관 운동시간(Tv)의 ±30%로 제한해 적합 범위 밖 세션의 폭주를 막는다.
 *
 * @param points (오프셋초, bpm) 목록 — [hrPoints] 결과. 60개 미만이면 추정 포기(null)
 * @param distanceM 세션 거리(미터) — 페이스 하한 클램프용
 * @param calories 소모 칼로리(kcal) — 페이스 보정 입력. 0 이하면 보정 없이 직관 Tv 사용
 */
internal fun estimateActive(
    points: List<Pair<Int, Int>>,
    distanceM: Int,
    calories: Int,
    gapSec: Int = 5,
): HrEstimate? {
    if (points.size < 60) return null

    val sm = hrSmoothed(points)
    // 기저 심박 = 세션 하위 5% (순간 글리치에 흔들리지 않는 개인 기저 근사)
    val floorBpm = points.map { it.second }.sorted()[points.size / 20]

    // 유효 기록시간 = dt가 1부터 gapSec까지인 구간의 합 + 그 구간 심박 적분
    var recorded = 0
    var integral = 0.0
    for (i in 0 until points.size - 1) {
        val dt = points[i + 1].first - points[i].first
        if (dt in 1..gapSec) {
            recorded += dt
            integral += maxOf(0.0, sm[i] - floorBpm) * dt
        }
    }
    if (recorded <= 0) return null

    // 차트용 휴식 구간 (직관 모델) — 그래프는 이걸 그대로 회색으로 표시
    val (restRanges, restSec) = intuitiveRestRanges(points, gapSec = gapSec)
    val tv = recorded - restSec
    if (tv <= 0) return null

    // 페이스용 실운동시간 — 칼로리 보정 (그래프 휴식과 분리)
    var act = if (calories > 0) {
        HR_W_K * calories + HR_W_I * integral + HR_W_TV * tv
    } else {
        tv.toDouble()
    }
    // 보정량을 직관 Tv의 0.5~1.6배로만 제한 — 고강도 날(거의 안 쉼)은 보정이 크게 당겨야
    // 실측에 맞지만(6/7), 적합 범위 밖 극단치는 막는다.
    act = act.coerceIn(tv * 0.5, tv * 1.6)
    act = act.coerceAtLeast(distanceM * 0.7)      // 페이스 하한 1'10"/100m
    act = act.coerceAtMost(recorded.toDouble())
    return HrEstimate(Math.round(act).toInt(), restRanges)
}

/**
 * HC Changes API 동기화 결과.
 *
 * @property addedSessions 추가/수정된 수영 세션 목록
 * @property deletedRecordIds 삭제된 HC 레코드 UID 목록
 * @property nextToken 다음 동기화에 사용할 변경 토큰
 */
data class HcSyncResult(
    val addedSessions: List<SwimSession>,
    val deletedRecordIds: List<String>,
    val nextToken: String,
)

/**
 * Health Connect API와의 통신을 담당하는 매니저.
 *
 * 수영 운동 세션, 거리, 칼로리 데이터를 읽어오며
 * 권한 확인 및 SDK 가용성 체크 기능을 제공한다.
 */
@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    /** Health Connect에서 필요한 읽기 권한 목록. */
    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    )

    /**
     * 필요한 모든 Health Connect 권한이 부여되었는지 확인한다.
     *
     * @return 모든 권한이 부여되었으면 true
     */
    /**
     * HC 백그라운드 읽기 권한이 이미 부여돼 있는지 확인한다.
     * 이미 있으면 권한 요청 액티비티를 띄우지 않기 위한 사전 조회 — 매번 띄우면
     * HC 화면이 순간 나타났다 사라지며 상단바가 깜빡인다.
     */
    suspend fun isBackgroundReadGranted(): Boolean = try {
        BG_READ_PERMISSION in healthConnectClient.permissionController.getGrantedPermissions()
    } catch (e: Exception) {
        Timber.w(e, "HC 백그라운드 권한 확인 실패")
        false
    }

    /**
     * HC 과거 데이터 권한('모든 기간의 데이터에 액세스')이 허용돼 있는지 확인한다.
     * 없으면 HC가 첫 허용 시점 30일 이전 기록을 주지 않아 긴 기간 가져오기가 조용히 짧아진다.
     *
     * @return 과거 데이터 권한이 부여돼 있으면 true. 조회 실패 시 false
     */
    suspend fun isHistoryReadGranted(): Boolean = try {
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in
            healthConnectClient.permissionController.getGrantedPermissions()
    } catch (e: Exception) {
        Timber.w(e, "HC 과거 데이터 권한 확인 실패")
        false
    }

    /**
     * 이 앱에 허용된 HC 권한을 전부 회수한다 — 탈퇴 시 온보딩이 처음처럼 권한을 다시 묻게 하기 위함.
     *
     * @return 회수에 성공하면 true. 실패해도 탈퇴는 진행하므로 예외를 던지지 않는다
     */
    suspend fun revokeAllPermissions(): Boolean = try {
        healthConnectClient.permissionController.revokeAllPermissions()
        true
    } catch (e: Exception) {
        Timber.w(e, "HC 권한 회수 실패")
        false
    }

    suspend fun hasAllPermissions(): Boolean {
        return try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            requiredPermissions.all { it in granted }
        } catch (e: Exception) {
            Timber.w(e, "Health Connect 권한 확인 실패")
            false
        }
    }

    /**
     * 지정한 시간 범위에서 수영 세션을 읽어온다.
     *
     * Health Connect에서 운동 세션을 조회한 뒤, 수영(수영장/오픈워터) 세션만 필터링한다.
     * 각 세션의 거리와 칼로리는 해당 세션 시간 범위 내의 DistanceRecord와
     * TotalCaloriesBurnedRecord에서 합산한다.
     *
     * Health Connect API에서는 영법(스트로크) 정보를 제공하지 않으므로
     * 모든 거리는 혼영(mixed)으로 처리된다.
     *
     * @param startTime 조회 시작 시간
     * @param endTime 조회 종료 시간
     * @return 수영 세션 목록. 오류 시 빈 리스트 반환
     */
    suspend fun readSwimSessions(startTime: Instant, endTime: Instant): List<SwimSession> {
        return try {
            // 세션 목록은 페이지 단위(기본 1000건)로 끊겨 올 수 있다 — pageToken을 따라 전부 읽는다.
            val sessions = mutableListOf<ExerciseSessionRecord>()
            var pageToken: String? = null
            do {
                val response = healthConnectClient.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                        pageToken = pageToken,
                    )
                )
                sessions += response.records
                pageToken = response.pageToken
            } while (pageToken != null)

            // 세션별 상세는 일일 동기화와 같은 [buildSwimSession]으로 계산한다 — 세션 시간
            // 범위로 HC에 직접 질의하므로(겹침 기준) 경계에 걸친 레코드가 누락되지 않고,
            // 긴 범위 일괄 조회의 페이지 절단으로 거리가 0이 되는 문제도 없다.
            sessions.filter { isSwimmingSession(it) }.mapNotNull { buildSwimSession(it) }
        } catch (e: Exception) {
            Timber.e(e, "Health Connect 수영 세션 읽기 실패")
            emptyList()
        }
    }

    /**
     * HC Changes API용 변경 토큰을 발급받는다.
     * ExerciseSessionRecord 변경분만 추적한다.
     */
    suspend fun getChangesToken(): String {
        return healthConnectClient.getChangesToken(
            ChangesTokenRequest(recordTypes = setOf(ExerciseSessionRecord::class))
        )
    }

    /**
     * 저장된 토큰 이후에 수영 세션 변경(추가/수정/삭제)이 있는지만 가볍게 확인한다.
     *
     * **토큰을 소비하지 않는다** — 실제 동기화(HcSwimSyncer)가 같은 토큰으로 변경분을
     * 처리해야 하므로, 여기서는 존재 여부만 보고 저장된 토큰은 건드리지 않는다.
     * 백그라운드 새 기록 알림 워커 전용.
     *
     * @return 새 수영 세션(추가/수정)이 있으면 true, 없으면 false, 토큰 만료/판단 불가면 null
     */
    suspend fun hasSwimChanges(token: String): Boolean? {
        return try {
            var currentToken = token
            do {
                val response = healthConnectClient.getChanges(currentToken)
                if (response.changesTokenExpired) return null
                for (change in response.changes) {
                    when (change) {
                        is UpsertionChange -> {
                            val record = change.record
                            if (record is ExerciseSessionRecord && isSwimmingSession(record)) return true
                        }
                        // 삭제는 무시 — 삭제 변경엔 UID만 있어 수영 여부를 구분할 수 없고,
                        // 워치/삼성헬스가 다른 운동 세션을 정리(삭제·재작성)만 해도
                        // "새 기록" 오알림이 났다. 실제 삭제 반영은 포그라운드 동기화가 처리한다.
                        is DeletionChange -> Unit
                    }
                }
                currentToken = response.nextChangesToken
            } while (response.hasMore)
            false
        } catch (e: Exception) {
            Timber.w(e, "HC 변경 확인 실패")
            null
        }
    }

    /**
     * 저장된 변경 토큰 이후의 HC 변경분을 읽어온다.
     *
     * 추가/수정된 수영 세션과 삭제된 레코드 UID를 반환한다.
     * 토큰이 만료되면 null을 반환하여 호출자가 전체 읽기로 폴백하게 한다.
     */
    suspend fun getChanges(token: String): HcSyncResult? {
        return try {
            val addedSessions = mutableListOf<SwimSession>()
            val deletedIds = mutableListOf<String>()
            var currentToken = token

            do {
                val response = healthConnectClient.getChanges(currentToken)
                if (response.changesTokenExpired) {
                    Timber.w("HC 변경 토큰 만료 — 전체 읽기로 폴백")
                    return null
                }

                for (change in response.changes) {
                    when (change) {
                        is UpsertionChange -> {
                            val record = change.record
                            if (record is ExerciseSessionRecord && isSwimmingSession(record)) {
                                val session = buildSwimSession(record)
                                if (session != null) addedSessions.add(session)
                            }
                        }
                        is DeletionChange -> {
                            deletedIds.add(change.recordId)
                        }
                    }
                }

                currentToken = response.nextChangesToken
            } while (response.hasMore)

            HcSyncResult(
                addedSessions = addedSessions,
                deletedRecordIds = deletedIds,
                nextToken = currentToken,
            )
        } catch (e: Exception) {
            Timber.e(e, "HC Changes API 호출 실패")
            null
        }
    }

    private fun isSwimmingSession(record: ExerciseSessionRecord): Boolean =
        record.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL ||
                record.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER

    /**
     * 단일 ExerciseSessionRecord에서 거리/칼로리를 읽어 SwimSession을 구성한다.
     */
    private suspend fun buildSwimSession(session: ExerciseSessionRecord): SwimSession? {
        return try {
            val duration = Duration.between(session.startTime, session.endTime)
            val timeFilter = TimeRangeFilter.between(session.startTime, session.endTime)

            // 총계+랩 공존이나 복수 소스(워치/폰) 이중 기록으로 값이 두 배가 되지 않게
            // 시간이 겹치는 레코드는 한 벌만 합산한다.
            val distanceM = sumNonOverlapping(
                healthConnectClient.readRecords(
                    ReadRecordsRequest(DistanceRecord::class, timeFilter)
                ).records.map { Triple(it.startTime, it.endTime, it.distance.inMeters) }
            ).toInt()

            val calories = sumNonOverlapping(
                healthConnectClient.readRecords(
                    ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeFilter)
                ).records.map { Triple(it.startTime, it.endTime, it.energy.inKilocalories) }
            ).toInt()

            val hrSamples = readHeartRateRecords(session.startTime, session.endTime)
                .flatMap { it.samples }
                .map { it.time to it.beatsPerMinute }
            val bpm = hrSamples.map { it.second }
            val hrEstimate = estimateActive(hrPoints(hrSamples), distanceM, calories)

            SwimSession(
                date = session.startTime.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                startEpochSec = session.startTime.epochSecond,
                distanceMeters = distanceM,
                durationSeconds = duration.seconds.toInt(),
                calories = calories,
                hcRecordId = session.metadata.id,
                maxHr = bpm.maxOrNull()?.toInt(),
                minHr = bpm.minOrNull()?.toInt(),
                // 세그먼트·랩이 없는 세션은 심박 추정으로 — 수영 세션엔 속도 샘플이 없어 속도 폴백은 뺐다.
                activeSeconds = computeActiveSeconds(session.segments, session.laps)
                    ?: hrEstimate?.activeSeconds,
                hrSeries = downsampleHr(hrSamples).takeIf { it.isNotEmpty() }
                    ?.let { encodeHrSeries(it, hrEstimate?.restRanges ?: emptyList()) },
            )
        } catch (e: Exception) {
            Timber.w(e, "수영 세션 상세 정보 읽기 실패: ${session.metadata.id}")
            null
        }
    }

    /**
     * 시간 범위 내 심박 레코드를 읽는다.
     * 심박 권한이 없거나 조회에 실패해도 빈 목록을 반환해 세션 동기화를 막지 않는다.
     */
    private suspend fun readHeartRateRecords(startTime: Instant, endTime: Instant): List<HeartRateRecord> {
        return try {
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                )
            ).records
        } catch (e: Exception) {
            Timber.w(e, "심박 레코드 읽기 실패 — 심박 없이 진행")
            emptyList()
        }
    }

    companion object {
        /**
         * Health Connect SDK의 가용 상태를 확인한다.
         *
         * @return [HealthConnectClient.SDK_AVAILABLE] — 사용 가능,
         *         [HealthConnectClient.SDK_UNAVAILABLE] — 사용 불가,
         *         [HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED] — 업데이트 필요
         */
        fun getSdkStatus(context: Context): Int {
            return HealthConnectClient.getSdkStatus(context)
        }

        /**
         * Health Connect를 사용할 수 있는지 확인한다.
         *
         * @return 사용 가능하면 true
         */
        fun isAvailable(context: Context): Boolean {
            return getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
        }

        /** 설정 "연결됨" 탭 → HC 설정 홈(앱별 권한 화면은 시스템 전용). 열지 못하면 false (호출자가 안내). */
        fun openPermissionScreen(context: Context): Boolean = openHealthConnectSettings(context)

        /**
         * 권한 요청 다이얼로그에 띄울 전체 권한 — 온보딩·설정 공용.
         * 앱은 HC에 기록을 쓰지 않으므로 읽기 전용만 요청한다 (Play 심사 최소 권한 원칙).
         */
        /** 새 기록 알림 워커가 쓰는 백그라운드 읽기 권한. */
        const val BG_READ_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

        /** 기본 요청 권한 — 수영 데이터 읽기 4종. 속도는 수영 세션에 샘플이 없어 요청하지 않는다. */
        private val BASE_REQUEST_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        )

        /** 설정 재연결용 전체 셋 — 과거 데이터 권한 포함 (재설치 후 이력 복원에 필요). */
        val requestPermissions: Set<String> = BASE_REQUEST_PERMISSIONS +
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

        /**
         * 온보딩용 요청 셋 — 지난 기록을 가져오기로 했을 때만 과거 데이터 권한을 포함한다.
         * 과거 데이터는 필수 권한이 아니라 requiredPermissions에는 넣지 않는다 (거부해도 동작).
         */
        fun requestPermissionsFor(includeHistory: Boolean): Set<String> =
            if (includeHistory) requestPermissions else BASE_REQUEST_PERMISSIONS
    }
}
