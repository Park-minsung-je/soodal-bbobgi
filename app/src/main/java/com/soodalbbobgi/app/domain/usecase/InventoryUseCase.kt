package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class InventoryUseCase @Inject constructor(
    private val inventoryRepo: InventoryRepository,
) {
    fun getByCategory(userId: String, category: String): Flow<List<InventoryItem>> =
        inventoryRepo.getByCategory(userId, category)

    fun getEquipped(userId: String, slot: String): Flow<InventoryItem?> =
        inventoryRepo.getEquipped(userId, slot)

    suspend fun equip(userId: String, itemId: Long, slot: String) {
        inventoryRepo.unequipSlot(userId, slot)
        inventoryRepo.equipItem(itemId, slot)
    }

    suspend fun unequip(userId: String, slot: String) =
        inventoryRepo.unequipSlot(userId, slot)
}
