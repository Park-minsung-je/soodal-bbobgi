package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.model.User
import com.soodalbbobgi.app.domain.repository.UserRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class CurrencyUseCaseTest {
    private lateinit var userRepo: UserRepository
    private lateinit var useCase: CurrencyUseCase

    private val testUser = User(
        id = "user1", nickname = "Test", shellBalance = 10,
        pearlBalance = 5, pityCounter = 0, lastShellGrantDate = null,
        authProvider = "GOOGLE",
    )

    @Before
    fun setup() {
        userRepo = mockk(relaxed = true)
        useCase = CurrencyUseCase(userRepo)
    }

    @Test
    fun `grantDailyShells gives 1 to 3 shells on first grant of the day`() = runTest {
        coEvery { userRepo.getUser("user1") } returns flowOf(testUser)

        val earned = useCase.grantDailyShells("user1", "2026-05-25")

        assertThat(earned).isIn(1..3)
        coVerify { userRepo.updateCurrency("user1", any(), 5) }
        coVerify { userRepo.updateLastShellGrantDate("user1", "2026-05-25") }
    }

    @Test
    fun `grantDailyShells returns 0 if already granted today`() = runTest {
        val user = testUser.copy(lastShellGrantDate = "2026-05-25")
        coEvery { userRepo.getUser("user1") } returns flowOf(user)

        val earned = useCase.grantDailyShells("user1", "2026-05-25")

        assertThat(earned).isEqualTo(0)
        coVerify(exactly = 0) { userRepo.updateCurrency(any(), any(), any()) }
    }

    @Test
    fun `spendShells deducts from balance`() = runTest {
        coEvery { userRepo.getUser("user1") } returns flowOf(testUser)

        useCase.spendShells("user1", 3)

        coVerify { userRepo.updateCurrency("user1", 7, 5) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `spendShells throws when insufficient`() = runTest {
        coEvery { userRepo.getUser("user1") } returns flowOf(testUser)
        useCase.spendShells("user1", 20)
    }

    @Test
    fun `addPearls increases balance`() = runTest {
        coEvery { userRepo.getUser("user1") } returns flowOf(testUser)

        useCase.addPearls("user1", 10)

        coVerify { userRepo.updateCurrency("user1", 10, 15) }
    }
}
