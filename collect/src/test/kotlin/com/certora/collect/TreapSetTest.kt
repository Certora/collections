package com.certora.collect

import java.util.Random
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.DeserializationStrategy

/** Tests for [TreapSet]. */
abstract class TreapSetTest {

    abstract fun makeKey(value: Int, code: Int = value.hashCode()): TestKey
    open val nullKeysAllowed: Boolean get() = true

    fun makeSet(): TreapSet.Builder<TestKey?> = treapSetOf<TestKey?>().builder()
    abstract fun makeBaseline(): MutableSet<TestKey?>

    fun makeSet(other: Collection<TestKey?>): TreapSet.Builder<TestKey?> = makeSet().also { it += other }
    fun makeBaseline(other: Collection<TestKey?>): MutableSet<TestKey?> = makeBaseline().also { it += other }

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
            baseline: MutableSet<TestKey?>,
            set: MutableSet<TestKey?>,
            action: MutableSet<TestKey?>.() -> TResult
    ) {
        assertEqualResult(baseline, set, action)
        assertVeryEqual(baseline, set)
    }

    fun <TResult> assertEqualResult(
            baseline: MutableSet<TestKey?>,
            set: MutableSet<TestKey?>,
            action: MutableSet<TestKey?>.() -> TResult
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

        val o1 = makeKey(5)
        assertEqualResult(b, s) { contains(o1) }
        assertEqualMutation(b, s) { add(o1) }
        assertEqualResult(b, s) { contains(o1) }

        assertEqualMutation(b, s) { add(o1) }
        assertEqualResult(b, s) { contains(o1) }

        val o2 = makeKey(3)
        assertEqualResult(b, s) { contains(o2) }
        assertEqualMutation(b, s) { add(o2) }
        assertEqualResult(b, s) { contains(o2) }

        assertEqualMutation(b, s) { add(o2) }
        assertEqualResult(b, s) { contains(o2) }

        assertEqualResult(b, s) { contains(o1) }
        assertEqualMutation(b, s) { add(o1) }
        assertEqualResult(b, s) { contains(o1) }

        val o3 = makeKey(4)
        assertEqualResult(b, s) { contains(o3) }
        assertEqualMutation(b, s) { add(o3) }
        assertEqualResult(b, s) { contains(o3) }

        val o4 = makeKey(-908857765)
        assertEqualResult(b, s) { contains(o4) }
        assertEqualMutation(b, s) { add(o4) }
        assertEqualResult(b, s) { contains(o4) }
    }

    @Test
    fun remove() {
        val b = makeBaseline()
        val s = makeSet()

        val objs = Array<TestKey>(5) { makeKey(it, code = 0) }
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

    @Test
    fun clear() {
        val s = makeSet()
        val k = makeKey(1)
        s.add(k)
        assertEquals(1, s.size)
        assertTrue(k in s)
        s.clear()
        assertEquals(0, s.size)
        assertFalse(k in s)
    }

    fun randomHashObjectMaybeNull(rand: Random): TestKey? {
        val r = rand.nextInt()
        if (nullKeysAllowed) {
            if (r % 20 == 0) {
                return null
            }
        }
        return makeKey(r % 2000)
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

            assertEqualMutation(b, s) { add(makeKey(0)) }
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
    fun addNullAndHashElementsByAddAndAddAll() {
        val plain = makeKey(0)
        val hash = HashTestKey(0)
        addSequence("addNullSetAtEnd", plain, null, useAddAll = true)
        addSequence("addNullSetAtStart", null, plain, useAddAll = true)
        addSequence("addNullElementAtEnd", plain, null, useAddAll = false)
        addSequence("addNullElementAtStart", null, plain, useAddAll = false)
        addSequence("addHashElementAtEnd", plain, hash, useAddAll = false)
        addSequence("addHashElementAtStart", hash, plain, useAddAll = false)
        addSequence("addHashSetAtEnd", plain, hash, useAddAll = true)
        addSequence("addHashSetAtStart", hash, plain, useAddAll = true)
    }

    private fun addSequence(name: String, first: TestKey?, second: TestKey?, useAddAll: Boolean) {
        val set = makeSet()
        for (element in listOf(first, second)) {
            if (useAddAll) {
                set.addAll(treapSetOf(element))
            } else {
                set.add(element)
            }
        }
        assertEquals(set, setOf<TestKey?>(first, second), name)
    }

    @Test
    fun nullQueries() {
        val withNull = makeSet()
        withNull.add(makeKey(0))
        withNull.add(null)

        val withoutNull = makeSet()
        withoutNull.add(makeKey(0))

        assertTrue(null in withNull)
        assertFalse(null in withoutNull)

        assertTrue(withNull.containsAll(treapSetOf(null)))
        assertFalse(withoutNull.containsAll(treapSetOf(null)))

        assertTrue(withNull.remove(null))
        assertFalse(withoutNull.remove(null))
    }

    @Test
    fun hashQueries() {
        val probe = HashTestKey(0)
        val withHash = makeSet()
        withHash.add(makeKey(0))
        withHash.add(probe)

        val withoutHash = makeSet()
        withoutHash.add(makeKey(0))

        // On hash sets makeKey(0) is a HashTestKey and equals probe. On sorted sets it is a
        // ComparableTestKey, so probe is a different element.
        val probeEqualsMakeKey = makeKey(0) == probe
        assertTrue(probe in withHash)
        assertEquals(probeEqualsMakeKey, probe in withoutHash)

        assertTrue(withHash.containsAll(treapSetOf(probe)))
        assertEquals(probeEqualsMakeKey, withoutHash.containsAll(treapSetOf(probe)))

        assertTrue(withHash.remove(probe))
        assertEquals(probeEqualsMakeKey, withoutHash.remove(probe))
    }

    @Test
    fun retainAll() {
        val b = makeBaseline()
        val s = makeSet()

        assertEqualMutation(b, s) { addAll((1..4).map { makeKey(it) }) }
        assertEqualMutation(b, s) { retainAll((1..4).map { makeKey(it) }) }

        assertEqualMutation(b, s) { retainAll((3..6).map { makeKey(it) }) }

        assertEqualMutation(b, s) { retainAll((5..100).map { makeKey(it) }) }

        assertTrue(s.isEmpty())
    }

    @Test
    fun copyFromSetAndList() {
        assertCopiedFrom(setOf<TestKey?>())
        assertCopiedFrom(listOf<TestKey?>())

        val rand = Random(1234)
        val generated = Array<TestKey>(10000) { makeKey(rand.nextInt()) }.toList()
        assertCopiedFrom(makeBaseline(generated))
        assertCopiedFrom(generated)
    }

    private fun assertCopiedFrom(source: Collection<TestKey?>) {
        val baseline = makeBaseline(source)
        val set = makeSet(source)
        assertVeryEqual(baseline, set)
        if (source is Set<*>) {
            assertVeryEqual(source, set)
        }
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
        b.add(makeKey(1))
        b.add(makeKey(9897))
        b.add(makeKey(3))
        b.add(null)
        b.add(makeKey(5))

        val s = makeSet(b)

        val bs = Json.encodeToString(b)
        val ss = Json.encodeToString(s)

        assertIdenticalJson(bs, ss)

        @Suppress("UNCHECKED_CAST")
        val db = Json.decodeFromString(getBaseDeserializer()!!, bs) as Set<TestKey?>
        @Suppress("UNCHECKED_CAST")
        val ds = Json.decodeFromString(getDeserializer()!!, ss) as Set<TestKey?>

        assertVeryEqual(db, ds)
    }

    fun roundTripSerialize(set: MutableSet<TestKey?>): MutableSet<TestKey?> {
        val bos = ByteArrayOutputStream()
        val oos = ObjectOutputStream(bos)
        oos.writeObject(set)
        oos.flush()

        val bytes = bos.toByteArray()
        val bis = ByteArrayInputStream(bytes)
        val ois = ObjectInputStream(bis)

        @Suppress("UNCHECKED_CAST")
        return ois.readObject() as MutableSet<TestKey?>
    }

    @Test
    fun javaSerialize() {
        val b = makeBaseline()
        b.add(makeKey(1))
        b.add(makeKey(9897))
        b.add(makeKey(3))
        if (nullKeysAllowed) {
            b.add(null)
        }
        b.add(makeKey(5))

        val s = makeSet(b)

        val rb = roundTripSerialize(b)
        val rs = roundTripSerialize(s)

        assertVeryEqual(b, rb)
        assertVeryEqual(s, rs)
        assertVeryEqual(b, rs)
    }

    @Test
    fun arbitraryOrNull() {
        val s = makeSet()
        assertNull(s.build().arbitraryOrNull())

        s += makeKey(1, 1)
        assertEquals(makeKey(1, 1), s.build().arbitraryOrNull())

        s += makeKey(2, 1)
        assertTrue(s.build().arbitraryOrNull() in (1..2).map { makeKey(it) })

        for (it in 3..100) {
            s += makeKey(it)
        }
        assertTrue(s.build().arbitraryOrNull() in (1..100).map { makeKey(it) })
    }

    private fun keySet(keys: Iterable<Int>): TreapSet<TestKey> {
        var set = treapSetOf<TestKey>()
        for (key in keys) {
            set = set.add(makeKey(key))
        }
        return set
    }

    @Test
    fun singleFindAndTraversal() {
        val empty = treapSetOf<TestKey>()
        assertFailsWith<NoSuchElementException> { empty.single() }
        assertNull(empty.singleOrNull())
        assertNull(empty.arbitraryOrNull())
        assertNull(empty.mapReduce({ it.hashCode() }, Int::plus))
        assertNull(empty.parallelMapReduce({ it.hashCode() }, Int::plus, 1))
        assertFalse(empty.containsAny(listOf(makeKey(1))))
        assertFalse(empty.containsAny { true })
        assertNull(empty.findEqual(makeKey(1)))

        val one = keySet(listOf(1))
        assertEquals(makeKey(1), one.single())
        assertEquals(makeKey(1), one.singleOrNull())
        val stored = makeKey(4, code = 11)
        val probe = makeKey(4, code = 11)
        assertSame(stored, one.add(stored).findEqual(probe))

        val many = keySet(1..30)
        assertFailsWith<IllegalArgumentException> { many.single() }
        assertNull(many.singleOrNull())
        val seen = mutableListOf<TestKey>()
        many.forEachElement { seen += it }
        assertEquals(many.toSet(), seen.toSet())
        assertEquals(many.sumOf { it.hashCode() }, many.mapReduce({ it.hashCode() }, Int::plus))
        assertEquals(many.mapReduce({ it.hashCode() }, Int::plus), many.parallelMapReduce({ it.hashCode() }, Int::plus, 1))
        assertTrue(many.containsAny(listOf(makeKey(100), makeKey(7))))
        assertFalse(many.containsAny(listOf(makeKey(100), makeKey(200))))
        assertTrue(many.containsAny(keySet(listOf(100, 7))))
        assertTrue(many.containsAny(keySet(listOf(1))))
        assertTrue(many.containsAny(keySet(listOf(30))))
        assertFalse(many.containsAny(keySet(100..110)))
        assertTrue(many.containsAny { it == makeKey(9) })
        assertFalse(many.containsAny { it == makeKey(100) })
    }

    @Test
    fun algebraAndPredicates() {
        val left = keySet(1..40)
        val right = keySet(20..60)
        assertEquals(keySet(1..60), left union right)
        assertEquals(keySet(20..40), left intersect right)
        assertEquals(keySet(1..19), left subtract right)
        assertEquals(keySet(1..60), left union (20..60).map { makeKey(it) })
        assertEquals(keySet(20..40), left intersect (20..60).map { makeKey(it) })
        assertEquals(keySet(1..19), left - (20..60).asSequence().map { makeKey(it) })
        assertEquals(keySet(1..41), left + makeKey(41) + arrayOf(makeKey(41)) + sequenceOf(makeKey(42)) - makeKey(42))
        assertEquals(
            keySet(listOf(2, 4)),
            left.retainAll { it == makeKey(1) || it == makeKey(2) || it == makeKey(3) || it == makeKey(4) }
                .removeAll { it == makeKey(1) || it == makeKey(3) }
        )
        assertEquals(setOf(1, 2), treapSetOf(1, 2).filterIsInstance<Int>())
        assertEquals(emptySet<String>(), treapSetOf(1, 2).filterIsInstance<String>())
        assertEquals(treapSetOf(1), treapSetOfNotNull(1))
        assertTrue(treapSetOfNotNull(null as Int?).isEmpty())
        assertEquals(setOf(1, 2), treapSetOfNotNull(1, null, 2))
        assertEquals(setOf('a', 'b'), "ab".toTreapSet())
        assertEquals(keySet(listOf(1, 2)), sequenceOf(makeKey(1), makeKey(2)).toTreapSet())
        assertEquals(keySet(listOf(1, 2)), listOf(makeKey(1), makeKey(2)).toTreapSet())
        val present: TreapSet<TestKey>? = left
        assertSame(left, present.orEmpty())
        assertTrue((null as TreapSet<Int>?).orEmpty().isEmpty())
        assertEquals(left, left.mutate { it.add(makeKey(1)) })
        assertEquals(left + makeKey(99), left.mutate { it.add(makeKey(99)) })
    }

    @Test
    fun builderIteratorRemove() {
        val set = keySet(1..20).builder()
        val iterator = set.iterator()
        val removed = mutableSetOf<TestKey>()
        var index = 0
        while (iterator.hasNext()) {
            val element = iterator.next()
            if (index % 2 == 0) {
                iterator.remove()
                removed += element
            }
            index++
        }
        val built = set.build()
        for (key in (1..20).map { makeKey(it) }) {
            assertEquals(key !in removed, key in built)
        }
        assertFailsWith<IllegalStateException> { set.iterator().remove() }
    }
}

