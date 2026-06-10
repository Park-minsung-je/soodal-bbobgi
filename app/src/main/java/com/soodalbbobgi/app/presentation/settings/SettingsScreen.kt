package com.soodalbbobgi.app.presentation.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.soodalbbobgi.app.BuildConfig
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.data.health.HealthConnectManager
import com.soodalbbobgi.app.core.ui.ShellRewardPopup
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.presentation.gacha.GachaResultItem
import com.soodalbbobgi.app.presentation.gacha.GachaResultOverlay

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onSignedOut: () -> Unit,
    onOpenLicenses: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    var swimReminder by remember { mutableStateOf(true) }
    var shellNotification by remember { mutableStateOf(false) }

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

    Box(Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgDeep),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                // 상태바 인셋을 스크롤되는 콘텐츠 패딩으로 — 전역 페이드 스크림과 함께
                // 콘텐츠가 상태바 밑으로 자연스럽게 사라진다.
                .statusBarsPadding()
                .padding(horizontal = spacing.s4, vertical = spacing.s4),
        ) {
            // -- Header --
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack,
                        )
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.ArrowLeft, tint = colors.textSecondary, size = 14.dp)
                    Text("돌아가기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Settings, tint = colors.textPrimary, size = 18.dp)
                    Text("설정", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                }
                Spacer(Modifier.width(80.dp))
            }

            Spacer(Modifier.height(spacing.s5))

            // -- 계정 Section --
            SectionLabel(text = "계정")
            Spacer(Modifier.height(spacing.s2))
            SoodalCard(modifier = Modifier.fillMaxWidth()) {
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
                            color = colors.textTertiary,
                        )
                    }
                    SettingsDivider()
                    SettingsRow(
                        label = "닉네임 변경",
                        trailing = null,
                        onClick = { dialog = "nickname" },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(profile?.nickname ?: "—", fontSize = 13.sp, color = colors.textTertiary)
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
            Spacer(Modifier.height(spacing.s2))
            SoodalCard(modifier = Modifier.fillMaxWidth()) {
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
                    SettingsDivider()
                    SettingsRow(
                        label = "수동 입력 모드",
                        trailing = null,
                        labelColor = colors.textTertiary,
                        onClick = {},
                    ) {
                        Text(
                            text = "준비 중",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textTertiary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(spacing.s5))

            // -- 알림 Section --
            SectionLabel(text = "알림")
            Spacer(Modifier.height(spacing.s2))
            SoodalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsToggleRow(
                        label = "수영 리마인더",
                        checked = swimReminder,
                        onCheckedChange = { swimReminder = it },
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        label = "조개 획득 알림",
                        checked = shellNotification,
                        onCheckedChange = { shellNotification = it },
                    )
                    SettingsDivider()
                    SettingsRow(
                        label = "알림 시간",
                        trailing = null,
                        onClick = {},
                    ) {
                        Text(
                            text = "21:00",
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
            Spacer(Modifier.height(spacing.s2))
            SoodalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        label = "앱 버전",
                        trailing = null,
                        onClick = {},
                    ) {
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            fontSize = 13.sp,
                            color = colors.textTertiary,
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
                Spacer(Modifier.height(spacing.s2))
                SoodalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SettingsRow(label = "조개 지급 팝업 미리보기", trailing = "→", onClick = { devPopup = "shell" })
                        SettingsDivider()
                        SettingsRow(label = "뽑기 결과 팝업 (1개)", trailing = "→", onClick = { devPopup = "gacha1" })
                        SettingsDivider()
                        SettingsRow(label = "뽑기 결과 팝업 (10연)", trailing = "→", onClick = { devPopup = "gacha10" })
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
                Text("Made with ", fontSize = 11.sp, color = colors.textTertiary)
                SoodalIcon(icon = SoodalIcons.Otter, size = 12.dp)
                Text(" · 수달 뽑기 v${BuildConfig.VERSION_NAME}", fontSize = 11.sp, color = colors.textTertiary)
            }

            Spacer(Modifier.height(spacing.s4))
        }
    }

    // -- 계정 다이얼로그 오버레이 --
    when (dialog) {
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

    // -- 개발자 팝업 미리보기 오버레이 (디버그 전용) --
    when (devPopup) {
        "shell" -> ShellRewardPopup(
            shellCount = 3,
            distanceM = 1250,
            durationMin = 42,
            onEditStrokes = { devPopup = null },
            onDismiss = { devPopup = null },
        )
        "gacha1" -> GachaResultOverlay(results = devGachaResults(1), onClose = { devPopup = null })
        "gacha10" -> GachaResultOverlay(results = devGachaResults(10), onClose = { devPopup = null })
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
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textSecondary,
        letterSpacing = 0.5.sp,
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            )
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
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
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(colors.textPrimary),
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
            .background(colors.glassBorder),
    )
}
