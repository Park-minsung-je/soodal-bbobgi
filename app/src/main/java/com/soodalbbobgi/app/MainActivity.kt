package com.soodalbbobgi.app

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.animation.AnticipateInterpolator
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
        // 시스템 스플래시(킥판 수달 아이콘) → 종료 시 아이콘이 커지며 사라져 Compose 스플래시로 이어진다.
        val splashScreen = installSplashScreen()
        splashScreen.setOnExitAnimationListener { provider ->
            val icon = provider.iconView
            // 살짝만 커지며 사라진다 — 크게 키우면 화면 밖으로 잘려 보이므로 1.25배까지만.
            val scaleX = ObjectAnimator.ofFloat(icon, android.view.View.SCALE_X, 1f, 1.25f)
            val scaleY = ObjectAnimator.ofFloat(icon, android.view.View.SCALE_Y, 1f, 1.25f)
            val fade = ObjectAnimator.ofFloat(icon, android.view.View.ALPHA, 1f, 0f)
            listOf(scaleX, scaleY, fade).forEach {
                it.interpolator = AnticipateInterpolator(0.4f)
                it.duration = 380L
            }
            fade.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) = provider.remove()
            })
            scaleX.start(); scaleY.start(); fade.start()
        }
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
