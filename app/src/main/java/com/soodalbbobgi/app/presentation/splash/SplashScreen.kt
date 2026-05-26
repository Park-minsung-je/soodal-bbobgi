package com.soodalbbobgi.app.presentation.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.R
import com.soodalbbobgi.app.core.theme.SoodalDesign
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val colors = SoodalDesign.colors
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(100), label = "splash",
    )

    LaunchedEffect(Unit) {
        repeat(25) {
            delay(60)
            progress = (it + 1) / 25f
        }
        delay(200)
        onDone()
    }

    Box(Modifier.fillMaxSize().background(colors.bgDeep), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(R.drawable.otter_swim),
                contentDescription = "수달 뽑기",
                modifier = Modifier.size(120.dp),
            )
            Spacer(Modifier.height(28.dp))
            Text("수달 뽑기", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.accentCyan)
            Text("수영하고, 뽑고, 수달을 모아요", fontSize = 13.sp, color = colors.textSecondary)
            Spacer(Modifier.height(40.dp))
            Box(Modifier.width(160.dp).height(4.dp).clip(RoundedCornerShape(999.dp)).background(colors.surface2)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).clip(RoundedCornerShape(999.dp)).background(colors.gradCyan))
            }
        }
    }
}
