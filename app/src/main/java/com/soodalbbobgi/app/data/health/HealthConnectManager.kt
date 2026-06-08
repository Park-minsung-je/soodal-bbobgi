package com.soodalbbobgi.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.soodalbbobgi.app.core.util.encodeHrSeries
import com.soodalbbobgi.app.core.util.hrRestMask
import com.soodalbbobgi.app.core.util.hrRestRanges
import com.soodalbbobgi.app.core.util.chartRestRanges
import com.soodalbbobgi.app.core.util.hrSmoothed
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
 * SpeedRecord 평균 속도(m/s) 기반 실운동시간(초) — 세그먼트/랩이 없을 때의 폴백.
 * 속도 샘플은 이동 중에만 찍히므로 거리/평균속도가 곧 실운동시간에 가깝다.
 */
internal fun speedBasedActiveSeconds(distanceM: Int, avgSpeedMps: Double): Int? =
    if (distanceM <= 0 || avgSpeedMps <= 0.0) null
    else Math.round(distanceM / avgSpeedMps).toInt()

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
 * @property activeSeconds 실운동시간(초) — 골짜기 추정에 강도 보정식을 적용한 값
 * @property restRanges 차트용 휴식 구간 (오프셋초) — 보정된 휴식 총량에 맞게 스케일됨
 */
internal data class HrEstimate(
    val activeSeconds: Int,
    val restRanges: List<IntRange>,
)

// 실운동시간 보정식 act = W_K·kcal + W_I·I + W_TV·Tv 의 계수.
// (I = Σ(평활심박-기저)dt, Tv = 골짜기 활동시간)
// 직관: 칼로리가 많으면 그만큼 수영한 것이고, 칼로리 없이 심박만 높으면 서서 보낸 시간이다 —
// 심박 모양이 같아도 "느린 연속 수영"과 "휴식 위주"를 세션 칼로리가 가른다.
// 2026-06-08 삼성헬스 실측 4세션(05-30/31, 06-04/05)에 적합 — "휴식 끝 = 상승 시작" 경계 기준.
// 적합오차 ±3.8s/100m, LOO 교차검증 ±19.5s/100m.
private const val HR_W_K = 2.080062
private const val HR_W_I = -0.012113
private const val HR_W_TV = 1.003630

/**
 * 심박 기반 실운동시간 추정 — 페이스 계산과 차트 휴식 표시의 단일 소스.
 *
 * 1) [hrRestMask] 골짜기 모델로 휴식을 분류해 골짜기 활동시간(Tv)을 얻고,
 * 2) 세션 칼로리와 심박 적분으로 보정한다 — 심박 모양이 같아도
 *    느린 연속 수영(칼로리 높음)과 휴식 위주(칼로리 낮음)를 칼로리가 가른다.
 * 3) 안전장치: 보정량은 Tv의 ±35% 이내, 페이스 하한 1'20"/100m, 기록시간 상한.
 *    칼로리가 없으면 보정 없이 Tv를 그대로 쓴다.
 * 차트 휴식 구간은 골짜기 위치·크기 그대로 두되, 총합이 보정된 휴식 총량을 넘으면
 * 긴 골짜기부터 예산만큼만 남긴다 ([pruneRestRanges]) — 페이스와 차트가 일치하도록.
 *
 * @param points (오프셋초, bpm) 목록 — [hrPoints] 결과. 60개 미만이면 추정 포기(null)
 * @param distanceM 세션 거리(미터) — 페이스 하한 클램프용
 * @param calories 세션 소모 칼로리(kcal) — 보정 입력. 0 이하면 보정 생략
 */
