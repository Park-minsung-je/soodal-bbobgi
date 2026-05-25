package com.soodalbbobgi.app.domain.repository

import com.soodalbbobgi.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUser(userId: String): Flow<User?>
    suspend fun createUser(user: User)
    suspend fun updateNickname(userId: String, nickname: String)
    suspend fun updateCurrency(userId: String, shells: Int, pearls: Int)
    suspend fun updatePityCounter(userId: String, pity: Int)
    suspend fun updateLastShellGrantDate(userId: String, date: String)
}
