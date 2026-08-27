package com.soodalbbobgi.app.presentation.onboarding

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.ui.pressable
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.core.ui.soodalScreenBackdrop
import com.soodalbbobgi.app.presentation.settings.ReminderTimeDialog

/**
 * 온보딩 3단계 — 수영 리마인더 알림 설정.
 *
 * 정한 시간에 그날 수영을 안 했으면 알려주는 리마인더를 켜고 시각을 고를 수 있다.
 * 토글을 켜면 알림 권한(13+)을 요청하고, 허용돼야 실제로 켜진다.
 *
 * @param onDone "시작하기" — 홈으로 이동
 */
@Composable
fun OnboardingNotificationScreen(
    onDone: () -> Unit,
    viewModel: OnboardingNotificationViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val context = LocalContext.current
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle()
    var showTimeDialog by remember { mutableStateOf(false) }

    // 토글 ON 시 알림 권한(13+) 요청 — 허용되면 켜고, 거부되면 끈 상태 유지
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.setReminderEnabled(true) }

    fun toggleReminder(on: Boolean) {
        if (!on) {
            viewModel.setReminderEnabled(false)
            return
        }
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.setReminderEnabled(true)
        }
    }

    // 자체 배경 필수 — 투명이면 슬라이드 전환 중 이전 화면과 겹쳐 보인다 (설정 화면과 동일 패턴).
    Column(Modifier.fillMaxSize().soodalScreenBackdrop().statusBarsPadding().padding(24.dp)) {
        Text("STEP 3 / 3", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = colors.accentBlue, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(16.dp))
        Text("수영, 잊지 않게\n알려드릴까요?", style = SoodalDesign.typography.xl, color = colors.textPrimary)
        Text("정한 시간에 그날 아직 수영 전이면 살짝 알려드려요. 이미 수영했다면 알림은 오지 않아요.",
            fontSize = 14.sp, color = colors.textSecondary, lineHeight = 22.sp,
            modifier = Modifier.padding(top = 12.dp))
        Spacer(Modifier.height(36.dp))
        SoodalIcon(SoodalIcons.Calendar, tint = colors.accentBlue, size = 64.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(30.dp))

        SoodalCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("수영 리마인더", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("매일 정한 시간에 알림", fontSize = 12.sp, color = colors.textSecondary)
                    }
                    ToggleSwitch(checked = reminderEnabled, onCheckedChange = ::toggleReminder)
                }
                AnimatedVisibility(visible = reminderEnabled) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.glassBorder))
                        Spacer(Modifier.height(14.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .pressable(onClick = { showTimeDialog = true }),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("알림 시간", fontSize = 14.sp, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Text(
                                String.format("%02d:%02d", reminderTime.first, reminderTime.second),
                                fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.accentBlue,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        SoodalButton("시작하기", onClick = onDone, modifier = Modifier.fillMaxWidth())
    }

    if (showTimeDialog) {
        ReminderTimeDialog(
            initialHour = reminderTime.first,
            initialMinute = reminderTime.second,
            onSave = { h, m -> viewModel.setReminderTime(h, m); showTimeDialog = false },
            onDismiss = { showTimeDialog = false },
        )
    }
}

/** 온보딩 리마인더 토글 — 트랙 + 흰 썸. 설정 화면 토글과 같은 모양. */
@Composable
private fun ToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = SoodalDesign.colors
    val trackColor = if (checked) colors.accentBlue else colors.surface3
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 2.dp,
        animationSpec = tween(200),
        label = "thumb",
    )
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .pressable(onClick = { onCheckedChange(!checked) }),
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(20.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}
