package com.soodalbbobgi.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soodalbbobgi.app.data.local.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity): Long

    @Query("SELECT * FROM inventory_items WHERE userId = :userId ORDER BY acquiredAt DESC")
    fun getAll(userId: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND category = :category ORDER BY grade DESC, acquiredAt DESC")
    fun getByCategory(userId: String, category: String): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE userId = :userId AND isEquippedAs = :slot LIMIT 1")
    fun getEquipped(userId: String, slot: String): Flow<InventoryItemEntity?>

    @Query("UPDATE inventory_items SET isEquippedAs = 'NONE' WHERE userId = :userId AND isEquippedAs = :slot")
    suspend fun unequipSlot(userId: String, slot: String)

    @Query("UPDATE inventory_items SET isEquippedAs = :slot WHERE id = :itemId")
    suspend fun equip(itemId: Long, slot: String)

    @Query("SELECT COUNT(*) FROM inventory_items WHERE boxItemId = :boxItemId AND userId = :userId")
    suspend fun countByBoxItemId(userId: String, boxItemId: Long): Int
}
