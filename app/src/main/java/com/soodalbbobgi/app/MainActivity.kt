package com.soodalbbobgi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val theme by themePreferences.themeFlow.collectAsState(initial = SoodalThemeType.Light)
            val navController = rememberNavController()
            SoodalTheme(theme = theme) {
                AppNavHost(navController = navController)
            }
        }
    }
}
