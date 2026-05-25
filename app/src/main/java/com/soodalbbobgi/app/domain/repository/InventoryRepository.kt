package com.soodalbbobgi.app.domain.repository

import com.soodalbbobgi.app.domain.model.InventoryItem
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getAll(userId: String): Flow<List<InventoryItem>>
    fun getByCategory(userId: String, category: String): Flow<List<InventoryItem>>
    fun getEquipped(userId: String, slot: String): Flow<InventoryItem?>
    suspend fun addItem(item: InventoryItem): Long
    suspend fun equipItem(itemId: Long, slot: String)
    suspend fun unequipSlot(userId: String, slot: String)
    suspend fun countDuplicates(userId: String, boxItemId: Long): Int
}
