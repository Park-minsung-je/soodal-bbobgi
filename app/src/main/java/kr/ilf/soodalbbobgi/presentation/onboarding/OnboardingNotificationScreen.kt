package kr.ilf.soodalbbobgi.presentation.onboarding

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.offset
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
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
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.SoodalButton
import kr.ilf.soodalbbobgi.core.ui.SoodalCard
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.core.ui.soodalScreenBackdrop
import kr.ilf.soodalbbobgi.presentation.settings.ReminderTimeDialog

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
    val newRecordEnabled by viewModel.newRecordEnabled.collectAsStateWithLifecycle()
    var showTimeDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // 수영 기록 알림은 HC 연동이 전제 — 연동 전이면 토글을 잠근다.
    var hcConnected by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hcConnected = viewModel.isHcConnected() }
    // 알림 권한 요청 후 어느 토글을 켜려던 것인지 기억한다.
    var pendingToggle by remember { mutableStateOf<String?>(null) }

    // 새 기록 알림용 HC 백그라운드 읽기 권한 — 거부돼도 토글은 유지(워커가 조용히 스킵).
    val hcBgPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { }

    // 실제로 토글을 켜는 처리 — 새 기록이면 HC 백그라운드 권한도 이어서 요청한다.
    val enableToggle: (String?) -> Unit = { target ->
        when (target) {
            "reminder" -> viewModel.setReminderEnabled(true)
            "newRecord" -> {
                viewModel.setNewRecordEnabled(true)
                // 이미 허용돼 있으면 요청 화면을 띄우지 않는다 — 매번 띄우면 HC 액티비티가
                // 순간 나타났다 사라지며 상단바가 깜빡인다.
                scope.launch {
                    if (!viewModel.isBgReadGranted()) hcBgPermissionLauncher.launch(setOf(HC_BG_READ_PERMISSION))
                }
            }
        }
    }

    // 토글 ON 시 알림 권한(13+) 요청 — 허용되면 대상 토글을 마저 켠다.
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val target = pendingToggle
        pendingToggle = null
        if (granted) enableToggle(target)
    }

    // 토글을 켜려 할 때 — 알림 권한이 없으면 먼저 요청하고, 있으면 바로 켠다.
    fun requestToggle(target: String) {
        val needsPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingToggle = target
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            enableToggle(target)
        }
    }

    // 자체 배경 필수 — 투명이면 슬라이드 전환 중 이전 화면과 겹쳐 보인다 (설정 화면과 동일 패턴).
    Column(Modifier.fillMaxSize().soodalScreenBackdrop().statusBarsPadding().padding(24.dp)) {
        Text("STEP 3 / 3", fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = colors.accentBlue, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(16.dp))
        Text("수영, 잊지 않게\n알려드릴까요?", style = SoodalDesign.typography.xl, color = colors.textPrimary)
        Text(
            "원하는 알림만 골라 켜 주세요. 알림을 받으려면 Android 알림 권한 동의가 필요해요.",
            fontSize = 14.sp, color = colors.textSecondary, lineHeight = 22.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
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
                        Text(
                            "정해둔 시간에 아직 수영 전이면 잊지 않게 알려드려요.\n" +
                                "Android 알림 권한 동의가 필요해요.",
                            fontSize = 12.sp, color = colors.textSecondary, lineHeight = 17.sp,
                        )
                    }
                    ToggleSwitch(
                        checked = reminderEnabled,
                        onCheckedChange = { on -> if (on) requestToggle("reminder") else viewModel.setReminderEnabled(false) },
                    )
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

        Spacer(Modifier.height(12.dp))

        // 수영 기록 알림 — 백그라운드로 HC 변경을 확인해 알린다. HC 연동 전에는
        // 토글만이 아니라 카드 전체를 흐려 "지금은 못 쓰는 항목"임을 보이게 한다.
        // alpha 모디파이어는 오프스크린 합성을 강제해 경계 밖 그림자가 사각형으로 비친다 —
        // ModulateAlpha는 레이어 없이 드로우별 알파만 낮춰 라운드·그림자가 온전히 남는다.
        SoodalCard(
            Modifier.fillMaxWidth().graphicsLayer {
                alpha = if (hcConnected) 1f else 0.45f
                compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("수영 기록 알림", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (hcConnected) {
                            "조개를 받을 수 있는 수영 기록이 확인되면 알려드려요.\n" +
                                "Health Connect 백그라운드 읽기 권한 동의가 필요해요."
                        } else {
                            "Health Connect 연동 후 사용할 수 있어요"
                        },
                        fontSize = 12.sp, color = colors.textSecondary, lineHeight = 17.sp,
                    )
                }
                ToggleSwitch(
                    checked = newRecordEnabled,
                    enabled = hcConnected,
                    onCheckedChange = { on -> if (on) requestToggle("newRecord") else viewModel.setNewRecordEnabled(false) },
                )
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
private fun ToggleSwitch(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    val colors = SoodalDesign.colors
    val trackColor = if (checked) colors.accentBlue else colors.surface3
    // ON 오프셋 = 트랙(44) − 썸(20) − 좌우 여백(2) = 22 → 켜짐/꺼짐 여백이 좌우 대칭 (설정과 동일).
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(200),
        label = "thumb",
    )
    Box(
        modifier = Modifier
            .alpha(if (enabled) 1f else 0.45f)
            .width(44.dp)
            .height(24.dp)
            .clip(CircleShape)
            .background(trackColor)
            .then(if (enabled) Modifier.pressable(onClick = { onCheckedChange(!checked) }) else Modifier),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .align(Alignment.CenterStart)
                .shadow(2.dp, CircleShape) // 흰 썸이 꺼짐 상태의 밝은 트랙과도 구분되도록
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

private const val HC_BG_READ_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
