package com.soodalbbobgi.app.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
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
 */
data class SwimSession(
    val date: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
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

                SwimSession(
                    date = session.startTime.atZone(ZoneId.systemDefault())
                        .toLocalDate().toString(),
                    distanceMeters = totalDistanceM,
                    durationSeconds = duration.seconds.toInt(),
                    calories = totalCalories,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Health Connect 수영 세션 읽기 실패")
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
