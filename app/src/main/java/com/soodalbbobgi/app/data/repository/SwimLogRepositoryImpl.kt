package com.soodalbbobgi.app.data.repository

import com.soodalbbobgi.app.data.local.db.SwimLogDao
import com.soodalbbobgi.app.data.local.entity.SwimLogEntity
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.model.SwimStats
import com.soodalbbobgi.app.domain.repository.SwimLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SwimLogRepositoryImpl @Inject constructor(
    private val dao: SwimLogDao,
) : SwimLogRepository {
    override suspend fun addSwimLog(log: SwimLog) {
        dao.insert(log.toEntity())
    }
    override fun getByDate(date: String): Flow<List<SwimLog>> =
        dao.getByDate(date).map { list -> list.map { it.toDomain() } }
    override suspend fun getLogsForDateOnce(date: String): List<SwimLog> =
        dao.getByDateOnce(date).map { it.toDomain() }
    override suspend fun getByHcRecordId(hcRecordId: String): SwimLog? =
        dao.getByHcRecordId(hcRecordId)?.toDomain()
    override fun getByDateRange(startDate: String, endDate: String): Flow<List<SwimLog>> =
        dao.getByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }
    override suspend fun updateShellsEarned(date: String, shellsEarned: Int) =
        dao.updateShellsEarned(date, shellsEarned)
    override suspend fun updateStrokes(date: String, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int) =
        dao.updateStrokes(date, free, breast, back, fly, mixed, kick)
    override suspend fun updateStrokesById(id: Long, free: Int, breast: Int, back: Int, fly: Int, mixed: Int, kick: Int) =
        dao.updateStrokesById(id, free, breast, back, fly, mixed, kick)
    override suspend fun markSynced(date: String) = dao.markSynced(date)
    override suspend fun getUnsyncedDates(): List<String> = dao.getUnsyncedDates()
    override suspend fun updateFromHc(id: Long, log: SwimLog) =
        dao.updateFromHc(
            id = id,
            hcRecordId = log.hcRecordId,
            startEpochSec = log.startEpochSec,
            distance = log.distanceMeters,
            duration = log.durationSeconds,
            calories = log.calories,
            maxHr = log.maxHr,
            minHr = log.minHr,
            activeSeconds = log.activeSeconds,
            hrSeries = log.hrSeries,
        )
    override suspend fun deleteByDate(date: String) = dao.deleteByDate(date)
    override suspend fun deleteByHcRecordId(hcRecordId: String) = dao.deleteByHcRecordId(hcRecordId)
    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun getStats(startDate: String, endDate: String) = SwimStats(
        totalDistanceMeters = dao.getTotalDistance(startDate, endDate),
        swimCount = dao.getSwimCount(startDate, endDate),
        totalCalories = dao.getTotalCalories(startDate, endDate),
    )
    override suspend fun getLatest(): SwimLog? = dao.getLatest()?.toDomain()
}

private fun SwimLogEntity.toDomain() = SwimLog(
    id = id, userId = userId, date = date, startEpochSec = startEpochSec,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds, calories = calories,
    strokeFreestyleM = strokeFreestyleM, strokeBreastM = strokeBreastM,
    strokeBackM = strokeBackM, strokeFlyM = strokeFlyM,
    strokeMixedM = strokeMixedM, strokeKickM = strokeKickM,
    source = source, shellsEarned = shellsEarned, synced = synced,
    hcRecordId = hcRecordId,
    maxHr = maxHr, minHr = minHr, avgHr = avgHr, activeSeconds = activeSeconds, hrSeries = hrSeries,
)

private fun SwimLog.toEntity() = SwimLogEntity(
    id = id, userId = userId, date = date, startEpochSec = startEpochSec,
    distanceMeters = distanceMeters,
    durationSeconds = durationSeconds, calories = calories,
    strokeFreestyleM = strokeFreestyleM, strokeBreastM = strokeBreastM,
    strokeBackM = strokeBackM, strokeFlyM = strokeFlyM,
    strokeMixedM = strokeMixedM, strokeKickM = strokeKickM,
    source = source, shellsEarned = shellsEarned, synced = synced,
    hcRecordId = hcRecordId,
    maxHr = maxHr, minHr = minHr, avgHr = avgHr, activeSeconds = activeSeconds, hrSeries = hrSeries,
    createdAt = System.currentTimeMillis(),
)
