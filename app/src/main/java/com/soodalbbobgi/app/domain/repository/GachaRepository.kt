package com.soodalbbobgi.app.domain.repository

import com.soodalbbobgi.app.domain.model.GachaBox
import com.soodalbbobgi.app.domain.model.GachaBoxItem
import kotlinx.coroutines.flow.Flow

interface GachaRepository {
    fun getAllActiveBoxes(): Flow<List<GachaBox>>
    suspend fun getBoxById(boxId: Long): GachaBox?
    fun getBoxItems(boxId: Long): Flow<List<GachaBoxItem>>
    suspend fun getBoxItemById(itemId: Long): GachaBoxItem?
}
