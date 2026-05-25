package com.soodalbbobgi.app.data.repository

import com.soodalbbobgi.app.data.local.db.UserDao
import com.soodalbbobgi.app.data.local.entity.UserEntity
import com.soodalbbobgi.app.domain.model.User
import com.soodalbbobgi.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val dao: UserDao,
) : UserRepository {
    override fun getUser(userId: String): Flow<User?> =
        dao.getById(userId).map { it?.toDomain() }
    override suspend fun createUser(user: User) {
        val now = System.currentTimeMillis()
        dao.insert(user.toEntity(now))
    }
    override suspend fun updateNickname(userId: String, nickname: String) =
        dao.updateNickname(userId, nickname, System.currentTimeMillis())
    override suspend fun updateCurrency(userId: String, shells: Int, pearls: Int) =
        dao.updateCurrency(userId, shells, pearls, System.currentTimeMillis())
    override suspend fun updatePityCounter(userId: String, pity: Int) =
        dao.updatePityCounter(userId, pity, System.currentTimeMillis())
    override suspend fun updateLastShellGrantDate(userId: String, date: String) =
        dao.updateLastShellGrantDate(userId, date, System.currentTimeMillis())
}

private fun UserEntity.toDomain() = User(
    id = id, nickname = nickname, shellBalance = shellBalance,
    pearlBalance = pearlBalance, pityCounter = pityCounter,
    lastShellGrantDate = lastShellGrantDate, authProvider = authProvider,
)

private fun User.toEntity(now: Long) = UserEntity(
    id = id, nickname = nickname, shellBalance = shellBalance,
    pearlBalance = pearlBalance, pityCounter = pityCounter,
    lastShellGrantDate = lastShellGrantDate, authProvider = authProvider,
    createdAt = now, updatedAt = now,
)
