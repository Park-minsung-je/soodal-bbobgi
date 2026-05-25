package com.soodalbbobgi.app.domain.usecase

import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.repository.SwimLogRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SwimLogUseCaseTest {
    private lateinit var swimLogRepo: SwimLogRepository
    private lateinit var currencyUseCase: CurrencyUseCase
    private lateinit var useCase: SwimLogUseCase

    private val testLog = SwimLog(
        userId = "u1", date = "2026-05-25", distanceMeters = 1500,
        durationSeconds = 2400, calories = 320, source = "HEALTH_CONNECT",
    )

    @Before
    fun setup() {
        swimLogRepo = mockk(relaxed = true)
        currencyUseCase = mockk(relaxed = true)
        useCase = SwimLogUseCase(swimLogRepo, currencyUseCase)
    }

    @Test
    fun `syncSwimLog adds log and grants shells when no existing log`() = runTest {
        coEvery { swimLogRepo.getByDate("2026-05-25") } returns flowOf(null)
        coEvery { currencyUseCase.grantDailyShells("u1", "2026-05-25") } returns 2

        val earned = useCase.syncSwimLog("u1", testLog)

        assertThat(earned).isEqualTo(2)
        coVerify { swimLogRepo.addSwimLog(testLog) }
    }

    @Test
    fun `syncSwimLog skips when log already exists for date`() = runTest {
        coEvery { swimLogRepo.getByDate("2026-05-25") } returns flowOf(testLog)

        val earned = useCase.syncSwimLog("u1", testLog)

        assertThat(earned).isEqualTo(0)
        coVerify(exactly = 0) { swimLogRepo.addSwimLog(any()) }
    }
}
