package kr.ilf.soodalbbobgi.data.health

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.ApiResponse
import kr.ilf.soodalbbobgi.data.remote.dto.ServerSwimLog
import kr.ilf.soodalbbobgi.data.remote.dto.ShellRewardData
import kr.ilf.soodalbbobgi.data.remote.dto.SwimLogResponseData
import kr.ilf.soodalbbobgi.domain.model.SwimLog
import kr.ilf.soodalbbobgi.domain.usecase.SwimLogUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * 동기화 오케스트레이터의 서버 전송 재시도 의미론 검증.
 * 날짜별 일 집계는 미전송(synced=false) 행이 있을 때만 보내고,
 * 네트워크 실패는 미전송으로 남겨 다음 동기화에 재시도한다.
 */
class HcSwimSyncerTest {
    private lateinit var hcm: HealthConnectManager
    private lateinit var useCase: SwimLogUseCase
    private lateinit var api: SoodalApi
    private lateinit var prefs: HcSyncPreferences
    private lateinit var loader: AppStateLoader
    private lateinit var syncer: HcSwimSyncer

    @Before
    fun setup() {
        hcm = mockk(relaxed = true)
        useCase = mockk(relaxed = true)
        api = mockk(relaxed = true)
        prefs = mockk(relaxed = true)
        loader = mockk(relaxed = true)
        every { prefs.getChangesToken() } returns null
        // relaxed mock의 Int? 기본값에 기대지 않고 '온보딩 기간 없음'을 명시한다
        every { prefs.getPendingInitialMonths() } returns null
        coEvery { hcm.getChangesToken() } returns "tok"
        coEvery { hcm.readSwimSessions(any(), any()) } returns listOf(hcSession())
        coEvery { useCase.getUnsyncedDates() } returns listOf("2026-06-07")
        coEvery { api.getSwimLogs(any(), any()) } returns ApiResponse(false, null, null)
        val session = UserSession().apply { setAuthenticatedUser("u1") }
        syncer = HcSwimSyncer(hcm, useCase, api, prefs, session, loader)
    }

    private fun hcSession() = SwimSession(
        date = "2026-06-07", startEpochSec = 0L, distanceMeters = 500,
        durationSeconds = 1800, calories = 200, hcRecordId = "hc-1",
    )

    private fun row(synced: Boolean, dist: Int = 500, id: Long = 1L) = SwimLog(
        id = id, userId = "u1", date = "2026-06-07", distanceMeters = dist,
        durationSeconds = 1800, calories = 200, source = "health_connect",
        hcRecordId = "hc-$id", synced = synced,
    )

    private fun okResponse(earned: Int) = ApiResponse(
        true,
        SwimLogResponseData(
            ServerSwimLog(
                id = "s1", date = "2026-06-07", distanceMeters = 1200, durationSeconds = 3600,
                calories = 400, strokeFreestyleM = 0, strokeBreastM = 0, strokeBackM = 0,
                strokeFlyM = 0, strokeMixedM = 1200, strokeKickM = 0,
                source = "health_connect", shellsEarned = earned, createdAt = 0L,
            ),
            ShellRewardData(earned = earned, newBalance = 10),
        ),
        null,
    )

    @Test
    fun `미전송 행이 있는 날짜는 세션 합계를 서버에 보내고 전송됨으로 표시한다`() = runTest {
        coEvery { useCase.getLogsForDate("2026-06-07") } returns
            listOf(row(synced = false, dist = 500, id = 1), row(synced = false, dist = 700, id = 2))
        coEvery { api.addSwimLog(any()) } returns okResponse(earned = 2)

        val earned = syncer.sync()

        assertThat(earned).isEqualTo(2)
        coVerify(exactly = 1) {
            api.addSwimLog(match { it.date == "2026-06-07" && it.distanceMeters == 1200 })
        }
        coVerify(exactly = 1) { useCase.markSynced("2026-06-07") }
    }

    @Test
    fun `모든 행이 전송된 상태면 보낼 날짜가 없다`() = runTest {
        coEvery { useCase.getUnsyncedDates() } returns emptyList()

        syncer.sync()

        coVerify(exactly = 0) { api.addSwimLog(any()) }
    }

    @Test
    fun `평소 동기화는 30일을 읽지 않는다 - 오늘 창만 읽는다`() = runTest {
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(row(synced = false))
        coEvery { api.addSwimLog(any()) } returns okResponse(earned = 1)

        syncer.sync()

        // 토큰 초기화 경로의 읽기 창이 이틀(어제~내일)을 넘지 않아야 한다
        coVerify(exactly = 1) {
            hcm.readSwimSessions(
                match { start ->
                    java.time.Duration.between(start, java.time.Instant.now()).toDays() <= 2
                },
                any(),
            )
        }
    }

