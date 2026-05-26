package com.soodalbbobgi.app.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.soodalbbobgi.app.R

enum class SoodalIcons(@DrawableRes val resId: Int) {
    Home(R.drawable.ic_home),
    Calendar(R.drawable.ic_calendar),
    Gacha(R.drawable.ic_gacha),
    Shop(R.drawable.ic_shop),
    Shell(R.drawable.ic_shell),
    Pearl(R.drawable.ic_pearl),
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
    Frame(R.drawable.ic_frame),
    Gift(R.drawable.ic_gift),
    Heart(R.drawable.ic_heart),
    Moon(R.drawable.ic_moon),
    Pin(R.drawable.ic_pin),
    Plus(R.drawable.ic_plus),
    Reset(R.drawable.ic_reset),
    Ruler(R.drawable.ic_ruler),
    Sparkle(R.drawable.ic_sparkle),
    Star(R.drawable.ic_star),
    Wave(R.drawable.ic_wave),
}

@Composable
fun SoodalIcon(
    icon: SoodalIcons,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    size: Dp = 20.dp,
    contentDescription: String? = null,
) {
    Icon(
        painter = painterResource(icon.resId),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        tint = tint,
    )
}
