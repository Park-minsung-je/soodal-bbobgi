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
 * @property activeSeconds 실제 운동 시간(초) — 세그먼트(휴식 제외)나 랩에서 계산. 둘 다 없으면 null
 */
data class SwimSession(
    val date: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
    val hcRecordId: String? = null,
    val maxHr: Int? = null,
    val minHr: Int? = null,
    val activeSeconds: Int? = null,
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
 * [실험] Otsu 방식 임계값 — 이중봉 분포(운동 고심박/휴식 저심박)를 두 무리로 가르는 경계.
 * 분포 폭이 20bpm 미만(단봉·변별력 없음)이면 null.
 */
internal fun otsuThreshold(values: List<Long>): Long? {
    if (values.isEmpty()) return null
    val min = values.min()
    val max = values.max()
    if (max - min < 20) return null

    val hist = IntArray((max - min + 1).toInt())
    values.forEach { hist[(it - min).toInt()]++ }
    val total = values.size

    var sumAll = 0.0
    hist.forEachIndexed { i, c -> sumAll += i.toDouble() * c }

    var sumB = 0.0
    var wB = 0
    var best = -1.0
    var bestIdx = 0
    for (i in hist.indices) {
        wB += hist[i]
        if (wB == 0) continue
        val wF = total - wB
        if (wF == 0) break
        sumB += i.toDouble() * hist[i]
        val mB = sumB / wB
        val mF = (sumAll - sumB) / wF
        val between = wB.toDouble() * wF * (mB - mF) * (mB - mF)
        if (between > best) {
            best = between
            bestIdx = i
        }
    }
    return min + bestIdx + 1
}

/**
 * [실험] 심박 기반 실운동시간 추정(초).
 * 심박 분포를 Otsu 임계값으로 운동/휴식 두 무리로 가르고, 임계 이상인 샘플 구간만 합산한다.
 * 심박은 운동을 멈춘 뒤에도 잠시 높게 유지되므로 약간 과대 추정될 수 있다.
 *
 * @param samples (시각, bpm) 목록. 60개 미만이거나 분포가 단봉이면 추정 포기(null)
 */
internal fun hrActiveSeconds(samples: List<Pair<Instant, Long>>): Int? {
    if (samples.size < 60) return null
    val threshold = otsuThreshold(samples.map { it.second }) ?: return null

    val sorted = samples.sortedBy { it.first }
    var active = 0L
    for (i in 0 until sorted.size - 1) {
        val dt = Duration.between(sorted[i].first, sorted[i + 1].first).seconds
        // 샘플 공백(워치 이탈 등)이 큰 구간은 활동으로 치지 않는다.
        if (dt in 1..10 && sorted[i].second >= threshold) active += dt
    }
    return if (active > 0) active.toInt() else null
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

                // 실운동시간: 세그먼트/랩 → 속도 → [실험] 심박추정 순 폴백
                val fromStructure = computeActiveSeconds(session.segments, session.laps)
                val fromSpeed = speedBasedActiveSeconds(totalDistanceM, avgSpeed)
                val fromHr = hrActiveSeconds(hrSamples)
                val activeSeconds = fromStructure ?: fromSpeed ?: fromHr
                // 진단: 워치가 세그먼트/랩/심박/속도를 실제로 써주는지 + 실운동시간 출처 확인용.
                Timber.d(
                    "수영 세션 %s: segments=%d, laps=%d, hr샘플=%d, 속도샘플=%d, 경과=%d초, 실운동=%s초(%s)",
                    session.startTime.atZone(ZoneId.systemDefault()).toLocalDate(),
                    session.segments.size, session.laps.size, bpm.size, speedSamples.size,
                    duration.seconds, activeSeconds?.toString() ?: "없음",
                    when {
                        fromStructure != null -> "세그먼트/랩"
                        fromSpeed != null -> "속도"
                        fromHr != null -> "심박추정"
                        else -> "-"
                    },
                )

                SwimSession(
                    date = session.startTime.atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(),
                    distanceMeters = totalDistanceM,
                    durationSeconds = duration.seconds.toInt(),
                    calories = totalCalories,
                    hcRecordId = session.metadata.id,
                    maxHr = bpm.maxOrNull()?.toInt(),
                    minHr = bpm.minOrNull()?.toInt(),
                    activeSeconds = activeSeconds,
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

            val speedSamples = readSpeedRecords(session.startTime, session.endTime)
                .flatMap { it.samples }
                .map { it.speed.inMetersPerSecond }
                .filter { it > 0 }
            val avgSpeed = if (speedSamples.isEmpty()) 0.0 else speedSamples.average()

            SwimSession(
                date = session.startTime.atZone(ZoneId.systemDefault()).toLocalDate().toString(),
                distanceMeters = distanceM,
                durationSeconds = duration.seconds.toInt(),
                calories = calories,
                hcRecordId = session.metadata.id,
                maxHr = bpm.maxOrNull()?.toInt(),
                minHr = bpm.minOrNull()?.toInt(),
                activeSeconds = computeActiveSeconds(session.segments, session.laps)
                    ?: speedBasedActiveSeconds(distanceM, avgSpeed)
                    ?: hrActiveSeconds(hrSamples),
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
