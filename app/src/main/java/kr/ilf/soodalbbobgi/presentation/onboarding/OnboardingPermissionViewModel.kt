package kr.ilf.soodalbbobgi.presentation.onboarding

import androidx.lifecycle.ViewModel
import kr.ilf.soodalbbobgi.core.di.ApplicationScope
import kr.ilf.soodalbbobgi.data.asset.AssetManager
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 온보딩 HC 권한 화면의 ViewModel.
 * 권한 허용 후 Home으로 가기 전에 에셋 동기화와 HC 동기화를 백그라운드로 시작한다.
 * 실패해도 화면 진입을 막지 않는다.
 */
@HiltViewModel
class OnboardingPermissionViewModel @Inject constructor(
    private val assetManager: AssetManager,
    private val hcSwimSyncer: HcSwimSyncer,
    private val healthConnectManager: HealthConnectManager,
    private val hcSyncPreferences: kr.ilf.soodalbbobgi.data.health.HcSyncPreferences,
    private val appState: kr.ilf.soodalbbobgi.core.state.AppState,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    /**
     * 필수 HC 권한이 실제로 부여돼 있는지 조회한다.
     * 요청 런처의 결과 셋은 신뢰할 수 없다 — 이미 전부 허용된 상태에서 재요청하면
     * HC가 빈 결과를 돌려줘, 결과 셋만 보면 허용해 놓고도 화면이 멈춘다.
     */
    suspend fun hasAllPermissions(): Boolean = healthConnectManager.hasAllPermissions()

    /**
     * HC 권한 허용 직후 — 선택한 기간의 최초 가져오기를 백그라운드로 시작하고 즉시 돌아온다.
     * 온보딩은 권한 확인까지만 맡고, 진행 표시는 홈이 [AppState.hcSyncing]으로 잇는다.
     * 지급된 조개는 [AppState.addPendingShellReward]로 홈 팝업에 전달된다.
     *
     * @param months 과거 기록을 가져올 기간(개월, 0 = 안 가져오기, 최대 3)
     */
    fun startInitialSync(months: Int) {
        // 프로세스가 죽어도 첫 동기화가 선택한 기간을 기억하게 저장해 둔다.
        // 0(안 가져오기)이면 저장하지 않는다 — 첫 동기화가 기본 범위(오늘)만 읽는다.
        if (months > 0) hcSyncPreferences.setPendingInitialMonths(months)
        appScope.launch {
            val result = assetManager.sync()
            if (result.isFailure) Timber.w(result.exceptionOrNull(), "권한 허용 후 에셋 동기화 실패 (앱 계속 진행)")
        }
        appScope.launch {
            appState.setHcSyncing(true)
            try {
                val earned = hcSwimSyncer.sync()
                if (earned > 0) appState.addPendingShellReward(earned)
            } catch (e: Exception) {
                Timber.w(e, "최초 HC 동기화 실패")
                val code = (e as? retrofit2.HttpException)?.code()?.toString()
                    ?: if (e is java.io.IOException) "901" else "900"
                android.widget.Toast.makeText(
                    appContext, "동기화에 실패했어요. 캘린더에서 다시 시도할 수 있어요. ($code)",
                    android.widget.Toast.LENGTH_LONG,
                ).show()
            } finally {
                appState.setHcSyncing(false)
            }
        }
    }
}
