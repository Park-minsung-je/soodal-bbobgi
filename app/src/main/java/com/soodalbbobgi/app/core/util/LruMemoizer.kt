package com.soodalbbobgi.app.core.util

/**
 * 키별 계산 결과를 LRU로 메모이즈하는 작은 캐시.
 *
 * 같은 키면 producer를 다시 호출하지 않고 저장값을 돌려준다.
 * 항목 수가 [maxSize]를 넘으면 가장 오래 접근하지 않은 항목부터 축출한다.
 * 컴포지션이 폐기·재생성되어도 살아남아 무거운 결과(예: 합성 비트맵) 재계산을 막는 용도.
 *
 * @param maxSize 보관할 최대 항목 수
 */
class LruMemoizer<K, V>(private val maxSize: Int) {

    private val map = object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean = size > maxSize
    }

    /**
     * 키에 해당하는 값을 반환한다. 없으면 [producer]로 계산해 저장한 뒤 반환한다.
     *
     * @param key 캐시 키
     * @param producer 캐시 미스 시 값을 계산하는 람다
     */
    @Synchronized
    fun getOrPut(key: K, producer: (K) -> V): V =
        map[key] ?: producer(key).also { map[key] = it }
}
