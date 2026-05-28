package com.soodalbbobgi.app.data.repository

import com.soodalbbobgi.app.data.local.db.GachaBoxDao
import com.soodalbbobgi.app.data.local.entity.GachaBoxEntity
import com.soodalbbobgi.app.data.local.entity.GachaBoxItemEntity
import com.soodalbbobgi.app.domain.model.GachaBox
import com.soodalbbobgi.app.domain.model.GachaBoxItem
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.repository.GachaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GachaRepositoryImpl @Inject constructor(
    private val dao: GachaBoxDao,
) : GachaRepository {
    override fun getAllActiveBoxes(): Flow<List<GachaBox>> =
        dao.getAllActive().map { list -> list.map { it.toDomain() } }
    override suspend fun getBoxById(boxId: Long): GachaBox? =
        dao.getById(boxId)?.toDomain()
    override fun getBoxItems(boxId: Long): Flow<List<GachaBoxItem>> =
        dao.getItemsForBox(boxId).map { list -> list.map { it.toDomain() } }
    override suspend fun getBoxItemById(itemId: Long): GachaBoxItem? =
        dao.getItemById(itemId)?.toDomain()
    override suspend fun getBoxItemsByIds(itemIds: List<Long>): List<GachaBoxItem> =
        if (itemIds.isEmpty()) emptyList() else dao.getItemsByIds(itemIds).map { it.toDomain() }
    override suspend fun saveBox(box: GachaBox) =
        dao.insertBox(box.toEntity())
    override suspend fun saveBoxItem(item: GachaBoxItem) =
        dao.insertBoxItems(listOf(item.toEntity()))
}

private fun GachaBoxEntity.toDomain() = GachaBox(
    id = id, name = name, description = description, category = category,
)
private fun GachaBoxItemEntity.toDomain() = GachaBoxItem(
    id = id, boxId = boxId, itemKey = itemKey, name = name,
    grade = Grade.fromString(grade), weight = weight, imageAsset = imageAsset,
)
private fun GachaBox.toEntity() = GachaBoxEntity(
    id = id, name = name, description = description, category = category,
    updatedAt = System.currentTimeMillis(),
)
private fun GachaBoxItem.toEntity() = GachaBoxItemEntity(
    id = id, boxId = boxId, itemKey = itemKey, name = name,
    grade = grade.name, weight = weight, imageAsset = imageAsset,
)
