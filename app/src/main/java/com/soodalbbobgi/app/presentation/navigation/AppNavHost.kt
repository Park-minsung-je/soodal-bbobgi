package com.soodalbbobgi.app.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.motion.soodalEnter
import com.soodalbbobgi.app.core.ui.motion.soodalExit
import com.soodalbbobgi.app.core.ui.motion.soodalPopEnter
import com.soodalbbobgi.app.core.ui.motion.soodalPopExit
import com.soodalbbobgi.app.presentation.auth.AuthRoute
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
import com.soodalbbobgi.app.presentation.splash.SplashDestination
import com.soodalbbobgi.app.presentation.splash.SplashScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(SoodalDesign.colors.bgDeep)
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            enterTransition = { soodalEnter() },
            exitTransition = { soodalExit() },
            popEnterTransition = { soodalPopEnter() },
            popExitTransition = { soodalPopExit() },
        ) {
        composable(Screen.Splash.route) {
            SplashScreen(onNavigate = { dest ->
                val target = when (dest) {
                    SplashDestination.Auth -> Screen.Auth.route
                    SplashDestination.Onboarding -> Screen.OnboardingNickname.route
                    SplashDestination.Permission -> Screen.OnboardingPermission.route
                    SplashDestination.Home -> Screen.Home.route
                    SplashDestination.Loading -> return@SplashScreen
                }
                navController.navigate(target) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            })
        }
        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigate = { route ->
                    val target = when (route) {
                        AuthRoute.Onboarding -> Screen.OnboardingNickname.route
                        AuthRoute.Permission -> Screen.OnboardingPermission.route
                        AuthRoute.Home -> Screen.Home.route
                    }
                    navController.navigate(target) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                },
            )
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
}
