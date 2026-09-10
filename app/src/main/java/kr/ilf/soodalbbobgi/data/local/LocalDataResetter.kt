package kr.ilf.soodalbbobgi.data.local

import android.content.Context
import androidx.annotation.VisibleForTesting
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.withContext
import kr.ilf.soodalbbobgi.core.di.ApplicationScope
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
import kr.ilf.soodalbbobgi.work.HcChangeCheckScheduler
import kr.ilf.soodalbbobgi.work.ReminderScheduler
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 로컬 상태를 초기 설치 상태로 되돌리는 단일 진입점 — 탈퇴·계정 전환·로그아웃이 공유한다.
 *
 * Hilt 싱글톤(AppState/UserSession)은 액티비티를 재시작해도 살아남으므로 여기서 함께 비운다.
 * 에셋 캐시(filesDir/assets)는 계정과 무관한 공용 자원이라 기본적으로 남긴다.
 */
@Singleton
class LocalDataResetter @VisibleForTesting internal constructor(
    private val db: SoodalDatabase,
    private val tokenStore: TokenStore,
    private val accountPrefs: AccountPrefs,
    private val hcSyncPreferences: HcSyncPreferences,
    private val notificationPrefs: NotificationPrefs,
    private val themePreferences: ThemePreferences,
    private val appState: AppState,
    private val userSession: UserSession,
    private val reminderScheduler: ReminderScheduler,
    private val hcChangeCheckScheduler: HcChangeCheckScheduler,
    private val notifier: SoodalNotifier,
    private val assetStore: AssetStore,
    private val appScope: CoroutineScope,
    private val cacheDir: File,
    private val ioDispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(
        db: SoodalDatabase,
        tokenStore: TokenStore,
        accountPrefs: AccountPrefs,
        hcSyncPreferences: HcSyncPreferences,
        notificationPrefs: NotificationPrefs,
        themePreferences: ThemePreferences,
        appState: AppState,
        userSession: UserSession,
        reminderScheduler: ReminderScheduler,
        hcChangeCheckScheduler: HcChangeCheckScheduler,
        notifier: SoodalNotifier,
        assetStore: AssetStore,
        @ApplicationScope appScope: CoroutineScope,
        @ApplicationContext context: Context,
    ) : this(
        db, tokenStore, accountPrefs, hcSyncPreferences, notificationPrefs, themePreferences,
        appState, userSession, reminderScheduler, hcChangeCheckScheduler, notifier, assetStore,
        appScope, context.cacheDir, Dispatchers.IO,
    )

    /**
     * 에셋을 제외한 로컬 데이터·설정·예약·메모리 상태를 전부 지운다 — 탈퇴·계정 전환용.
     *
     * @param keepAssets false면 filesDir/assets(매니페스트 포함)까지 지운다
     */
    suspend fun clearAll(keepAssets: Boolean = true) {
        clearSession()
        reminderScheduler.cancel()
        hcChangeCheckScheduler.cancel()
        notifier.cancelAll()
        withContext(ioDispatcher) {
            // Room은 메인 스레드에서 clearAllTables()를 거부한다 (생성 코드 assertNotMainThread)
            db.clearAllTables()
            hcSyncPreferences.clearAll()
            notificationPrefs.clear()
            accountPrefs.clear()
            themePreferences.clear()
            File(cacheDir, SHARED_CARDS_DIR).deleteRecursively()
            if (!keepAssets) assetStore.deleteAll()
        }
    }

    /**
     * 세션만 끊는다 — 토큰·메모리 상태·진행 중인 백그라운드 동기화. 로컬 데이터는 남긴다(로그아웃용).
     * 다른 계정이 이어서 로그인하면 [kr.ilf.soodalbbobgi.data.auth.AccountSwitchGuard]가 [clearAll]로 정리한다.
     */
    fun clearSession() {
        // 로그인 직후/온보딩의 HC 동기화가 지운 뒤에 Room을 다시 채우지 않도록 먼저 끊는다
        appScope.coroutineContext.cancelChildren()
        tokenStore.clearTokens()
        appState.clear()
        userSession.clear()
    }

    companion object {
        /** ProfileFullscreenViewModel.share()가 쓰는 공유 카드 임시 폴더. */
        private const val SHARED_CARDS_DIR = "shared_cards"
    }
}
