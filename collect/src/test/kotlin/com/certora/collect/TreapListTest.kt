package com.certora.collect

import kotlin.random.Random
import kotlin.test.*
import kotlinx.collections.immutable.*
import org.junit.Rule

/** Tests for [TreapList]. */
class TreapListTest {
    @field:Rule
    @JvmField
    val treapListPriorities = TreapListPriorityRule()

    @Test
    fun empty() {
        val empty = treapListOf<Int>()
        val emptyList = persistentListOf<Int>()

        assertTrue(empty.isEmpty())
        assertTrue(empty.equals(emptyList))
        assertSame<TreapList<*>>(empty, treapListOf<Float>())
        assertTrue(emptyList.equals(empty))
        assertEquals(0, empty.size)
        assertEquals(emptyList.hashCode(), empty.hashCode())
        assertEquals(emptyList.toString(), empty.toString())
        assertEquals(emptyList.joinToString(), empty.joinToString())

        empty.forEachElement { fail() }
        empty.forEachElementIndexed { _, _ -> fail() }
        empty.updateElements { fail() }
        empty.updateElementsIndexed { _, _ -> fail() }

        assertEquals(listOf(1), empty.add(1))
        assertEquals(listOf(1), empty.addFirst(1))
        assertEquals(listOf(1), empty.addLast(1))
        assertEquals(listOf(1, 2), empty.addAll(listOf(1, 2)))
        assertEquals(listOf(1, 2), empty.addAll(empty.addAll(listOf(1, 2))))

        assertEquals(listOf(1), empty.add(0, 1))
        assertFailsWith<IndexOutOfBoundsException> { empty.add(1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { empty.add(-1, 1) }

        assertEquals(listOf(1, 2), empty.addAll(0, listOf(1, 2)))
        assertFailsWith<IndexOutOfBoundsException> {empty.addAll(1, listOf(1, 2)) }
        assertFailsWith<IndexOutOfBoundsException> {empty.addAll(-1, listOf(1, 2)) }

        assertSame(empty, empty.remove(1))
        assertSame(empty, empty.removeAll(listOf(1, 2)))
        assertSame(empty, empty.removeAll { false })
        assertSame(empty, empty.removeAll { true })
        assertSame(empty, empty.retainAll(listOf(1)))

        assertFailsWith<IndexOutOfBoundsException> { empty.removeAt(0) }
        assertFailsWith<IndexOutOfBoundsException> { empty.removeAt(1) }
        assertFailsWith<IndexOutOfBoundsException> { empty.removeAt(-1) }

        assertSame(empty, empty.clear())

        assertFailsWith<IndexOutOfBoundsException> { empty.get(0) }
        assertFailsWith<IndexOutOfBoundsException> { empty.get(1) }
        assertFailsWith<IndexOutOfBoundsException> { empty.get(-1) }

        assertFailsWith<IndexOutOfBoundsException> { empty.set(0, 1) }
        assertFailsWith<IndexOutOfBoundsException> { empty.set(1, 1) }
        assertFailsWith<IndexOutOfBoundsException> { empty.set(-1, 1) }

        assertEquals(-1, empty.indexOf(1))
        assertEquals(-1, empty.lastIndexOf(1))
        assertFalse(empty.contains(1))

        assertTrue(empty.containsAll(listOf()))
        assertFalse(empty.containsAll(listOf(1)))

        assertFailsWith<NoSuchElementException> { empty.first() }
        assertNull(empty.firstOrNull())
        assertFailsWith<NoSuchElementException> { empty.last() }
        assertNull(empty.lastOrNull())

        assertFailsWith<NoSuchElementException> { empty.removeFirst() }
        assertFailsWith<NoSuchElementException> { empty.removeLast() }

        empty.builder().also {
            it.add(1)
            assertEquals(listOf(1), it.build())
        }
    }

    @Test
    fun hashCodes() {
        assertEquals(persistentListOf<Int>().hashCode(), treapListOf<Int>().hashCode())
        assertEquals(persistentListOf<Int>(1).hashCode(), treapListOf<Int>(1).hashCode())
        assertEquals(persistentListOf<Int>(1, 23).hashCode(), treapListOf<Int>(1, 23).hashCode())
        assertEquals(persistentListOf<Int>(1, 23, 456).hashCode(), treapListOf<Int>(1, 23, 456).hashCode())
    }

    @Test
    fun toStrings() {
        assertEquals(persistentListOf<Int>().toString(), treapListOf<Int>().toString())
        assertEquals(persistentListOf<Int>(1).toString(), treapListOf<Int>(1).toString())
        assertEquals(persistentListOf<Int>(1, 23).toString(), treapListOf<Int>(1, 23).toString())
        assertEquals(persistentListOf<Int>(1, 23, 456).toString(), treapListOf<Int>(1, 23, 456).toString())
    }

    @Test
    fun clear() {
        assertSame(treapListOf<Int>(), treapListOf(1, 2, 3, 4).clear())
    }

    @Test
    fun add() {
        val a = (1..1000).fold(treapListOf<Int>()) { acc, n -> acc + n }
        val b = (1..1000).fold(treapListOf<Int>()) { acc, n -> acc + n }
        val c = (1..1000).fold(persistentListOf<Int>()) { acc, n -> acc + n }

        assertFalse(a.isEmpty())
        assertEquals(c.size, a.size)

        assertEquals(c, a)
        assertEquals(c, b)
        assertTrue(a.equals(b))
        assertTrue(b.equals(a))
        assertTrue(a.equals(c))
        assertTrue(c.equals(a))
    }

    @Test
    fun addFirst() {
        val a = (1..1000).fold(treapListOf<Int>()) { acc, n -> acc.addFirst(n) }
        val b = (1..1000).fold(treapListOf<Int>()) { acc, n -> acc.addFirst(n) }
        val c = (1..1000).fold(persistentListOf<Int>()) { acc, n -> acc.add(n) }.asReversed()

        assertFalse(a.isEmpty())
        assertEquals(c.size, a.size)

        assertEquals(c, a)
        assertEquals(c, b)
        assertTrue(a.equals(b))
        assertTrue(b.equals(a))
        assertTrue(a.equals(c))
        assertTrue(c.equals(a))
    }

    @Test
    fun addAll() {
        val a = (1..1000).fold(treapListOf<Int>()) { acc, n -> acc + listOf(n, n+1) }
        val b = (1..1000).fold(treapListOf<Int>()) { acc, n -> acc + listOf(n, n+1) }
        val c = (1..1000).fold(persistentListOf<Int>()) { acc, n -> acc + listOf(n, n+1) }

        assertEquals(c, a)
        assertEquals(c, b)
        assertTrue(a.equals(b))
        assertTrue(b.equals(a))
        assertTrue(a.equals(c))
        assertTrue(c.equals(a))

        assertEquals(c + c, a + b)
        assertEquals(c + c, a + c)
        assertEquals(c + c, c + a)
    }

    @Test
    fun addAt() {
        assertEquals(listOf(1, 2, 3), treapListOf(2, 3).add(0, 1))
        assertEquals(listOf(1, 2, 3), treapListOf(1, 3).add(1, 2))
        assertEquals(listOf(1, 2, 3), treapListOf(1, 2).add(2, 3))
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2).add(-1, 3) }
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2).add(3, 3) }
    }

    @Test
    fun addAllAt() {
        assertEquals(listOf(1, 2, 3, 4), treapListOf(3, 4).addAll(0, treapListOf(1, 2)))
        assertEquals(listOf(1, 2, 3, 4), treapListOf(3, 4).addAll(0, listOf(1, 2)))
        assertEquals(listOf(1, 2, 3, 4), treapListOf(1, 4).addAll(1, treapListOf(2, 3)))
        assertEquals(listOf(1, 2, 3, 4), treapListOf(1, 4).addAll(1, listOf(2, 3)))
        assertEquals(listOf(1, 2, 3, 4), treapListOf(1, 2).addAll(2, treapListOf(3, 4)))
        assertEquals(listOf(1, 2, 3, 4), treapListOf(1, 2).addAll(2, listOf(3, 4)))
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2).addAll(-1, listOf(3, 4)) }
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2).addAll(3, listOf(3, 4)) }
    }

    @Test
    fun remove() {
        assertEquals(treapListOf<Int>(), treapListOf(1) - 1)
        assertEquals(treapListOf(1), treapListOf(1) - 2)
        assertEquals(treapListOf(2, 3), treapListOf(1, 2, 3) - 1)
        assertEquals(treapListOf(1, 3), treapListOf(1, 2, 3) - 2)
        assertEquals(treapListOf(1, 2), treapListOf(1, 2, 3) - 3)
        assertEquals(treapListOf(1, 3, 2), treapListOf(1, 2, 3, 2) - 2)
    }

    @Test
    fun removeFirst() {
        assertEquals(treapListOf<Int>(), treapListOf(1).removeFirst())
        assertEquals(treapListOf(2), treapListOf(1, 2).removeFirst())
        assertEquals(treapListOf(2, 3), treapListOf(1, 2, 3).removeFirst())
        assertEquals(treapListOf(2, 3, 4), treapListOf(1, 2, 3, 4).removeFirst())
    }

    @Test
    fun removeLast() {
        assertEquals(treapListOf<Int>(), treapListOf(1).removeLast())
        assertEquals(treapListOf(1), treapListOf(1, 2).removeLast())
        assertEquals(treapListOf(1, 2), treapListOf(1, 2, 3).removeLast())
        assertEquals(treapListOf(1, 2, 3), treapListOf(1, 2, 3, 4).removeLast())
    }

    @Test
    fun removeAt() {
        assertEquals(treapListOf<Int>(), treapListOf(1).removeAt(0))
        assertEquals(treapListOf(2, 3), treapListOf(1, 2, 3).removeAt(0))
        assertEquals(treapListOf(1, 3), treapListOf(1, 2, 3).removeAt(1))
        assertEquals(treapListOf(1, 2), treapListOf(1, 2, 3).removeAt(2))
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3).removeAt(-1) }
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3).removeAt(3) }
    }

    @Test
    fun removeAll() {
        assertEquals(treapListOf(2, 2, 2, 3, 3, 3), treapListOf(1, 1, 2, 2, 2, 1, 3, 3, 1, 1, 4, 3, 1, 5, 6).removeAll(setOf(1, 4, 5, 6)))
        assertEquals(treapListOf(1, 2, 3), treapListOf(1, 2, 3).removeAll(setOf()))
        assertEquals(treapListOf(1, 2, 3), treapListOf(1, 2, 3).removeAll(setOf(4, 5, 6)))
        assertEquals(treapListOf(2, 4, 6, 8), treapListOf(1, 2, 3, 4, 5, 6, 7, 8).removeAll { (it % 2) == 1})
    }

    @Test
    fun retainAll() {
        assertEquals(treapListOf(1, 1, 1, 1, 1, 4, 1, 5, 6), treapListOf(1, 1, 2, 1, 3, 1, 1, 4, 1, 5, 6).retainAll(setOf(1, 4, 5, 6)))
        assertEquals(treapListOf<Int>(), treapListOf(1, 2, 3).retainAll(setOf()))
        assertEquals(treapListOf(1, 2, 3), treapListOf(1, 2, 3).retainAll(setOf(1, 2, 3)))
    }

    @Test
    fun get() {
        assertEquals(1, treapListOf(1, 2, 3)[0])
        assertEquals(2, treapListOf(1, 2, 3)[1])
        assertEquals(3, treapListOf(1, 2, 3)[2])
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3)[-1] }
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3)[3] }
    }

    @Test
    fun set() {
        assertEquals(treapListOf(4, 2, 3), treapListOf(1, 2, 3).set(0, 4))
        assertEquals(treapListOf(1, 4, 3), treapListOf(1, 2, 3).set(1, 4))
        assertEquals(treapListOf(1, 2, 4), treapListOf(1, 2, 3).set(2, 4))
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3).set(-1, 4) }
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3).set(3, 4) }
    }

    @Test
    fun first() {
        assertEquals(1, treapListOf(1).first())
        assertEquals(1, treapListOf(1, 2).first())
        assertEquals(1, treapListOf(1, 2, 3).first())
    }

    @Test
    fun firstOrNull() {
        assertEquals(1, treapListOf(1).firstOrNull())
        assertEquals(1, treapListOf(1, 2).firstOrNull())
        assertEquals(1, treapListOf(1, 2, 3).firstOrNull())
    }

    @Test
    fun last() {
        assertEquals(1, treapListOf(1).last())
        assertEquals(2, treapListOf(1, 2).last())
        assertEquals(3, treapListOf(1, 2, 3).last())
    }

    @Test
    fun lastOrNull() {
        assertEquals(1, treapListOf(1).lastOrNull())
        assertEquals(2, treapListOf(1, 2).lastOrNull())
        assertEquals(3, treapListOf(1, 2, 3).lastOrNull())
    }

    @Test
    fun indexOf() {
        assertEquals(0, treapListOf(1, 1, 2, 2, 3, 3).indexOf(1))
        assertEquals(2, treapListOf(1, 1, 2, 2, 3, 3).indexOf(2))
        assertEquals(4, treapListOf(1, 1, 2, 2, 3, 3).indexOf(3))
        assertEquals(-1, treapListOf(1, 1, 2, 2, 3, 3).indexOf(4))
    }

    @Test
    fun lastIndexOf() {
        assertEquals(1, treapListOf(1, 1, 2, 2, 3, 3).lastIndexOf(1))
        assertEquals(3, treapListOf(1, 1, 2, 2, 3, 3).lastIndexOf(2))
        assertEquals(5, treapListOf(1, 1, 2, 2, 3, 3).lastIndexOf(3))
        assertEquals(-1, treapListOf(1, 1, 2, 2, 3, 3).lastIndexOf(4))
    }

    @Test
    fun contains() {
        assertTrue(treapListOf(1, 2, 3).contains(1))
        assertTrue(treapListOf(1, 2, 3).contains(2))
        assertTrue(treapListOf(1, 2, 3).contains(3))
        assertFalse(treapListOf(1, 2, 3).contains(4))
    }

    @Test
    fun containsAll() {
        assertTrue(treapListOf(1, 2, 3).containsAll(setOf(1)))
        assertTrue(treapListOf(1, 2, 3).containsAll(setOf(1, 2)))
        assertTrue(treapListOf(1, 2, 3).containsAll(setOf(1, 3)))
        assertTrue(treapListOf(1, 2, 3).containsAll(setOf(1, 2, 3)))
        assertFalse(treapListOf(1, 2, 3).containsAll(setOf(1, 2, 3, 4)))
        assertTrue(treapListOf(1, 2, 3).containsAll(setOf()))
    }

    @Test
    fun forEachElement() {
        val buf = mutableListOf<Int>()
        val l = (0..1000).toList()

        l.toTreapList().forEachElement { buf.add(it) }
        assertEquals(l, buf)
    }

    @Test
    fun forEachElementIndexed() {
        val buf = mutableListOf<Int>()
        val l = (0..1000).toList()

        l.toTreapList().forEachElementIndexed { i, it ->
            assertEquals(it, i)
            buf.add(it)
        }
        assertEquals(l, buf)
    }

    @Test
    fun updateElements() {
        assertEquals(
            treapListOf(1, 3, 42, 7, 9),
            treapListOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).updateElements {
                when {
                    it % 2 == 0 -> null
                    it == 5 -> 42
                    else -> it
                }
            }
        )
    }

    @Test
    fun updateElementsIndexed() {
        assertEquals(
            treapListOf(1, 3, 42, 7, 9),
            treapListOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9).updateElementsIndexed { i, it ->
                assertEquals(it, i)
                when {
                    it % 2 == 0 -> null
                    it == 5 -> 42
                    else -> it
                }
            }
        )
    }

    @Test
    fun forwardIteration() {
        val a = (1..1000).fold(treapListOf<Int>()) { acc, n -> acc + n }
        val c = (1..1000).fold(persistentListOf<Int>()) { acc, n -> acc + n }
        assertEquals(c, a.toList())
    }

    @Test
    fun emptyListIterator() {
        val empty = treapListOf<Int>().listIterator()
        assertFalse(empty.hasNext())
        assertFalse(empty.hasPrevious())
        assertEquals(0, empty.nextIndex())
        assertEquals(-1, empty.previousIndex())
        assertFailsWith<NoSuchElementException> { empty.next() }
        assertFailsWith<NoSuchElementException> { empty.previous() }
    }

    @Test
    fun listIterator() {
        val it = treapListOf(1, 2, 3, 4, 5).listIterator()
        assertTrue(it.hasNext())
        assertFalse(it.hasPrevious())
        assertFailsWith<NoSuchElementException> { it.previous() }
        assertEquals(1, it.next())
        assertEquals(1, it.previous())
        assertEquals(1, it.next())
        assertEquals(2, it.next())
        assertEquals(3, it.next())
        assertEquals(4, it.next())
        assertTrue(it.hasNext())
        assertTrue(it.hasPrevious())
        assertEquals(5, it.next())
        assertFalse(it.hasNext())
        assertTrue(it.hasPrevious())
        assertFailsWith<NoSuchElementException> { it.next() }
        assertEquals(5, it.previous())
        assertEquals(4, it.previous())
    }

    @Test
    fun toTreapList() {
        assertEquals((1..4).toList(), (1..4).toTreapList())

        // Ensure that large lists constructed by toTreapList are balanced.
        // If not, the removeAt call will overflow the stack.
        val large = (1..100000).toTreapList()
        large.removeAt(large.size / 2)
    }

    /**
        Priorities, high to low, build this cartesian tree (in-order is 0..6):

            3
          /   \
         1     5
        / \   / \
       0   2 4   6

        getNodeAt, setNodeAt, split, contains, remove, and listIterator.previous each have a branch that this shape
        takes and that a right or left spine does not.
     */
    @Test
    fun shapeDependentPaths() {
        // Higher priority sits closer to the root. One draw per element, in order.
        TreapListNode.setTreapListPriorityRandom(FixedPriorityRandom(intArrayOf(10, 50, 30, 100, 20, 40, 5)))
        val list = (0..6).toTreapList()
        assertEquals((0..6).toList(), list.toList())
        // computeHashCode recurses into right only when a node has one. The lists hashCodes() builds
        // under the per-test seed have none, so hash this tree, which does.
        assertEquals(persistentListOf(0, 1, 2, 3, 4, 5, 6).hashCode(), list.hashCode())

        for (i in list.indices) {
            assertEquals(i, list[i])
            assertEquals(i, list.indexOf(i))
            assertEquals(i, list.lastIndexOf(i))
            assertTrue(list.contains(i))
            val expectedSet = (0..6).map { value ->
                if (value == i) {
                    100 + i
                } else {
                    value
                }
            }
            assertEquals(expectedSet, list.set(i, 100 + i).toList())
            assertEquals((0..6).filter { it != i }, list.removeAt(i).toList())
            assertEquals((0..6).filter { it != i }, (list - i).toList())
        }
        assertFalse(list.contains(7))
        assertEquals(list, list - 7)

        // Splits that walk left, right, and land on the root.
        assertEquals(listOf(0, 1, 2), list.subList(0, 3))
        assertEquals(listOf(2, 3, 4), list.subList(2, 5))
        assertEquals(listOf(4, 5, 6), list.subList(4, 7))
        assertEquals(listOf(3), list.subList(3, 4))

        walkBothDirections(list)
    }

    /** A seeded list large enough that predecessor and successor walks see nested left and right children. */
    @Test
    fun bidirectionalIteration() {
        val list = (0 until 64).toTreapList()
        for (i in list.indices) {
            assertEquals(i, list[i])
        }
        walkBothDirections(list)
    }

    @Test
    fun subList() {
        assertEquals(listOf<Int>(), treapListOf<Int>().subList(0, 0))
        assertFailsWith<IndexOutOfBoundsException> { treapListOf<Int>().subList(0, 1) }
        assertFailsWith<IndexOutOfBoundsException> { treapListOf<Int>().subList(1, 1) }

        assertEquals(listOf<Int>(), treapListOf(1).subList(0, 0))
        assertEquals(listOf<Int>(), treapListOf(1).subList(1, 1))
        assertEquals(listOf(1), treapListOf(1).subList(0, 1))
        assertFailsWith<IndexOutOfBoundsException> { treapListOf<Int>().subList(1, 2) }
        assertFailsWith<IndexOutOfBoundsException> { treapListOf<Int>().subList(2, 2) }

        assertEquals(listOf<Int>(), treapListOf(1, 2).subList(2, 2))
        assertEquals(listOf(1), treapListOf(1, 2).subList(0, 1))
        assertEquals(listOf(2), treapListOf(1, 2).subList(1, 2))
        assertEquals(listOf(1, 2), treapListOf(1, 2).subList(0, 2))

        assertEquals(listOf(1), treapListOf(1, 2, 3).subList(0, 1))
        assertEquals(listOf(1, 2), treapListOf(1, 2, 3).subList(0, 2))
        assertEquals(listOf(2, 3), treapListOf(1, 2, 3).subList(1, 3))
        assertEquals(listOf(3), treapListOf(1, 2, 3).subList(2, 3))
        assertEquals(listOf<Int>(), treapListOf(1, 2, 3).subList(3, 3))
        assertEquals(listOf(1, 2, 3), treapListOf(1, 2, 3).subList(0, 3))
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3).subList(2, 1) }

        val large = (1..50).toTreapList()
        assertEquals((11..20).toList(), large.subList(10, 20))
        assertEquals((1..10).toList(), large.subList(0, 10))
        assertEquals((41..50).toList(), large.subList(40, 50))
        assertEquals(emptyList<Int>(), large.subList(25, 25))
    }

    @Test
    @Suppress("ArrayPrimitive")
    fun mapReduceAndOperators() {
        val list = (1..40).toTreapList()
        assertEquals((1..40).sum(), list.mapReduce({ it }, Int::plus))
        assertEquals((1..40).sum(), list.parallelMapReduce({ it }, Int::plus, 1))
        assertNull(treapListOf<Int>().mapReduce({ it }, Int::plus))
        assertNull(treapListOf<Int>().parallelMapReduce({ it }, Int::plus, 1))

        assertEquals(listOf(1, 2, 3), treapListOf(1) + listOf(2, 3))
        assertEquals(listOf(1, 2, 3), treapListOf(1) + arrayOf(2, 3))
        assertEquals(listOf(1, 2, 3), treapListOf(1) + sequenceOf(2, 3))
        assertEquals(listOf(1, 3), treapListOf(1, 2, 3) - listOf(2))
        assertEquals(listOf(1, 3), treapListOf(1, 2, 3) - arrayOf(2))
        assertEquals(listOf(1, 3), treapListOf(1, 2, 3) - sequenceOf(2))
        assertEquals(listOf(1, 2), treapListOf(1, 2, 3).mutate { it.removeAt(2) })
        assertEquals(listOf('a', 'b'), "ab".toTreapList())
        assertEquals(listOf(1, 2), sequenceOf(1, 2).toTreapList())
        assertEquals(listOf(1, 2), listOf(1, 2).toTreapList())
        val present = treapListOf(1, 2)
        assertSame(present, present.orEmpty())
        assertSame(present, present.toTreapList())
        assertTrue((null as TreapList<Int>?).orEmpty().isEmpty())
        assertSame(treapListOf<Int>(), emptyList<Int>().toTreapList())
        val builder = treapListOf(1, 2).builder()
        assertSame(builder.build(), builder.toTreapList())
    }

    @Test
    fun builderEdits() {
        val builder = treapListOf(1, 2, 3).builder()
        assertEquals(1, builder.first())
        assertEquals(3, builder.last())
        assertEquals(1, builder.firstOrNull())
        assertEquals(3, builder.lastOrNull())
        assertEquals(1, builder.removeFirst())
        assertEquals(3, builder.removeLast())
        builder.addFirst(0)
        builder.addLast(4)
        assertEquals(listOf(0, 2, 4), builder.build())
        assertEquals(0, builder[0])
        assertEquals(0, builder.set(0, 9))
        assertEquals(9, builder[0])
        assertTrue(builder.remove(2))
        assertFalse(builder.remove(2))
        builder.addAll(1, listOf(7, 8))
        assertEquals(listOf(9, 7, 8, 4), builder.build())
        assertEquals(7, builder.removeAt(1))
        builder.retainAll(setOf(9, 4))
        assertEquals(listOf(9, 4), builder.build())
        assertEquals(0, builder.indexOf(9))
        assertEquals(1, builder.lastIndexOf(4))
        assertTrue(builder.containsAll(listOf(9, 4)))
        assertFalse(builder.contains(7))

        val cursor = builder.listIterator()
        assertEquals(9, cursor.next())
        cursor.remove()
        assertEquals(4, cursor.next())
        cursor.add(5)
        assertEquals(listOf(4, 5), builder.build())
        assertEquals(5, cursor.previous())
        cursor.set(6)
        assertEquals(listOf(4, 6), builder.build())

        builder.clear()
        assertTrue(builder.isEmpty())
        assertNull(builder.firstOrNull())
        assertNull(builder.lastOrNull())
        assertEquals(treapListOf<Int>(), builder.build())
        assertFailsWith<NoSuchElementException> { builder.first() }
        assertFailsWith<NoSuchElementException> { builder.removeFirst() }
    }

    @Test
    fun listIteratorFromMiddle() {
        val iterator = treapListOf(1, 2, 3, 4, 5).listIterator(3)
        assertEquals(3, iterator.nextIndex())
        assertEquals(2, iterator.previousIndex())
        assertEquals(4, iterator.next())
        assertEquals(4, iterator.previous())
        assertEquals(3, iterator.previous())
        val atEnd = treapListOf(1, 2, 3).listIterator(3)
        assertFalse(atEnd.hasNext())
        assertTrue(atEnd.hasPrevious())
        assertEquals(3, atEnd.previous())
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3).listIterator(-1) }
        assertFailsWith<IndexOutOfBoundsException> { treapListOf(1, 2, 3).listIterator(4) }
    }

    private fun walkBothDirections(list: TreapList<Int>) {
        for (start in 0..list.size) {
            val forward = mutableListOf<Int>()
            val it = list.listIterator(start)
            while (it.hasNext()) {
                val value = it.next()
                forward.add(value)
                assertEquals(value, it.previous())
                assertEquals(value, it.next())
            }
            assertEquals((start until list.size).toList(), forward)
            assertFailsWith<NoSuchElementException> { it.next() }

            val backward = mutableListOf<Int>()
            while (it.hasPrevious()) {
                backward.add(it.previous())
            }
            assertEquals((list.size - 1 downTo 0).toList(), backward)
            assertFailsWith<NoSuchElementException> { it.previous() }
        }
    }

    /**
        Yields a fixed priority for each successive node. [kotlin.random.Random.nextInt] is the only draw
        [TreapListNode] makes.
     */
    private class FixedPriorityRandom(private val priorities: IntArray) : Random() {
        private var index = 0

        override fun nextBits(bitCount: Int): Int =
            throw UnsupportedOperationException("list priorities are drawn with nextInt()")

        override fun nextInt(): Int {
            check(index < priorities.size) { "priority sequence exhausted at $index" }
            return priorities[index++]
        }
    }

}
