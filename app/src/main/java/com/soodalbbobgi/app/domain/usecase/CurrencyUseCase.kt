package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class CurrencyUseCase @Inject constructor(
    private val userRepo: UserRepository,
) {
    suspend fun grantDailyShells(userId: String, todayDate: String): Int {
        val user = userRepo.getUser(userId).first() ?: return 0
        if (user.lastShellGrantDate == todayDate) return 0

        val earned = Random.nextInt(1, 4)
        userRepo.updateCurrency(userId, user.shellBalance + earned, user.pearlBalance)
        userRepo.updateLastShellGrantDate(userId, todayDate)
        return earned
    }

    suspend fun spendShells(userId: String, amount: Int) {
        val user = userRepo.getUser(userId).first()
            ?: throw IllegalStateException("User not found")
        require(user.shellBalance >= amount) { "Insufficient shells: has ${user.shellBalance}, needs $amount" }
        userRepo.updateCurrency(userId, user.shellBalance - amount, user.pearlBalance)
    }

    suspend fun addPearls(userId: String, amount: Int) {
        val user = userRepo.getUser(userId).first()
            ?: throw IllegalStateException("User not found")
        userRepo.updateCurrency(userId, user.shellBalance, user.pearlBalance + amount)
    }

    suspend fun spendPearls(userId: String, amount: Int) {
        val user = userRepo.getUser(userId).first()
            ?: throw IllegalStateException("User not found")
        require(user.pearlBalance >= amount) { "Insufficient pearls: has ${user.pearlBalance}, needs $amount" }
        userRepo.updateCurrency(userId, user.shellBalance, user.pearlBalance - amount)
    }
}
