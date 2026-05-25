package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.model.ProfileCard
import com.soodalbbobgi.app.domain.repository.ProfileCardRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProfileUseCase @Inject constructor(
    private val profileRepo: ProfileCardRepository,
) {
    fun getProfileCard(userId: String): Flow<ProfileCard?> =
        profileRepo.getProfileCard(userId)

    suspend fun saveProfileCard(card: ProfileCard) {
        val clamped = card.copy(
            characterX = card.characterX.coerceIn(0f, 1f),
            characterY = card.characterY.coerceIn(0f, 1f),
            characterScale = card.characterScale.coerceIn(0.3f, 1f),
        )
        profileRepo.saveProfileCard(clamped)
    }
}
