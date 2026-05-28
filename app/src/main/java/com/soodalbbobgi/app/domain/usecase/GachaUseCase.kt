package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.model.GachaBoxItem
import com.soodalbbobgi.app.domain.model.GachaHistory
import com.soodalbbobgi.app.domain.model.GachaResult
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.InventoryItem
import com.soodalbbobgi.app.domain.repository.GachaHistoryRepository
import com.soodalbbobgi.app.domain.repository.GachaRepository
import com.soodalbbobgi.app.domain.repository.InventoryRepository
import com.soodalbbobgi.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.random.Random

class GachaUseCase @Inject constructor(
    private val userRepo: UserRepository,
    private val gachaRepo: GachaRepository,
    private val inventoryRepo: InventoryRepository,
    private val historyRepo: GachaHistoryRepository,
    private val currencyUseCase: CurrencyUseCase,
) {
    suspend fun pull(userId: String, boxId: Long, count: Int = 1): List<GachaResult> {
        val shellCost = if (count >= 10) 9 else count
        currencyUseCase.spendShells(userId, shellCost)

        val items = gachaRepo.getBoxItems(boxId).first()
        require(items.isNotEmpty()) { "Box $boxId has no items" }

        val user = userRepo.getUser(userId).first()!!
        var pity = user.pityCounter

        val results = mutableListOf<GachaResult>()
        repeat(count) {
            val selected = selectItem(items, pity)
            val isDuplicate = inventoryRepo.countDuplicates(userId, selected.id) > 0
            val pearlsEarned = if (isDuplicate) selected.grade.pearlValue else 0

            inventoryRepo.addItem(
                InventoryItem(
                    userId = userId,
                    itemId = selected.id,
                    grade = selected.grade,
                    category = gachaRepo.getBoxById(boxId)!!.category,
                    acquiredAt = System.currentTimeMillis(),
                )
            )

            if (isDuplicate) {
                currencyUseCase.addPearls(userId, pearlsEarned)
            }

            pity = if (selected.grade == Grade.SSR) 0 else pity + 1

            historyRepo.record(
                GachaHistory(
                    userId = userId,
                    timestamp = System.currentTimeMillis(),
                    boxId = boxId,
                    itemId = selected.id,
                    grade = selected.grade,
                    wasNew = !isDuplicate,
                    pearlsReceived = pearlsEarned,
                    shellsSpent = if (results.isEmpty()) shellCost else 0,
                    pityCountAtPull = pity,
                )
            )

            results.add(
                GachaResult(
                    item = selected,
                    wasNew = !isDuplicate,
                    pearlsEarned = pearlsEarned,
                    shellsSpent = if (results.isEmpty()) shellCost else 0,
                )
            )
        }

        userRepo.updatePityCounter(userId, pity)
        return results
    }

    internal fun selectItem(items: List<GachaBoxItem>, pityCounter: Int): GachaBoxItem {
        if (pityCounter >= PITY_THRESHOLD) {
            val ssrItems = items.filter { it.grade == Grade.SSR }
            if (ssrItems.isNotEmpty()) return ssrItems.random()
        }

        val totalWeight = items.sumOf { it.weight }
        var roll = Random.nextInt(totalWeight)
        for (item in items) {
            roll -= item.weight
            if (roll < 0) return item
        }
        return items.last()
    }

    companion object {
        const val PITY_THRESHOLD = 90
    }
}
