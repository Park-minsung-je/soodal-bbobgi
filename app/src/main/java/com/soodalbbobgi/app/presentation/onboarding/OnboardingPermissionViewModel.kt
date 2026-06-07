package com.soodalbbobgi.app.presentation.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.soodalbbobgi.app.data.asset.AssetManager
import com.soodalbbobgi.app.data.health.HcSwimSyncer
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ViewModel() {

    /**
     * HC 권한 허용 직후 호출한다. 에셋 동기화 + HC 동기화를 백그라운드로 실행한다.
     * Home 화면 진입 전에 데이터가 준비되도록 onConnect() 콜백 호출 전에 시작한다.
     */
    fun onPermissionGranted() {
        viewModelScope.launch {
            val result = assetManager.sync()
            if (result.isFailure) {
                Timber.w(result.exceptionOrNull(), "권한 허용 후 에셋 동기화 실패 (앱 계속 진행)")
            }
        }
        viewModelScope.launch {
            try {
                hcSwimSyncer.sync()
            } catch (e: Exception) {
                Timber.w(e, "권한 허용 후 HC 동기화 실패 (앱 계속 진행)")
            }
        }
    }
}
