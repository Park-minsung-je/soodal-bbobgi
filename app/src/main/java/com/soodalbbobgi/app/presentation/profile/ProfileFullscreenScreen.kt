package com.soodalbbobgi.app.presentation.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalShape
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalCard

@Composable
fun ProfileFullscreenScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    val colors = SoodalDesign.colors
    val spacing = SoodalDesign.spacing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // -- Center Card --
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 80.dp),
            contentAlignment = Alignment.Center,
        ) {
            SoodalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEdit,
                    ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(SoodalShape.sm)
                                .background(colors.accentCyanSoft)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = "SOODAL.CARD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accentCyan,
                                letterSpacing = 1.sp,
                            )
                        }
                        Text(
                            text = "002",
                            style = SoodalDesign.typography.mono,
                            color = colors.textTertiary,
                        )
                    }

                    Spacer(Modifier.height(spacing.s5))

                    // Character placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(SoodalShape.md)
                            .background(colors.surface2),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "🦦", fontSize = 64.sp)
                    }

                    Spacer(Modifier.height(spacing.s4))

                    // Nickname
                    Text(
                        text = "Soodal",
                        style = SoodalDesign.typography.xl,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(spacing.s2))

                    Text(
                        text = "수달 마스터",
                        style = SoodalDesign.typography.body,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(spacing.s4))

                    // Stats row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ProfileStat(label = "수영", value = "18회")
                        ProfileStat(label = "거리", value = "12.5km")
                        ProfileStat(label = "수집", value = "7/24")
                    }
                }
            }
        }

        // -- Bottom Controls --
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = spacing.s4, vertical = spacing.s5),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
                SoodalButton(
                    text = "💾 저장",
                    onClick = {},
                    style = ButtonStyle.Secondary,
                )
                SoodalButton(
                    text = "↗ 공유",
                    onClick = {},
                    style = ButtonStyle.Secondary,
                )
            }
            SoodalButton(
                text = "← 돌아가기",
                onClick = onBack,
                style = ButtonStyle.Ghost,
            )
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    val colors = SoodalDesign.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colors.accentCyan,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textTertiary,
        )
    }
}
