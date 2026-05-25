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
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

data class SwimSession(
    val date: String,
    val distanceMeters: Int,
    val durationSeconds: Int,
    val calories: Int,
)

@Singleton
class HealthConnectManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val healthConnectClient by lazy {
        HealthConnectClient.getOrCreate(context)
    }

    val requiredPermissions = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
    )

    suspend fun hasAllPermissions(): Boolean {
        val granted = healthConnectClient.permissionController.getGrantedPermissions()
        return requiredPermissions.all { it in granted }
    }

    suspend fun readSwimSessions(startTime: Instant, endTime: Instant): List<SwimSession> {
        val sessions = healthConnectClient.readRecords(
            ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
            )
        )

        return sessions.records
            .filter { it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL ||
                    it.exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER }
            .map { session ->
                val duration = java.time.Duration.between(session.startTime, session.endTime)
                SwimSession(
                    date = session.startTime.atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),
                    distanceMeters = 0,
                    durationSeconds = duration.seconds.toInt(),
                    calories = 0,
                )
            }
    }

    companion object {
        fun isAvailable(context: Context): Int {
            return HealthConnectClient.getSdkStatus(context)
        }
    }
}
