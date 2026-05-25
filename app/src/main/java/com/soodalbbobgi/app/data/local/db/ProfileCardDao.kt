package com.soodalbbobgi.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soodalbbobgi.app.data.local.entity.ProfileCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: ProfileCardEntity)

    @Query("SELECT * FROM profile_cards WHERE userId = :userId")
    fun getByUserId(userId: String): Flow<ProfileCardEntity?>
}
