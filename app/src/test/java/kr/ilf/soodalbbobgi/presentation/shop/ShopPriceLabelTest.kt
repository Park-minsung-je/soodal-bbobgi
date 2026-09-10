package kr.ilf.soodalbbobgi.presentation.shop

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** 상점 가격 라벨 규칙 — 아이콘이 통화를 말하므로 숫자만 쓴다 (R14). */
class ShopPriceLabelTest {

    @Test
    fun `가격 라벨은 숫자만 보여준다`() {
        assertThat(priceLabel(200)).isEqualTo("200")
        assertThat(priceLabel(5)).isEqualTo("5")
    }

    @Test
    fun `가격 라벨에 통화 단위 글자를 붙이지 않는다`() {
        val label = priceLabel(30)
        assertThat(label).doesNotContain("진주")
        assertThat(label).doesNotContain("개")
        assertThat(label).doesNotContain(" ")
    }
}
