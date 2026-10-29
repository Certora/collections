package com.certora.collect

import kotlin.test.*
import org.junit.Rule

/**
 * Each test states the behavior the public API should have.
 */
class ProductBugTest {
    @field:Rule
    @JvmField
    val treapListPriorities = TreapListPriorityRule()

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
    fun parallelMergeAcceptsZeroThreshold() {
        val left = treapMapOf(1 to 1, 2 to 2)
        val right = treapMapOf(2 to 3, 3 to 4)
        val merger = { _: Int, a: Int?, b: Int? -> (a ?: 0) + (b ?: 0) }
        assertEquals(left.merge(right, merger), left.parallelMerge(right, 0, merger))
        val keepRight = { _: Int, _: Int, b: Int -> b }
        val keepLeft = { _: Int, a: Int, _: Int -> a }
        assertEquals(left.union(right, keepRight), left.parallelUnion(right, 0, keepRight))
        assertEquals(left.intersect(right, keepLeft), left.parallelIntersect(right, 0, keepLeft))
    }
}
