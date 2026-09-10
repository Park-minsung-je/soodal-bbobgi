package kr.ilf.soodalbbobgi.data.local

import com.google.common.truth.Truth.assertThat
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kr.ilf.soodalbbobgi.core.notify.SoodalNotifier
import kr.ilf.soodalbbobgi.core.session.UserSession
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.theme.ThemePreferences
import kr.ilf.soodalbbobgi.data.asset.AssetStore
import kr.ilf.soodalbbobgi.data.auth.AccountPrefs
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.health.HcSyncPreferences
import kr.ilf.soodalbbobgi.data.local.db.SoodalDatabase
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.domain.model.UserProfile
import kr.ilf.soodalbbobgi.work.HcChangeCheckScheduler
import kr.ilf.soodalbbobgi.work.ReminderScheduler
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/** 탈퇴·계정 전환 초기화가 에셋만 남기고 전부 지우는지, 로그아웃은 로컬을 남기는지 검증. */
@OptIn(ExperimentalCoroutinesApi::class)
class LocalDataResetterTest {
    @get:Rule val tmp = TemporaryFolder()

    private val db: SoodalDatabase = mockk(relaxed = true)
    private val tokenStore: TokenStore = mockk(relaxed = true)
    private val accountPrefs: AccountPrefs = mockk(relaxed = true)
    private val hcPrefs: HcSyncPreferences = mockk(relaxed = true)
    private val notifyPrefs: NotificationPrefs = mockk(relaxed = true)
    private val themePrefs: ThemePreferences = mockk(relaxed = true)
    private val reminder: ReminderScheduler = mockk(relaxed = true)
    private val hcCheck: HcChangeCheckScheduler = mockk(relaxed = true)
    private val notifier: SoodalNotifier = mockk(relaxed = true)
    private val assetStore: AssetStore = mockk(relaxed = true)
    private val appState = AppState()
    private val userSession = UserSession().apply { setAuthenticatedUser("u1") }

    private fun resetter(scope: CoroutineScope, dispatcher: CoroutineDispatcher) =
        LocalDataResetter(
            db, tokenStore, accountPrefs, hcPrefs, notifyPrefs, themePrefs, appState, userSession,
            reminder, hcCheck, notifier, assetStore, scope, tmp.root, dispatcher,
        )

    @Test
    fun `clearAll은 Room·토큰·prefs·메모리·예약을 전부 비우고 에셋은 남긴다`() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        appState.applyProfile(UserProfile("u1", "수달이", null, null, "google"))
        appState.setHcSyncing(true)
        File(tmp.root, "shared_cards").apply { mkdirs(); File(this, "soodal_card.png").writeText("x") }

        resetter(CoroutineScope(SupervisorJob() + io), io).clearAll()

        coVerify(exactly = 1) { db.clearAllTables() }
        verify { tokenStore.clearTokens(); hcPrefs.clearAll(); notifyPrefs.clear(); accountPrefs.clear() }
        coVerify { themePrefs.clear() }
        verify { reminder.cancel(); hcCheck.cancel(); notifier.cancelAll() }
        verify(exactly = 0) { assetStore.deleteAll() }
        assertThat(appState.profile.value).isNull()
        assertThat(appState.hcSyncing.value).isFalse()
        assertThat(userSession.userId).isNotEqualTo("u1")
        assertThat(File(tmp.root, "shared_cards").exists()).isFalse()
    }

    @Test
    fun `keepAssets가 false면 에셋도 지운다`() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        resetter(CoroutineScope(SupervisorJob() + io), io).clearAll(keepAssets = false)
        verify(exactly = 1) { assetStore.deleteAll() }
    }

    @Test
    fun `clearAll은 진행 중인 앱 스코프 동기화를 취소한다`() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + io)
        val inflight = scope.launch { awaitCancellation() }
        testScheduler.runCurrent()

        resetter(scope, io).clearAll()

        assertThat(inflight.isCancelled).isTrue()
        assertThat(scope.isActive).isTrue()
    }

    @Test
    fun `clearSession은 토큰과 메모리만 끊고 로컬 데이터는 남긴다`() = runTest {
        val io = StandardTestDispatcher(testScheduler)
        appState.applyProfile(UserProfile("u1", "수달이", null, null, "google"))

        resetter(CoroutineScope(SupervisorJob() + io), io).clearSession()

        verify { tokenStore.clearTokens() }
        assertThat(appState.profile.value).isNull()
        assertThat(userSession.userId).isNotEqualTo("u1")
        coVerify(exactly = 0) { db.clearAllTables() }
        verify(exactly = 0) { hcPrefs.clearAll(); notifyPrefs.clear(); accountPrefs.clear(); reminder.cancel() }
    }
}