    @Test
    fun `온보딩에서 고른 기간이 있으면 그만큼 과거부터 읽고 기간을 지운다`() = runTest {
        every { prefs.getPendingInitialMonths() } returns 12
        coEvery { useCase.getUnsyncedDates() } returns emptyList()
        // 자정 넘김 경계에서 start/end가 어긋나지 않게 오늘 날짜를 한 번만 잡는다
        val today = java.time.LocalDate.now()
        val zone = java.time.ZoneId.systemDefault()
        val expectedStart = today.minusMonths(12).atStartOfDay(zone).toInstant()
        val expectedEnd = today.plusDays(1).atStartOfDay(zone).toInstant()

        syncer.sync()

        coVerify(exactly = 1) { hcm.readSwimSessions(expectedStart, expectedEnd) }
        coVerify(exactly = 1) { prefs.clearPendingInitialMonths() }
        coVerify(exactly = 1) { prefs.saveChangesToken("tok") } // 긴 읽기 뒤에도 읽기 전 발급한 토큰을 저장
    }

    @Test
    fun `네트워크 실패면 미전송으로 남겨 다음 동기화에 재시도한다`() = runTest {
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(row(synced = false))
        coEvery { api.addSwimLog(any()) } throws IOException("offline")

        val earned = syncer.sync() // 예외가 밖으로 새지 않아야 한다

        assertThat(earned).isEqualTo(0)
        coVerify(exactly = 0) { useCase.markSynced(any()) }
    }

    @Test
    fun `서버가 409로 거부하면 전송됨 처리해 재전송 루프를 막는다`() = runTest {
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(row(synced = false))
        coEvery { api.addSwimLog(any()) } throws retrofit2.HttpException(
            retrofit2.Response.error<Any>(
                409,
                okhttp3.ResponseBody.create(null, "{\"success\":false}"),
            ),
        )

        val earned = syncer.sync()

        assertThat(earned).isEqualTo(0)
        coVerify(exactly = 1) { useCase.markSynced("2026-06-07") }
    }

    @Test
    fun `동시에 들어온 sync는 직렬화돼 첫 동기화가 끝나기 전에 서버를 다시 치지 않는다`() = runTest {
        // 로그인 직후·온보딩 권한 허용·설정 재연결이 겹치면 같은 날짜를 두 번 보고할 수 있다 (R15).
        val gate = CompletableDeferred<Unit>()
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(row(synced = false))
        coEvery { api.addSwimLog(any()) } coAnswers { gate.await(); okResponse(earned = 2) }

        val first = async { syncer.sync() }
        runCurrent()
        val second = async { syncer.sync() }
        runCurrent()

        coVerify(exactly = 1) { api.addSwimLog(any()) }
        gate.complete(Unit)
        assertThat(first.await()).isEqualTo(2)
        second.await()
    }

    @Test
    fun `서버가 거부해도 전송됨 처리해 재전송 루프를 막는다`() = runTest {
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(row(synced = false))
        coEvery { api.addSwimLog(any()) } returns ApiResponse(false, null, null)

        val earned = syncer.sync()

        assertThat(earned).isEqualTo(0)
        coVerify(exactly = 1) { useCase.markSynced("2026-06-07") }
    }

    @Test
    fun `수동 등록은 영법 분배를 저장하고 잔여분은 혼영으로 배정한다`() = runTest {
        coEvery { useCase.getUnsyncedDates() } returns emptyList()

        syncer.registerManual(
            distanceMeters = 1000, durationMin = 40,
            avgHr = 132,
            date = java.time.LocalDate.parse("2026-06-10"),
            strokeFreeM = 400, strokeBreastM = 200, strokeKickM = 100,
        )

        coVerify(exactly = 1) {
            useCase.addManualLog(
                match {
                    it.date == "2026-06-10" && it.source == "manual" &&
                        it.strokeFreestyleM == 400 && it.strokeBreastM == 200 &&
                        it.strokeKickM == 100 && it.strokeMixedM == 300 &&
                        it.avgHr == 132
                },
            )
        }
    }

    @Test
    fun `수동 등록에 시작 시각을 주면 그 시각이 기록 시작이 된다`() = runTest {
        coEvery { useCase.getUnsyncedDates() } returns emptyList()
        val date = java.time.LocalDate.parse("2026-06-10")
        val expected = date.atTime(14, 30)
            .atZone(java.time.ZoneId.systemDefault()).toEpochSecond()

        syncer.registerManual(
            distanceMeters = 500, durationMin = 30,
            date = date, startTime = java.time.LocalTime.of(14, 30),
        )

        coVerify(exactly = 1) { useCase.addManualLog(match { it.startEpochSec == expected }) }
    }

    // ─── 세션 삭제 ───────────────────────────────────────────

