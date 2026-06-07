package com.soodalbbobgi.app.data.health

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.ApiResponse
import com.soodalbbobgi.app.data.remote.dto.ServerSwimLog
import com.soodalbbobgi.app.data.remote.dto.ShellRewardData
import com.soodalbbobgi.app.data.remote.dto.SwimLogResponseData
import com.soodalbbobgi.app.domain.model.SwimLog
import com.soodalbbobgi.app.domain.usecase.SwimLogUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
        every { prefs.getAlgoVersion() } returns HC_ALGO_VERSION // 재계산 경로 비활성
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
    fun `알고리즘 버전이 바뀌면 한 번만 30일 재계산을 한다`() = runTest {
        every { prefs.getAlgoVersion() } returns HC_ALGO_VERSION - 1
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(row(synced = false))
        coEvery { api.addSwimLog(any()) } returns okResponse(earned = 1)

        syncer.sync()

        // 오늘 창 1회 + 30일 재계산 1회
        coVerify(exactly = 2) { hcm.readSwimSessions(any(), any()) }
        coVerify(exactly = 1) { prefs.saveAlgoVersion(HC_ALGO_VERSION) }
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
    fun `서버가 거부해도 전송됨 처리해 재전송 루프를 막는다`() = runTest {
        coEvery { useCase.getLogsForDate("2026-06-07") } returns listOf(row(synced = false))
        coEvery { api.addSwimLog(any()) } returns ApiResponse(false, null, null)

        val earned = syncer.sync()

        assertThat(earned).isEqualTo(0)
        coVerify(exactly = 1) { useCase.markSynced("2026-06-07") }
    }
}
