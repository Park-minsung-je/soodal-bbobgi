package com.soodalbbobgi.app.presentation.profile

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
import com.soodalbbobgi.app.core.ui.SoodalIcon
import com.soodalbbobgi.app.core.ui.SoodalIcons

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
                        text = "수영을 사랑하는 수달",
                        fontSize = 14.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(spacing.s3))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "누적 12,540m · 89회 · 38",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accentCyan,
                            letterSpacing = 0.4.sp,
                        )
                        SoodalIcon(icon = SoodalIcons.Shell, tint = colors.accentGold, size = 12.dp)
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
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(SoodalShape.md)
                        .background(colors.glassBg, SoodalShape.md)
                        .border(1.dp, colors.glassBorder, SoodalShape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Save, tint = colors.textPrimary, size = 14.dp)
                    Text("저장", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
                Row(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(SoodalShape.md)
                        .background(colors.glassBg, SoodalShape.md)
                        .border(1.dp, colors.glassBorder, SoodalShape.md)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                        )
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    SoodalIcon(icon = SoodalIcons.Share, tint = colors.textPrimary, size = 14.dp)
                    Text("공유", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                }
            }
            Row(
                modifier = Modifier
                    .height(36.dp)
                    .clip(SoodalShape.md)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack,
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SoodalIcon(icon = SoodalIcons.ArrowLeft, tint = colors.textSecondary, size = 14.dp)
                Text("돌아가기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
            }
        }
    }
}

