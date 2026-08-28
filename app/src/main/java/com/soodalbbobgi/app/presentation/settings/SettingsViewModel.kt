package com.soodalbbobgi.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.di.ApplicationScope
import com.soodalbbobgi.app.core.notify.SoodalNotifier
import com.soodalbbobgi.app.core.state.AppState
import com.soodalbbobgi.app.core.state.AppStateLoader
import com.soodalbbobgi.app.data.auth.TokenStore
import com.soodalbbobgi.app.data.health.HcSwimSyncer
import com.soodalbbobgi.app.data.health.HcSyncPreferences
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.data.notify.NotificationPrefs
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.DevResetRequest
import com.soodalbbobgi.app.data.remote.dto.RefreshRequest
import com.soodalbbobgi.app.data.remote.dto.UpdateUserRequest
import com.soodalbbobgi.app.domain.model.UserProfile
import com.soodalbbobgi.app.domain.repository.SwimLogRepository
import com.soodalbbobgi.app.work.HcChangeCheckScheduler
import com.soodalbbobgi.app.work.ReminderScheduler
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

    /** HC 권한 상태를 다시 확인한다 (화면 진입/권한 요청 결과 후). */
    fun refreshHcStatus() {
        viewModelScope.launch { _hcConnected.value = healthConnectManager.hasAllPermissions() }
    }

    /** 설정에서 HC 권한이 허용된 직후 — 상태 갱신 + 백그라운드 동기화 시작. */
    fun onHcPermissionGranted() {
        _hcConnected.value = true
        appScope.launch {
            try {
                hcSwimSyncer.sync()
            } catch (e: Exception) {
                Timber.w(e, "설정에서 HC 재연결 후 동기화 실패")
            }
        }
    }

    /** 닉네임을 검증하고 서버에 저장한다. 성공 시 AppState 즉시 갱신. */
    fun saveNickname(name: String) {
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
                _nicknameState.value = NicknameSaveState.Error("네트워크 오류가 발생했어요.")
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

    /** 개발자 초기화 결과 문구 — 표시 후 [clearDevResetResult]로 비운다. */
    private val _devResetResult = MutableStateFlow<String?>(null)
    val devResetResult: StateFlow<String?> = _devResetResult

    fun clearDevResetResult() { _devResetResult.value = null }

    /**
     * 로그아웃 — 서버 토큰 무효화는 실패해도 진행하고, 로컬 토큰/메모리/Room을 정리한다.
     * 로컬 수영 기록도 삭제한다 (다른 계정 재로그인 시 데이터 혼입 방지,
     * 같은 계정은 HC 재동기화로 복구).
     */
    fun logout() {
        viewModelScope.launch {
            _accountAction.value = AccountActionState.Working
            runCatching {
                tokenStore.getRefreshToken()?.let { api.logout(RefreshRequest(it)) }
            }.onFailure { Timber.w(it, "서버 로그아웃 실패 — 로컬 정리는 계속 진행") }
            clearLocalAndSignOut()
        }
    }

    /** 계정 탈퇴 — 서버 삭제가 성공해야만 로컬을 정리한다 (실패 시 계정이 남아있으므로). */
    fun deleteAccount() {
        viewModelScope.launch {
            _accountAction.value = AccountActionState.Working
            try {
                val res = api.deleteMe()
                if (res.success) {
                    clearLocalAndSignOut()
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

    private suspend fun clearLocalAndSignOut() {
        tokenStore.clearTokens()
        hcSyncPreferences.clearChangesToken()
        swimLogRepository.deleteAll()
        appState.clear()
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
