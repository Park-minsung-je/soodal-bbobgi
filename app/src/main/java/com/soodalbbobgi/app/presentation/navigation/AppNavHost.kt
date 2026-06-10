package com.soodalbbobgi.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.SoodalTabBar
import com.soodalbbobgi.app.core.ui.motion.soodalEnter
import com.soodalbbobgi.app.core.ui.motion.soodalExit
import com.soodalbbobgi.app.core.ui.motion.soodalPopEnter
import com.soodalbbobgi.app.core.ui.motion.soodalPopExit
import com.soodalbbobgi.app.core.ui.motion.tabIndexOf
import com.soodalbbobgi.app.presentation.auth.AuthRoute
import com.soodalbbobgi.app.presentation.auth.AuthScreen
import com.soodalbbobgi.app.presentation.calendar.CalendarScreen
import com.soodalbbobgi.app.presentation.gacha.GachaScreen
import com.soodalbbobgi.app.presentation.home.HomeScreen
import com.soodalbbobgi.app.presentation.onboarding.OnboardingNicknameScreen
import com.soodalbbobgi.app.presentation.onboarding.OnboardingPermissionScreen
import com.soodalbbobgi.app.presentation.profile.ProfileFullscreenOverlay
import com.soodalbbobgi.app.presentation.settings.SettingsScreen
import com.soodalbbobgi.app.presentation.shop.ShopScreen
import com.soodalbbobgi.app.presentation.splash.SplashDestination
import com.soodalbbobgi.app.presentation.splash.SplashScreen

@Composable
fun AppNavHost(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // 홈 프로필 편집 시트 열림 상태. 시트가 탭바 위(화면 바닥까지)를 덮도록 끌어올려 둔다.
    var homeEditorOpen by rememberSaveable { mutableStateOf(false) }
    // 전체보기 오버레이 열림 상태. 편집 시트와 마찬가지로 열리면 탭바를 감춘다.
    var fullscreenOpen by rememberSaveable { mutableStateOf(false) }
    // 오버레이 카드가 측정되어 홈 카드 자리를 덮을 준비가 됐는지. 이때 비로소 홈 카드를 숨겨
    // 교체 순간의 빈 프레임(깜빡임)을 없앤다.
    var cardOverlayReady by remember { mutableStateOf(false) }
    // 탭 화면(홈/캘린더/뽑기/상점)에서만 하단 탭바를 노출하되, 편집 시트/전체보기가 열리면 감춘다.
    val showTabBar = tabIndexOf(currentRoute) != null && !homeEditorOpen && !fullscreenOpen

    // 탭 선택 시 공통 네비게이션: Home을 루트로 두고 상태를 보존/복원한다.
    val onSelectTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(SoodalDesign.colors.bgDeep)
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 콘텐츠 영역: 화면 전환 슬라이드가 이 Box 안에서만 일어난다(탭바는 제외).
            Box(modifier = Modifier.weight(1f)) {
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
                            onNavigateToTab = onSelectTab,
                            onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                            onOpenFullscreen = { cardOverlayReady = false; fullscreenOpen = true },
                            hideCard = cardOverlayReady,
                            editorOpen = homeEditorOpen,
                            onEditorOpenChange = { homeEditorOpen = it },
                        )
                    }
                    composable(Screen.Calendar.route) {
                        CalendarScreen()
                    }
                    composable(Screen.Gacha.route) {
                        GachaScreen()
                    }
                    composable(Screen.Shop.route) {
                        ShopScreen()
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }

            // 하단 탭바: 화면 바깥에 고정. 탭 화면에서만 보이며, 진입/이탈 시 아래로 슬라이드.
            AnimatedVisibility(
                visible = showTabBar,
                enter = slideInVertically { it } + expandVertically(),
                exit = slideOutVertically { it } + shrinkVertically(),
            ) {
                SoodalTabBar(
                    activeTab = currentRoute ?: Screen.Home.route,
                    onTabSelected = onSelectTab,
                )
            }
        }

        // 상태바 경계 페이드 — 스크롤 콘텐츠가 상태바 라인에서 딱 잘리지 않고
        // bgDeep으로 자연스럽게 사라지게 하는 그라데이션 오버레이 (전 화면 공통).
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(SoodalDesign.colors.bgDeep, SoodalDesign.colors.bgDeep.copy(alpha = 0f)),
                    ),
                ),
        )

        // 전체보기 오버레이: 탭바/콘텐츠 위(최상위). 닫힘 애니메이션이 끝나면 상태를 되돌린다.
        if (fullscreenOpen) {
            ProfileFullscreenOverlay(
                onReady = { cardOverlayReady = true },
                onClosed = { fullscreenOpen = false; cardOverlayReady = false },
            )
        }
    }
}
