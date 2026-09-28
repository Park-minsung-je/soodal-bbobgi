package kr.ilf.soodalbbobgi.domain.usecase

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.domain.model.SwimLog
import kr.ilf.soodalbbobgi.domain.repository.SwimLogRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * 수영 기록 동기화 의미론 검증 — 하루 여러 세션 전제.
 * HC 세션은 hcRecordId가 정체성이고, 날짜는 더 이상 유니크가 아니다.
 */
class SwimLogUseCaseTest {
    private lateinit var swimLogRepo: SwimLogRepository
    private lateinit var useCase: SwimLogUseCase

    private val testLog = SwimLog(
        id = 0, userId = "u1", date = "2026-05-25", distanceMeters = 1500,
        durationSeconds = 2400, calories = 320, source = "health_connect",
        hcRecordId = "hc-1", startEpochSec = 1_000_000L,
    )

    @Before
    fun setup() {
        swimLogRepo = mockk(relaxed = true)
        useCase = SwimLogUseCase(swimLogRepo)
    }

    // ── HC 동기화 (hcRecordId 기준) ──────────────────────────────

    @Test
    fun `syncSwimLog는 새 hcRecordId면 저장한다 - 같은 날짜에 다른 세션이 있어도`() = runTest {
        coEvery { swimLogRepo.getByHcRecordId("hc-1") } returns null
        // 같은 날 다른 HC 세션이 이미 존재 — 날짜 중복은 더 이상 차단 사유가 아니다
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 7, hcRecordId = "hc-0"))

        val rows = useCase.syncSwimLog("u1", testLog)

        assertThat(rows).isEqualTo(1)
        coVerify(exactly = 1) { swimLogRepo.addSwimLog(testLog) }
    }

    @Test
    fun `syncSwimLog는 같은 hcRecordId가 있으면 HC 핵심 필드만 갱신한다`() = runTest {
        coEvery { swimLogRepo.getByHcRecordId("hc-1") } returns testLog.copy(id = 3)

        val rows = useCase.syncSwimLog("u1", testLog)

        assertThat(rows).isEqualTo(0)
        coVerify(exactly = 0) { swimLogRepo.addSwimLog(any()) }
        coVerify(exactly = 1) { swimLogRepo.updateFromHc(3, testLog) }
    }

    @Test
    fun `syncSwimLog는 서버산 행이 있는 날짜의 HC 세션을 그 행에 승격시킨다`() = runTest {
        // 서버 복원 행(hcRecordId 없음)이 있으면 새 행을 만들지 않고 HC 정체성을 입힌다
        coEvery { swimLogRepo.getByHcRecordId("hc-1") } returns null
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 5, hcRecordId = null, source = "server"))

        val rows = useCase.syncSwimLog("u1", testLog)

        assertThat(rows).isEqualTo(0)
        coVerify(exactly = 0) { swimLogRepo.addSwimLog(any()) }
        coVerify(exactly = 1) { swimLogRepo.updateFromHc(5, testLog) }
    }

    // ── 서버 풀 ──────────────────────────────────────────────────

    @Test
    fun `saveFromServer는 해당 날짜에 행이 없을 때만 저장한다`() = runTest {
        val serverLog = testLog.copy(hcRecordId = null, startEpochSec = null)
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns emptyList()

        useCase.saveFromServer(serverLog)

        coVerify(exactly = 1) { swimLogRepo.addSwimLog(serverLog) }
    }

    @Test
    fun `saveFromServer는 행이 있으면 추가하지 않는다`() = runTest {
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns listOf(testLog.copy(id = 1))

        useCase.saveFromServer(testLog.copy(hcRecordId = null))

        coVerify(exactly = 0) { swimLogRepo.addSwimLog(any()) }
    }

    @Test
    fun `saveFromServer는 단일 세션 날의 빈 영법에 서버 분배를 적용한다`() = runTest {
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns listOf(testLog.copy(id = 1))
        val server = testLog.copy(hcRecordId = null, strokeMixedM = 1500)

        useCase.saveFromServer(server)

        coVerify(exactly = 1) { swimLogRepo.updateStrokes("2026-05-25", 0, 0, 0, 0, 1500, 0) }
    }

    @Test
    fun `saveFromServer는 HC 원시(혼영 전부) 로컬 행에 서버의 영법 수정을 적용한다`() = runTest {
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 1, strokeMixedM = 1500))
        val server = testLog.copy(hcRecordId = null, strokeFreestyleM = 600, strokeMixedM = 900)

        useCase.saveFromServer(server)

        coVerify(exactly = 1) { swimLogRepo.updateStrokes("2026-05-25", 600, 0, 0, 0, 900, 0) }
    }

    @Test
    fun `saveFromServer는 로컬에 없는 심박을 서버 값으로 채운다`() = runTest {
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns listOf(testLog.copy(id = 4))
        val server = testLog.copy(maxHr = 168, minHr = 96, avgHr = 132, hrSeries = "0:120,60:140")

        useCase.saveFromServer(server)

        coVerify(exactly = 1) { swimLogRepo.fillMissingVitals("2026-05-25", 168, 96, 132, "0:120,60:140") }
    }

    @Test
    fun `saveFromServer는 로컬에 심박이 있으면 건드리지 않는다`() = runTest {
        // 로컬이 더 정확하다 — 서버는 하루치로 합친 값이라 세션별 값을 덮으면 손해다
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 4, maxHr = 170, minHr = 90, avgHr = 130, hrSeries = "0:130"))
        val server = testLog.copy(maxHr = 168, minHr = 96, avgHr = 132, hrSeries = "0:120,60:140")

        useCase.saveFromServer(server)

        coVerify(exactly = 0) { swimLogRepo.fillMissingVitals(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveFromServer는 서버에 심박이 없으면 채우지 않는다`() = runTest {
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns listOf(testLog.copy(id = 4))

        useCase.saveFromServer(testLog)

        coVerify(exactly = 0) { swimLogRepo.fillMissingVitals(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveFromServer는 하루에 세션이 여럿이면 심박을 건드리지 않는다`() = runTest {
        // 서버 값은 하루를 합친 것이라 개별 세션 행에 그대로 넣으면 틀린다
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns listOf(
            testLog.copy(id = 4, hcRecordId = "hc-1"),
            testLog.copy(id = 5, hcRecordId = "hc-2"),
        )
        val server = testLog.copy(maxHr = 168, avgHr = 132)

        useCase.saveFromServer(server)

        coVerify(exactly = 0) { swimLogRepo.fillMissingVitals(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveFromServer는 로컬에서 편집한 영법 분배를 덮어쓰지 않는다`() = runTest {
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 1, strokeFreestyleM = 600, strokeMixedM = 900))
        val server = testLog.copy(hcRecordId = null, strokeMixedM = 1500)

        useCase.saveFromServer(server)

        coVerify(exactly = 0) { swimLogRepo.updateStrokes(any(), any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveFromServer는 여러 세션이 있는 날엔 영법을 건드리지 않는다`() = runTest {
        // 서버는 일 단위 집계라 세션별 분배를 모름 — 다중 세션 날 치유는 스킵
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 1), testLog.copy(id = 2, hcRecordId = "hc-2"))
        val server = testLog.copy(hcRecordId = null, strokeMixedM = 3000)

        useCase.saveFromServer(server)

        coVerify(exactly = 0) { swimLogRepo.updateStrokes(any(), any(), any(), any(), any(), any(), any()) }
    }

    // ── 기타 ─────────────────────────────────────────────────────

    @Test
    fun `getDateByHcRecordId returns date when found`() = runTest {
        coEvery { swimLogRepo.getByHcRecordId("hc-record-1") } returns
            testLog.copy(hcRecordId = "hc-record-1")

        assertThat(useCase.getDateByHcRecordId("hc-record-1")).isEqualTo("2026-05-25")
    }

    @Test
    fun `getDateByHcRecordId returns null when not found`() = runTest {
        coEvery { swimLogRepo.getByHcRecordId(any()) } returns null

        assertThat(useCase.getDateByHcRecordId("missing")).isNull()
    }

    // ── HC 레코드 ID 보관 (R48) ──────────────────────────────

    @Test
    fun `saveFromServer는 서버가 보관한 HC 레코드 ID를 복원 행에 채운다`() = runTest {
        // 서버 일 기록의 ID 목록: 첫 ID는 hcRecordId, 나머지는 보조 ID — 하루 여러 세션이 하나의 복원 행에 합쳐진다
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns emptyList()
        val restored = testLog.copy(startEpochSec = null, hcRecordId = "hc-1", extraHcRecordIds = listOf("hc-2"), synced = true)

        useCase.saveFromServer(restored)

        coVerify(exactly = 1) {
            swimLogRepo.addSwimLog(match { it.hcRecordId == "hc-1" && it.extraHcRecordIds == listOf("hc-2") })
        }
    }

    @Test
    fun `saveFromServer는 ID 없는 복원 행이 있으면 서버 ID를 채워 넣는다`() = runTest {
        // 이 변경 전에 복원된 행(hcRecordId·시작 시각 없음)도 HC 삭제 매칭이 되도록
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 5, hcRecordId = null, startEpochSec = null))
        val fromServer = testLog.copy(startEpochSec = null, hcRecordId = "hc-1", extraHcRecordIds = listOf("hc-2"), synced = true)

        useCase.saveFromServer(fromServer)

        coVerify(exactly = 1) { swimLogRepo.fillHcRecordIds(5, "hc-1", listOf("hc-2")) }
    }

    @Test
    fun `saveFromServer는 HC 유래 행(시작 시각 있음)에는 서버 ID를 덮지 않는다`() = runTest {
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 5, hcRecordId = "hc-9", startEpochSec = 1_000L))
        val fromServer = testLog.copy(startEpochSec = null, hcRecordId = "hc-1", synced = true)

        useCase.saveFromServer(fromServer)

        coVerify(exactly = 0) { swimLogRepo.fillHcRecordIds(any(), any(), any()) }
    }

    @Test
    fun `syncSwimLog는 ID가 채워진 복원 행(시작 시각 없음)도 HC 세션으로 승격시킨다`() = runTest {
        // 복원 행이 hcRecordId를 갖게 되면서 "hcRecordId 없음"만으로는 서버산 행을 못 가려낸다 —
        // 시작 시각이 없는 행이 서버산이다. 승격하지 않으면 같은 날 세션이 두 배로 잡힌다.
        coEvery { swimLogRepo.getByHcRecordId("hc-1") } returns null
        coEvery { swimLogRepo.getLogsForDateOnce("2026-05-25") } returns
            listOf(testLog.copy(id = 5, hcRecordId = "hc-9", extraHcRecordIds = listOf("hc-1"), startEpochSec = null))

        val rows = useCase.syncSwimLog("u1", testLog)

        assertThat(rows).isEqualTo(0)
        coVerify(exactly = 0) { swimLogRepo.addSwimLog(any()) }
        coVerify(exactly = 1) { swimLogRepo.updateFromHc(5, testLog) }
    }

    @Test
    fun `findByHcRecordId는 보조 ID까지 보고 찾는다`() = runTest {
        val restored = testLog.copy(id = 5, hcRecordId = "hc-1", extraHcRecordIds = listOf("hc-2"), startEpochSec = null)
        coEvery { swimLogRepo.getByAnyHcRecordId("hc-2") } returns restored

        assertThat(useCase.findByHcRecordId("hc-2")).isEqualTo(restored)
    }
}