    @Test
    fun `세션 삭제는 로컬 행을 지우고 서버 일 기록을 삭제한다`() = runTest {
        val target = row(synced = true, id = 1)
        coEvery { useCase.getLogsForDate("2026-06-07") } returns emptyList()
        coEvery { api.deleteSwimLog("2026-06-07") } returns
            ApiResponse(true, kr.ilf.soodalbbobgi.data.remote.dto.DeleteSwimLogData("2026-06-07", true), null)

        syncer.deleteSession(target)

        coVerify(exactly = 1) { useCase.deleteById(1L) }
        coVerify(exactly = 1) { api.deleteSwimLog("2026-06-07") }
        // HC 유래 기록 — 재수입 차단 블랙리스트에 등록
        coVerify(exactly = 1) { prefs.addDeletedHcRecordId("hc-1") }
    }

    @Test
    fun `잔여 세션이 있으면 서버 삭제 후 일 집계를 재전송한다`() = runTest {
        val target = row(synced = true, id = 1)
        val remaining = row(synced = false, dist = 700, id = 2)
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(remaining)
        coEvery { useCase.getUnsyncedDates() } returns listOf("2026-06-07")
        coEvery { api.deleteSwimLog("2026-06-07") } returns
            ApiResponse(true, kr.ilf.soodalbbobgi.data.remote.dto.DeleteSwimLogData("2026-06-07", true), null)
        coEvery { api.addSwimLog(any()) } returns okResponse(earned = 0)

        syncer.deleteSession(target)

        coVerify(exactly = 1) { useCase.markUnsynced("2026-06-07") }
        coVerify(exactly = 1) { api.addSwimLog(match { it.distanceMeters == 700 }) }
    }

    @Test
    fun `서버 삭제 실패면 재시도 큐에 넣고 다음 동기화에서 재시도한다`() = runTest {
        val target = row(synced = true, id = 1)
        coEvery { useCase.getLogsForDate("2026-06-07") } returns emptyList()
        coEvery { api.deleteSwimLog("2026-06-07") } throws IOException("offline")

        syncer.deleteSession(target)

        // 로컬 삭제는 확정, 서버 삭제는 큐 적재
        coVerify(exactly = 1) { useCase.deleteById(1L) }
        coVerify(exactly = 1) { prefs.addPendingServerDelete("2026-06-07") }

        // 다음 sync에서 큐 재시도 — 성공하면 큐에서 제거
        every { prefs.getPendingServerDeletes() } returns setOf("2026-06-07")
        coEvery { useCase.getUnsyncedDates() } returns emptyList()
        coEvery { api.deleteSwimLog("2026-06-07") } returns
            ApiResponse(true, kr.ilf.soodalbbobgi.data.remote.dto.DeleteSwimLogData("2026-06-07", true), null)

        syncer.sync()

        coVerify(exactly = 1) { prefs.removePendingServerDelete("2026-06-07") }
    }

    @Test
    fun `블랙리스트에 있는 HC 세션은 재수입하지 않는다`() = runTest {
        every { prefs.getDeletedHcRecordIds() } returns setOf("hc-1")
        coEvery { useCase.getUnsyncedDates() } returns emptyList()

        syncer.sync() // 토큰 없음 → readSwimSessions 폴백이 hc-1 세션을 돌려줌

        coVerify(exactly = 0) { useCase.syncSwimLog(any(), any()) }
    }

    @Test
    fun `서버 삭제 대기 중인 날짜는 pull로 복원하지 않는다`() = runTest {
        every { prefs.getDeletedHcRecordIds() } returns setOf("hc-1")
        every { prefs.getPendingServerDeletes() } returns setOf("2026-06-05")
        coEvery { useCase.getUnsyncedDates() } returns emptyList()
        coEvery { api.deleteSwimLog("2026-06-05") } throws IOException("offline") // 재시도도 실패
        coEvery { api.getSwimLogs(any(), any()) } returns ApiResponse(
            true,
            kr.ilf.soodalbbobgi.data.remote.dto.SwimLogsData(listOf(
                ServerSwimLog(
                    id = "s5", date = "2026-06-05", distanceMeters = 800, durationSeconds = 1800,
                    calories = 300, strokeFreestyleM = 0, strokeBreastM = 0, strokeBackM = 0,
                    strokeFlyM = 0, strokeMixedM = 800, strokeKickM = 0,
                    source = "health_connect", shellsEarned = 1, createdAt = 0L,
                ),
            )),
            null,
        )

        syncer.sync()

        coVerify(exactly = 0) { useCase.saveFromServer(match { it.date == "2026-06-05" }) }
    }

    @Test
    fun `일 집계 전송은 행에 저장된 영법 값을 합산해 보낸다`() = runTest {
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(
            row(synced = false, dist = 500, id = 1).copy(strokeFreestyleM = 300, strokeMixedM = 200),
            row(synced = false, dist = 700, id = 2).copy(strokeBreastM = 100, strokeMixedM = 600),
        )
        coEvery { api.addSwimLog(any()) } returns okResponse(earned = 0)

        syncer.sync()

        coVerify(exactly = 1) {
            api.addSwimLog(
                match {
                    it.strokeFreestyleM == 300 && it.strokeBreastM == 100 && it.strokeMixedM == 800
                },
            )
        }
    }
}
