package com.soodalbbobgi.app.presentation.collection

import com.google.common.truth.Truth.assertThat
import com.soodalbbobgi.app.domain.model.Grade
import com.soodalbbobgi.app.domain.model.Item
import org.junit.Test

/** 도감 정렬 규칙 — 카테고리 → 등급 낮은 순 → 번호 순. */
class CollectionOrderTest {

    private fun item(id: Long, category: String, grade: Grade) = Item(
        id = id,
        itemKey = "key$id",
        name = "item$id",
        grade = grade,
        category = category,
        imageAsset = null,
    )

    @Test
    fun `같은 카테고리 안에서는 등급이 낮은 것이 먼저 온다`() {
        val items = listOf(
            item(1, "char", Grade.N),
            item(2, "char", Grade.SSR),
            item(3, "char", Grade.R),
            item(4, "char", Grade.SR),
        )

        val sorted = items.sortedWith(collectionOrder())

        assertThat(sorted.map { it.grade })
            .containsExactly(Grade.N, Grade.R, Grade.SR, Grade.SSR).inOrder()
    }

    @Test
    fun `등급이 같으면 번호가 작은 것이 먼저 온다`() {
        val items = listOf(
            item(30, "char", Grade.SR),
            item(10, "char", Grade.SR),
            item(20, "char", Grade.SR),
        )

        val sorted = items.sortedWith(collectionOrder())

        assertThat(sorted.map { it.id }).containsExactly(10L, 20L, 30L).inOrder()
    }

    @Test
    fun `카테고리가 등급보다 우선한다 — 캐릭터 배경 액자 순`() {
        val items = listOf(
            item(1, "frame", Grade.SSR),
            item(2, "bg", Grade.SSR),
            item(3, "char", Grade.N),
        )

        val sorted = items.sortedWith(collectionOrder())

        assertThat(sorted.map { it.category }).containsExactly("char", "bg", "frame").inOrder()
    }

    @Test
    fun `알 수 없는 카테고리는 맨 뒤로 간다`() {
        val items = listOf(
            item(1, "misc", Grade.SSR),
            item(2, "frame", Grade.N),
        )

        val sorted = items.sortedWith(collectionOrder())

        assertThat(sorted.map { it.category }).containsExactly("frame", "misc").inOrder()
    }

    @Test
    fun `전체 정렬 — 카테고리 안에서 등급 그다음 번호`() {
        val items = listOf(
            item(5, "bg", Grade.R),
            item(1, "char", Grade.R),
            item(4, "bg", Grade.SSR),
            item(2, "char", Grade.SSR),
            item(3, "char", Grade.R),
        )

        val sorted = items.sortedWith(collectionOrder())

        // char: R(1) → R(3) → SSR(2), bg: R(5) → SSR(4)
        assertThat(sorted.map { it.id }).containsExactly(1L, 3L, 2L, 5L, 4L).inOrder()
    }
}
