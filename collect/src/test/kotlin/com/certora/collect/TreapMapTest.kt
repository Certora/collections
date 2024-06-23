package com.certora.collect

import com.certora.collect.*
import java.util.Random
import kotlinx.collections.immutable.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.DeserializationStrategy
import kotlin.test.*

/** Tests for [TreapMap]. */
abstract class TreapMapTest {

    abstract fun makeKey(value: Int, code: Int = value.hashCode()): TestKey

    open val allowNullKeys = true
    abstract fun makeMap(): MutableMap<TestKey?, Any?>
    abstract fun makeBaseline(): MutableMap<TestKey?, Any?>
    abstract fun makeMap(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?>
    abstract fun makeBaseline(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?>

    open fun assertOrderedIteration(expected: Iterator<*>, actual: Iterator<*>) { }

    fun assertVeryEqual(expected: Map<*,*>, actual: Map<*,*>) {
        assertEquals(expected, actual)
        assertTrue(actual.equals(expected))
        assertEquals(expected.hashCode(), actual.hashCode())
        assertEquals(expected.size, actual.size)
        assertEquals(expected.isEmpty(), actual.isEmpty())
        val expectedEntries = expected.entries
        val actualEntries = actual.entries
        assertEquals(expectedEntries.size, actualEntries.size)
        assertEquals(expectedEntries, actualEntries)
        assertOrderedIteration(expectedEntries.iterator(), actualEntries.iterator())
        val expectedKeys = expected.keys
        val actualKeys = actual.keys
        assertEquals(expectedKeys.size, actualKeys.size)
        assertEquals(expectedKeys, actualKeys)
        assertOrderedIteration(expectedKeys.iterator(), actualKeys.iterator())
        val expectedValues = expected.values
        val actualValues = actual.values
        assertEquals(expectedValues.size, actualValues.size)
        assertOrderedIteration(expectedValues.iterator(), actualValues.iterator())
    }

    fun <TResult> assertEqualMutation(baseline: MutableMap<TestKey?, Any?>, map: MutableMap<TestKey?,Any?>, action: MutableMap<TestKey?,Any?>.() -> TResult) {
        assertEqualResult(baseline, map, action)
        assertVeryEqual(baseline, map)
    }

    fun <TResult> assertEqualResult(baseline: MutableMap<TestKey?, Any?>, map: MutableMap<TestKey?,Any?>, action: MutableMap<TestKey?,Any?>.() -> TResult) {
        assertEquals(baseline.action(), map.action())
    }

    @Test
    fun construct() {
        val b = makeBaseline()
        val m = makeMap()
        assertVeryEqual(b, m)
    }

    @Test
    fun putAndGet() {
        val b = makeBaseline()
        val m = makeMap()

        val o1 = makeKey(5)
        val v1 = Object()
        assertEqualMutation(b, m) { put(o1, v1) }
        assertEqualResult(b, m) { get(o1) }

        val v2 = Object()
        assertEqualMutation(b, m) { put(o1, v2) }
        assertEqualResult(b, m) { get(o1) }

        val o2 = makeKey(3)
        val v3 = Object()
        assertEqualMutation(b, m) { put(o2, v3) }
        assertEqualResult(b, m) { get(o2) }

        val v4 = Object()
        val v5 = Object()
        assertEqualMutation(b, m) { put(o1, v4) }
        assertEqualMutation(b, m) { put(o2, v5) }

        val o3 = makeKey(4)
        val v6 = Object()
        assertEqualMutation(b, m) { put(o3, v6) }
        assertEqualMutation(b, m) { put(o2, v6) }
    }

    @Test
    fun remove() {
        val b = makeBaseline()
        val m = makeMap()

        val objs = Array<TestKey>(5) { makeKey(it, code = 0) }
        val vals = Array<Any>(5) { Object() }

        for (i in objs.indices) {
            assertEqualMutation(b, m) { put(objs[i], vals[i]) }
        }

        assertEqualMutation(b, m) { remove(objs[0]) }
        assertEqualResult(b, m) { containsKey(objs[0]) }

        assertEqualMutation(b, m) { remove(objs[1]) }
        assertEqualResult(b, m) { containsKey(objs[1]) }

        assertEqualMutation(b, m) { remove(objs[4]) }
        assertEqualResult(b, m) { containsKey(objs[4]) }

        assertEqualMutation(b, m) { remove(objs[2]) }
        assertEqualResult(b, m) { containsKey(objs[2]) }

        assertEqualMutation(b, m) { remove(objs[3]) }
        assertEqualResult(b, m) { containsKey(objs[3]) }

        for (i in objs.indices) {
            assertEqualMutation(b, m) { put(objs[i], vals[i]) }
            assertEqualResult(b, m) { get(objs[i]) }
        }
    }

    @Test
    fun clear() {
        val m = makeMap()
        val k = makeKey(1)
        val v = Object()
        m.put(k, v)
        assertEquals(1, m.size)
        assertEquals(v, m[k])
        m.clear()
        assertEquals(0, m.size)
        assertNull(m[k])
    }

    @Test
    fun addMany() {
        val b = makeBaseline()
        val m = makeMap()

        val rand = Random(1234)

        for (i in 1..65550) {
            val elem = makeKey(rand.nextInt())
            when (i) {
                1, 255, 256, 257, 65535, 65536, 65537 -> {
                    assertEqualMutation(b, m) { put(elem, elem) }
                }
                else -> {
                    assertEqualResult(b, m) { put(elem, elem) }
                }
            }
            assertEqualResult(b, m) { contains(elem) }
        }
    }

    @Test
    fun nullKeyOrValue() {
        val b = makeBaseline()
        val m = makeMap()

        val obj = makeKey(1)
        val notThere = Object()
        assertEqualResult(b, m) { getOrDefault(obj, notThere) }
        assertEqualResult(b, m) { get(obj) }

        assertEqualMutation(b, m) { put(obj, null) }
        assertEqualResult(b, m) { getOrDefault(obj, notThere) }
        assertEqualResult(b, m) { get(obj) }

        assertEqualMutation(b, m) { put(obj, obj) }
        assertEqualResult(b, m) { getOrDefault(obj, notThere) }
        assertEqualResult(b, m) { get(obj) }

        assertEqualMutation(b, m) { put(obj, null) }

        if (allowNullKeys) {
            assertEqualResult(b, m) { getOrDefault(null, notThere) }
            assertEqualResult(b, m) { get(null) }

            assertEqualMutation(b, m) { put(null, obj) }
            assertEqualResult(b, m) { getOrDefault(null, notThere) }
            assertEqualResult(b, m) { get(null) }

            assertEqualMutation(b, m) { remove(null) }
            assertEqualResult(b, m) { contains(null) }
            assertEqualMutation(b, m) { remove(null) }
            assertEqualResult(b, m) { contains(null) }
        }
    }

    @Test
    fun addNullMapAtEnd() {
        val s = makeMap()
        s.putAll(treapMapOf(makeKey(0) to null))
        s.putAll(treapMapOf(null to null))
        assertEquals(s, mapOf<TestKey?, Any?>(null to null, makeKey(0) to null))
    }

    @Test
    fun addNullMapAtStart() {
        val s = makeMap()
        s.putAll(treapMapOf(null to null))
        s.putAll(treapMapOf(makeKey(0) to null))
        assertEquals(s, mapOf<TestKey?, Any?>(null to null, makeKey(0) to null))
    }

    @Test
    fun addHashKeyAtEnd() {
        val s = makeMap()
        s.put(makeKey(0), null)
        s.put(null, null)
        assertEquals(s, mapOf<TestKey?, Any?>(null to null, makeKey(0) to null))
    }

    @Test
    fun addNullKeyAtStart() {
        val s = makeMap()
        s.put(null, null)
        s.put(makeKey(0), null)
        assertEquals(s, mapOf<TestKey?, Any?>(null to null, makeKey(0) to null))
    }

    @Test
    fun addHashKeyAtStart() {
        val s = makeMap()
        s.put(HashTestKey(0), null)
        s.put(makeKey(0), null)
        assertEquals(s, mapOf<TestKey?, Any?>(HashTestKey(0) to null, makeKey(0) to null))
    }

    @Test
    fun addHashMapAtEnd() {
        val s = makeMap()
        s.putAll(treapMapOf(makeKey(0) to null))
        s.putAll(treapMapOf(HashTestKey(0) to null))
        assertEquals(s, mapOf<TestKey?, Any?>(HashTestKey(0) to null, makeKey(0) to null))
    }

    @Test
    fun addHashMapAtStart() {
        val s = makeMap()
        s.putAll(treapMapOf(HashTestKey(0) to null))
        s.putAll(treapMapOf(makeKey(0) to null))
        assertEquals(s, mapOf<TestKey?, Any?>(HashTestKey(0) to null, makeKey(0) to null))
    }

    @Test
    fun addNullKeyAtEnd() {
        val s = makeMap()
        s.put(makeKey(0), null)
        s.put(HashTestKey(0), null)
        assertEquals(s, mapOf<TestKey?, Any?>(HashTestKey(0) to null, makeKey(0) to null))
    }

    @Test
    fun copyConstructorEmpty() {
        val empty = mapOf<TestKey?, Any?>()
        val b = makeBaseline(empty)
        val m = makeMap(empty)
        assertVeryEqual(b, m)
        assertVeryEqual(empty, m)
    }

    @Test
    fun copyConstructorNonEmpty() {
        val other = mutableMapOf<TestKey?, Any?>()
        val rand = Random(1234)
        val elems = Array<TestKey>(10000) { makeKey(rand.nextInt()) }
        for (elem in elems) {
            other[elem] = Object()
        }

        val b = makeBaseline(other)
        val m = makeMap(other)
        assertVeryEqual(b, m)
        assertVeryEqual(other, m)
    }

    fun removeWhileIterating(toRemove: Int) {
        val b = makeBaseline()
        val m = makeMap()

        for (i in 1..3) {
            val key = makeKey(i)
            assertEqualMutation(b, m) { put(key, null) }
        }

        var count = 0
        val iterator = m.entries.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            if (++count == toRemove) {
                b.remove(e.key)
                iterator.remove()
            }
        }

        assertVeryEqual(b, m)
    }


    @Test
    fun removeFirstWhileIterating() {
        removeWhileIterating(1)
    }

    @Test
    fun removeSecondWhileIterating() {
        removeWhileIterating(2)
    }

    @Test
    fun removeLastWhileIterating() {
        removeWhileIterating(3)
    }

    open fun assertIdenticalJson(expected: String, actual: String) {}
    abstract fun getBaseDeserializer(): DeserializationStrategy<*>?
    abstract fun getDeserializer(): DeserializationStrategy<*>?
    abstract fun makeMapOfInts(): TreapMap<Int?, Int?>
    abstract fun makeMapOfInts(other: Map<Int?, Int?>): TreapMap<Int?, Int?>
    abstract fun makeBaselineOfInts(): MutableMap<Int?, Int?>

    @Test
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    fun kotlinJsonSerialize() {
        if (getDeserializer() == null) {
            return
        }

        val b = makeBaselineOfInts()
        b[1] = 11
        b[9897] = 12
        b[3] = 13
        if (allowNullKeys) {
            b[null] = 14
        }
        b[5] = null

        val m = makeMapOfInts(b)

        val bs = Json.encodeToString(b)
        val ms = Json.encodeToString(m)

        assertIdenticalJson(bs, ms)

        @Suppress("UNCHECKED_CAST")
        val db = Json.decodeFromString(getBaseDeserializer()!!, bs) as Map<TestKey?, Any?>
        @Suppress("UNCHECKED_CAST")
        val dm = Json.decodeFromString(getDeserializer()!!, ms) as Map<TestKey?, Any?>

        assertVeryEqual(db, dm)
    }

    fun testMapOf(vararg pairs: Pair<Int, Int>): TreapMap<Int, Int> {
        @Suppress("UNCHECKED_CAST")
        val m = makeMapOfInts() as TreapMap<Int, Int>
        return m.putAll(pairs.toMap())
    }

    @Test
    fun merge() {
        val merger = { _: Int?, v1: Int?, v2: Int? ->
            (v1?:0) + (v2?:0)
        }

        assertEquals(testMapOf(), testMapOf().merge(testMapOf(), merger))
        assertEquals(testMapOf(1 to 2), testMapOf(1 to 2).merge(testMapOf(), merger))
        assertEquals(testMapOf(1 to 2), testMapOf().merge(testMapOf(1 to 2), merger))

        assertEquals(
            testMapOf(1 to 3, 2 to 3, 3 to 9, 4 to 6),
            testMapOf(1 to 2, 2 to 3, 3 to 4).merge(testMapOf(1 to 1, 3 to 5, 4 to 6), merger))

        val merger2 = { _: Int?, v1: Int?, v2: Int? ->
            when {
                v1 == null || v2 == null -> -1
                else -> v1 + v2
            }
        }

        val m1 = testMapOf(2 to 2, 3 to 3)
        val m2 = testMapOf(3 to 3)
        assertEquals(
            mapOf(2 to -1, 3 to 6),
            m2.merge(m1, merger2))
        assertEquals(
            mapOf(2 to -1, 3 to 6),
            m1.merge(m2, merger2))

        val merger3 = { _: Int?, v1: Int?, v2: Int? ->
            when {
                v1 == null || v2 == null -> -1
                else -> null
            }
        }
        assertEquals(
            mapOf(2 to -1),
            m2.merge(m1, merger3))
        assertEquals(
            mapOf(2 to -1),
            m1.merge(m2, merger3))
    }

    @Test
    fun zip() {
        assertEquals(
            setOf<Map.Entry<Int, Pair<Int?, Int?>>>(),
            testMapOf().zip(testMapOf()).toSet()
        )
        assertEquals(
            setOf(MapEntry(1, null to 2)),
            testMapOf().zip(testMapOf(1 to 2)).toSet()
        )
        assertEquals(
            setOf(MapEntry(1, 2 to null)),
            testMapOf(1 to 2).zip(testMapOf()).toSet()
        )
        assertEquals(
            setOf(MapEntry(1, 1 to null), MapEntry(2, null to 2)),
            testMapOf(1 to 1).zip(testMapOf(2 to 2)).toSet()
        )
        assertEquals(
            setOf(MapEntry(1, 2 to 3)),
            testMapOf(1 to 2).zip(testMapOf(1 to 3)).toSet()
        )
        assertEquals(
            setOf(MapEntry(1, 2 to 3), MapEntry(2, 3 to 4)),
            testMapOf(1 to 2, 2 to 3).zip(testMapOf(1 to 3, 2 to 4)).toSet()
        )
    }
}
