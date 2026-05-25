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
    override suspend fun addSwimLog(log: SwimLog) = dao.insert(log.toEntity())
    override fun getByDate(date: String): Flow<SwimLog?> =
        dao.getByDate(date).map { it?.toDomain() }
    override fun getByDateRange(startDate: String, endDate: String): Flow<List<SwimLog>> =
        dao.getByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }
    override suspend fun getStats(startDate: String, endDate: String) = SwimStats(
        totalDistanceMeters = dao.getTotalDistance(startDate, endDate),
        swimCount = dao.getSwimCount(startDate, endDate),
        totalCalories = dao.getTotalCalories(startDate, endDate),
    )
    override suspend fun getLatest(): SwimLog? = dao.getLatest()?.toDomain()
}

private fun SwimLogEntity.toDomain() = SwimLog(
    id = id, userId = userId, date = date, distanceMeters = distanceMeters,
    durationSeconds = durationSeconds, calories = calories,
    strokeFreeStyle = strokeFreeStyle, strokeBreast = strokeBreast,
    strokeBack = strokeBack, strokeFly = strokeFly,
    source = source, shellsEarned = shellsEarned,
)

private fun SwimLog.toEntity() = SwimLogEntity(
    id = id, userId = userId, date = date, distanceMeters = distanceMeters,
    durationSeconds = durationSeconds, calories = calories,
    strokeFreeStyle = strokeFreeStyle, strokeBreast = strokeBreast,
    strokeBack = strokeBack, strokeFly = strokeFly,
    source = source, shellsEarned = shellsEarned,
    createdAt = System.currentTimeMillis(),
)
