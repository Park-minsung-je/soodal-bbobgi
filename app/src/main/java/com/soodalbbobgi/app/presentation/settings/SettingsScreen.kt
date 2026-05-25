package com.soodalbbobgi.app.presentation.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.SoodalCard

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    var swimReminder by remember { mutableStateOf(true) }
    var shellNotification by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bgDeep),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.s4, vertical = spacing.s4),
        ) {
            // -- Header --
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.s3),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.surface2)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "←", fontSize = 16.sp, color = colors.textPrimary)
                }
                Text(
                    text = "⚙️ 설정",
                    style = SoodalDesign.typography.lg,
                    color = colors.textPrimary,
                )
            }

            Spacer(Modifier.height(spacing.s5))

            // -- 계정 Section --
            SectionLabel(text = "계정")
            Spacer(Modifier.height(spacing.s2))
            SoodalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        label = "닉네임 변경",
                        trailing = "→",
                        onClick = {},
                    )
                    SettingsDivider()
                    SettingsRow(
                        label = "데이터 초기화",
                        trailing = "→",
                        labelColor = colors.warn,
                        onClick = {},
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
                        onClick = {},
                    ) {
                        Text(
                            text = "연결됨",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.success,
                        )
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
                            color = colors.accentCyan,
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
                            text = "1.0.0",
                            fontSize = 13.sp,
                            color = colors.textTertiary,
                        )
                    }
                    SettingsDivider()
                    SettingsRow(label = "이용약관", trailing = "→", onClick = {})
                    SettingsDivider()
                    SettingsRow(label = "개인정보처리방침", trailing = "→", onClick = {})
                    SettingsDivider()
                    SettingsRow(label = "오픈소스 라이선스", trailing = "→", onClick = {})
                }
            }

            Spacer(Modifier.height(spacing.s6))

            // -- Footer --
            Text(
                text = "Made with 🦦 · 수달 뽑기 v1.0.0",
                fontSize = 11.sp,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(spacing.s4))
        }
    }
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
            Text(
                text = trailing,
                fontSize = 14.sp,
                color = colors.textTertiary,
            )
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
    val trackColor = if (checked) colors.accentCyan else colors.surface3
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
