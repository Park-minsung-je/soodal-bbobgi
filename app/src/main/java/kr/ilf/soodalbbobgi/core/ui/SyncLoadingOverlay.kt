package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ilf.soodalbbobgi.core.theme.SoodalDesign

/**
 * 동기화·로딩 중 화면 조작을 막는 전체 화면 오버레이 — 옅은 스크림 + 하단 중앙 2줄 진행 카드.
 *
 * 스크림은 닿는 포인터 이벤트를 전부 소비해 아래 콘텐츠가 눌리지 않게 하고 눌림 표시도 내지 않는다.
 * 카드는 탭바 바로 위(탭바 여백 + 12dp)에 놓여 화면 위쪽 내용을 가리지 않고 어디서든 같은 자리에 뜬다.
 * 최초 HC 가져오기(홈)·수동 동기화(홈·캘린더)·상점 로딩이 모두 이 하나를 쓴다.
 *
 * @param message 첫 줄 — 무엇을 하는 중인지 (예: "수영 기록 동기화 중이에요…")
 * @param hint 둘째 줄 — 기다려 달라는 안내
 * @param dimTabBar 화면 안에서 그릴 때 true — 탭바는 이 오버레이 밖에 있어 따로 어둡게 한다.
 *   [AppOverlay] 안에서 그려 탭바까지 스크림이 덮는 경우 false (두 번 어두워지지 않게).
 */
@Composable
fun SyncLoadingOverlay(
    message: String = "동기화 중이에요…",
    hint: String = "잠시만 기다려 주세요",
    dimTabBar: Boolean = true,
) {
    if (dimTabBar) DimTabBarWhileVisible()
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SoodalDimAlpha))
            // 스크림에 닿는 모든 포인터 이벤트를 소비 — 아래 레이어로 내려가지 않는다.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            }
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = TabBarClearance + 12.dp)
                .clip(shape)
                .background(colors.surface1)
                .border(1.dp, colors.glassBorder, shape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = colors.accentBlue,
                )
                Text(message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
            }
            Text(hint, fontSize = 12.sp, color = colors.textTertiary)
        }
    }
}
