package com.soodalbbobgi.app.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soodalbbobgi.app.core.theme.SoodalDesign

/**
 * 서브 화면 공통 뒤로가기 링크 — 화살표 + "돌아가기" 텍스트.
 *
 * 유리 버튼 같은 장식을 두지 않는다. 화면마다 한 번뿐인 보조 동작이라
 * 눌리는 면적을 그려 강조할 이유가 없고, 제목보다 앞서 보이면 안 되기 때문이다.
 *
 * @param onBack 탭 시 동작
 */
@Composable
fun BackLink(onBack: () -> Unit) {
    val colors = SoodalDesign.colors
    Row(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onBack,
            )
            // 글자만 있는 링크라 탭 영역을 여백으로 넓혀 준다.
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SoodalIcon(icon = SoodalIcons.ArrowLeft, tint = colors.textSecondary, size = 14.dp)
        Text("돌아가기", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
    }
}
