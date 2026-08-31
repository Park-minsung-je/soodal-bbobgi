package kr.ilf.soodalbbobgi.presentation.settings

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ilf.soodalbbobgi.BuildConfig
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.data.health.HealthConnectManager
import kr.ilf.soodalbbobgi.core.ui.pressable
import kr.ilf.soodalbbobgi.core.ui.ShellRewardKind
import kr.ilf.soodalbbobgi.core.ui.ShellRewardPopup
import kr.ilf.soodalbbobgi.core.ui.soodalScreenBackdrop
import kr.ilf.soodalbbobgi.core.ui.topFadeEdge
import kr.ilf.soodalbbobgi.core.ui.AppOverlay
import kr.ilf.soodalbbobgi.core.ui.SoodalCard
import kr.ilf.soodalbbobgi.core.ui.BackLink
import kr.ilf.soodalbbobgi.core.ui.SoodalIcon
import kr.ilf.soodalbbobgi.core.ui.SoodalIcons
import kr.ilf.soodalbbobgi.domain.model.Grade
import kr.ilf.soodalbbobgi.presentation.gacha.GachaResultItem
import kr.ilf.soodalbbobgi.presentation.gacha.GachaResultOverlay
import kotlin.math.roundToInt

/** 설정 헤더가 스크롤에 따라 위로 올라갔다 멈추는 최대 거리. */
private val SettingsHeaderShift = 16.dp

