package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign
import kr.ilf.soodalbbobgi.core.theme.SoodalShape

/**
 * 한 줄 텍스트 입력 필드 — 앱 공통 룩(입력 배경·테두리·플레이스홀더).
 *
 * @param enabled false면 입력을 막고 글자색을 흐리게 한다 (닉네임 쿨다운 잠금 등)
 */
@Composable
fun SoodalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    maxLength: Int = Int.MAX_VALUE,
    enabled: Boolean = true,
) {
    val colors = SoodalDesign.colors
    BasicTextField(
        value = value,
        onValueChange = { if (it.length <= maxLength) onValueChange(it) },
        enabled = enabled,
        textStyle = TextStyle(
            color = if (enabled) colors.textPrimary else colors.textTertiary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
        ),
        cursorBrush = SolidColor(colors.accentBlue),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(SoodalShape.md)
                    .background(colors.inputBg)
                    .border(1.dp, colors.inputBorder, SoodalShape.md)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = if (enabled) colors.inputPlaceholder else colors.textTertiary,
                        fontSize = 15.sp,
                    )
                }
                innerTextField()
            }
        },
    )
}
