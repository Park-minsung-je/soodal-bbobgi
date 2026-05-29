package com.soodalbbobgi.app.domain.model

/**
 * 프로필 카드 저장 상태.
 *
 * 텍스트 블록(닉네임·소개·기록 3줄)은 하나의 블록으로 다뤄지며, 정렬/크기/표시여부/
 * 색상을 사용자가 커스터마이즈한다.
 *
 * @param textAlign 텍스트 블록 가로 정렬 ("LEFT" | "RIGHT").
 * @param textScaleStep 블록 크기 단계 (1~5, 3이 기본).
 * @param showStats 기록(통계) 줄 표시 여부.
 * @param nicknameColor 닉네임 색상 ("#RRGGBB").
 * @param taglineColor 소개 줄 색상 ("#RRGGBB").
 * @param statsColor 기록 줄 색상 ("#RRGGBB").
 */
data class ProfileCard(
    val userId: String,
    val backgroundItemId: Long? = null,
    val characterItemId: Long? = null,
    val borderItemId: Long? = null,
    val characterX: Float = 0.5f,
    val characterY: Float = 0.5f,
    val characterScale: Float = 1.0f,
    val customText: String = "",
    val textStyle: String = "REGULAR",
    val textAlign: String = "RIGHT",
    val textScaleStep: Int = 3,
    val showStats: Boolean = true,
    val nicknameColor: String = "#FFFFFF",
    val taglineColor: String = "#FFFFFF",
    val statsColor: String = "#00F5FF",
)