/** Health Connect 백그라운드 읽기 권한 — 새 기록 알림 워커용. */
private const val HC_BG_READ_PERMISSION = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenLicenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    // 디버그 전용 개발자 모드 — 미리보기로 띄울 팝업 ("shell" | "gacha1" | "gacha10")
    var devPopup by remember { mutableStateOf<String?>(null) }

    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val hcConnected by viewModel.hcConnected.collectAsStateWithLifecycle()
    val nicknameState by viewModel.nicknameState.collectAsStateWithLifecycle()
    val accountAction by viewModel.accountAction.collectAsStateWithLifecycle()
    val signedOut by viewModel.signedOut.collectAsStateWithLifecycle()

    // 열려 있는 계정 다이얼로그: "nickname" | "logout" | "delete" | null
    var dialog by remember { mutableStateOf<String?>(null) }

    // 로그아웃/탈퇴 완료 → Auth로
    LaunchedEffect(signedOut) { if (signedOut) onSignedOut() }


    // 닉네임 저장 성공 → 다이얼로그 닫기
    LaunchedEffect(nicknameState) {
        if (nicknameState is NicknameSaveState.Success) {
            dialog = null
            viewModel.resetNicknameState()
        }
    }

    // Health Connect 권한 요청 런처 — 온보딩과 같은 권한 셋
    val hcPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        if (granted.isNotEmpty()) viewModel.onHcPermissionGranted() else viewModel.refreshHcStatus()
    }

    // ── 알림 설정 상태 + 권한 플로우 ──
    val context = androidx.compose.ui.platform.LocalContext.current
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    val reminderTime by viewModel.reminderTime.collectAsStateWithLifecycle()
    val newRecordEnabled by viewModel.newRecordEnabled.collectAsStateWithLifecycle()

    // 개발자 초기화 결과 — 서버까지 지워졌는지 눈으로 확인해야 다음 단계로 넘어갈 수 있다.
    val devResetResult by viewModel.devResetResult.collectAsStateWithLifecycle()
    LaunchedEffect(devResetResult) {
        devResetResult?.let {
            android.widget.Toast.makeText(
                context,
                "$it · 캘린더에서 동기화하세요",
                android.widget.Toast.LENGTH_LONG,
            ).show()
            viewModel.clearDevResetResult()
        }
    }

    // HC 백그라운드 읽기 권한 — 새 기록 알림용. 거부돼도 토글은 유지 (워커가 조용히 스킵).
    val hcBgPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { }

    // POST_NOTIFICATIONS(13+) 허용 후 켜려던 토글을 마저 켠다
    var pendingNotifToggle by remember { mutableStateOf<String?>(null) }
    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val target = pendingNotifToggle
        pendingNotifToggle = null
        if (granted) {
            when (target) {
                "reminder" -> viewModel.setReminderEnabled(true)
                "newRecord" -> {
                    viewModel.setNewRecordEnabled(true)
                    hcBgPermissionLauncher.launch(setOf(HC_BG_READ_PERMISSION))
                }
            }
        }
    }

    // 토글 켜기 — 알림 권한이 없으면 먼저 요청하고, 허용되면 마저 켠다
    val enableNotifToggle: (String) -> Unit = { target ->
        val needsPermission = android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS,
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingNotifToggle = target
            notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            when (target) {
                "reminder" -> viewModel.setReminderEnabled(true)
                "newRecord" -> {
                    viewModel.setNewRecordEnabled(true)
                    hcBgPermissionLauncher.launch(setOf(HC_BG_READ_PERMISSION))
                }
            }
        }
    }

    val scrollState = rememberScrollState()
    val maxHeaderShiftPx = with(LocalDensity.current) { SettingsHeaderShift.toPx() }

    // 헤더가 접힌 정도(0 = 펼침, -maxHeaderShiftPx = 다 접힘).
    // layout 단계에서만 읽어 스크롤 중 리컴포지션이 일어나지 않게 한다.
    val headerShift = remember { mutableFloatStateOf(0f) }
    // 목록보다 헤더가 먼저 스크롤을 먹는다 — 헤더가 다 접히기 전엔 목록이 움직이지 않는다.
    val headerCollapse = remember(maxHeaderShiftPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val before = headerShift.floatValue
                val after = (before + available.y).coerceIn(-maxHeaderShiftPx, 0f)
                headerShift.floatValue = after
                return Offset(0f, after - before)
            }
        }
    }

    Box(Modifier.fillMaxSize().soodalScreenBackdrop()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .nestedScroll(headerCollapse),
    ) {
            // -- Header (고정) --
            // 스크롤을 시작하면 [SettingsHeaderShift]만큼만 위로 올라간 뒤 그 자리에 멈춘다.
            // 목록을 끝까지 내려도 돌아가기 버튼이 계속 보여야 하기 때문.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val shift = (-headerShift.floatValue).roundToInt()
                        layout(placeable.width, placeable.height - shift) {
                            placeable.place(0, -shift)
                        }
                    }
                    .padding(horizontal = spacing.s4)
                    .padding(top = spacing.s4 + 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                BackLink(onBack)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Settings, tint = colors.textPrimary, size = 18.dp)
                    Text("설정", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                }
                Spacer(Modifier.width(80.dp))
            }

        // -- 목록 (헤더 아래에서만 스크롤) --
        Column(
            modifier = Modifier
                .weight(1f)
                // 헤더 경계에서 항목이 뚝 잘리지 않게 알파 마스크로 사라지게 한다.
                .topFadeEdge()
                .verticalScroll(scrollState)
                .padding(horizontal = spacing.s4)
                .padding(bottom = spacing.s4),
        ) {
            Spacer(Modifier.height(spacing.s5))

            // -- 계정 Section --
            SectionLabel(text = "계정")
            Spacer(Modifier.height(12.dp))
            SoodalCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 로그인 정보 (읽기 전용)
                    SettingsRow(
                        label = "로그인 정보",
                        trailing = null,
                        onClick = {},
                    ) {
                        Text(
                            text = when (profile?.authProvider) {
                                "google" -> "Google 계정"
                                "kakao" -> "Kakao 계정"
                                else -> "—"
                            },
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                        )
                    }
                    SettingsDivider()
                    SettingsRow(
                        label = "닉네임 변경",
                        trailing = null,
                        onClick = { dialog = "nickname" },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile?.nickname ?: "—", fontSize = 13.sp, color = colors.textSecondary)
                            Spacer(Modifier.width(4.dp))
                            Text("›", fontSize = 14.sp, color = colors.textTertiary)
                        }
                    }
                    SettingsDivider()
                    SettingsRow(
                        label = "로그아웃",
                        trailing = "→",
                        onClick = { dialog = "logout" },
                    )
                    SettingsDivider()
                    SettingsRow(
                        label = "계정 탈퇴",
                        trailing = "→",
                        labelColor = colors.warn,
                        onClick = { dialog = "delete" },
                    )
                }
            }

            Spacer(Modifier.height(spacing.s5))

            // -- 연동 Section --
            SectionLabel(text = "연동")
            Spacer(Modifier.height(12.dp))
            SoodalCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        label = "Health Connect",
                        trailing = null,
                        onClick = {
                            if (hcConnected == false) {
                                hcPermissionLauncher.launch(HealthConnectManager.requestPermissions)
                            }
                        },
                    ) {
                        when (hcConnected) {
                            true -> Text("연결됨", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.success)
                            false -> Text("연결 안 됨 · 탭하여 연결", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.warn)
                            null -> Text("확인 중…", fontSize = 12.sp, color = colors.textTertiary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(spacing.s5))

            // -- 알림 Section --
            SectionLabel(text = "알림")
            Spacer(Modifier.height(12.dp))
            SoodalCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsToggleRow(
                        label = "수영 리마인더",
                        checked = reminderEnabled,
                        onCheckedChange = { on ->
                            if (on) enableNotifToggle("reminder") else viewModel.setReminderEnabled(false)
                        },
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        label = "조개 획득 알림",
                        checked = newRecordEnabled,
                        onCheckedChange = { on ->
                            if (on) enableNotifToggle("newRecord") else viewModel.setNewRecordEnabled(false)
                        },
                    )
                    SettingsDivider()
                    SettingsRow(
                        label = "알림 시간",
                        trailing = null,
                        onClick = { dialog = "time" },
                    ) {
                        Text(
                            text = String.format("%02d:%02d", reminderTime.first, reminderTime.second),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.accentBlue,
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.s5))

            // -- 정보 Section --
            SectionLabel(text = "정보")
            Spacer(Modifier.height(12.dp))
            SoodalCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        label = "앱 버전",
                        trailing = null,
                        onClick = {},
                    ) {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            fontSize = 13.sp,
                            color = colors.textSecondary,
                        )
                    }
                    SettingsDivider()
                    SettingsRow(label = "이용약관", trailing = "→", onClick = {})
                    SettingsDivider()
                    SettingsRow(label = "개인정보처리방침", trailing = "→", onClick = {})
                    SettingsDivider()
                    SettingsRow(label = "오픈소스 라이선스", trailing = "→", onClick = onOpenLicenses)
                }
            }

            // -- 개발자 Section (디버그 빌드 전용) --
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(spacing.s5))
                SectionLabel(text = "개발자 (디버그 전용)")
                Spacer(Modifier.height(12.dp))
                SoodalCard(modifier = Modifier.fillMaxWidth(), contentPadding = 0.dp) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(label = "조개 지급 팝업 (영법 유도)", trailing = "→", onClick = { devPopup = "shell" })
                        SettingsDivider()
                        SettingsRow(label = "조개 지급 팝업 (유도 없음)", trailing = "→", onClick = { devPopup = "shellPlain" })
                        SettingsDivider()
                        SettingsRow(label = "영법 보너스 팝업", trailing = "→", onClick = { devPopup = "strokeBonus" })
                        SettingsDivider()
                        SettingsRow(label = "뽑기 결과 팝업 (1개)", trailing = "→", onClick = { devPopup = "gacha1" })
                        SettingsDivider()
                        SettingsRow(label = "뽑기 결과 팝업 (10연)", trailing = "→", onClick = { devPopup = "gacha10" })
                        SettingsDivider()
                        SettingsRow(label = "리마인더 알림 보내기", trailing = "🔔", onClick = { viewModel.sendTestReminder() })
                        SettingsDivider()
                        SettingsRow(label = "새 기록 알림 보내기", trailing = "🔔", onClick = { viewModel.sendTestNewRecord() })
                        SettingsDivider()
                        // 지운 기록은 블랙리스트에 남아 HC에서 다시 안 들어온다 — 보상 흐름을
                        // 처음부터 다시 보려면 이걸 눌러 잊게 한 뒤 캘린더에서 동기화한다.
                        SettingsRow(
                            label = "오늘 기록 초기화 (서버 + 앱)",
                            trailing = "↺",
                            onClick = { viewModel.resetSyncState() },
                        )
                        SettingsDivider()
                        // 심박·활동시간은 서버에 없어 로컬을 잃으면 HC에서만 되살릴 수 있다.
                        SettingsRow(
                            label = "심박 복구 — HC 전체 기간 다시 읽기",
                            trailing = "♡",
                            onClick = { viewModel.resyncFromHealthConnect() },
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.s6))

            // -- Footer --
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Made with Claude Code, Antigravity · 수달 뽑기 v${BuildConfig.VERSION_NAME}",
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                )
            }

            Spacer(Modifier.height(spacing.s4))
        }
    }

    // -- 계정 다이얼로그 오버레이 (오버레이 레이어로 호이스팅 — 패널이 뒤 콘텐츠를 진짜 블러) --
    if (dialog != null) {
        AppOverlay {
    when (dialog) {
        "time" -> ReminderTimeDialog(
            initialHour = reminderTime.first,
            initialMinute = reminderTime.second,
            onSave = { h, m ->
                viewModel.setReminderTime(h, m)
                dialog = null
            },
            onDismiss = { dialog = null },
        )
        "nickname" -> NicknameEditDialog(
            initial = profile?.nickname ?: "",
            state = nicknameState,
            onSave = { viewModel.saveNickname(it) },
            onDismiss = { dialog = null; viewModel.resetNicknameState() },
        )
        "logout" -> ConfirmActionDialog(
            title = "로그아웃",
            message = "로그아웃하면 이 기기의 수영 기록이 지워져요. 다시 로그인하면 Health Connect에서 다시 가져올 수 있어요.",
            confirmText = "로그아웃",
            working = accountAction is AccountActionState.Working,
            errorMessage = (accountAction as? AccountActionState.Error)?.message,
            onConfirm = { viewModel.logout() },
            onDismiss = { dialog = null; viewModel.resetAccountAction() },
        )
        "delete" -> ConfirmActionDialog(
            title = "계정 탈퇴",
            message = "계정과 모든 데이터(수영 기록, 수달, 재화)가 영구 삭제되며 되돌릴 수 없어요. 정말 탈퇴할까요?",
            confirmText = "탈퇴하기",
            working = accountAction is AccountActionState.Working,
            errorMessage = (accountAction as? AccountActionState.Error)?.message,
            onConfirm = { viewModel.deleteAccount() },
            onDismiss = { dialog = null; viewModel.resetAccountAction() },
        )
    }
        }
    }

    // -- 개발자 팝업 미리보기 오버레이 (디버그 전용, 오버레이 레이어) --
    if (devPopup != null) {
        AppOverlay {
    when (devPopup) {
        // 유도 있음 — 자동으로 닫히지 않고 두 버튼이 뜬다 (오늘 영법이 비어 있는 경우).
        "shell" -> ShellRewardPopup(
            shellCount = 3,
            distanceM = 1250,
            durationMin = 42,
            onEditStrokes = { devPopup = null },
            onDismiss = { devPopup = null },
        )
        // 유도 없음 — 2.6초 뒤 자동으로 닫힌다 (영법을 이미 채운 경우).
        "shellPlain" -> ShellRewardPopup(
            shellCount = 2,
            distanceM = 1250,
            durationMin = 42,
            onDismiss = { devPopup = null },
        )
        "strokeBonus" -> ShellRewardPopup(
            shellCount = 1,
            kind = ShellRewardKind.StrokeBonus,
            onDismiss = { devPopup = null },
        )
        "gacha1" -> GachaResultOverlay(results = devGachaResults(1), onClose = { devPopup = null })
        "gacha10" -> GachaResultOverlay(results = devGachaResults(10), onClose = { devPopup = null })
    }
        }
    }
    }
}

