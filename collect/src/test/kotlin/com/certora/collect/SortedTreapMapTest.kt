package com.certora.collect

import com.certora.collect.*
import kotlin.test.*
import kotlinx.serialization.DeserializationStrategy
import java.util.TreeMap

/** Tests for [SortedTreapMap]. */
@Suppress("UNCHECKED_CAST")
class SortedTreapMapTest: TreapMapTest() {
    override fun makeKey(value: Int, code: Int) = ComparableTestKey(value, code)
    override val allowNullKeys = false
    override fun makeMap(): TreapMap.Builder<TestKey?, Any?> = treapMapOf<ComparableTestKey, Any?>().builder() as TreapMap.Builder<TestKey?, Any?>
    override fun makeBaseline(): MutableMap<TestKey?, Any?> = TreeMap()
    override fun makeMap(other: Map<TestKey?,Any?>): TreapMap.Builder<TestKey?, Any?> = makeMap().apply { putAll(other) }
    override fun makeBaseline(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = TreeMap(other)
    override fun makeMapOfInts(): TreapMap<Int?, Int?> = treapMapOf<Int, Int?>() as TreapMap<Int?, Int?>
    override fun makeMapOfInts(other: Map<Int?, Int?>) = makeMapOfInts().apply { putAll(other) }
    override fun makeBaselineOfInts(): MutableMap<Int?, Int?> = TreeMap()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null

    @Test
    fun ceilingFloorHigherLowerLastFirst() {

        val empty = treapMapOf<Int, Int>()
        assertNull(empty.firstKey())
        assertNull(empty.lastKey())
        assertNull(empty.floorKey(0))
        assertNull(empty.lowerKey(0))
        assertNull(empty.ceilingKey(0))
        assertNull(empty.higherKey(0))

        val map = treapMapOf<Int, Int>().mutate {
            for (i in 0..1000 step 2) {
                it[i] = i + 1
            }
        }

        assertEquals(0, map.firstKey())
        assertEquals(1000, map.lastKey())

        assertNull(map.floorKey(-1))
        assertNull(map.lowerKey(-1))
        assertEquals(0, map.ceilingKey(-1))
        assertEquals(0, map.higherKey(-1))

        assertEquals(0, map.floorKey(0))
        assertNull(map.lowerKey(0))
        assertEquals(0, map.ceilingKey(0))
        assertEquals(2, map.higherKey(0))

        for (i in 2..998 step 2) {
            assertEquals(i, map.floorKey(i))
            assertEquals(i - 2, map.lowerKey(i))
            assertEquals(i, map.ceilingKey(i))
            assertEquals(i + 2, map.higherKey(i))
        }

        for (i in 1..999 step 2) {
            assertEquals(i - 1, map.floorKey(i))
            assertEquals(i - 1, map.lowerKey(i))
            assertEquals(i + 1, map.ceilingKey(i))
            assertEquals(i + 1, map.higherKey(i))
        }

        assertEquals(1000, map.floorKey(1000))
        assertEquals(998, map.lowerKey(1000))
        assertEquals(1000, map.ceilingKey(1000))
        assertNull(map.higherKey(1000))

        assertEquals(1000, map.floorKey(1001))
        assertEquals(1000, map.lowerKey(1001))
        assertNull(map.ceilingKey(1001))
        assertNull(map.higherKey(1001))
    }
}
