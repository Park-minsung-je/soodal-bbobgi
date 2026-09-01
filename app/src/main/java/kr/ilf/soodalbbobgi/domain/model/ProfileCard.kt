package kr.ilf.soodalbbobgi.domain.model

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
    /** 테두리 장착 id — 테두리 기능 보류 중(렌더/편집 비활성)이지만 데이터는 보존한다. */
    val borderItemId: Long? = null,
    val characterX: Float = 0.5f,
    val characterY: Float = 0.5f,
    val characterScale: Float = 1.0f,
    /** 캐릭터 부유 그림자(엘리베이션) 표시 여부. */
    val characterShadow: Boolean = true,
    val customText: String = "",
    val textStyle: String = "REGULAR",
    // ── 구 블록 단위 필드 (서버/구버전 호환용 — 신규 요소별 필드의 기본값 유도에만 사용) ──
    val textAlign: String = "RIGHT",
    val textX: Float = 0.95f,
    val textY: Float = 0.5f,
    val textScaleStep: Int = 3,
    /** (구) 이름표 블록 표시 여부 — 신규 showNickname/showTagline의 유도 원본. */
    val showText: Boolean = true,
    // ── 요소별 커스텀: 닉네임 / 한마디(소개) / 기록 각각 표시·중심 위치(0~1)·크기 단계 ──
    val showNickname: Boolean = true,
    val nicknameX: Float = 0.83f,
    val nicknameY: Float = 0.40f,
    val nicknameScaleStep: Int = 6,
    val showTagline: Boolean = true,
    val taglineX: Float = 0.83f,
    val taglineY: Float = 0.57f,
    val taglineScaleStep: Int = 3,
    val showStats: Boolean = true,
    val statsX: Float = 0.16f,
    val statsY: Float = 0.90f,
    val statsScaleStep: Int = 3,
    /** 닉네임 알약 스타일 ("NONE" | "BLACK" | "WHITE" | "BLUR"). */
    val nicknamePill: String = "WHITE",
    /** 소개 알약 스타일. */
    val taglinePill: String = "NONE",
    /** 기록 칩 알약 스타일. */
    val statsPill: String = "BLUR",
    val nicknameColor: String = "#000000",
    val taglineColor: String = "#000000",
    val statsColor: String = "#000000",
    // ── 요소별 글꼴 스타일("REGULAR"|"BOLD"|"ITALIC"|"BOLD_ITALIC")과 외곽선 ──
    // (구 전역 textStyle/textOutline은 구버전 호환용 — 신규 요소별 필드가 우선한다)
    val nicknameStyle: String = "BOLD",
    val taglineStyle: String = "REGULAR",
    val statsStyle: String = "REGULAR",
    val nicknameOutline: Boolean = false,
    val taglineOutline: Boolean = false,
    val statsOutline: Boolean = false,
    /** 요소별 글자 테두리 색 "#RRGGBB" — null이면 글자색 대비로 자동 결정. */
    val nicknameOutlineColor: String? = null,
    val taglineOutlineColor: String? = null,
    val statsOutlineColor: String? = null,
    val textOutline: Boolean = false,
)
