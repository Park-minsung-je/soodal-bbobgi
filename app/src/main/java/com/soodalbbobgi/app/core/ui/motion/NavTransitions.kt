package com.soodalbbobgi.app.core.ui.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.navigation.NavBackStackEntry

/** 화면 전환 종류. route 쌍으로 결정된다. */
enum class TransitionKind {
    TAB_FORWARD,   // 탭 오른쪽으로 이동(인덱스 증가)
    TAB_BACKWARD,  // 탭 왼쪽으로 이동(인덱스 감소)
    PUSH,          // 오른쪽에서 슬라이드 인 (설정/온보딩)
    EDITOR,        // 아래에서 살짝 올라오며 페이드 (프로필 편집)
    FADE,          // 단순 페이드 (앱 시작 플로우: 스플래시→홈 등)
}

/** 탭 화면 route → 탭 인덱스. 탭이 아니면 null. */
fun tabIndexOf(route: String?): Int? = when (route) {
    "home" -> 0
    "calendar" -> 1
    "gacha" -> 2
    "shop" -> 3
    else -> null
}

/**
 * 출발/도착 route로 전환 종류를 결정한다.
 *
 * 우선순위: 탭간(FORWARD/BACKWARD) > PUSH(기본). 앱 시작 플로우는 FADE.
 *
 * @param from 출발 route (없으면 PUSH)
 * @param to 도착 route
 */
fun transitionFor(from: String?, to: String?): TransitionKind {
    // 앱 시작 플로우: 첫 화면 진입은 슬라이드 없이 페이드 (스플래시에서 나가거나 홈 첫 진입).
    if (from == "splash") return TransitionKind.FADE
    if (to == "home" && (from == "auth" || from == "onboarding_permission")) return TransitionKind.FADE

    // profile_fullscreen/profile_editor 화면 제거: 전체보기는 홈 오버레이, 편집은 홈 바텀시트로 이동.

    val fromTab = tabIndexOf(from)
    val toTab = tabIndexOf(to)
    if (fromTab != null && toTab != null) {
        return if (toTab >= fromTab) TransitionKind.TAB_FORWARD else TransitionKind.TAB_BACKWARD
    }
    return TransitionKind.PUSH
}

private fun routeOf(entry: NavBackStackEntry): String? = entry.destination.route

/** 진입(enter) 전환: 새 화면이 들어올 때. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.soodalEnter(): EnterTransition {
    val kind = transitionFor(routeOf(initialState), routeOf(targetState))
    return when (kind) {
        TransitionKind.TAB_FORWARD ->
            slideInHorizontally(tween(Motion.DUR_TAB, easing = Motion.easeStandard)) { it } +
                fadeIn(tween(Motion.DUR_TAB))
        TransitionKind.TAB_BACKWARD ->
            slideInHorizontally(tween(Motion.DUR_TAB, easing = Motion.easeStandard)) { -it } +
                fadeIn(tween(Motion.DUR_TAB))
        TransitionKind.PUSH ->
            slideInHorizontally(tween(Motion.DUR_PUSH, easing = Motion.easeEmphasized)) { it } +
                fadeIn(tween(Motion.DUR_PUSH))
        TransitionKind.EDITOR ->
            slideInVertically(tween(Motion.DUR_EDITOR, easing = Motion.easeEmphasized)) {
                (it * Motion.EDITOR_SLIDE_FRACTION).toInt()
            } + fadeIn(tween(Motion.DUR_EDITOR))
        TransitionKind.FADE ->
            fadeIn(tween(Motion.DUR_FADE))
    }
}

/** 퇴장(exit) 전환: 기존 화면이 나갈 때(앞으로 진행). */
fun AnimatedContentTransitionScope<NavBackStackEntry>.soodalExit(): ExitTransition {
    val kind = transitionFor(routeOf(initialState), routeOf(targetState))
    return when (kind) {
        TransitionKind.TAB_FORWARD ->
            slideOutHorizontally(tween(Motion.DUR_TAB, easing = Motion.easeStandard)) { -it } +
                fadeOut(tween(Motion.DUR_TAB))
        TransitionKind.TAB_BACKWARD ->
            slideOutHorizontally(tween(Motion.DUR_TAB, easing = Motion.easeStandard)) { it } +
                fadeOut(tween(Motion.DUR_TAB))
        TransitionKind.PUSH ->
            slideOutHorizontally(tween(Motion.DUR_PUSH, easing = Motion.easeEmphasized)) {
                -(it * Motion.PARALLAX_FRACTION).toInt()
            } + fadeOut(tween(Motion.DUR_PUSH))
        TransitionKind.EDITOR ->
            fadeOut(tween(Motion.DUR_EDITOR))
        TransitionKind.FADE ->
            fadeOut(tween(Motion.DUR_FADE))
    }
}

/** 뒤로가기 진입(popEnter): 이전 화면이 다시 들어올 때. 정방향의 역동작. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.soodalPopEnter(): EnterTransition {
    // pop에서는 initialState=현재(사라지는) 화면, targetState=복귀 화면.
    // 전환 종류는 "원래 진입할 때" 기준으로 잡아야 역동작이 맞으므로 from=target, to=initial 로 평가.
    val kind = transitionFor(routeOf(targetState), routeOf(initialState))
    return when (kind) {
        TransitionKind.TAB_FORWARD ->
            slideInHorizontally(tween(Motion.DUR_TAB, easing = Motion.easeStandard)) { -it } +
                fadeIn(tween(Motion.DUR_TAB))
        TransitionKind.TAB_BACKWARD ->
            slideInHorizontally(tween(Motion.DUR_TAB, easing = Motion.easeStandard)) { it } +
                fadeIn(tween(Motion.DUR_TAB))
        TransitionKind.PUSH ->
            slideInHorizontally(tween(Motion.DUR_PUSH, easing = Motion.easeEmphasized)) {
                -(it * Motion.PARALLAX_FRACTION).toInt()
            } + fadeIn(tween(Motion.DUR_PUSH))
        TransitionKind.EDITOR ->
            fadeIn(tween(Motion.DUR_EDITOR))
        TransitionKind.FADE ->
            fadeIn(tween(Motion.DUR_FADE))
    }
}

/** 뒤로가기 퇴장(popExit): 현재 화면이 뒤로 사라질 때. 정방향 enter의 역동작. */
fun AnimatedContentTransitionScope<NavBackStackEntry>.soodalPopExit(): ExitTransition {
    val kind = transitionFor(routeOf(targetState), routeOf(initialState))
    return when (kind) {
        TransitionKind.TAB_FORWARD ->
            slideOutHorizontally(tween(Motion.DUR_TAB, easing = Motion.easeStandard)) { it } +
                fadeOut(tween(Motion.DUR_TAB))
        TransitionKind.TAB_BACKWARD ->
            slideOutHorizontally(tween(Motion.DUR_TAB, easing = Motion.easeStandard)) { -it } +
                fadeOut(tween(Motion.DUR_TAB))
        TransitionKind.PUSH ->
            slideOutHorizontally(tween(Motion.DUR_PUSH, easing = Motion.easeEmphasized)) { it } +
                fadeOut(tween(Motion.DUR_PUSH))
        TransitionKind.EDITOR ->
            slideOutVertically(tween(Motion.DUR_EDITOR, easing = Motion.easeEmphasized)) {
                (it * Motion.EDITOR_SLIDE_FRACTION).toInt()
            } + fadeOut(tween(Motion.DUR_EDITOR))
        TransitionKind.FADE ->
            fadeOut(tween(Motion.DUR_FADE))
    }
}
