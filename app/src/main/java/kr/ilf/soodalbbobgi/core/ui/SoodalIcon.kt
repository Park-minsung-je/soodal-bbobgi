package kr.ilf.soodalbbobgi.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kr.ilf.soodalbbobgi.R

enum class SoodalIcons(@DrawableRes val resId: Int, val fullColor: Boolean = false) {
    Home(R.drawable.ic_home),
    Calendar(R.drawable.ic_calendar),
    Gacha(R.drawable.ic_gacha),
    Shop(R.drawable.ic_shop),
    Shell(R.drawable.ic_shell, fullColor = true),
    Pearl(R.drawable.ic_pearl, fullColor = true),
    Settings(R.drawable.ic_settings),
    Edit(R.drawable.ic_edit),
    Save(R.drawable.ic_save),
    Share(R.drawable.ic_share),
    Warn(R.drawable.ic_warn),
    Sync(R.drawable.ic_sync),
    Swimmer(R.drawable.ic_swimmer),
    Otter(R.drawable.ic_otter),
    Lock(R.drawable.ic_lock),
    ArrowLeft(R.drawable.ic_arrow_left),
    ArrowRight(R.drawable.ic_arrow_right),
    Aurora(R.drawable.ic_aurora),
    Box(R.drawable.ic_box),
    Camera(R.drawable.ic_camera),
    Check(R.drawable.ic_check),
    Close(R.drawable.ic_close),
    Coral(R.drawable.ic_coral),
    Diamond(R.drawable.ic_diamond),
    Fire(R.drawable.ic_fire, fullColor = true),
    Frame(R.drawable.ic_frame),
    Gift(R.drawable.ic_gift),
    Heart(R.drawable.ic_heart),
    HealthConnect(R.drawable.ic_health_connect),
    Clock(R.drawable.ic_clock),
    Moon(R.drawable.ic_moon),
    Pin(R.drawable.ic_pin),
    Plus(R.drawable.ic_plus),
    Reset(R.drawable.ic_reset),
    Ruler(R.drawable.ic_ruler),
    Sparkle(R.drawable.ic_sparkle),
    Star(R.drawable.ic_star),
    Wave(R.drawable.ic_wave),
}

/**
 * 공용 아이콘 컴포저블.
 *
 * [SoodalIcons.fullColor] 아이콘(조개/진주/불꽃 등 일러스트 에셋)은 원본 색 그대로 그리며
 * [tint]의 색상은 무시하고 알파만 이어받는다 — 비활성 dim(알파 틴트) 의도를 보존하기 위함.
 */
@Composable
fun SoodalIcon(
    icon: SoodalIcons,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 20.dp,
    contentDescription: String? = null,
) {
    if (icon.fullColor) {
        Image(
            painter = painterResource(icon.resId),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            alpha = if (tint == Color.Unspecified) 1f else tint.alpha,
        )
    } else {
        Icon(
            painter = painterResource(icon.resId),
            contentDescription = contentDescription,
            modifier = modifier.size(size),
            tint = tint,
        )
    }
}
