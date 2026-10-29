package com.certora.collect

import kotlin.test.*

/**
 * Each test states the behavior the public API should have.
 */
class ProductBugTest {
    @Test
    fun mergeUnionWithNullLeftValueKeepsOneEntry() {
        val key = HashTestKey(1, code = 7)
        val other = HashTestKey(2, code = 7)
        var calls = 0
        val merged = treapMapOf(key to null as String?, other to "keep").merge(
            treapMapOf(key to "b")
        ) { _, left, right ->
            calls += 1
            right ?: left
        }
        assertEquals(2, calls)
        assertEquals(2, merged.size)
        val expected: Map<HashTestKey, String?> = mapOf(key to "b", other to "keep")
        assertEquals(expected, merged)
    }

    @Test
    @Ignore(
        "AbstractTreapMap.fallbackUnion and fallbackIntersect force the left value with !!, " +
            "so union/intersect with a plain Map throws NullPointerException when the treap holds a null value. " +
            "Reproduce: treapMapOf(HashTestKey(1) to null).union(mapOf(HashTestKey(1) to \"b\")) { _, old, new -> new ?: old }."
    )
    fun unionAndIntersectWithPlainMapAcceptNullValues() {
        val key = HashTestKey(1)
        val left = treapMapOf<HashTestKey, String?>(key to null, HashTestKey(2) to "keep")
        val right = mapOf(key to "from-right", HashTestKey(3) to "new")
        val unionExpected: Map<HashTestKey, String?> = mapOf(key to "from-right", HashTestKey(2) to "keep", HashTestKey(3) to "new")
        assertEquals(unionExpected, left.union(right) { _, old, new -> new ?: old })
        val intersectExpected: Map<HashTestKey, String?> = mapOf(key to "from-right")
        assertEquals(intersectExpected, left.intersect(right) { _, old, new -> new ?: old })
    }

    @Test
    fun listBuilderSizeTracksEdits() {
        val builder = treapListOf(1).builder()
        builder.add(2)
        assertEquals(2, builder.size)
        assertEquals(1, builder.lastIndex)
        assertEquals(listOf(1, 2), builder.subList(0, builder.size))
        builder.clear()
        assertEquals(0, builder.size)
        assertTrue(builder.isEmpty())
    }

    @Test
    fun listBuilderIteratorSetIsVisibleToThatIterator() {
        val builder = treapListOf(10, 20, 30).builder()
        val iterator = builder.listIterator()
        assertEquals(10, iterator.next())
        iterator.set(99)
        assertEquals(listOf(99, 20, 30), builder.build())
        assertEquals(99, iterator.previous())

        assertEquals(99, iterator.next())
        assertEquals(20, iterator.next())
        assertEquals(20, iterator.previous())
        iterator.set(77)
        assertEquals(77, iterator.next())
        assertEquals(listOf(99, 77, 30), builder.build())
    }

    @Test
    @Ignore(
        "parallelMerge, parallelUnion, and parallelIntersect subtract 1 before the size check, so a threshold " +
            "of 0 throws IllegalArgumentException. parallelUpdateValues and parallelMapReduce accept 0. " +
            "Reproduce: treapMapOf(1 to 1).parallelMerge(treapMapOf(1 to 2), parallelThresholdLog2 = 0) { _, a, b -> (a ?: 0) + (b ?: 0) }."
    )
    fun parallelMergeAcceptsZeroThreshold() {
        val left = treapMapOf(1 to 1, 2 to 2)
        val right = treapMapOf(2 to 3, 3 to 4)
        val merger = { _: Int, a: Int?, b: Int? -> (a ?: 0) + (b ?: 0) }
        assertEquals(left.merge(right, merger), left.parallelMerge(right, 0, merger))
    }
}
