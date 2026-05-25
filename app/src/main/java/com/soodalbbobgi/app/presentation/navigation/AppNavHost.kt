package com.soodalbbobgi.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.soodalbbobgi.app.presentation.auth.AuthScreen
import com.soodalbbobgi.app.presentation.home.HomeScreen
import com.soodalbbobgi.app.presentation.onboarding.OnboardingNicknameScreen
import com.soodalbbobgi.app.presentation.onboarding.OnboardingPermissionScreen
import com.soodalbbobgi.app.presentation.splash.SplashScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(onDone = {
                navController.navigate(Screen.Auth.route) { popUpTo(Screen.Splash.route) { inclusive = true } }
            })
        }
        composable(Screen.Auth.route) {
            AuthScreen(onAuthed = {
                navController.navigate(Screen.OnboardingNickname.route) { popUpTo(Screen.Auth.route) { inclusive = true } }
            })
        }
        composable(Screen.OnboardingNickname.route) {
            OnboardingNicknameScreen(onNext = { navController.navigate(Screen.OnboardingPermission.route) })
        }
        composable(Screen.OnboardingPermission.route) {
            OnboardingPermissionScreen(
                onConnect = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
                onSkip = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToTab = { tab ->
                    val route = when (tab) {
                        "calendar" -> Screen.Calendar.route
                        "gacha" -> Screen.Gacha.route
                        "shop" -> Screen.Shop.route
                        else -> return@HomeScreen
                    }
                    navController.navigate(route)
                },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToProfileFullscreen = { navController.navigate(Screen.ProfileFullscreen.route) },
                onNavigateToProfileEditor = { navController.navigate(Screen.ProfileEditor.route) },
            )
        }
    }
}
