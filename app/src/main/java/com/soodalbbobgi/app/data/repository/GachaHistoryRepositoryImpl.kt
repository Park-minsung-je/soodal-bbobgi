package com.soodalbbobgi.app.data.repository

import com.soodalbbobgi.app.data.local.db.GachaHistoryDao
import com.soodalbbobgi.app.data.local.entity.GachaHistoryEntity
import com.soodalbbobgi.app.domain.model.GachaHistory
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.repository.GachaHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GachaHistoryRepositoryImpl @Inject constructor(
    private val dao: GachaHistoryDao,
) : GachaHistoryRepository {
    override suspend fun record(history: GachaHistory): Long = dao.insert(history.toEntity())
    override fun getRecent(userId: String, limit: Int): Flow<List<GachaHistory>> =
        dao.getRecent(userId, limit).map { list -> list.map { it.toDomain() } }
}

private fun GachaHistoryEntity.toDomain() = GachaHistory(
    id = id, userId = userId, timestamp = timestamp, boxId = boxId,
    itemId = itemId, grade = Grade.fromString(grade), wasNew = wasNew,
    pearlsReceived = pearlsReceived, shellsSpent = shellsSpent,
    pityCountAtPull = pityCountAtPull,
)
private fun GachaHistory.toEntity() = GachaHistoryEntity(
    id = id, userId = userId, timestamp = timestamp, boxId = boxId,
    itemId = itemId, grade = grade.name, wasNew = wasNew,
    pearlsReceived = pearlsReceived, shellsSpent = shellsSpent,
    pityCountAtPull = pityCountAtPull,
)
