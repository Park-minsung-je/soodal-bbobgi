package kr.ilf.soodalbbobgi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import kr.ilf.soodalbbobgi.core.theme.SoodalTheme
import kr.ilf.soodalbbobgi.core.theme.SoodalThemeType
import kr.ilf.soodalbbobgi.core.theme.ThemePreferences
import kr.ilf.soodalbbobgi.data.update.InAppUpdateCoordinator
import kr.ilf.soodalbbobgi.presentation.update.UpdateReadyDialog
import kr.ilf.soodalbbobgi.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var inAppUpdate: InAppUpdateCoordinator

    // Play 업데이트 화면의 결과 — 즉시 방식을 취소해도 다음 onResume이 다시 띄우므로 여기서 할 일은 없다.
    private val updateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {}

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        // 시스템 스플래시 — 안드로이드 12+가 런처 아이콘 → 스플래시로 확대 전환을 자동으로 해준다.
        // 커스텀 exit 애니메이션은 그 위에 겹쳐 아이콘이 잘려 보이므로 두지 않고 기본 연출에 맡긴다.
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        setContent {
            val theme by themePreferences.themeFlow.collectAsState(initial = SoodalThemeType.Light)
            val navController = rememberNavController()
            val updateReady by inAppUpdate.readyToInstall.collectAsState()
            SoodalTheme(theme = theme) {
                AppNavHost(navController = navController)
                // 유연 업데이트를 다 받았으면 어느 화면 위에서든 "다시 시작"을 안내한다.
                if (updateReady) {
                    UpdateReadyDialog(onLater = inAppUpdate::dismissReady, onRestart = inAppUpdate::completeUpdate)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 새 버전 확인 + 진행 중이던 즉시 업데이트 재개 + 내려받은 유연 업데이트 감지.
        inAppUpdate.onResume(updateLauncher)
    }
}
