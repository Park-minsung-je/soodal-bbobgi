package com.soodalbbobgi.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 앱 레벨 오버레이 호스트 — 화면 안에서 선언한 팝업/시트를 **콘텐츠 haze 소스 바깥**
 * (탭바와 같은 레이어)에서 그리게 한다. 이렇게 해야 팝업 패널이 hazeEffect로
 * 뒤 콘텐츠를 샘플링(진짜 backdrop blur)할 수 있다 — 소스 안에서는 재귀 드로잉이라 불가.
 */
class OverlayHostState {
    private var nextId = 0L

    /** 등록된 오버레이들 (등록 순서대로 그린다). */
    val entries = mutableStateMapOf<Long, @Composable () -> Unit>()

    internal fun allocateId(): Long = nextId++
}

/** AppNavHost가 제공. null이면(프리뷰 등) [AppOverlay]가 인라인으로 폴백 렌더한다. */
val LocalOverlayHost = staticCompositionLocalOf<OverlayHostState?> { null }

/**
 * 이 콘텐츠를 앱 오버레이 레이어(콘텐츠/탭바 위)에서 렌더한다.
 * 조건부로 감싸 사용: `if (open) AppOverlay { MyPopup(...) }`.
 * 콘텐츠 람다가 읽는 상태는 호스트 쪽에서 그대로 반응(리컴포지션)한다.
 */
@Composable
fun AppOverlay(content: @Composable () -> Unit) {
    val host = LocalOverlayHost.current
    if (host == null) {
        // 호스트가 없는 환경(프리뷰/테스트) — 제자리 렌더 폴백.
        content()
        return
    }
    val id = remember { host.allocateId() }
    // 최신 람다로 갱신 — 캡처 값이 바뀌어 새 람다가 내려와도 호스트가 항상 최신을 그린다.
    host.entries[id] = content
    DisposableEffect(Unit) {
        onDispose { host.entries.remove(id) }
    }
}
