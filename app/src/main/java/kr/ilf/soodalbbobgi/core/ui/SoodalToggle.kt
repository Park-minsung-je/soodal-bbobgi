package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign

/**
 * 앱 공용 토글 — 켜짐 트랙은 편집 시트 저장 버튼과 같은 진한 하늘색 그라데이션, 꺼짐은 옅은 표면색.
 *
 * 설정·온보딩·프로필 편집 시트가 모두 이 하나를 써서 크기(44×24)·색·썸 이동이 어디서나 같다.
 * 비활성이면 흐리게 보이고 탭을 받지 않는다.
 *
 * @param checked 켜짐 여부
 * @param onCheckedChange 탭 시 바뀔 값으로 호출
 * @param modifier 배치용 Modifier
 * @param enabled false면 흐리게 표시하고 탭을 무시한다
 */
@Composable
fun SoodalToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = SoodalDesign.colors
    val trackBackground = if (checked) Modifier.background(colors.gradBlueVivid) else Modifier.background(colors.surface3)
    // ON 오프셋 = 트랙(44) − 썸(20) − 좌우 여백(2) = 22 → 켜짐/꺼짐 여백이 좌우 대칭.
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = tween(200),
        label = "thumb",
    )
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .width(44.dp)
            .height(24.dp)
            .clip(CircleShape)
            .then(trackBackground)
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
