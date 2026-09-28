package kr.ilf.soodalbbobgi

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.kakao.sdk.common.KakaoSdk
import dagger.hilt.android.HiltAndroidApp
import kr.ilf.soodalbbobgi.core.notify.SoodalNotifier
import kr.ilf.soodalbbobgi.work.NotificationSchedules
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class SoodalBbobgiApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationSchedules: NotificationSchedules
    @Inject lateinit var notifier: SoodalNotifier

    /** WorkManager on-demand 초기화 설정 — @HiltWorker 주입을 위해 HiltWorkerFactory 사용. */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        KakaoSdk.init(this, BuildConfig.KAKAO_NATIVE_APP_KEY)
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // 알림 채널은 첫 알림 전에도 시스템 설정에 보여야 한다 (설치 직후 카테고리 조정 가능)
        notifier.ensureChannels()
        // 강제 종료 등으로 알람이 지워졌어도 프로세스가 다시 뜨면 되살린다 (같은 시각이면 교체라 중복 없음). 로그아웃 상태면 무동작
        notificationSchedules.restoreIfLoggedIn()
    }
}
