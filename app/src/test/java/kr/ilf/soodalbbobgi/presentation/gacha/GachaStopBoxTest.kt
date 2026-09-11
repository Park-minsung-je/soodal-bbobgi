package kr.ilf.soodalbbobgi.presentation.gacha

import com.google.common.truth.Truth.assertThat
import kr.ilf.soodalbbobgi.data.remote.dto.ServerGachaBoxItem
import kr.ilf.soodalbbobgi.data.remote.dto.ServerGachaResult
import kr.ilf.soodalbbobgi.domain.model.GachaBoxWithDrops
import org.junit.Test

/**
 * 룰렛이 멈출 상자는 서버가 실제로 뽑은 상자여야 한다 —
 * 캐릭터 상자에 멈췄는데 배경이 나오는 불일치를 막는다.
 */
class GachaStopBoxTest {

    private fun box(id: Long, category: String) = GachaBoxWithDrops(
        id = id, name = "$category 상자", description = "", category = category, iconAsset = null,
        shellCost = 1, tenPullCost = 10,
        drops = emptyList(),
    )

    private fun result(boxId: Long?, category: String) = ServerGachaResult(
        item = ServerGachaBoxItem(id = 1, itemKey = "k", name = "n", grade = "N", category = category, weight = 1, imageAsset = null),
        wasNew = true, pearlsEarned = 0, shellsSpent = 1, pityCountAtPull = 0, boxId = boxId,
    )

    private val boxes = listOf(box(1, "char"), box(2, "bg"), box(3, "frame"))

    @Test
    fun `첫 결과의 boxId에 해당하는 상자에서 멈춘다`() {
        val stop = resolveStopBox(boxes, listOf(result(2, "bg"), result(1, "char")))
        assertThat(stop?.id).isEqualTo(2L)
    }

    @Test
    fun `boxId가 없으면 아이템 카테고리와 같은 상자로 대신 맞춘다`() {
        val stop = resolveStopBox(boxes, listOf(result(null, "frame")))
        assertThat(stop?.id).isEqualTo(3L)
    }

    @Test
    fun `맞는 상자를 못 찾으면 null - 호출부가 임의 상자로 연출한다`() {
        assertThat(resolveStopBox(boxes, listOf(result(99, "unknown")))).isNull()
        assertThat(resolveStopBox(boxes, emptyList())).isNull()
    }
}