/** 개발자 모드 미리보기용 샘플 뽑기 결과 — 등급/신규/중복(진주) 케이스를 섞어 돌려준다. */
private fun devGachaResults(count: Int): List<GachaResultItem> {
    val samples = listOf(
        GachaResultItem("황금 수달", Grade.SSR, "char", isNew = true, pearlsEarned = 0),
        GachaResultItem("오로라", Grade.SR, "bg", isNew = true, pearlsEarned = 0),
        GachaResultItem("바다 수달", Grade.R, "char", isNew = false, pearlsEarned = 3),
        GachaResultItem("시안 라인", Grade.R, "frame", isNew = true, pearlsEarned = 0),
        GachaResultItem("수달이", Grade.N, "char", isNew = false, pearlsEarned = 1),
    )
    return List(count) { samples[it % samples.size] }
}

@Composable
private fun SectionLabel(text: String) {
    val colors = SoodalDesign.colors
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textSecondary,
        letterSpacing = 0.7.sp,
    )
}

@Composable
private fun SettingsRow(
    label: String,
    trailing: String?,
    onClick: () -> Unit,
    labelColor: androidx.compose.ui.graphics.Color = SoodalDesign.colors.textPrimary,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    val colors = SoodalDesign.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = labelColor,
        )
        if (trailingContent != null) {
            trailingContent()
        } else if (trailing != null) {
            SoodalIcon(icon = SoodalIcons.ArrowRight, tint = colors.textTertiary, size = 14.dp)
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = SoodalDesign.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = { onCheckedChange(!checked) })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
        )
        ToggleSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = SoodalDesign.colors
    val trackColor = if (checked) colors.accentBlue else colors.surface3
    // ON 오프셋 = 트랙(44) − 썸(20) − 좌우 여백(2) = 22 → 켜짐/꺼짐 여백이 좌우 대칭.
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
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
                .offset(x = thumbOffset)
                .size(20.dp)
                .align(Alignment.CenterStart)
                .shadow(2.dp, CircleShape) // 흰 썸이 꺼짐 상태의 밝은 트랙과도 구분되도록
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun SettingsDivider() {
    val colors = SoodalDesign.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            // 밝은 흰 보더(glassBorder) 대신 은은한 잉크 구분선 (디자인 대조).
            .background(colors.textTertiary.copy(alpha = 0.20f)),
    )
}
