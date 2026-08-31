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
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    /**
     * 필수 HC 권한이 실제로 부여돼 있는지 조회한다.
     * 요청 런처의 결과 셋은 신뢰할 수 없다 — 이미 전부 허용된 상태에서 재요청하면
     * HC가 빈 결과를 돌려줘, 결과 셋만 보면 허용해 놓고도 화면이 멈춘다.
     */
    suspend fun hasAllPermissions(): Boolean = healthConnectManager.hasAllPermissions()

    /**
     * HC 권한 허용 직후 호출한다. 에셋·HC 동기화를 백그라운드로 실행한다.
     * 앱 스코프라 화면 전환과 무관하게 완료된다.
     */
    fun onPermissionGranted() {
        appScope.launch {
            val result = assetManager.sync()
            if (result.isFailure) {
                Timber.w(result.exceptionOrNull(), "권한 허용 후 에셋 동기화 실패 (앱 계속 진행)")
            }
        }
        appScope.launch {
            try {
                hcSwimSyncer.sync()
            } catch (e: Exception) {
                Timber.w(e, "권한 허용 후 HC 동기화 실패 (앱 계속 진행)")
            }
        }
    }
}
