package kr.ilf.soodalbbobgi.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.ui.draw.paint
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
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import androidx.compose.foundation.LocalIndication
import kr.ilf.soodalbbobgi.core.ui.LocalHazeContent
import kr.ilf.soodalbbobgi.core.ui.rememberPressTint
import kr.ilf.soodalbbobgi.core.ui.LocalOverlayHost
import kr.ilf.soodalbbobgi.core.ui.LocalTabBarDim
import kr.ilf.soodalbbobgi.core.ui.OverlayHostState
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kr.ilf.soodalbbobgi.core.ui.SoodalTabBar
import kr.ilf.soodalbbobgi.core.ui.soodalBackground
import kr.ilf.soodalbbobgi.core.ui.TabBarDimLayer
import kr.ilf.soodalbbobgi.core.ui.TabBarDimState
import kr.ilf.soodalbbobgi.core.ui.motion.soodalEnter
import kr.ilf.soodalbbobgi.core.ui.motion.soodalExit
import kr.ilf.soodalbbobgi.core.ui.motion.soodalPopEnter
import kr.ilf.soodalbbobgi.core.ui.motion.soodalPopExit
import kr.ilf.soodalbbobgi.core.ui.motion.tabIndexOf
import kr.ilf.soodalbbobgi.presentation.auth.AuthRoute
import kr.ilf.soodalbbobgi.presentation.auth.AuthScreen
import kr.ilf.soodalbbobgi.presentation.calendar.CalendarScreen
import kr.ilf.soodalbbobgi.presentation.collection.CollectionScreen
import kr.ilf.soodalbbobgi.presentation.gacha.GachaScreen
import kr.ilf.soodalbbobgi.presentation.home.HomeScreen
import kr.ilf.soodalbbobgi.presentation.onboarding.OnboardingNicknameScreen
import kr.ilf.soodalbbobgi.presentation.onboarding.OnboardingNotificationScreen
import kr.ilf.soodalbbobgi.presentation.onboarding.OnboardingPermissionScreen
import kr.ilf.soodalbbobgi.presentation.profile.ProfileFullscreenOverlay
import kr.ilf.soodalbbobgi.presentation.settings.LicensesScreen
import kr.ilf.soodalbbobgi.presentation.settings.SettingsScreen
import kr.ilf.soodalbbobgi.presentation.shop.ShopScreen
import kr.ilf.soodalbbobgi.presentation.splash.SplashDestination
import kr.ilf.soodalbbobgi.presentation.splash.SplashScreen

/**
 * 전환 애니메이션 도중의 연타로 같은 목적지가 백스택에 여러 번 쌓이는 것을 막는다.
 *
 * 화면 전환이 진행되는 동안 출발 화면은 RESUMED에서 내려오므로, 현재 최상단 항목이
 * RESUMED일 때(= 전환이 없을 때)만 navigate를 수행한다.
 *
 * @param route 이동할 목적지 라우트
 */
