package com.soodalbbobgi.app.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import com.soodalbbobgi.app.core.ui.LocalTabBarDim
import com.soodalbbobgi.app.core.ui.SoodalTabBar
import com.soodalbbobgi.app.core.ui.TabBarDimLayer
import com.soodalbbobgi.app.core.ui.TabBarDimState
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
import com.soodalbbobgi.app.presentation.onboarding.OnboardingNotificationScreen
import com.soodalbbobgi.app.presentation.onboarding.OnboardingPermissionScreen
import com.soodalbbobgi.app.presentation.profile.ProfileFullscreenOverlay
import com.soodalbbobgi.app.presentation.settings.LicensesScreen
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
    // 풀스크린 dim 팝업이 떠 있는 동안 탭바를 함께 dim 처리하기 위한 공유 카운터.
    val tabBarDim = remember { TabBarDimState() }

    // 탭 선택 시 공통 네비게이션: Home을 루트로 두고 상태를 보존/복원한다.
    val onSelectTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // 상태바 패딩은 루트가 아닌 화면별로 적용한다 — 캘린더처럼 콘텐츠가
    // 상태바 밑으로 스크롤되며 페이드되는 화면을 허용하기 위함.
    Box(modifier = Modifier
        .fillMaxSize()
        .background(SoodalDesign.colors.bgDeep)
        .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 콘텐츠는 화면 전체를 쓰고 탭바는 그 위에 떠 있는 오버레이 —
        // 콘텐츠가 플로팅 바 뒤로 스크롤되고, 바 주변은 투명하게 비친다.
        // 탭 화면들은 스크롤 끝에 TabBarClearance 여백을 둬 마지막 콘텐츠가 가려지지 않게 한다.
        // 화면 안 풀스크린 dim 팝업이 DimTabBarWhileVisible()로 탭바 dim을 켤 수 있게 제공한다.
        CompositionLocalProvider(LocalTabBarDim provides tabBarDim) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                        // HC 권한 단계 다음은 알림 설정 단계 — 둘 다 알림 단계로 넘긴다.
                        OnboardingPermissionScreen(
                            onConnect = { navController.navigate(Screen.OnboardingNotification.route) },
                            onSkip = { navController.navigate(Screen.OnboardingNotification.route) },
                        )
                    }
                    composable(Screen.OnboardingNotification.route) {
                        OnboardingNotificationScreen(
                            onDone = { navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } } },
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
                        SettingsScreen(
                            onBack = { navController.popBackStack() },
                            onSignedOut = {
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onOpenLicenses = { navController.navigate(Screen.Licenses.route) },
                        )
                    }
                    composable(Screen.Licenses.route) {
                        LicensesScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }

            // 하단 플로팅 탭바: 탭 화면에서만 보이며, 진입/이탈 시 아래로 슬라이드.
            AnimatedVisibility(
                visible = showTabBar,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) {
                // 화면 안 팝업 스크림은 탭바를 덮지 못하므로(탭바가 위 레이어),
                // 팝업이 떠 있는 동안 같은 모양의 dim 레이어를 탭바 위에 직접 덮어 비활성화한다.
                Box {
                    SoodalTabBar(
                        activeTab = currentRoute ?: Screen.Home.route,
                        onTabSelected = onSelectTab,
                    )
                    TabBarDimLayer(
                        state = tabBarDim,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

        // 상태바 페이드 스크림 — 상태바 상단 70%는 불투명(아이콘 가독) 유지,
        // 하단 30% 지점부터 경계 아래까지 smoothstep S-커브로 완만하게 풀려
        // 스크롤 콘텐츠가 상태바 밑에서 자연스럽게 사라진다.
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val scrim = SoodalDesign.colors.bgDeep
        val scrimHeight = statusBarHeight + 20.dp
        // 페이드 시작점 = 상태바 높이의 65% 지점 (전체 스크림 높이 기준 비율로 환산)
        val solidEnd = (statusBarHeight.value * 0.65f) / scrimHeight.value
        fun fadeStop(t: Float, alpha: Float) = (solidEnd + (1f - solidEnd) * t) to scrim.copy(alpha = alpha)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(scrimHeight)
                .background(
                    Brush.verticalGradient(
                        0f to scrim,
                        solidEnd to scrim,
                        fadeStop(0.2f, 0.9f),
                        fadeStop(0.4f, 0.65f),
                        fadeStop(0.6f, 0.35f),
                        fadeStop(0.8f, 0.1f),
                        fadeStop(1f, 0f),
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
