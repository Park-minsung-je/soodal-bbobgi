package com.soodalbbobgi.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.core.session.UserSession
import com.soodalbbobgi.app.data.remote.api.SoodalApi
import com.soodalbbobgi.app.data.remote.dto.UpdateUserRequest
import com.soodalbbobgi.app.data.remote.dto.UserData
import com.soodalbbobgi.app.domain.model.User
import com.soodalbbobgi.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 온보딩 화면의 닉네임/성별/연령대 저장을 서버에 전송하고
 * 응답을 받아 로컬 Room에도 동일하게 반영한다.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val soodalApi: SoodalApi,
    private val userSession: UserSession,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _saveState = MutableStateFlow<OnboardingSaveState>(OnboardingSaveState.Idle)
    val saveState: StateFlow<OnboardingSaveState> = _saveState

    /**
     * 닉네임, 성별, 연령대를 서버에 저장하고 서버 응답으로 Room을 갱신한다.
     *
     * @param nickname 닉네임 (필수)
     * @param gender 성별 (선택, null 가능)
     * @param ageRange 연령대 (선택, null 가능)
     */
    fun saveProfile(nickname: String, gender: String?, ageRange: String?) {
        viewModelScope.launch {
            _saveState.value = OnboardingSaveState.Saving
            try {
                val response = soodalApi.updateMe(
                    UpdateUserRequest(
                        nickname = nickname,
                        gender = gender,
                        ageRange = ageRange,
                    )
                )
                if (response.success && response.data != null) {
                    saveUserToRoom(response.data)
                    _saveState.value = OnboardingSaveState.Success
                } else {
                    _saveState.value = OnboardingSaveState.Error(
                        response.error?.message ?: "저장에 실패했어요."
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "프로필 저장 실패")
                _saveState.value = OnboardingSaveState.Error("네트워크 오류가 발생했어요.")
            }
        }
    }

    /** 서버 응답 사용자 데이터를 Room에 반영한다 (REPLACE 전략으로 기존 행 덮어쓰기). */
    private suspend fun saveUserToRoom(u: UserData) {
        userRepository.createUser(User(
            id = u.id,
            nickname = u.nickname ?: "",
            shellBalance = u.shellBalance,
            pearlBalance = u.pearlBalance,
            pityCounter = u.pityCounter,
            lastShellGrantDate = u.lastShellGrantDate,
            gender = u.gender,
            ageRange = u.ageRange,
            authProvider = u.authProvider,
        ))
    }
}

sealed interface OnboardingSaveState {
    data object Idle : OnboardingSaveState
    data object Saving : OnboardingSaveState
    data object Success : OnboardingSaveState
    data class Error(val message: String) : OnboardingSaveState
}
