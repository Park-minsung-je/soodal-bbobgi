package kr.ilf.soodalbbobgi.presentation.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint
import kr.ilf.soodalbbobgi.BuildConfig
import kr.ilf.soodalbbobgi.core.theme.SoodalTheme
import kr.ilf.soodalbbobgi.core.theme.SoodalThemeType
import kr.ilf.soodalbbobgi.core.theme.ThemePreferences
import kr.ilf.soodalbbobgi.core.util.LegalPages
import kr.ilf.soodalbbobgi.core.util.openInBrowser
import javax.inject.Inject

/**
 * Health Connect 권한 사용 근거 액티비티 — 시스템이 `ACTION_SHOW_PERMISSIONS_RATIONALE`(13 이하)와
 * `VIEW_PERMISSION_USAGE` + `HEALTH_PERMISSIONS`(14+)로 여는 진입점.
 *
 * [kr.ilf.soodalbbobgi.MainActivity]와 분리한 이유: 그쪽은 스플래시 → 로그인 → 홈으로 흐르므로
 * 시스템 설정에서 들어온 사용자에게 앱이 그냥 켜지는 것으로 보였다. 이 화면은 로그인 없이
 * 설명만 보여 주고 돌아가기로 닫힌다.
 */
@AndroidEntryPoint
class HealthPermissionRationaleActivity : ComponentActivity() {
    @Inject lateinit var themePreferences: ThemePreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true
        setContent {
            val theme by themePreferences.themeFlow.collectAsState(initial = SoodalThemeType.Light)
            SoodalTheme(theme = theme) {
                HealthPermissionRationaleScreen(
                    onBack = { finish() },
                    onOpenPrivacyPolicy = {
                        openInBrowser(this, LegalPages.privacyUrl(BuildConfig.BASE_URL))
                    },
                )
            }
        }
    }
}
