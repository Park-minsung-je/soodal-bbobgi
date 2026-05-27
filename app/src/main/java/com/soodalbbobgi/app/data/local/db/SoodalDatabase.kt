package com.soodalbbobgi.app.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.soodalbbobgi.app.data.local.entity.GachaBoxEntity
import com.soodalbbobgi.app.data.local.entity.GachaBoxItemEntity
import com.soodalbbobgi.app.data.local.entity.GachaHistoryEntity
import com.soodalbbobgi.app.data.local.entity.InventoryItemEntity
import com.soodalbbobgi.app.data.local.entity.ProfileCardEntity
import com.soodalbbobgi.app.data.local.entity.SwimLogEntity
import com.soodalbbobgi.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        SwimLogEntity::class,
        InventoryItemEntity::class,
        ProfileCardEntity::class,
        GachaHistoryEntity::class,
        GachaBoxEntity::class,
        GachaBoxItemEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class SoodalDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun swimLogDao(): SwimLogDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun profileCardDao(): ProfileCardDao
    abstract fun gachaHistoryDao(): GachaHistoryDao
    abstract fun gachaBoxDao(): GachaBoxDao
}
