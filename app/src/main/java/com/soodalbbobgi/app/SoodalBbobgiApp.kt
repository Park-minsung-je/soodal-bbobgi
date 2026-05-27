package com.soodalbbobgi.app

import android.app.Application
import com.kakao.sdk.common.KakaoSdk
import com.soodalbbobgi.app.core.init.DebugInitializer
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SoodalBbobgiApp : Application() {

    @Inject
    lateinit var debugInitializer: DebugInitializer

    override fun onCreate() {
        super.onCreate()
        // 카카오 SDK 초기화 — 앱 시작 시 반드시 먼저 호출해야 함
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            debugInitializer.initialize()
        }
    }
}
