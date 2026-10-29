package com.certora.collect

import com.certora.collect.*
import com.certora.collect.TreapMap.MergeMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
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
    abstract fun makeMap(): TreapMap.Builder<TestKey?, Any?>
    abstract fun makeBaseline(): MutableMap<TestKey?, Any?>
    abstract fun makeMap(other: Map<TestKey?,Any?>): TreapMap.Builder<TestKey?, Any?>
    abstract fun makeBaseline(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?>

    open fun assertOrderedIteration(expected: Iterator<*>, actual: Iterator<*>) { }

    fun assertVeryEqual(expected: Map<*,*>, actual: TreapMap<*,*>) {
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

        val actualForEachEntries = mutableListOf<Map.Entry<*, *>>()
        actual.forEach {
            actualForEachEntries += it
        }
        assertOrderedIteration(expected.entries.iterator(), actualForEachEntries.iterator())

        val actualForEachEntryEntries = mutableListOf<Map.Entry<*, *>>()
        actual.forEachEntry {
            actualForEachEntryEntries += it
        }
        assertOrderedIteration(expected.entries.iterator(), actualForEachEntryEntries.iterator())

        assertEquals(actualForEachEntries, actualForEachEntryEntries)
    }

    fun assertVeryEqual(expected: Map<*, *>, actual: TreapMap.Builder<*, *>) {
        assertVeryEqual(expected, actual.build())
    }

    fun <TResult> assertEqualMutation(baseline: MutableMap<TestKey?, Any?>, map: TreapMap.Builder<TestKey?,Any?>, action: MutableMap<TestKey?,Any?>.() -> TResult) {
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
    fun addNullAndHashKeysByPutAndPutAll() {
        val plain = makeKey(0)
        val hash = HashTestKey(0)
        putSequence("addNullMapAtEnd", plain, null, usePutAll = true)
        putSequence("addNullMapAtStart", null, plain, usePutAll = true)
        putSequence("addNullKeyAtEnd", plain, null, usePutAll = false)
        putSequence("addNullKeyAtStart", null, plain, usePutAll = false)
        putSequence("addHashKeyAtStart", hash, plain, usePutAll = false)
        putSequence("addHashKeyAtEnd", plain, hash, usePutAll = false)
        putSequence("addHashMapAtEnd", plain, hash, usePutAll = true)
        putSequence("addHashMapAtStart", hash, plain, usePutAll = true)
    }

    private fun putSequence(name: String, first: TestKey?, second: TestKey?, usePutAll: Boolean) {
        val map = makeMap()
        for (key in listOf(first, second)) {
            if (usePutAll) {
                map.putAll(treapMapOf(key to null))
            } else {
                map.put(key, null)
            }
        }
        assertEquals(map, mapOf<TestKey?, Any?>(first to null, second to null), name)
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

    @Test
    fun removeWhileIterating() {
        removeNthWhileIterating(1) // first
        removeNthWhileIterating(2) // second
        removeNthWhileIterating(3) // last
    }

    private fun removeNthWhileIterating(toRemove: Int) {
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
        val dm = Json.decodeFromString(getDeserializer()!!, ms) as TreapMap<TestKey?, Any?>

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
    fun mergeIntersectMode() {
        val merger = { _: Int?, v1: Int?, v2: Int? ->
            v1!! + v2!!
        }

        assertEquals(testMapOf(), testMapOf().merge(testMapOf(), MergeMode.INTERSECTION, merger))
        assertEquals(testMapOf(), testMapOf(1 to 2).merge(testMapOf(), MergeMode.INTERSECTION, merger))
        assertEquals(testMapOf(), testMapOf().merge(testMapOf(1 to 2), MergeMode.INTERSECTION, merger))

        assertEquals(
            testMapOf(1 to 3, 3 to 9),
            testMapOf(1 to 2, 2 to 3, 3 to 4).merge(testMapOf(1 to 1, 3 to 5, 4 to 6), MergeMode.INTERSECTION, merger))

        val m1 = testMapOf(2 to 2, 3 to 3)
        val m2 = testMapOf(3 to 3)
        assertEquals(
            mapOf(3 to 6),
            m2.merge(m1, MergeMode.INTERSECTION, merger))
        assertEquals(
            mapOf(3 to 6),
            m1.merge(m2, MergeMode.INTERSECTION, merger))

        val merger3 = { _: Int?, _: Int?, _: Int? -> null }
        assertEquals(
            mapOf(),
            m2.merge(m1, MergeMode.INTERSECTION, merger3))
        assertEquals(
            mapOf(),
            m1.merge(m2, MergeMode.INTERSECTION, merger3))
    }

    @Test
    fun union() {
        assertEquals(testMapOf(), testMapOf().union(testMapOf()) { _, a, _ -> a })
        assertEquals(testMapOf(1 to 2), testMapOf(1 to 2).union(testMapOf()) { _, a, _ -> a })
        assertEquals(testMapOf(1 to 2), testMapOf().union(testMapOf(1 to 2)) { _, a, _ -> a })
        assertEquals(
            testMapOf(1 to 2, 2 to 3, 3 to 4),
            testMapOf(1 to 2, 2 to 3).union(testMapOf(2 to 3, 3 to 4)) { _, a, _ -> a }
        )

        val m1 = testMapOf(2 to 2, 3 to 3)
        val m2 = testMapOf(3 to 4)
        assertEquals(
            mapOf(2 to 2, 3 to 3),
            m1.union(m2) { _, a, _ -> a }
        )
        assertEquals(
            mapOf(2 to 2, 3 to 4),
            m2.union(m1) { _, a, _ -> a }
        )
        assertEquals(
            mapOf(2 to 2, 3 to 4),
            m1.union(m2) { _, _, b -> b }
        )
        assertEquals(
            mapOf(2 to 2, 3 to 3),
            m2.union(m1) { _, _, b -> b }
        )
    }

    @Test
    fun intersect() {
        assertEquals(testMapOf(), testMapOf().intersect(testMapOf()) { _, a, _ -> a })
        assertEquals(testMapOf(), testMapOf(1 to 2).intersect(testMapOf()) { _, a, _ -> a })
        assertEquals(testMapOf(), testMapOf().intersect(testMapOf(1 to 2)) { _, a, _ -> a })
        assertEquals(
            testMapOf(2 to 3),
            testMapOf(1 to 2, 2 to 3).intersect(testMapOf(2 to 3, 3 to 4)) { _, a, _ -> a }
        )

        val m1 = testMapOf(2 to 2, 3 to 3)
        val m2 = testMapOf(3 to 4)
        assertEquals(
            mapOf(3 to 3),
            m1.intersect(m2) { _, a, _ -> a }
        )
        assertEquals(
            mapOf(3 to 4),
            m2.intersect(m1) { _, a, _ -> a }
        )
        assertEquals(
            mapOf(3 to 4),
            m1.intersect(m2) { _, _, b -> b }
        )
        assertEquals(
            mapOf(3 to 3),
            m2.intersect(m1) { _, _, b -> b }
        )
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

    @Test
    fun arbitraryOrNull() {
        val m = makeMap()
        assertNull(m.build().arbitraryOrNull())

        m[makeKey(1, 1)] = 1
        assertEquals(1, m.build().arbitraryOrNull()!!.value)

        m[makeKey(2, 1)] = 2
        assertTrue(m.build().arbitraryOrNull()!!.value in 1..2)

        for (it in 3..100) {
            m[makeKey(it)] = it
        }
        assertTrue(m.build().arbitraryOrNull()!!.value in 1..100)
    }

    private fun intMap(keys: Iterable<Int>): TreapMap<TestKey, Int> {
        var map = treapMapOf<TestKey, Int>()
        for (key in keys) {
            map = map.put(makeKey(key), key)
        }
        return map
    }

    private fun <T> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream()
        ObjectOutputStream(bytes).use { it.writeObject(value) }
        @Suppress("UNCHECKED_CAST")
        return ObjectInputStream(ByteArrayInputStream(bytes.toByteArray())).use { it.readObject() as T }
    }

    @Test
    fun singleAndSingleOrNull() {
        val empty = treapMapOf<TestKey, Int>()
        assertFailsWith<NoSuchElementException> { empty.single() }
        assertNull(empty.singleOrNull())
        assertNull(empty.arbitraryOrNull())
        assertEquals(intMap(listOf(3)), empty.updateEntry(makeKey(3), 3) { current, extra ->
            assertNull(current)
            extra
        })
        assertSame(empty, empty.updateEntry(makeKey(3), 1) { _, _ -> null })
        assertNull(empty.mapReduce({ _, v -> v }, Int::plus))
        assertNull(empty.parallelMapReduce({ _, v -> v }, Int::plus, 1))
        assertEquals(empty, empty.updateValues { _, v -> v })
        assertEquals(empty, empty.parallelUpdateValues(1) { _, v -> v })

        val one = intMap(listOf(1))
        assertEquals(makeKey(1), one.single().key)
        assertEquals(1, one.single().value)
        assertEquals(1, one.singleOrNull()!!.value)
        assertEquals(makeKey(1), one.keys.single())

        val two = intMap(listOf(1, 2))
        assertFailsWith<IllegalArgumentException> { two.single() }
        assertNull(two.singleOrNull())
        assertFailsWith<IllegalArgumentException> { two.keys.single() }
        assertTrue(two.arbitraryOrNull()!!.value in 1..2)
    }

    @Test
    fun removeOnlyMatchingValue() {
        val map = intMap(listOf(1, 2, 3))
        assertEquals(map, map.remove(makeKey(1), 99))
        assertEquals(map, map.remove(makeKey(99), 1))
        val removed = map.remove(makeKey(2), 2)
        assertFalse(makeKey(2) in removed)
        assertEquals(1, removed[makeKey(1)])
        assertEquals(3, removed[makeKey(3)])

        var nullValues = treapMapOf<TestKey, String?>()
        nullValues = nullValues.put(makeKey(1), null).put(makeKey(2), "x")
        assertEquals(nullValues, nullValues.remove(makeKey(1), "x"))
        assertFalse(makeKey(1) in nullValues.remove(makeKey(1), null))
        assertEquals("x", nullValues.remove(makeKey(1), null)[makeKey(2)])
    }

    @Test
    fun updateEntryAndValues() {
        val map = intMap(1..20)
        val updated = map.updateEntry(makeKey(4), 10) { current, extra -> current!! + extra }
        assertEquals(14, updated[makeKey(4)])
        assertEquals(map.size, updated.size)

        val inserted = map.updateEntry(makeKey(100), 7) { current, extra ->
            assertNull(current)
            extra
        }
        assertEquals(7, inserted[makeKey(100)])

        val removed = map.updateEntry(makeKey(4), 0) { _, _ -> null }
        assertFalse(makeKey(4) in removed)
        assertEquals(19, removed.size)

        assertEquals(map, map.updateEntry(makeKey(100), 1) { current, _ -> current })

        val transformed = map.updateValues { _, value ->
            if (value % 2 == 0) {
                value * 10
            } else {
                null
            }
        }
        val parallel = map.parallelUpdateValues(1) { _, value ->
            if (value % 2 == 0) {
                value * 10
            } else {
                null
            }
        }
        assertEquals(transformed, parallel)
        assertEquals((1..20).filter { it % 2 == 0 }.map { makeKey(it) to it * 10 }.toMap(), transformed)
        assertEquals(transformed, transformed.updateValues { _, value -> value })
    }

    @Test
    fun parallelBulkOperationsMatchSequential() {
        val left = intMap(1..40)
        val right = intMap(20..60)
        val merge = { _: TestKey, a: Int?, b: Int? -> (a ?: 0) + (b ?: 0) }
        assertEquals(left.merge(right, merge), left.parallelMerge(right, 1, merge))
        assertEquals(
            left.merge(right, MergeMode.INTERSECTION, merge),
            left.parallelMerge(right, MergeMode.INTERSECTION, 1, merge)
        )
        val keepLeft = { _: TestKey, a: Int, _: Int -> a }
        val keepRight = { _: TestKey, _: Int, b: Int -> b }
        assertEquals(left.union(right, keepRight), left.parallelUnion(right, 1, keepRight))
        assertEquals(left.intersect(right, keepLeft), left.parallelIntersect(right, 1, keepLeft))

        val plain = hashMapOf(makeKey(40) to 400, makeKey(70) to 70)
        assertEquals(left.union(plain, keepRight), left.put(makeKey(40), 400).put(makeKey(70), 70))
        assertEquals(treapMapOf(makeKey(40) to 40), left.intersect(plain, keepLeft))
        assertEquals(
            left.merge(plain, merge),
            left.put(makeKey(40), 440).put(makeKey(70), 70)
        )

        assertEquals((1..40).sum(), left.mapReduce({ _, value -> value }, Int::plus))
        assertEquals((1..40).sum(), left.parallelMapReduce({ _, value -> value }, Int::plus, 1))
        assertEquals(left.keys.sumOf { it.hashCode() }, left.keys.mapReduce({ key -> key.hashCode() }, Int::plus))
        assertEquals(
            left.keys.mapReduce({ key -> key.hashCode() }, Int::plus),
            left.keys.parallelMapReduce({ key -> key.hashCode() }, Int::plus, 1)
        )
    }

    @Test
    fun emptyBulkOperations() {
        val empty = treapMapOf<TestKey, Int>()
        val other = intMap(listOf(1, 2))
        assertSame(other, empty.putAll(other))
        assertSame(other, empty.union(other) { _, a, _ -> a })
        assertSame(other, empty.parallelUnion(other, 1) { _, a, _ -> a })
        assertSame(empty, empty.intersect(other) { _, a, _ -> a })
        assertSame(empty, empty.parallelIntersect(other, 1) { _, a, _ -> a })
        assertEquals(other, empty.merge(other) { _, _, b -> b })
        assertEquals(empty, empty.merge(other, MergeMode.INTERSECTION) { _, a, b -> (a ?: 0) + (b ?: 0) })
        assertEquals(other, empty.parallelMerge(other, 1) { _, _, b -> b })
        assertEquals(setOf(MapEntry(makeKey(1), null to 1), MapEntry(makeKey(2), null to 2)), empty.zip(other).toSet())

        val builder = treapMapBuilderOf(makeKey(3) to 3)
        assertEquals(mapOf(makeKey(3) to 3), empty.putAll(builder))
        assertEquals(empty, empty.putAll(emptyMap()))
    }

    @Test
    fun predicatesKeysAndOperators() {
        val map = intMap(1..8)
        assertEquals(intMap(listOf(2, 4, 6, 8)), map.retainAll { it.value % 2 == 0 })
        assertEquals(intMap(listOf(1, 3, 5, 7)), map.removeAll { it.value % 2 == 0 })
        assertEquals(intMap(listOf(1, 2)), map.retainAllKeys { it == makeKey(1) || it == makeKey(2) })
        assertEquals(intMap(3..8), map.removeAllKeys { it == makeKey(1) || it == makeKey(2) })
        assertEquals(intMap(listOf(2, 4, 6, 8)), map.retainAllValues { it % 2 == 0 })
        assertEquals(intMap(listOf(1, 3, 5, 7)), map.removeAllValues { it % 2 == 0 })
        assertTrue(map.containsValue(3))
        assertFalse(map.containsValue(30))

        assertEquals(mapOf(makeKey(10) to 1, makeKey(20) to 2), map.retainAllKeys { it == makeKey(1) || it == makeKey(2) }.mapKeys { makeKey(it.value * 10) })
        assertEquals(mapOf(makeKey(1) to 10, makeKey(2) to 20), intMap(listOf(1, 2)).mapValues { it.value * 10 })

        assertEquals(intMap(listOf(2, 3)), map - makeKey(1) - listOf(makeKey(4)) - arrayOf(makeKey(5)) - sequenceOf(makeKey(6), makeKey(7), makeKey(8)))
        assertEquals(intMap(listOf(1, 2, 9)), map.retainAllKeys { it == makeKey(1) || it == makeKey(2) } + (makeKey(9) to 9))
        assertEquals(intMap(1..8), (map - makeKey(8)) + arrayOf(makeKey(8) to 8))
        assertEquals(intMap(1..8), (map - makeKey(8)) + sequenceOf(makeKey(8) to 8))
        assertEquals(intMap(1..8), (map - makeKey(8)) + listOf(makeKey(8) to 8))

        val keys = map.keys
        assertEquals((1..8).map { makeKey(it) }.toSet(), keys)
        assertTrue(keys.containsAll((1..8).map { makeKey(it) }))
        assertTrue(keys.containsAny(listOf(makeKey(100), makeKey(3))))
        assertFalse(keys.containsAny(listOf(makeKey(100))))
        assertTrue(keys.containsAny { it == makeKey(4) })
        val original = makeKey(4)
        assertSame(original, treapMapOf(original to 4).keys.findEqual(makeKey(4, code = original.hashCode())))
        val seen = mutableListOf<TestKey>()
        keys.forEachElement { seen += it }
        assertEquals(keys.toSet(), seen.toSet())
        assertEquals(setOf(makeKey(2)), keys.retainAll(listOf(makeKey(2), makeKey(9))))
        assertEquals(setOf(makeKey(1)), keys.removeAll { it != makeKey(1) })
        assertEquals(keys + makeKey(9), keys.add(makeKey(9)))
        assertSame(keys, keys.orEmpty())
        val absent: TreapSet<TestKey>? = null
        assertTrue(absent.orEmpty().isEmpty())

        assertEquals(map, (1..8).map { makeKey(it) to it }.toMap().toTreapMap())
        assertEquals(map, (1..8).map { makeKey(it) to it }.toTreapMap())
        val nullable: TreapMap<TestKey, Int>? = map
        assertSame(map, nullable.orEmpty())
        assertTrue((null as TreapMap<TestKey, Int>?).orEmpty().isEmpty())
        assertEquals("{}" , treapMapOf<TestKey, Int>().toString())
        assertTrue(map.toString().contains(makeKey(1).toString()))
    }

    @Test
    fun javaSerialize() {
        val stored = treapMapOf<TestKey?, Int?>()
            .put(makeKey(1), 1)
            .put(makeKey(4), null)
            .put(makeKey(9), 9)
        if (allowNullKeys) {
            val withNullKey = stored.put(null, 3)
            assertEquals(withNullKey, roundTrip(withNullKey))
        }
        assertEquals(stored, roundTrip(stored))
        assertEquals(stored.hashCode(), roundTrip(stored).hashCode())
        assertEquals(stored, roundTrip(stored.builder()).build())
    }
}
