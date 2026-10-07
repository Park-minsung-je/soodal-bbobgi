package kr.ilf.soodalbbobgi.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
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
    SyncScrim {
        SyncStatusCard(
            message = message,
            hint = hint,
            modifier = Modifier.padding(bottom = TabBarClearance + 12.dp),
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = colors.accentBlue,
            )
        }
    }
}

/** 동기화 실패 안내가 떠 있는 시간 — 두 줄을 읽을 수 있을 만큼. */
private const val SYNC_FAILURE_VISIBLE_MS = 4_000L

/**
 * 동기화 실패 안내 카드 — [SyncLoadingOverlay]의 진행 카드와 같은 자리·같은 모양이라
 * 로딩이 실패로 끝나면 카드가 그 자리에서 실패 문구로 바뀐 것처럼 보인다.
 *
 * 토스트는 진행 카드와 같은 아래쪽에 겹쳐 떠서 이 카드로 대신한다. 4초 뒤 스스로 닫히고
 * 탭하면 바로 닫힌다. 원인을 가리지 않고 같은 문구를 쓴다 — Health Connect 쪽 일시 오류는
 * 재부팅으로 풀리는 경우가 있어 그 방법을 함께 알린다.
 *
 * @param visible 표시 여부
 * @param onDismiss 시간 경과 또는 탭으로 닫힐 때 호출 — 호출부가 [visible]을 내린다
 * @param dimmed 진행 오버레이([SyncLoadingOverlay])에 이어서 뜰 때 true — 그 스크림과 탭바 딤을
 *   그대로 유지해 카드 내용만 바뀐 것처럼 이어지게 한다. 이때는 화면 어디를 눌러도 닫힌다.
 *   앞선 오버레이 없이 홀로 뜰 때는 false — 스크림 없이 카드만 떠 화면 조작을 막지 않는다.
 */
@Composable
fun SyncFailureNotice(visible: Boolean, onDismiss: () -> Unit, dimmed: Boolean = false) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(visible) {
        if (visible) {
            delay(SYNC_FAILURE_VISIBLE_MS)
            currentOnDismiss()
        }
    }
    if (dimmed) {
        // 진행 오버레이가 내려가는 같은 프레임에 같은 스크림으로 바로 그린다 —
        // 페이드를 넣으면 그 사이 스크림이 한 번 꺼졌다 켜져 끊겨 보인다. 닫힘도 진행 오버레이처럼 즉시.
        if (visible) {
            DimTabBarWhileVisible()
            SyncScrim(onTap = { currentOnDismiss() }) {
                SyncFailureCard(onClick = { currentOnDismiss() })
            }
        }
    } else {
        AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
            // 스크림·포인터 소비 없이 카드만 띄운다 — 카드 밖은 그대로 눌린다.
            Box(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SyncFailureCard(onClick = { currentOnDismiss() })
            }
        }
    }
}

/** 실패 문구가 담긴 2줄 카드 — 딤 유무와 무관하게 같은 자리·같은 모양. */
@Composable
private fun SyncFailureCard(onClick: () -> Unit) {
    SyncStatusCard(
        message = "동기화에 실패했어요",
        hint = "같은 증상이 반복되면 휴대폰을 재부팅해 주세요",
        // 둘째 줄이 길어 좁은 화면에서는 화면 가장자리에 붙지 않고 줄바꿈되게 한다
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = TabBarClearance + 12.dp),
        onClick = onClick,
    ) {
        SoodalIcon(SoodalIcons.Warn, tint = SoodalDesign.colors.warn, size = 14.dp)
    }
}

/**
 * 동기화 오버레이의 전체 화면 스크림 — 아래 콘텐츠로 가는 입력을 막고 내용을 하단 중앙에 둔다.
 *
 * @param onTap 스크림을 눌렀을 때 — null이면 닿는 포인터 이벤트를 전부 소비하기만 한다(진행 중)
 * @param content 스크림 위 내용 (상태 카드)
 */
@Composable
private fun SyncScrim(
    onTap: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SoodalDimAlpha))
            .then(
                if (onTap != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onTap,
                    )
                } else {
                    // 스크림에 닿는 모든 포인터 이벤트를 소비 — 아래 레이어로 내려가지 않는다.
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                    }
                },
            )
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter,
        content = content,
    )
}

/**
 * 동기화 상태 2줄 카드 — 진행([SyncLoadingOverlay])과 실패([SyncFailureNotice])가 같은 모양을 쓴다.
 *
 * @param message 첫 줄
 * @param hint 둘째 줄
 * @param modifier 카드 바깥 여백(위치) 지정용
 * @param onClick 카드를 눌렀을 때 — null이면 누를 수 없다
 * @param leading 첫 줄 앞 표시(진행 스피너·경고 아이콘)
 */
@Composable
private fun SyncStatusCard(
    message: String,
    hint: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: @Composable () -> Unit,
) {
    val colors = SoodalDesign.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
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
            leading()
            Text(message, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
        }
        Text(hint, fontSize = 12.sp, color = colors.textTertiary, textAlign = TextAlign.Center)
    }
}
