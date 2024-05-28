package com.certora.collect

import kotlinx.collections.immutable.*

internal class EmptyTreapSet<@Treapable E> private constructor() : TreapSet<E>, java.io.Serializable {
    override val size = 0
    override fun isEmpty() = true

    override fun hashCode() = 0
    override fun equals(other: Any?) = (other is Set<*>) && (other.isEmpty())
    override fun toString() = "[]"

    override fun iterator(): Iterator<E> = emptySet<E>().iterator()

    override fun clear(): TreapSet<E> = this
    override fun contains(element: E): Boolean = false
    override fun containsAll(elements: Collection<E>): Boolean = elements.isEmpty()
    override fun containsAny(elements: Iterable<E>): Boolean = false
    override fun findEqual(element: E): E? = null
    override fun forEachElement(action: (element: E) -> Unit): Unit {}
    override fun remove(element: E): TreapSet<E> = this
    override fun removeAll(predicate: (E) -> Boolean): TreapSet<E> = this
    override fun removeAll(elements: Collection<E>): TreapSet<E> = this
    override fun retainAll(elements: Collection<E>): TreapSet<E> = this
    override fun single(): E = throw NoSuchElementException("Empty set.")
    override fun singleOrNull(): E? = null
    override fun <R : Any> mapReduce(map: (E) -> R, reduce: (R, R) -> R): R? = null
    override fun <R : Any> parallelMapReduce(map: (E) -> R, reduce: (R, R) -> R, parallelThresholdLog2: Int): R? = null

    override fun add(element: E): TreapSet<E> = when (element) {
        !is Comparable<*>?, is PrefersHashTreap -> HashTreapSet(element)
        else -> SortedTreapSet(element as E)
    }

    @Suppress("UNCHECKED_CAST")
    override fun addAll(elements: Collection<E>): TreapSet<E> = when {
        elements.isEmpty() -> this
        elements is TreapSet<*> -> elements as TreapSet<E>
        elements is PersistentSet.Builder<*> -> elements.build() as TreapSet<E>
        else -> elements.fold(this as TreapSet<E>) { set, element -> set.add(element) }
    }

    companion object {
        private val instance = EmptyTreapSet<Nothing>()
        @Suppress("UNCHECKED_CAST")
        operator fun <@Treapable E> invoke(): EmptyTreapSet<E> = instance as EmptyTreapSet<E>
    }
}
