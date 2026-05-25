package com.soodalbbobgi.app.domain.repository

import com.soodalbbobgi.app.domain.model.ProfileCard
import kotlinx.coroutines.flow.Flow

interface ProfileCardRepository {
    fun getProfileCard(userId: String): Flow<ProfileCard?>
    suspend fun saveProfileCard(card: ProfileCard)
}