internal fun estimateActive(
    points: List<Pair<Int, Int>>,
    distanceM: Int,
    calories: Int,
    gapSec: Int = 5,
): HrEstimate? {
    if (points.size < 60) return null
    val rest = hrRestMask(points, gapSec = gapSec)
    val sm = hrSmoothed(points)
    // 기저 심박 = 세션 하위 5% (순간 글리치에 흔들리지 않는 개인 기저 근사)
    val floorBpm = points.map { it.second }.sorted()[points.size / 20]

    var recorded = 0L
    var valleyActive = 0L
    var integral = 0.0
    for (i in 0 until points.size - 1) {
        val dt = (points[i + 1].first - points[i].first).toLong()
        if (dt in 1..gapSec.toLong()) {
            recorded += dt
            integral += maxOf(0.0, sm[i] - floorBpm) * dt
            if (!rest[i]) valleyActive += dt
        }
    }
    if (recorded <= 0 || valleyActive <= 0) return null

    var act = if (calories > 0) {
        HR_W_K * calories + HR_W_I * integral + HR_W_TV * valleyActive
    } else {
        valleyActive.toDouble()
    }
    act = act.coerceIn(valleyActive * 0.65, valleyActive * 1.35) // 보정량 제한
    act = act.coerceAtLeast(distanceM * 0.8) // 페이스 하한 1'20"/100m
    act = act.coerceAtMost(recorded.toDouble())
    val activeSeconds = Math.round(act).toInt()

    // 차트 밴드: 마스크 골짜기를 그대로 쓰되 봉우리 진입부만 트림한다.
    // 페이스(activeSeconds)와 회색 총량이 정확히 일치하진 않지만, 차트는 심박이
    // 보여주는 골짜기를 정직하게 표시하는 게 더 자연스럽다.
    return HrEstimate(activeSeconds, chartRestRanges(points, gapSec = gapSec))
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
            val sessionsResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                )
            )

            val swimSessions = sessionsResponse.records.filter {
                it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL ||
                        it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
            }

            if (swimSessions.isEmpty()) return emptyList()

            // 같은 시간 범위의 거리/칼로리 레코드를 일괄 조회한다
            val distanceRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = DistanceRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                )
            ).records

            val calorieRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                )
            ).records

            // 심박/속도 레코드 — 권한이 없거나 실패해도 세션 수집은 계속한다.
            val heartRateRecords = readHeartRateRecords(startTime, endTime)
            val speedRecords = readSpeedRecords(startTime, endTime)

            swimSessions.map { session ->
                val duration = Duration.between(session.startTime, session.endTime)

                // 세션 시간 범위에 겹치는 거리 레코드의 합산
                val totalDistanceM = distanceRecords
                    .filter { it.startTime >= session.startTime && it.endTime <= session.endTime }
                    .sumOf { it.distance.inMeters }
                    .toInt()

                // 세션 시간 범위에 겹치는 칼로리 레코드의 합산
                val totalCalories = calorieRecords
                    .filter { it.startTime >= session.startTime && it.endTime <= session.endTime }
                    .sumOf { it.energy.inKilocalories }
                    .toInt()

                // 세션 시간 범위 내 심박 샘플 (시각+bpm — 최대/최소와 심박추정 양쪽에 사용)
                val hrSamples = heartRateRecords.asSequence()
                    .flatMap { it.samples }
                    .filter { it.time >= session.startTime && it.time <= session.endTime }
                    .map { it.time to it.beatsPerMinute }
                    .toList()
                val bpm = hrSamples.map { it.second }

                // 세션 시간 범위 내 속도 샘플 평균 (세그먼트/랩 없을 때 실운동시간 폴백)
                val speedSamples = speedRecords.asSequence()
                    .flatMap { it.samples }
                    .filter { it.time >= session.startTime && it.time <= session.endTime }
                    .map { it.speed.inMetersPerSecond }
                    .filter { it > 0 }
                    .toList()
                val avgSpeed = if (speedSamples.isEmpty()) 0.0 else speedSamples.average()

                // 실운동시간: 세그먼트/랩 → 속도 → 심박추정 순 폴백
                val hrPts = hrPoints(hrSamples)
                val hrEstimate = estimateActive(hrPts, totalDistanceM, totalCalories)
                val fromStructure = computeActiveSeconds(session.segments, session.laps)
                val fromSpeed = speedBasedActiveSeconds(totalDistanceM, avgSpeed)
                val activeSeconds = fromStructure ?: fromSpeed ?: hrEstimate?.activeSeconds
                // 진단: 워치가 세그먼트/랩/심박/속도를 실제로 써주는지 + 실운동시간 출처 확인용.
                Timber.d(
                    "수영 세션 %s: segments=%d, laps=%d, hr샘플=%d, 속도샘플=%d, 경과=%d초, 실운동=%s초(%s)",
                    session.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                    session.segments.size, session.laps.size, bpm.size, speedSamples.size,
                    duration.seconds, activeSeconds?.toString() ?: "없음",
                    when {
                        fromStructure != null -> "세그먼트/랩"
                        fromSpeed != null -> "속도"
                        hrEstimate != null -> "심박추정"
                        else -> "-"
                    },
                )

                SwimSession(
                    date = session.startTime.atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(),
                    startEpochSec = session.startTime.epochSecond,
                    distanceMeters = totalDistanceM,
                    durationSeconds = duration.seconds.toInt(),
                    calories = totalCalories,
                    hcRecordId = session.metadata.id,
                    maxHr = bpm.maxOrNull()?.toInt(),
                    minHr = bpm.minOrNull()?.toInt(),
                    activeSeconds = activeSeconds,
                    hrSeries = downsampleHr(hrSamples).takeIf { it.isNotEmpty() }
                        ?.let { encodeHrSeries(it, hrEstimate?.restRanges ?: emptyList()) },
                )
            }
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

            val distanceM = healthConnectClient.readRecords(
                ReadRecordsRequest(DistanceRecord::class, timeFilter)
            ).records.sumOf { it.distance.inMeters }.toInt()

            val calories = healthConnectClient.readRecords(
                ReadRecordsRequest(TotalCaloriesBurnedRecord::class, timeFilter)
            ).records.sumOf { it.energy.inKilocalories }.toInt()

            val hrSamples = readHeartRateRecords(session.startTime, session.endTime)
                .flatMap { it.samples }
                .map { it.time to it.beatsPerMinute }
            val bpm = hrSamples.map { it.second }
            val hrEstimate = estimateActive(hrPoints(hrSamples), distanceM, calories)

            val speedSamples = readSpeedRecords(session.startTime, session.endTime)
                .flatMap { it.samples }
                .map { it.speed.inMetersPerSecond }
                .filter { it > 0 }
            val avgSpeed = if (speedSamples.isEmpty()) 0.0 else speedSamples.average()

            SwimSession(
                date = session.startTime.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                startEpochSec = session.startTime.epochSecond,
                distanceMeters = distanceM,
                durationSeconds = duration.seconds.toInt(),
                calories = calories,
                hcRecordId = session.metadata.id,
                maxHr = bpm.maxOrNull()?.toInt(),
                minHr = bpm.minOrNull()?.toInt(),
                activeSeconds = computeActiveSeconds(session.segments, session.laps)
                    ?: speedBasedActiveSeconds(distanceM, avgSpeed)
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

    /**
     * 시간 범위 내 속도 레코드를 읽는다.
     * 속도 권한이 없거나 조회에 실패해도 빈 목록을 반환해 세션 동기화를 막지 않는다.
     */
    private suspend fun readSpeedRecords(startTime: Instant, endTime: Instant): List<SpeedRecord> {
        return try {
            healthConnectClient.readRecords(
                ReadRecordsRequest(
                    recordType = SpeedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                )
            ).records
        } catch (e: Exception) {
            Timber.w(e, "속도 레코드 읽기 실패 — 속도 없이 진행")
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
    }
}
