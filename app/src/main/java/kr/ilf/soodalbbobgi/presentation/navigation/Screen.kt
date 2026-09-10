package kr.ilf.soodalbbobgi.presentation.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Auth : Screen("auth")
    data object OnboardingNickname : Screen("onboarding_nickname")
    data object OnboardingPermission : Screen("onboarding_permission")
    data object OnboardingNotification : Screen("onboarding_notification")
    data object Home : Screen("home")
    data object Calendar : Screen("calendar")
    data object Gacha : Screen("gacha")
    data object Shop : Screen("shop")
    data object Settings : Screen("settings")
    data object Licenses : Screen("licenses")
    data object Collection : Screen("collection")
}
