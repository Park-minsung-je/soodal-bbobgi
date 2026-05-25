package com.soodalbbobgi.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soodalbbobgi.app.data.local.entity.GachaBoxEntity
import com.soodalbbobgi.app.data.local.entity.GachaBoxItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GachaBoxDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBox(box: GachaBoxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBoxItems(items: List<GachaBoxItemEntity>)

    @Query("SELECT * FROM gacha_boxes WHERE isActive = 1 ORDER BY id ASC")
    fun getAllActive(): Flow<List<GachaBoxEntity>>

    @Query("SELECT * FROM gacha_boxes WHERE id = :boxId")
    suspend fun getById(boxId: Long): GachaBoxEntity?

    @Query("SELECT * FROM gacha_box_items WHERE boxId = :boxId ORDER BY grade DESC, weight DESC")
    fun getItemsForBox(boxId: Long): Flow<List<GachaBoxItemEntity>>

    @Query("SELECT * FROM gacha_box_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Long): GachaBoxItemEntity?
}
