package com.soodalbbobgi.app.core.ui.motion

/** 화면 전환 종류. route 쌍으로 결정된다. */
enum class TransitionKind {
    TAB_FORWARD,   // 탭 오른쪽으로 이동(인덱스 증가)
    TAB_BACKWARD,  // 탭 왼쪽으로 이동(인덱스 감소)
    PUSH,          // 오른쪽에서 슬라이드 인 (설정/온보딩)
    EDITOR,        // 아래에서 살짝 올라오며 페이드 (프로필 편집)
    ZOOM,          // 카드 확대/축소 (전체보기)
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
 * 우선순위: ZOOM(전체보기) > EDITOR(편집) > 탭간(FORWARD/BACKWARD) > PUSH(기본).
 *
 * @param from 출발 route (없으면 PUSH)
 * @param to 도착 route
 */
fun transitionFor(from: String?, to: String?): TransitionKind {
    if (to == "profile_fullscreen") return TransitionKind.ZOOM
    if (to == "profile_editor") return TransitionKind.EDITOR

    val fromTab = tabIndexOf(from)
    val toTab = tabIndexOf(to)
    if (fromTab != null && toTab != null) {
        return if (toTab >= fromTab) TransitionKind.TAB_FORWARD else TransitionKind.TAB_BACKWARD
    }
    return TransitionKind.PUSH
}
