package kr.ilf.soodalbbobgi.domain.model

/**
 * 가챠 박스 + 박스에서 드롭되는 아이템 목록.
 * 서버 GET /gacha/boxes 응답을 1:1로 표현한다.
 */
data class GachaBoxWithDrops(
    val id: Long,
    val name: String,
    val description: String,
    /** 박스 카테고리 (char/bg/frame). 서버에서 제거 예정인 필드라 null 허용. */
    val category: String?,
    val iconAsset: String?,
    val shellCost: Int,
    val tenPullCost: Int,
    val drops: List<GachaDrop>,
)

/** 박스 안에서 한 아이템과 그 가중치. */
data class GachaDrop(
    val item: Item,
    val weight: Int,
)
