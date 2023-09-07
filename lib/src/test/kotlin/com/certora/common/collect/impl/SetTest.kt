package com.certora.common.collect

import java.util.Random
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.DeserializationStrategy

abstract class SetTest {

    open val nullKeysAllowed: Boolean get() = true
    abstract fun makeSet(): MutableSet<HashTestObject?>
    abstract fun makeBaseline(): MutableSet<HashTestObject?>
    abstract fun makeSet(other: Collection<HashTestObject?>): MutableSet<HashTestObject?>
    abstract fun makeBaseline(other: Collection<HashTestObject?>): MutableSet<HashTestObject?>

    open fun assertOrderedIteration(expected: Iterator<*>, actual: Iterator<*>) {}

    fun assertVeryEqual(expected: Set<*>, actual: Set<*>) {
        assertEquals(expected, actual)
        assertTrue(actual.equals(expected))
        assertEquals(expected.hashCode(), actual.hashCode())
        assertEquals(expected.size, actual.size)
        assertEquals(expected.isEmpty(), actual.isEmpty())
        assertOrderedIteration(expected.iterator(), actual.iterator())
    }

    fun <TResult> assertEqualMutation(
            baseline: MutableSet<HashTestObject?>,
            set: MutableSet<HashTestObject?>,
            action: MutableSet<HashTestObject?>.() -> TResult
    ) {
        assertEqualResult(baseline, set, action)
        assertVeryEqual(baseline, set)
    }

    fun <TResult> assertEqualResult(
            baseline: MutableSet<HashTestObject?>,
            set: MutableSet<HashTestObject?>,
            action: MutableSet<HashTestObject?>.() -> TResult
    ) {
        assertEquals(baseline.action(), set.action())
    }

    @Test
    fun construct() {
        val b = makeBaseline()
        val s = makeSet()
        assertVeryEqual(b, s)
    }

    @Test
    fun addAndContains() {
        val b = makeBaseline()
        val s = makeSet()

        val o1 = HashTestObject(5)
        assertEqualResult(b, s) { contains(o1) }
        assertEqualMutation(b, s) { add(o1) }
        assertEqualResult(b, s) { contains(o1) }

        assertEqualMutation(b, s) { add(o1) }
        assertEqualResult(b, s) { contains(o1) }

        val o2 = HashTestObject(3)
        assertEqualResult(b, s) { contains(o2) }
        assertEqualMutation(b, s) { add(o2) }
        assertEqualResult(b, s) { contains(o2) }

        assertEqualMutation(b, s) { add(o2) }
        assertEqualResult(b, s) { contains(o2) }

        assertEqualResult(b, s) { contains(o1) }
        assertEqualMutation(b, s) { add(o1) }
        assertEqualResult(b, s) { contains(o1) }

        val o3 = HashTestObject(4)
        assertEqualResult(b, s) { contains(o3) }
        assertEqualMutation(b, s) { add(o3) }
        assertEqualResult(b, s) { contains(o3) }

        val o4 = HashTestObject(-908857765)
        assertEqualResult(b, s) { contains(o4) }
        assertEqualMutation(b, s) { add(o4) }
        assertEqualResult(b, s) { contains(o4) }
    }

    @Test
    fun remove() {
        val b = makeBaseline()
        val s = makeSet()

        val objs = Array<HashTestObject>(5) { HashTestObject(it, code = 0) }
        for (o in objs) {
            assertEqualMutation(b, s) { add(o) }
            assertEqualResult(b, s) { contains(o) }
        }

        assertEqualMutation(b, s) { remove(objs[0]) }
        assertEqualResult(b, s) { contains(objs[0]) }

        assertEqualMutation(b, s) { remove(objs[1]) }
        assertEqualResult(b, s) { contains(objs[1]) }

        assertEqualMutation(b, s) { remove(objs[4]) }
        assertEqualResult(b, s) { contains(objs[4]) }

        assertEqualMutation(b, s) { remove(objs[2]) }
        assertEqualResult(b, s) { contains(objs[2]) }

        assertEqualMutation(b, s) { remove(objs[3]) }
        assertEqualResult(b, s) { contains(objs[3]) }

        for (o in objs) {
            assertEqualMutation(b, s) { add(o) }
            assertEqualResult(b, s) { contains(o) }
        }
    }

    fun randomHashObjectMaybeNull(rand: Random): HashTestObject? {
        val r = rand.nextInt()
        if (nullKeysAllowed) {
            if (r % 20 == 0) {
                return null
            }
        }
        return HashTestObject(r % 2000)
    }

