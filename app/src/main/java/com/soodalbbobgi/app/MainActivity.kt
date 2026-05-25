package com.soodalbbobgi.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soodalbbobgi.app.core.theme.SoodalDesign
import com.soodalbbobgi.app.core.theme.SoodalTheme
import com.soodalbbobgi.app.core.theme.SoodalThemeType
import com.soodalbbobgi.app.core.ui.ButtonStyle
import com.soodalbbobgi.app.core.ui.ChipColor
import com.soodalbbobgi.app.core.ui.GradeBadge
import com.soodalbbobgi.app.core.ui.SoodalButton
import com.soodalbbobgi.app.core.ui.SoodalCard
import com.soodalbbobgi.app.core.ui.SoodalChip
import com.soodalbbobgi.app.core.ui.SoodalTextField
import com.soodalbbobgi.app.domain.model.Grade
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var theme by remember { mutableStateOf(SoodalThemeType.Light) }

            SoodalTheme(theme = theme) {
                val colors = SoodalDesign.colors
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.bgDeep)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "수달 뽑기 — ${theme.name}",
                        style = SoodalDesign.typography.lg,
                        color = colors.textPrimary,
                    )

                    SoodalButton(
                        "테마 전환 →",
                        onClick = { theme = SoodalThemeType.entries[(theme.ordinal + 1) % 4] },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SoodalButton("Gold", onClick = {}, style = ButtonStyle.Gold)
                    SoodalButton("Purple", onClick = {}, style = ButtonStyle.Purple)

                    SoodalCard {
                        Column {
                            Text("Card Component", color = colors.textPrimary)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                GradeBadge(Grade.SSR)
                                GradeBadge(Grade.SR)
                                GradeBadge(Grade.R)
                                GradeBadge(Grade.N)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SoodalChip("조개 34", color = ChipColor.Gold, icon = "🐚")
                        SoodalChip("진주 12", color = ChipColor.Purple, icon = "🔮")
                    }

                    var text by remember { mutableStateOf("") }
                    SoodalTextField(
                        text, { text = it },
                        placeholder = "닉네임 입력",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
