package com.soodalbbobgi.app.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.soodalbbobgi.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getById(userId: String): Flow<UserEntity?>

    @Query("UPDATE users SET nickname = :nickname, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateNickname(userId: String, nickname: String, updatedAt: Long)

    @Query("UPDATE users SET shellBalance = :shells, pearlBalance = :pearls, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateCurrency(userId: String, shells: Int, pearls: Int, updatedAt: Long)

    @Query("UPDATE users SET pityCounter = :pity, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updatePityCounter(userId: String, pity: Int, updatedAt: Long)

    @Query("UPDATE users SET lastShellGrantDate = :date, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateLastShellGrantDate(userId: String, date: String, updatedAt: Long)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun delete(userId: String)
}