    @Test
    fun addMany() {
        val b = makeBaseline()
        val s = makeSet()

        val rand = Random(1234)

        for (i in 1..65550) {
            val elem = randomHashObjectMaybeNull(rand)
            when (i) {
                1, 255, 256, 257, 65535, 65536, 65537 -> {
                    assertEqualMutation(b, s) { add(elem) }
                }
                else -> {
                    assertEqualResult(b, s) { add(elem) }
                }
            }
            assertEqualResult(b, s) { contains(elem) }
        }
    }

    @Test
    fun nullKey() {
        if (nullKeysAllowed) {
            val b = makeBaseline()
            val s = makeSet()

            assertEqualMutation(b, s) { add(HashTestObject(0)) }
            assertEqualResult(b, s) { contains(null) }
            assertEqualMutation(b, s) { add(null) }
            assertEqualMutation(b, s) { add(null) }
            assertEqualResult(b, s) { contains(null) }

            assertEqualMutation(b, s) { remove(null) }
            assertEqualResult(b, s) { contains(null) }
            assertEqualMutation(b, s) { remove(null) }
            assertEqualResult(b, s) { contains(null) }
        }
    }

    @Test
    fun retainAll() {
        val b = makeBaseline()
        val s = makeSet()

        assertEqualMutation(b, s) { addAll((1..4).map { HashTestObject(it) }) }
        assertEqualMutation(b, s) { retainAll((1..4).map { HashTestObject(it) }) }

        assertEqualMutation(b, s) { retainAll((3..6).map { HashTestObject(it) }) }

        assertEqualMutation(b, s) { retainAll((5..100).map { HashTestObject(it) }) }

        assertTrue(s.isEmpty())
    }

    @Test
    fun copyConstructorEmptySet() {
        val empty = setOf<HashTestObject?>()
        val b = makeBaseline(empty)
        val s = makeSet(empty)
        assertVeryEqual(b, s)
        assertVeryEqual(empty, s)
    }

    @Test
    fun copyConstructorEmptyList() {
        val empty = listOf<HashTestObject?>()
        val b = makeBaseline(empty)
        val s = makeSet(empty)
        assertVeryEqual(b, s)
    }

    @Test
    fun copyConstructorNonEmptySet() {
        val rand = Random(1234)
        val elems = Array<HashTestObject>(10000) { HashTestObject(rand.nextInt()) }
        val list = elems.toList()
        val other = makeBaseline(list)

        val b = makeBaseline(other)
        val s = makeSet(other)
        assertVeryEqual(b, s)
        assertVeryEqual(other, s)
    }

    @Test
    fun copyConstructorNonEmptyList() {
        val rand = Random(1234)
        val elems = Array<HashTestObject>(10000) { HashTestObject(rand.nextInt()) }
        val other = elems.toList()

        val b = makeBaseline(other)
        val s = makeSet(other)
        assertVeryEqual(b, s)
    }

    open fun assertIdenticalJson(expected: String, actual: String) {}
    abstract fun getBaseDeserializer(): DeserializationStrategy<*>?
    abstract fun getDeserializer(): DeserializationStrategy<*>?

    @Test
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    fun kotlinJsonSerialize() {
        if (getDeserializer() == null) {
            return
        }
        val b = makeBaseline()
        b.add(HashTestObject(1))
        b.add(HashTestObject(9897))
        b.add(HashTestObject(3))
        if (nullKeysAllowed) {
            b.add(null)
        }
        b.add(HashTestObject(5))

        val s = makeSet(b)

        val bs = Json.encodeToString(b)
        val ss = Json.encodeToString(s)

        assertIdenticalJson(bs, ss)

        @Suppress("UNCHECKED_CAST")
        val db = Json.decodeFromString(getBaseDeserializer()!!, bs) as Set<HashTestObject?>
        @Suppress("UNCHECKED_CAST")
        val ds = Json.decodeFromString(getDeserializer()!!, ss) as Set<HashTestObject?>

        assertVeryEqual(db, ds)
    }

    fun roundTripSerialize(set: MutableSet<HashTestObject?>): MutableSet<HashTestObject?> {
        val bos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bos)
        oos.writeObject(set)
        oos.flush()

        val bytes = bos.toByteArray()
        val bis = ByteArrayInputStream(bytes)
        val ois = ObjectInputStream(bis)

        @Suppress("UNCHECKED_CAST")
        return ois.readObject() as MutableSet<HashTestObject?>
    }

    @Test
    fun javaSerialize() {
        val b = makeBaseline()
        b.add(HashTestObject(1))
        b.add(HashTestObject(9897))
        b.add(HashTestObject(3))
        if (nullKeysAllowed) {
            b.add(null)
        }
        b.add(HashTestObject(5))

        val s = makeSet(b)

        val rb = roundTripSerialize(b)
        val rs = roundTripSerialize(s)

        assertVeryEqual(b, rb)
        assertVeryEqual(s, rs)
        assertVeryEqual(b, rs)
    }
}

