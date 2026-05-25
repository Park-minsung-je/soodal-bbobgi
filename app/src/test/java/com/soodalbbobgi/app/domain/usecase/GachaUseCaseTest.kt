package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.model.GachaBox
import com.soodalbbobgi.app.domain.model.GachaBoxItem
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.User
import com.soodalbbobgi.app.domain.repository.GachaHistoryRepository
import com.soodalbbobgi.app.domain.repository.GachaRepository
import com.soodalbbobgi.app.domain.repository.InventoryRepository
import com.soodalbbobgi.app.domain.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class GachaUseCaseTest {
    private lateinit var userRepo: UserRepository
    private lateinit var gachaRepo: GachaRepository
    private lateinit var inventoryRepo: InventoryRepository
    private lateinit var historyRepo: GachaHistoryRepository
    private lateinit var currencyUseCase: CurrencyUseCase
    private lateinit var useCase: GachaUseCase

    private val testItems = listOf(
        GachaBoxItem(1, 1, "n1", "N Item", Grade.N, 5000, "n.png"),
        GachaBoxItem(2, 1, "r1", "R Item", Grade.R, 4000, "r.png"),
        GachaBoxItem(3, 1, "sr1", "SR Item", Grade.SR, 950, "sr.png"),
        GachaBoxItem(4, 1, "ssr1", "SSR Item", Grade.SSR, 50, "ssr.png"),
    )

    private val testUser = User("u1", "Test", 10, 0, 0, null, "GOOGLE")
    private val testBox = GachaBox(1, "Test Box", "desc", "CHARACTER")

    @Before
    fun setup() {
        userRepo = mockk(relaxed = true)
        gachaRepo = mockk(relaxed = true)
        inventoryRepo = mockk(relaxed = true)
        historyRepo = mockk(relaxed = true)
        currencyUseCase = mockk(relaxed = true)
        useCase = GachaUseCase(userRepo, gachaRepo, inventoryRepo, historyRepo, currencyUseCase)

        coEvery { userRepo.getUser("u1") } returns flowOf(testUser)
        coEvery { gachaRepo.getBoxItems(1) } returns flowOf(testItems)
        coEvery { gachaRepo.getBoxById(1) } returns testBox
        coEvery { inventoryRepo.countDuplicates(any(), any()) } returns 0
        coEvery { inventoryRepo.addItem(any()) } returns 1L
        coEvery { historyRepo.record(any()) } returns 1L
    }

    @Test
    fun `selectItem returns valid item from weighted pool`() {
        val result = useCase.selectItem(testItems, 0)
        assertThat(result).isIn(testItems)
    }

    @Test
    fun `selectItem guarantees SSR at pity threshold`() {
        val result = useCase.selectItem(testItems, 90)
        assertThat(result.grade).isEqualTo(Grade.SSR)
    }

    @Test
    fun `pull single costs 1 shell`() = runTest {
        useCase.pull("u1", 1, 1)
        coVerify { currencyUseCase.spendShells("u1", 1) }
    }

    @Test
    fun `pull 10 costs 9 shells`() = runTest {
        useCase.pull("u1", 1, 10)
        coVerify { currencyUseCase.spendShells("u1", 9) }
    }

    @Test
    fun `duplicate item grants pearls`() = runTest {
        coEvery { inventoryRepo.countDuplicates("u1", any()) } returns 1

        val results = useCase.pull("u1", 1, 1)

        assertThat(results[0].wasNew).isFalse()
        assertThat(results[0].pearlsEarned).isGreaterThan(0)
        coVerify { currencyUseCase.addPearls("u1", any()) }
    }

    @Test
    fun `pity counter resets on SSR`() = runTest {
        val highPityUser = testUser.copy(pityCounter = 90)
        coEvery { userRepo.getUser("u1") } returns flowOf(highPityUser)

        useCase.pull("u1", 1, 1)

        coVerify { userRepo.updatePityCounter("u1", 0) }
    }
}
