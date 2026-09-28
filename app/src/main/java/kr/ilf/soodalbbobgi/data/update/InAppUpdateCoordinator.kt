package kr.ilf.soodalbbobgi.data.update

import android.app.Application
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kr.ilf.soodalbbobgi.BuildConfig
import kr.ilf.soodalbbobgi.core.di.ApplicationScope
import kr.ilf.soodalbbobgi.core.update.UpdateMode
import kr.ilf.soodalbbobgi.core.update.decideUpdateMode
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Play 인앱 업데이트 조율자.
 *
 * 앱이 앞으로 올 때마다 [onResume]을 부르면:
 * - 즉시 업데이트가 진행 중이었으면(사용자가 도중에 벗어남) 다시 이어 띄운다.
 * - 유연 업데이트가 다 받아졌으면 [readyToInstall]을 켜 "다시 시작" 안내를 띄우게 한다.
 * - 아직 이번 실행에서 확인한 적 없으면 새 버전을 확인해 서버 최소 버전과 비교, 방식을 정해 띄운다.
 *
 * Play로 설치한 앱에서만 동작하고, 그 밖(사이드로드·에뮬레이터)에서는 조용히 아무 일도 하지 않는다.
 */
@Singleton
class InAppUpdateCoordinator @Inject constructor(
    app: Application,
    private val soodalApi: SoodalApi,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val manager: AppUpdateManager = AppUpdateManagerFactory.create(app)

    private val _readyToInstall = MutableStateFlow(false)
    /** 유연 업데이트가 내려받아져 재시작만 남은 상태. */
    val readyToInstall: StateFlow<Boolean> = _readyToInstall.asStateFlow()

    /** 이번 프로세스에서 새 버전 확인을 이미 했는지 — 화면이 앞으로 올 때마다 Play를 두드리지 않는다. */
    private var checkedThisProcess = false

    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) _readyToInstall.value = true
    }

    /**
     * 액티비티가 앞으로 올 때 호출한다.
     *
     * @param launcher Play 업데이트 화면을 띄우는 런처 — 액티비티가 `StartIntentSenderForResult`로 등록한 것
     */
    fun onResume(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        appScope.launch {
            val info = runCatching { manager.appUpdateInfo.await() }
                .onFailure { Timber.d(it, "인앱 업데이트 정보 없음 — Play 설치본이 아니거나 오프라인") }
                .getOrNull() ?: return@launch

            // 즉시 업데이트를 하다 벗어난 상태 — 다시 이어서 띄운다 (강제 업데이트는 건너뛸 수 없어야 한다)
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                start(info, AppUpdateType.IMMEDIATE, launcher)
                return@launch
            }
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _readyToInstall.value = true
                return@launch
            }
            if (checkedThisProcess) return@launch
            checkedThisProcess = true

            val minVersionCode = fetchMinVersionCode()
            val mode = decideUpdateMode(
                updateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE,
                currentVersionCode = BuildConfig.VERSION_CODE,
                minVersionCode = minVersionCode,
                immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE),
                flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE),
            )
            when (mode) {
                UpdateMode.IMMEDIATE -> start(info, AppUpdateType.IMMEDIATE, launcher)
                UpdateMode.FLEXIBLE -> {
                    manager.registerListener(installListener)
                    start(info, AppUpdateType.FLEXIBLE, launcher)
                }
                UpdateMode.NONE -> Unit
            }
        }
    }

    /** 내려받은 유연 업데이트를 설치한다 — 앱이 재시작된다. */
    fun completeUpdate() {
        _readyToInstall.value = false
        manager.unregisterListener(installListener)
        manager.completeUpdate()
    }

    /** "나중에" — 안내를 접는다. 다음 실행에서 다시 내려받은 상태로 감지돼 안내가 다시 뜬다. */
    fun dismissReady() {
        _readyToInstall.value = false
    }

    private fun start(info: AppUpdateInfo, type: Int, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }.onFailure { Timber.w(it, "인앱 업데이트 화면을 띄우지 못함") }
    }

    /** 서버 최소 버전 — 못 받으면 null(강제하지 않음). 로그인 여부와 무관한 공개 API. */
    private suspend fun fetchMinVersionCode(): Int? = withContext(Dispatchers.IO) {
        runCatching { soodalApi.getAppVersion().data?.minVersionCode }
            .onFailure { Timber.d(it, "앱 최소 버전 조회 실패 — 강제 업데이트 없이 진행") }
            .getOrNull()
    }
}

/** Play Task를 코루틴에서 기다린다 (play-services 코루틴 어댑터 없이). */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { if (cont.isActive) cont.resume(it) }
    addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
    addOnCanceledListener { if (cont.isActive) cont.cancel() }
}
