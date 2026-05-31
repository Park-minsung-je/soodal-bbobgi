package com.soodalbbobgi.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * LruMemoizer 동작 검증.
 * 같은 키 재사용, 키별 분리, 용량 초과 시 LRU 축출, 접근 순서 갱신을 다룬다.
 */
class LruMemoizerTest {

    @Test
    fun `같은 키는 producer를 한 번만 호출하고 캐시값을 돌려준다`() {
        val memo = LruMemoizer<String, Int>(maxSize = 2)
        var calls = 0

        val first = memo.getOrPut("a") { calls++; 10 }
        val second = memo.getOrPut("a") { calls++; 99 }

        assertEquals(10, first)
        assertEquals(10, second)
        assertEquals(1, calls)
    }

    @Test
    fun `다른 키는 각각 producer를 호출한다`() {
        val memo = LruMemoizer<String, Int>(maxSize = 2)
        var calls = 0

        memo.getOrPut("a") { calls++; 1 }
        memo.getOrPut("b") { calls++; 2 }

        assertEquals(2, calls)
    }

    @Test
    fun `용량을 넘으면 가장 오래 접근하지 않은 키가 축출된다`() {
        val memo = LruMemoizer<String, Int>(maxSize = 2)
        var calls = 0

        memo.getOrPut("a") { calls++; 1 } // (a)
        memo.getOrPut("b") { calls++; 2 } // (a, b)
        memo.getOrPut("c") { calls++; 3 } // a 축출 → (b, c)
        memo.getOrPut("a") { calls++; 1 } // a 재계산

        assertEquals(4, calls)
    }

    @Test
    fun `최근 접근한 키는 축출에서 보호된다`() {
        val memo = LruMemoizer<String, Int>(maxSize = 2)
        var calls = 0

        memo.getOrPut("a") { calls++; 1 } // (a)
        memo.getOrPut("b") { calls++; 2 } // (a, b)
        memo.getOrPut("a") { calls++; 1 } // a 접근 → 최신화 (b, a)
        memo.getOrPut("c") { calls++; 3 } // b 축출 → (a, c)
        memo.getOrPut("a") { calls++; 1 } // a는 살아있음 → 호출 없음

        assertEquals(3, calls)
    }
}
