package com.soodalbbobgi.app.data.repository

import com.soodalbbobgi.app.data.local.db.ProfileCardDao
import com.soodalbbobgi.app.data.local.entity.ProfileCardEntity
import com.soodalbbobgi.app.domain.model.ProfileCard
import com.soodalbbobgi.app.domain.repository.ProfileCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ProfileCardRepositoryImpl @Inject constructor(
    private val dao: ProfileCardDao,
) : ProfileCardRepository {
    override fun getProfileCard(userId: String): Flow<ProfileCard?> =
        dao.getByUserId(userId).map { it?.toDomain() }
    override suspend fun saveProfileCard(card: ProfileCard) = dao.upsert(card.toEntity())
}

private fun ProfileCardEntity.toDomain() = ProfileCard(
    userId = userId, backgroundItemId = backgroundItemId,
    characterItemId = characterItemId, borderItemId = borderItemId,
    characterX = characterX, characterY = characterY,
    characterScale = characterScale, customText = customText,
    textStyle = textStyle,
)
private fun ProfileCard.toEntity() = ProfileCardEntity(
    userId = userId, backgroundItemId = backgroundItemId,
    characterItemId = characterItemId, borderItemId = borderItemId,
    characterX = characterX, characterY = characterY,
    characterScale = characterScale, customText = customText,
    textStyle = textStyle, lastEditedAt = System.currentTimeMillis(),
)
