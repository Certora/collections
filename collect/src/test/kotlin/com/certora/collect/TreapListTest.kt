package com.certora.collect

import kotlin.test.*
import kotlinx.collections.immutable.*

/** Tests for [TreapList]. */
class TreapListTest {
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
}
