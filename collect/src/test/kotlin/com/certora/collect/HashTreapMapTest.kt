package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.DeserializationStrategy
import kotlin.test.*

/** Tests for [HashTreapMap]. */
class HashTreapMapTest: TreapMapTest() {
    override fun makeKey(value: Int, code: Int) = HashTestKey(value, code)
    override fun makeMap(): TreapMap.Builder<TestKey?, Any?> = treapMapOf<TestKey?, Any?>().builder()
    override fun makeBaseline(): MutableMap<TestKey?, Any?> = HashMap()
    override fun makeMap(other: Map<TestKey?,Any?>): TreapMap.Builder<TestKey?, Any?> = makeMap().apply { putAll(other) }
    override fun makeBaseline(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = HashMap(other)
    override fun makeMapOfInts(): TreapMap<Int?, Int?> = treapMapOf<Int?, Int?>()
    override fun makeMapOfInts(other: Map<Int?, Int?>) = makeMapOfInts().apply { putAll(other) }
    override fun makeBaselineOfInts(): MutableMap<Int?, Int?> = HashMap()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null

    @Test
    fun hashCollisions() {
        val a = HashTestKey(1, code = 7)
        val b = HashTestKey(2, code = 7)
        val c = HashTestKey(3, code = 8)
        val d = HashTestKey(4, code = 7)
        val left = treapMapOf(a to 1, b to 2, c to 3)
        val right = treapMapOf(b to 20, c to 30, d to 4)

        assertEquals(mapOf(a to 1, b to 22, c to 33, d to 4), left.merge(right) { _, v1, v2 -> (v1 ?: 0) + (v2 ?: 0) })
        assertEquals(mapOf(b to 2, c to 3), left.intersect(right) { _, v1, _ -> v1 })
        assertEquals(mapOf(a to 1, b to 20, c to 30, d to 4), left.union(right) { _, _, v2 -> v2 })
        assertEquals(
            setOf(MapEntry(a, 1 to null), MapEntry(b, 2 to 20), MapEntry(c, 3 to 30), MapEntry(d, null to 4)),
            left.zip(right).toSet()
        )

        val updated = left.updateEntry(b, 5) { current, extra -> current!! + extra }
        assertEquals(7, updated[b])
        assertEquals(1, updated[a])
        val removed = left.updateEntry(a, 0) { _, _ -> null }
        assertFalse(a in removed)
        assertEquals(2, removed[b])
        val inserted = left.updateEntry(d, 9) { current, extra ->
            assertNull(current)
            extra
        }
        assertEquals(9, inserted[d])

        assertEquals(mapOf(b to 20, c to 30), left.updateValues { key, value ->
            if (key == a) {
                null
            } else {
                value * 10
            }
        })
        assertFalse(b in left.remove(b, 2))
        assertEquals(left, left.remove(b, 99))

        val onlyLeft = HashTestKey(8, code = 42)
        val onlyRight = HashTestKey(9, code = 99)
        val withDisjointHashes = treapMapOf(a to 1, onlyLeft to 5).merge(treapMapOf(onlyRight to 6, b to 2)) { _, v1, v2 ->
            (v1 ?: 0) + (v2 ?: 0)
        }
        assertEquals(mapOf(a to 1, onlyLeft to 5, onlyRight to 6, b to 2), withDisjointHashes)

        val sameValue = treapMapOf(a to "x", b to "x")
        assertEquals(mapOf(b to "x"), sameValue.remove(a, "x"))
        assertEquals(mapOf(a to "x"), sameValue.remove(b, "x"))

        assertSame(left, left.updateEntry(a, 0) { current, _ -> current })
        assertSame(left, left.updateEntry(b, 0) { current, _ -> current })
        val missingInBucket = HashTestKey(5, code = 7)
        assertSame(left, left.updateEntry(missingInBucket, 1) { _, _ -> null })

        assertEquals(6, left.mapReduce({ _, value -> value }, Int::plus))
    }

    @Test
    fun mergeUnionWithNullValueInCollidingBucketMergesOnce() {
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
        val expected: Map<HashTestKey, String?> = mapOf(key to "b", other to "keep")
        assertEquals(expected, merged)
    }
}
