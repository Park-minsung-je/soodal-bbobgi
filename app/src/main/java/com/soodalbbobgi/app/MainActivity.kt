package com.soodalbbobgi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.soodalbbobgi.app.core.theme.SoodalTheme
import com.soodalbbobgi.app.core.theme.SoodalThemeType
import com.soodalbbobgi.app.core.theme.ThemePreferences
import com.soodalbbobgi.app.presentation.navigation.AppNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var themePreferences: ThemePreferences

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
            SoodalTheme(theme = theme) {
                AppNavHost(navController = navController)
            }
        }
    }
}