private fun NavController.navigateOnce(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route)
    }
}

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

    // backdrop blur — Haze 문서의 zIndex 레이어링: 배경=소스(z0), 카드 각각=소스(z1)+
    // 배경만 블러, 탭바/팝업=모든 소스(배경+카드) 블러. 어떤 이펙트도 다른 소스의
    // 녹화물 '안'에 갇히지 않아 전부 실시간으로 렌더된다.
    val hazeState = remember { HazeState() }
    // 누름 피드백(테마별 스크림) — 아래 CompositionLocalProvider로 화면 전체에 깐다.
    val pressTint = rememberPressTint()
    // 팝업/시트를 콘텐츠 소스 밖(탭바 레이어)에서 그리게 하는 호스트 — 소스 안에서는
    // hazeEffect가 재귀 드로잉이라 불가하므로, 오버레이는 전부 여기로 호이스팅한다.
    val overlayHost = remember { OverlayHostState() }

    // 상태바 패딩은 루트가 아닌 화면별로 적용한다 — 캘린더처럼 콘텐츠가
    // 상태바 밑으로 스크롤되며 페이드되는 화면을 허용하기 위함.
    val bgColors = SoodalDesign.colors
    // 라이트: 디자인 CSS를 픽셀 그대로 렌더한 배경 이미지(soodal_bg) — 근사 대신 정확 일치.
    // 다크: Compose 그라데이션(soodalBackground) 유지.
    val bgModifier = if (!bgColors.isDark) {
        Modifier.paint(
            painter = androidx.compose.ui.res.painterResource(id = kr.ilf.soodalbbobgi.R.drawable.soodal_bg),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds,
        )
    } else {
        Modifier.soodalBackground(bgColors)
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // 배경 레이어 (풀블리드) — z=0 소스. 카드/탭바/팝업 모두의 블러에 배경이 깔린다.
        Box(Modifier.fillMaxSize().hazeSource(hazeState, zIndex = 0f)) {
            Box(Modifier.fillMaxSize().then(bgModifier))
        }

        Box(modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        // 콘텐츠는 화면 전체를 쓰고 탭바는 그 위에 떠 있는 오버레이 —
        // 콘텐츠가 플로팅 바 뒤로 스크롤되고, 바 주변은 투명하게 비친다.
        // 탭 화면들은 스크롤 끝에 TabBarClearance 여백을 둬 마지막 콘텐츠가 가려지지 않게 한다.
        // 화면 안 풀스크린 dim 팝업이 DimTabBarWhileVisible()로 탭바 dim을 켤 수 있게 제공한다.
        CompositionLocalProvider(
            LocalTabBarDim provides tabBarDim,
            // 기본 리플 대신 앱 공용 누름 틴트 — indication을 명시하지 않는 clickable에 적용된다.
            LocalIndication provides pressTint,
            LocalHazeContent provides hazeState,
            LocalOverlayHost provides overlayHost,
        ) {
            // 콘텐츠는 더 이상 통소스가 아니다 — 카드 각각이 자기를 z=1 소스로 등록한다(cardFrost).
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
                        OnboardingNicknameScreen(onNext = { navController.navigateOnce(Screen.OnboardingPermission.route) })
                    }
                    composable(Screen.OnboardingPermission.route) {
                        // HC 권한 단계 다음은 알림 설정 단계 — 둘 다 알림 단계로 넘긴다.
                        OnboardingPermissionScreen(
                            onConnect = { navController.navigateOnce(Screen.OnboardingNotification.route) },
                            onSkip = { navController.navigateOnce(Screen.OnboardingNotification.route) },
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
                            onNavigateToSettings = { navController.navigateOnce(Screen.Settings.route) },
                            onNavigateToCollection = { navController.navigateOnce(Screen.Collection.route) },
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
                            onOpenLicenses = { navController.navigateOnce(Screen.Licenses.route) },
                        )
                    }
                    composable(Screen.Licenses.route) {
                        LicensesScreen(onBack = { navController.popBackStack() })
                    }
                    composable(Screen.Collection.route) {
                        CollectionScreen(onBack = { navController.popBackStack() })
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
                        hazeState = hazeState,
                    )
                    TabBarDimLayer(
                        state = tabBarDim,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }

        // (상태바 스크림 제거 — 통일 배경 위에서 이음매가 생기지 않도록. 상태바 아이콘은
        //  밝은 물빛 배경 위 어두운 아이콘이라 스크림 없이도 가독성 확보.)

        // 호이스팅된 팝업/시트 — 콘텐츠 소스 밖(탭바 위)에서 그려 hazeEffect(콘텐츠 블러)가 동작한다.
        CompositionLocalProvider(
            LocalTabBarDim provides tabBarDim,
            // 기본 리플 대신 앱 공용 누름 틴트 — indication을 명시하지 않는 clickable에 적용된다.
            LocalIndication provides pressTint,
            LocalHazeContent provides hazeState,
        ) {
            overlayHost.entries.entries.sortedBy { it.key }.forEach { (_, overlay) -> overlay() }
        }

        // 전체보기 오버레이: 탭바/콘텐츠 위(최상위). 닫힘 애니메이션이 끝나면 상태를 되돌린다.
        if (fullscreenOpen) {
            ProfileFullscreenOverlay(
                onReady = { cardOverlayReady = true },
                onClosed = { fullscreenOpen = false; cardOverlayReady = false },
            )
        }
        } // 인셋 레이어
    }
}
