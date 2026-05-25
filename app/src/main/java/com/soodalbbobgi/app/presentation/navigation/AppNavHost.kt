package com.soodalbbobgi.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.soodalbbobgi.app.presentation.auth.AuthScreen
import com.soodalbbobgi.app.presentation.calendar.CalendarScreen
import com.soodalbbobgi.app.presentation.gacha.GachaScreen
import com.soodalbbobgi.app.presentation.home.HomeScreen
import com.soodalbbobgi.app.presentation.onboarding.OnboardingNicknameScreen
import com.soodalbbobgi.app.presentation.onboarding.OnboardingPermissionScreen
import com.soodalbbobgi.app.presentation.profile.ProfileEditorScreen
import com.soodalbbobgi.app.presentation.profile.ProfileFullscreenScreen
import com.soodalbbobgi.app.presentation.settings.SettingsScreen
import com.soodalbbobgi.app.presentation.shop.ShopScreen
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
        composable(Screen.Calendar.route) {
            CalendarScreen(
                onNavigateToTab = { tab ->
                    val route = when (tab) {
                        "home" -> Screen.Home.route
                        "gacha" -> Screen.Gacha.route
                        "shop" -> Screen.Shop.route
                        else -> return@CalendarScreen
                    }
                    navController.navigate(route) { popUpTo(Screen.Home.route) }
                },
            )
        }
        composable(Screen.Gacha.route) {
            GachaScreen(onNavigateToTab = { tab ->
                val route = when (tab) {
                    "home" -> Screen.Home.route
                    "calendar" -> Screen.Calendar.route
                    "shop" -> Screen.Shop.route
                    else -> return@GachaScreen
                }
                navController.navigate(route) { popUpTo(Screen.Home.route) }
            })
        }
        composable(Screen.Shop.route) {
            ShopScreen(onNavigateToTab = { tab ->
                val route = when (tab) {
                    "home" -> Screen.Home.route
                    "calendar" -> Screen.Calendar.route
                    "gacha" -> Screen.Gacha.route
                    else -> return@ShopScreen
                }
                navController.navigate(route) { popUpTo(Screen.Home.route) }
            })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ProfileEditor.route) {
            ProfileEditorScreen(
                onBack = { navController.popBackStack() },
                onPreview = { navController.navigate(Screen.ProfileFullscreen.route) },
            )
        }
        composable(Screen.ProfileFullscreen.route) {
            ProfileFullscreenScreen(
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Screen.ProfileEditor.route) },
            )
        }
    }
}
