package com.soodalbbobgi.app.presentation.profile

import androidx.compose.ui.geometry.Offset

/**
 * 홈↔전체보기 카드 전환용 공유 위치 홀더.
 *
 * 홈 프로필 카드의 화면(window) 중심을 기록해, 전체보기 진입 애니메이션이
 * 그 자리에서 떠오르도록 시작점을 잡는다. 계산 근사 대신 실제 측정값을 쓰므로
 * 상태바·네비게이션바·하단 탭바로 인한 영역 차이가 자동으로 반영된다.
 */
object ProfileCardBounds {
    /** 홈 프로필 카드의 window 기준 중심(px). 아직 측정 전이면 null. */
    var homeCardCenter: Offset? = null
}
