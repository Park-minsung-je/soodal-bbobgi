package com.soodalbbobgi.app.data.repository

import com.soodalbbobgi.app.data.local.db.InventoryDao
import com.soodalbbobgi.app.data.local.entity.InventoryItemEntity
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val dao: InventoryDao,
) : InventoryRepository {
    override fun getAll(userId: String): Flow<List<InventoryItem>> =
        dao.getAll(userId).map { list -> list.map { it.toDomain() } }
    override fun getByCategory(userId: String, category: String): Flow<List<InventoryItem>> =
        dao.getByCategory(userId, category).map { list -> list.map { it.toDomain() } }
    override fun getEquipped(userId: String, slot: String): Flow<InventoryItem?> =
        dao.getEquipped(userId, slot).map { it?.toDomain() }
    override suspend fun addItem(item: InventoryItem): Long = dao.insert(item.toEntity())
    override suspend fun equipItem(itemId: Long, slot: String) = dao.equip(itemId, slot)
    override suspend fun unequipSlot(userId: String, slot: String) = dao.unequipSlot(userId, slot)
    override suspend fun countDuplicates(userId: String, boxItemId: Long): Int =
        dao.countByBoxItemId(userId, boxItemId)
}

private fun InventoryItemEntity.toDomain() = InventoryItem(
    id = id, userId = userId, boxItemId = boxItemId,
    grade = Grade.fromString(grade), category = category,
    isEquippedAs = isEquippedAs, acquiredAt = acquiredAt,
)
private fun InventoryItem.toEntity() = InventoryItemEntity(
    id = id, userId = userId, boxItemId = boxItemId,
    grade = grade.name, category = category,
    isEquippedAs = isEquippedAs, acquiredAt = acquiredAt,
)
