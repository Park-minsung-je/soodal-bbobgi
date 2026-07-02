package com.soodalbbobgi.app.domain.model

/**
 * 프로필 카드 저장 상태.
 *
 * 텍스트 블록(닉네임·소개·기록 3줄)은 하나의 블록으로 다뤄지며, 정렬/크기/표시여부/
 * 색상을 사용자가 커스터마이즈한다.
 *
 * @param textAlign 텍스트 블록 내부 줄 정렬 ("LEFT" | "RIGHT").
 * @param textX 텍스트 블록 가로 위치 (0~1). 정렬에 따라 좌/우 앵커 기준.
 * @param textY 텍스트 블록 세로 중심 위치 (0~1).
 * @param textScaleStep 블록 크기 단계 (1~5, 3이 기본).
 * @param showStats 기록(통계) 줄 표시 여부.
 * @param nicknameColor 닉네임 색상 ("#RRGGBB").
 * @param taglineColor 소개 줄 색상 ("#RRGGBB").
 * @param statsColor 기록 줄 색상 ("#RRGGBB").
 * @param textOutline 텍스트 외곽선(테두리) 표시 여부.
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
    val textX: Float = 0.95f,
    val textY: Float = 0.5f,
    val textScaleStep: Int = 3,
    val showStats: Boolean = true,
    /** 이름표(닉네임 알약+소개) 표시 여부. */
    val showText: Boolean = true,
    /** 닉네임 알약 스타일 ("NONE" | "BLACK" | "WHITE" | "BLUR"). */
    val nicknamePill: String = "WHITE",
    /** 소개 알약 스타일. */
    val taglinePill: String = "NONE",
    /** 기록 칩 알약 스타일. */
    val statsPill: String = "BLUR",
    val nicknameColor: String = "#FFFFFF",
    val taglineColor: String = "#FFFFFF",
    val statsColor: String = "#00F5FF",
    val textOutline: Boolean = false,
)
