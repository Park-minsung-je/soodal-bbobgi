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
    fun `saveFromServer는 영법 정보가 빈 로컬 행에 서버 분배를 적용한다`() = runTest {
        // 로컬: 영법 정보 없음(전부 0), 서버: 혼영 분배 보유 — 서버 값으로 치유
        coEvery { swimLogRepo.getByDateOnce("2026-05-25") } returns testLog
        val server = testLog.copy(strokeMixedM = 1500)

        useCase.saveFromServer(server)

        coVerify(exactly = 1) { swimLogRepo.updateStrokes("2026-05-25", 0, 0, 0, 0, 1500, 0) }
    }

    @Test
    fun `saveFromServer는 HC 원시(혼영 전부) 로컬 행에 서버의 영법 수정을 적용한다`() = runTest {
        // 로컬: HC 동기화 직후(전부 혼영), 서버: 사용자가 수정한 분배 — 서버가 진실
        coEvery { swimLogRepo.getByDateOnce("2026-05-25") } returns testLog.copy(strokeMixedM = 1500)
        val server = testLog.copy(strokeFreestyleM = 600, strokeMixedM = 900)

        useCase.saveFromServer(server)

        coVerify(exactly = 1) { swimLogRepo.updateStrokes("2026-05-25", 600, 0, 0, 0, 900, 0) }
    }

    @Test
    fun `saveFromServer는 로컬에서 편집한 영법 분배를 덮어쓰지 않는다`() = runTest {
        // 로컬: 수동 편집됨(자유형>0) — 서버 값이 달라도 보존
        coEvery { swimLogRepo.getByDateOnce("2026-05-25") } returns
            testLog.copy(strokeFreestyleM = 600, strokeMixedM = 900)
        val server = testLog.copy(strokeMixedM = 1500)

        useCase.saveFromServer(server)

        coVerify(exactly = 0) { swimLogRepo.updateStrokes(any(), any(), any(), any(), any(), any(), any()) }
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
