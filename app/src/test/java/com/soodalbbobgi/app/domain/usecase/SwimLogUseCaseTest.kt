package com.soodalbbobgi.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.repository.SwimLogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SwimLogUseCaseTest {
    private lateinit var swimLogRepo: SwimLogRepository
    private lateinit var useCase: SwimLogUseCase

    private val testLog = SwimLog(
        userId = "u1", date = "2026-05-25", distanceMeters = 1500,
        durationSeconds = 2400, calories = 320, source = "health_connect",
    )

    @Before
    fun setup() {
        swimLogRepo = mockk(relaxed = true)
        useCase = SwimLogUseCase(swimLogRepo)
    }

    @Test
    fun `syncSwimLog inserts when no existing log for date`() = runTest {
        coEvery { swimLogRepo.getByDateOnce("2026-05-25") } returns null

        val rows = useCase.syncSwimLog("u1", testLog)

        assertThat(rows).isEqualTo(1)
        coVerify(exactly = 1) { swimLogRepo.addSwimLog(testLog) }
    }

    @Test
    fun `syncSwimLog skips when log already exists for date`() = runTest {
        coEvery { swimLogRepo.getByDateOnce("2026-05-25") } returns testLog

        val rows = useCase.syncSwimLog("u1", testLog)

        assertThat(rows).isEqualTo(0)
        coVerify(exactly = 0) { swimLogRepo.addSwimLog(any()) }
    }

    @Test
    fun `saveFromServer inserts if no local row`() = runTest {
        coEvery { swimLogRepo.getByDateOnce("2026-05-25") } returns null

        useCase.saveFromServer(testLog)

        coVerify(exactly = 1) { swimLogRepo.addSwimLog(testLog) }
    }

    @Test
    fun `saveFromServer skips when row already exists`() = runTest {
        coEvery { swimLogRepo.getByDateOnce("2026-05-25") } returns testLog

        useCase.saveFromServer(testLog)

        coVerify(exactly = 0) { swimLogRepo.addSwimLog(any()) }
    }

    @Test
    fun `getDateByHcRecordId returns date when found`() = runTest {
        val hcId = "hc-record-1"
        coEvery { swimLogRepo.getByHcRecordId(hcId) } returns testLog.copy(hcRecordId = hcId)

        val date = useCase.getDateByHcRecordId(hcId)

        assertThat(date).isEqualTo("2026-05-25")
    }

    @Test
    fun `getDateByHcRecordId returns null when not found`() = runTest {
        coEvery { swimLogRepo.getByHcRecordId(any()) } returns null

        val date = useCase.getDateByHcRecordId("missing")

        assertThat(date).isNull()
    }
}
