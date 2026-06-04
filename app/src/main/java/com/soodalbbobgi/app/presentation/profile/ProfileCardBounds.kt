package com.soodalbbobgi.app.presentation.profile

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * 홈↔전체보기 카드 전환용 공유 위치/크기 홀더.
 *
 * 홈 프로필 카드의 화면(window) 중심과 크기를 기록해, 전체보기 오버레이가
 * 그 자리·그 크기에서 시작하도록 한다. 계산 근사 대신 실제 측정값을 쓰므로
 * 상태바·네비게이션바·하단 탭바로 인한 영역 차이가 자동으로 반영된다.
 */
object ProfileCardBounds {
    /** 홈 프로필 카드의 window 기준 중심(px). 아직 측정 전이면 null. */
    var homeCardCenter: Offset? = null

    /** 홈 프로필 카드의 크기(px). 오버레이 카드를 같은 크기로 그릴 때 쓴다. 측정 전이면 null. */
    var homeCardSize: Size? = null
}

/** 오버레이 카드의 한 진행도(progress) 변환 묶음. */
data class CardTransform(
    val rotationZ: Float,
    val scale: Float,
    val translationX: Float,
    val translationY: Float,
)

/**
 * 90도 회전한 카드가 화면을 최대한 채우는 배율을 구한다.
 * 회전 후에는 카드의 가로(cardW)가 세로 방향, 세로(cardH)가 가로 방향이 된다.
 *
 * @param cardW 회전 전 카드 가로(px)
 * @param cardH 회전 전 카드 세로(px)
 * @param screenW 화면 가로(px)
 * @param screenH 화면 세로(px)
 * @return 높이/폭 제약 중 더 작은(안 넘치는) 배율
 */
fun fullscreenCardScale(cardW: Float, cardH: Float, screenW: Float, screenH: Float): Float {
    val byHeight = screenH / cardW
    val byWidth = screenW / cardH
    return minOf(byHeight, byWidth)
}

/**
 * 오버레이 카드의 progress(0=홈 자리, 1=전체화면) 변환을 계산한다.
 * 회전·확대·이동을 한 진행도로 동시에 보간하므로 어긋남 없이 한 덩어리로 움직인다.
 *
 * @param progress 0~1 진행도
 * @param homeCenterX 홈 카드 중심 x(px, window 기준)
 * @param homeCenterY 홈 카드 중심 y(px, window 기준)
 * @param overlayCenterX 변환 전 오버레이 카드 중심 x(px, window 기준)
 * @param overlayCenterY 변환 전 오버레이 카드 중심 y(px, window 기준)
 * @param fullscreenScale 전체화면에서의 확대 배율([fullscreenCardScale])
 */
fun fullscreenCardTransform(
    progress: Float,
    homeCenterX: Float,
    homeCenterY: Float,
    overlayCenterX: Float,
    overlayCenterY: Float,
    fullscreenScale: Float,
): CardTransform {
    val rotation = 90f * progress
    val scale = 1f + (fullscreenScale - 1f) * progress
    val tx = (homeCenterX - overlayCenterX) * (1f - progress)
    val ty = (homeCenterY - overlayCenterY) * (1f - progress)
    return CardTransform(rotation, scale, tx, ty)
}
