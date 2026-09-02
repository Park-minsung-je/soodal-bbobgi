package kr.ilf.soodalbbobgi.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kr.ilf.soodalbbobgi.core.di.ApplicationScope
import kr.ilf.soodalbbobgi.core.notify.SoodalNotifier
import kr.ilf.soodalbbobgi.core.state.AppState
import kr.ilf.soodalbbobgi.core.state.AppStateLoader
import kr.ilf.soodalbbobgi.data.auth.TokenStore
import kr.ilf.soodalbbobgi.data.health.HcSwimSyncer
import kr.ilf.soodalbbobgi.data.health.HcSyncPreferences
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.data.local.LocalDataResetter
import kr.ilf.soodalbbobgi.data.notify.NotificationPrefs
import kr.ilf.soodalbbobgi.data.remote.api.SoodalApi
import kr.ilf.soodalbbobgi.data.remote.dto.DevResetRequest
import kr.ilf.soodalbbobgi.data.remote.dto.RefreshRequest
import kr.ilf.soodalbbobgi.data.remote.dto.UpdateUserRequest
import kr.ilf.soodalbbobgi.data.remote.toApiError
import kr.ilf.soodalbbobgi.domain.model.UserProfile
import kr.ilf.soodalbbobgi.domain.repository.SwimLogRepository
import kr.ilf.soodalbbobgi.work.HcChangeCheckScheduler
import kr.ilf.soodalbbobgi.work.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

