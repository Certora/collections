package com.certora.collect

internal class EmptyTreapList<E> private constructor() : TreapList<E>, java.io.Serializable, AbstractList<E>() {
    override fun hashCode() = 1
    override fun equals(other: Any?) = (other is List<*>) && (other.isEmpty())
    override fun toString() = "[]"

    override fun isEmpty(): Boolean = true
    override val size: Int get() = 0
    override fun iterator() = emptyList<E>().iterator()

    override fun forEachElement(action: (E) -> Unit) {}
    override fun forEachElementIndexed(action: (Int, E) -> Unit) {}
    override fun updateElements(transform: (E) -> E?): TreapList<E> = this
    override fun updateElementsIndexed(transform: (Int, E) -> E?): TreapList<E> = this

    override fun builder(): TreapList.Builder<E> = TreapListBuilder(this)

    override fun addFirst(element: E): TreapList<E> = TreapListNode(element)
    override fun addLast(element: E): TreapList<E> = TreapListNode(element)

    override fun addAll(elements: Collection<E>): TreapList<E> = elements.toTreapList()

    override fun add(index: Int, element: E): TreapList<E> = when {
        index == 0 -> add(element)
        else -> throw IndexOutOfBoundsException("Empty list")
    }
    override fun addAll(index: Int, c: Collection<E>): TreapList<E> = when {
        index == 0 -> addAll(c)
        else -> throw IndexOutOfBoundsException("Empty list")
    }

    override fun remove(element: E): TreapList<E> = this
    override fun removeAll(elements: Collection<@UnsafeVariance E>): TreapList<E> = this
    override fun removeAll(predicate: (E) -> Boolean): TreapList<E> = this
    override fun retainAll(elements: Collection<@UnsafeVariance E>): TreapList<E> = this
    override fun removeAt(index: Int): TreapList<E> = throw IndexOutOfBoundsException("Empty list")

    override fun clear(): TreapList<E> = this

    override fun get(index: Int): E = throw IndexOutOfBoundsException("Empty list")
    override fun set(index: Int, element: E): TreapList<E> = throw IndexOutOfBoundsException("Empty list")
    override fun indexOf(element: E): Int = -1
    override fun contains(element: E): Boolean = false
    override fun containsAll(elements: Collection<E>): Boolean = elements.isEmpty()
    override fun lastIndexOf(element: E): Int = -1

    override fun first(): E = throw NoSuchElementException("Empty list")
    override fun firstOrNull(): E? = null
    override fun last(): E = throw NoSuchElementException("Empty list")
    override fun lastOrNull(): E? = null

    override fun removeFirst(): TreapList<E> = throw NoSuchElementException("Empty list")
    override fun removeLast(): TreapList<E> = throw NoSuchElementException("Empty list")

    override fun subList(fromIndex: Int, toIndex: Int): TreapList<E> = when {
        fromIndex == 0 && toIndex == 0 -> this
        else -> throw IndexOutOfBoundsException("Empty list")
    }

    override fun <R : Any> mapReduce(map: (E) -> R, reduce: (R, R) -> R): R? = null
    override fun <R : Any> parallelMapReduce(map: (E) -> R, reduce: (R, R) -> R, parallelThresholdLog2: Int): R? = null

    companion object {
        private val instance = EmptyTreapList<Nothing>()
        @Suppress("UNCHECKED_CAST")
        operator fun <E> invoke(): EmptyTreapList<E> = instance as EmptyTreapList<E>
    }
}
