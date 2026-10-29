package com.certora.collect

import kotlin.test.*

/**
 * Each test states the behavior the public API should have. They are ignored because the
 * implementation does not do that yet, and this change set does not modify product code.
 */
class ProductBugTest {
    @Test
    @Ignore(
        "HashTreapMap.getShallowMerger treats a null value as a missing key in UNION mode, " +
            "so a key stored as null on the left is merged twice and the map can contain duplicate keys. " +
            "Reproduce: treapMapOf(HashTestKey(1) to null).merge(treapMapOf(HashTestKey(1) to \"b\")) { _, _, right -> right }."
    )
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
    @Ignore(
        "TreapListBuilder.size is captured once from the list passed to the constructor and never updated. " +
            "isEmpty() reads the live list, so after add/clear the reported size disagrees with the contents " +
            "and subList checks the stale size. Reproduce: treapListOf(1).builder().apply { add(2) }.size."
    )
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
    @Ignore(
        "TreapListBuilder.listIterator.set replaces the element in the builder but leaves the iterator " +
            "walking the previous treap, so previous() still returns the old value. " +
            "Reproduce: iterate a builder, set the first element, then previous()."
    )
    fun listBuilderIteratorSetIsVisibleToThatIterator() {
        val builder = treapListOf(10, 20, 30).builder()
        val iterator = builder.listIterator()
        assertEquals(10, iterator.next())
        iterator.set(99)
        assertEquals(listOf(99, 20, 30), builder.build())
        assertEquals(99, iterator.previous())
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