/**
 * 설정 화면의 상태/액션 — 프로필 표시, 닉네임 변경, 로그아웃, 계정 탈퇴, HC 연결 상태.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: SoodalApi,
    private val appState: AppState,
    private val appStateLoader: AppStateLoader,
    private val tokenStore: TokenStore,
    private val hcSyncPreferences: HcSyncPreferences,
    private val healthConnectManager: HealthConnectManager,
    private val swimLogRepository: SwimLogRepository,
    private val hcSwimSyncer: HcSwimSyncer,
    private val notificationPrefs: NotificationPrefs,
    private val reminderScheduler: ReminderScheduler,
    private val hcChangeCheckScheduler: HcChangeCheckScheduler,
    private val notifier: SoodalNotifier,
    private val localDataResetter: LocalDataResetter,
    @ApplicationScope private val appScope: CoroutineScope,
) : ViewModel() {

    /** 현재 사용자 프로필 (닉네임, authProvider). */
    val profile: StateFlow<UserProfile?> = appState.profile

    // HC 권한 상태 — null이면 아직 확인 중
    private val _hcConnected = MutableStateFlow<Boolean?>(null)
    val hcConnected: StateFlow<Boolean?> = _hcConnected

    private val _nicknameState = MutableStateFlow<NicknameSaveState>(NicknameSaveState.Idle)
    val nicknameState: StateFlow<NicknameSaveState> = _nicknameState

    private val _accountAction = MutableStateFlow<AccountActionState>(AccountActionState.Idle)
    val accountAction: StateFlow<AccountActionState> = _accountAction

    // 로그아웃/탈퇴 완료 → 화면이 관찰해 Auth로 이동
    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut

    // ── 알림 설정 (영속화) ──
    private val _reminderEnabled = MutableStateFlow(notificationPrefs.reminderEnabled)
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled

    private val _reminderTime = MutableStateFlow(notificationPrefs.reminderHour to notificationPrefs.reminderMinute)
    /** 리마인더 시각 (시, 분). */
    val reminderTime: StateFlow<Pair<Int, Int>> = _reminderTime

    private val _newRecordEnabled = MutableStateFlow(notificationPrefs.newRecordEnabled)
    val newRecordEnabled: StateFlow<Boolean> = _newRecordEnabled

    init {
        refreshHcStatus()
        // 프로세스 복원 등으로 메모리가 비어 있으면 서버에서 재수화
        viewModelScope.launch { appStateLoader.ensureHydrated() }
    }

    /** HC 백그라운드 읽기 권한이 이미 있는지 — 있으면 요청 화면을 띄우지 않는다. */
    suspend fun isBgReadGranted(): Boolean = healthConnectManager.isBackgroundReadGranted()

    /** HC 권한 상태를 다시 확인한다 (화면 진입·재개·권한 요청 결과 후). */
    fun refreshHcStatus() {
        viewModelScope.launch { applyHcConnected(healthConnectManager.hasAllPermissions()) }
    }

    /**
     * HC 권한 요청 플로우에서 돌아온 직후 — 결과 셋 대신 실제 권한 상태를 재조회해
     * 판단한다 (이미 전부 허용된 상태에서 재요청하면 HC가 빈 결과를 돌려주기 때문).
     */
    fun onHcPermissionFlowReturned() {
        viewModelScope.launch {
            if (healthConnectManager.hasAllPermissions()) onHcPermissionGranted()
            else applyHcConnected(false)
        }
    }

    /**
     * HC 연결 상태를 반영한다. 끊겨 있으면 수영 기록 알림도 끈다 — 저장값까지 내려
     * 토글이 "켜진 채 잠긴" 모습으로 남지 않게 하고, 변경 감지 워커가 권한 없이 헛돌지 않게 한다.
     * 다시 켤 때 백그라운드 읽기 권한 요청을 다시 거치게 하려는 의도이기도 하다.
     *
     * @param connected 필수 HC 권한 3종이 모두 허용돼 있는지
     */
    private fun applyHcConnected(connected: Boolean) {
        _hcConnected.value = connected
        if (!connected && notificationPrefs.newRecordEnabled) setNewRecordEnabled(false)
    }

    /** 설정에서 HC 권한이 허용된 직후 — 상태 갱신 + 백그라운드 동기화 시작. 지급분은 홈 팝업으로 넘긴다. */
    fun onHcPermissionGranted() {
        _hcConnected.value = true
        appScope.launch {
            try {
                val earned = hcSwimSyncer.sync()
                if (earned > 0) appState.addPendingShellReward(earned)
            } catch (e: Exception) {
                Timber.w(e, "설정에서 HC 재연결 후 동기화 실패")
            }
        }
    }

    /**
     * 닉네임을 검증하고 서버에 저장한다. 성공 시 AppState 즉시 갱신.
     *
     * 쿨다운 중이면 서버를 부르지 않고 안내만 띄운다. 서버가 거부(4xx)하면 그 메시지를 쓰고,
     * 쿨다운 거부면 서버가 준 다음 가능 시각으로 프로필을 맞춰 다이얼로그가 잠기게 한다.
     *
     * @param name 입력한 닉네임
     * @param nowMillis 현재 시각(epoch ms) — 테스트 주입용
     */
    fun saveNickname(name: String, nowMillis: Long = System.currentTimeMillis()) {
        val current = profile.value
        val changeableAt = current?.nicknameChangeableAt
        // 같은 이름은 변경이 아니다 — 서버도 그대로 통과시킨다.
        if (name != current?.nickname && changeableAt != null && isNicknameCooldownActive(changeableAt, nowMillis)) {
            _nicknameState.value = NicknameSaveState.Error(nicknameCooldownMessage(changeableAt))
            return
        }
        val error = validateNickname(name)
        if (error != null) {
            _nicknameState.value = NicknameSaveState.Error(
                when (error) {
                    NicknameError.EMPTY -> "닉네임을 입력해주세요."
                    NicknameError.TOO_LONG -> "닉네임은 10자 이하로 입력해주세요."
                    NicknameError.INVALID_CHAR -> "한글, 영문, 숫자만 사용할 수 있어요."
                },
            )
            return
        }
        viewModelScope.launch {
            _nicknameState.value = NicknameSaveState.Saving
            try {
                val res = api.updateMe(UpdateUserRequest(nickname = name))
                if (res.success && res.data != null) {
                    appStateLoader.applyProfileUpdate(res.data)
                    _nicknameState.value = NicknameSaveState.Success
                } else {
                    _nicknameState.value = NicknameSaveState.Error(res.error?.message ?: "저장에 실패했어요.")
                }
            } catch (e: Exception) {
                Timber.e(e, "닉네임 변경 실패")
                val apiError = e.toApiError()
                // 기기 시계가 느려 앱 선검사를 통과했더라도 서버 판정에 맞춰 즉시 잠근다.
                if (apiError?.code == "NICKNAME_COOLDOWN") {
                    apiError.details?.nextAllowedAt?.let { next ->
                        appState.profile.value?.let { appState.applyProfile(it.copy(nicknameChangeableAt = next)) }
                    }
                }
                _nicknameState.value = NicknameSaveState.Error(apiError?.message ?: "네트워크 오류가 발생했어요.")
            }
        }
    }

    /** 닉네임 다이얼로그를 닫을 때 상태 초기화. */
    fun resetNicknameState() { _nicknameState.value = NicknameSaveState.Idle }

    // ── 알림 설정 액션 ──

    /** 수영 리마인더 on/off — 켜면 다음 발화 시각으로 예약, 끄면 취소. */
    fun setReminderEnabled(enabled: Boolean) {
        notificationPrefs.reminderEnabled = enabled
        _reminderEnabled.value = enabled
        if (enabled) reminderScheduler.schedule() else reminderScheduler.cancel()
    }

    /** 리마인더 시각 변경 — 켜져 있으면 새 시각으로 재예약. */
    fun setReminderTime(hour: Int, minute: Int) {
        notificationPrefs.reminderHour = hour
        notificationPrefs.reminderMinute = minute
        _reminderTime.value = hour to minute
        if (notificationPrefs.reminderEnabled) reminderScheduler.schedule()
    }

    /** 새 수영 기록(조개) 알림 on/off — 백그라운드 변경 감지 주기 작업 예약/취소. */
    fun setNewRecordEnabled(enabled: Boolean) {
        notificationPrefs.newRecordEnabled = enabled
        _newRecordEnabled.value = enabled
        if (enabled) hcChangeCheckScheduler.schedule() else hcChangeCheckScheduler.cancel()
    }

    /** (개발자 전용) 실제 리마인더 알림을 즉시 발송 — 문구/모양 확인용. */
    fun sendTestReminder() = notifier.showSwimReminder()

    /** (개발자 전용) 실제 새 기록 알림을 즉시 발송 — 문구/모양 확인용. */
    fun sendTestNewRecord() = notifier.showNewSwimRecord()

    /**
     * (개발자 전용) 동기화 상태를 처음으로 되돌린다 — 변경 토큰·삭제 블랙리스트·로컬 기록.
     *
     * 지운 기록은 블랙리스트에 남아 Health Connect에서 다시 들어오지 않는다.
     * 보상 흐름을 처음부터 다시 보려면 그걸 잊게 해야 해서, 앱 데이터를 통째로
     * 지우는 대신 여기서 초기화한다. 다음 동기화가 HC를 전체 범위로 다시 읽는다.
     */
    fun resetSyncState() {
        viewModelScope.launch {
            // 서버 기록도 함께 되돌린다 — 앱만 지우면 같은 날짜가 서버에 남아
            // 다음 등록이 409로 막히고 조개가 다시 지급되지 않는다.
            _devResetResult.value = try {
                val res = api.devResetSwimLogs(DevResetRequest(days = DEV_RESET_DAYS))
                val d = res.data
                if (d != null) {
                    appStateLoader.refreshCurrency()
                    "서버 ${d.deletedLogs}건 삭제 · 조개 ${d.revokedShells}개 회수"
                } else {
                    "서버 초기화 실패 — 앱만 초기화됨"
                }
            } catch (e: Exception) {
                Timber.w(e, "서버 초기화 실패")
                // 서버가 ALLOW_DEV_RESET을 끄면 404다 — 앱 쪽 초기화는 그대로 진행한다.
                "서버 초기화 불가(꺼져 있음) — 앱만 초기화됨"
            }
            hcSyncPreferences.clearAll()
            // 로컬도 같은 범위만 지운다 — 전체를 지우면 과거 기록이 다 사라지고
            // 다시 받아오는 동안 통계·달력이 텅 빈다.
            val today = LocalDate.now()
            repeat(DEV_RESET_DAYS) { i ->
                swimLogRepository.deleteByDate(today.minusDays(i.toLong()).toString())
            }
        }
    }

    /**
     * (개발자 전용) Health Connect를 최근 [HC_RESYNC_DAYS]일만큼 다시 읽어 로컬을 채운다.
     *
     * 심박·활동시간은 서버에 없어 로컬을 지우면 복구할 곳이 HC뿐이다.
     * 기존 행은 지우지 않고 HC 값으로 덮어쓴다.
     */
    fun resyncFromHealthConnect() {
        viewModelScope.launch {
            _devResetResult.value = try {
                if (!healthConnectManager.hasAllPermissions()) {
                    "Health Connect 권한이 없어요"
                } else {
                    val added = hcSwimSyncer.resyncRange(HC_RESYNC_DAYS)
                    "HC 최근 ${HC_RESYNC_DAYS}일 다시 읽음 · 새 기록 ${added}건"
                }
            } catch (e: Exception) {
                Timber.w(e, "HC 재동기화 실패")
                "HC 다시 읽기 실패"
            }
        }
    }

    /** 개발자 초기화 결과 문구 — 표시 후 [clearDevResetResult]로 비운다. */
    private val _devResetResult = MutableStateFlow<String?>(null)
    val devResetResult: StateFlow<String?> = _devResetResult

    fun clearDevResetResult() { _devResetResult.value = null }

    /**
     * 로그아웃 — 서버 토큰 무효화는 실패해도 진행. 로컬 데이터는 남기고 세션·메모리만 끊는다.
     * 다른 계정이 이어서 로그인하면 AccountSwitchGuard가 로컬을 초기화하고, 같은 계정은 그대로 이어 쓴다.
     */
    fun logout() {
        viewModelScope.launch {
            _accountAction.value = AccountActionState.Working
            runCatching {
                tokenStore.getRefreshToken()?.let { api.logout(RefreshRequest(it)) }
            }.onFailure { Timber.w(it, "서버 로그아웃 실패 — 로컬 정리는 계속 진행") }
            localDataResetter.clearSession()
            finishSignOut()
        }
    }

    /**
     * 계정 탈퇴 — 서버 삭제 성공 시에만 HC 권한을 회수하고 에셋을 제외한 로컬 전부를 지운 뒤 앱을 재시작한다.
     * 실패 시 계정이 남아 있으므로 로컬을 건드리지 않는다.
     */
    fun deleteAccount() {
        viewModelScope.launch {
            _accountAction.value = AccountActionState.Working
            try {
                val res = api.deleteMe()
                if (res.success) {
                    // 재가입 시 온보딩이 처음처럼 권한을 다시 묻도록 — 실패해도 탈퇴는 진행한다
                    healthConnectManager.revokeAllPermissions()
                    localDataResetter.clearAll(keepAssets = true)
                    finishSignOut()
                } else {
                    _accountAction.value = AccountActionState.Error(res.error?.message ?: "탈퇴에 실패했어요.")
                }
            } catch (e: Exception) {
                Timber.e(e, "계정 탈퇴 실패")
                _accountAction.value = AccountActionState.Error("네트워크 오류가 발생했어요. 다시 시도해주세요.")
            }
        }
    }

    fun resetAccountAction() { _accountAction.value = AccountActionState.Idle }

    /** 로컬 정리가 끝난 뒤 화면에 종료를 알린다 — 화면은 이를 보고 앱을 스플래시부터 재시작한다. */
    private fun finishSignOut() {
        _accountAction.value = AccountActionState.Idle
        _signedOut.value = true
    }
}

/** 닉네임 저장 진행 상태. */
sealed interface NicknameSaveState {
    data object Idle : NicknameSaveState
    data object Saving : NicknameSaveState
    data object Success : NicknameSaveState
    data class Error(val message: String) : NicknameSaveState
}

/** 로그아웃/탈퇴 진행 상태. */
sealed interface AccountActionState {
    data object Idle : AccountActionState
    data object Working : AccountActionState
    data class Error(val message: String) : AccountActionState
}

/** 개발자 초기화가 되돌리는 범위 — 오늘 하루. 서버에도 같은 값을 넘긴다. */
private const val DEV_RESET_DAYS = 1

/** HC 재읽기 범위 — Health Connect가 허용하는 과거 조회 한도(약 2개월)에 맞춘다. */
// HC가 보관 중인 전 기간을 사실상 다 덮는 범위 — 60일로는 5월 시작 데이터에 못 미쳤다.
private const val HC_RESYNC_DAYS = 365
