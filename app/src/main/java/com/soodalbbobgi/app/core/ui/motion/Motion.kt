package com.soodalbbobgi.app.core.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * 앱 전역 모션 토큰 단일 소스.
 *
 * 화면 전환/팝업의 duration·easing·scale 값을 한곳에서 관리한다.
 * 실기기 확인 후 미세 조정은 이 파일만 수정하면 전체 반영된다.
 */
object Motion {
    /** 탭 좌우 슬라이드 전환 시간(ms). */
    const val DUR_TAB = 280

    /** 설정 등 push 전환 시간(ms). */
    const val DUR_PUSH = 320

    /** 프로필 편집 진입/복귀(아래↔위 페이드) 시간(ms). 끊김을 줄이려 넉넉히 둔다. */
    const val DUR_EDITOR = 440

    /** 전체보기 카드 확대/축소 시간(ms). */
    const val DUR_ZOOM = 420

    /** 앱 시작 플로우(스플래시→홈 등) 단순 페이드 시간(ms). */
    const val DUR_FADE = 240

    /** 대부분의 전환에 쓰는 표준 이징. */
    val easeStandard: Easing = FastOutSlowInEasing

    /** push/zoom의 정착감을 주는 강조 이징. */
    val easeEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** push 시 뒤 화면이 밀려나는 시차 비율(전체폭 대비). */
    const val PARALLAX_FRACTION = 0.3f

    /** 전체보기 진입/복귀 시 카드 쪽 스케일(작은 쪽). */
    const val ZOOM_MIN_SCALE = 0.88f

    /** 프로필 편집 페이드업 초기 오프셋 비율(화면 높이 대비). */
    const val EDITOR_SLIDE_FRACTION = 0.10f
}
