package com.certora.collect

internal class EmptyTreapSet<@Treapable E> : TreapSet<E> {
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

    @Suppress("Treapability", "UNCHECKED_CAST")
    override fun add(element: E): TreapSet<E> = when (element) {
        is PrefersHashTreap -> HashTreapSet(element)
        is Comparable<*> -> SortedTreapSet<Comparable<Comparable<*>>>(element as Comparable<Comparable<*>>) as TreapSet<E>
        else -> HashTreapSet(element)
    }

    override fun addAll(elements: Collection<E>): TreapSet<E> = 
        elements.fold(this as TreapSet<E>) { set, element -> set.add(element) }

    companion object {
        private val instance = EmptyTreapSet<Nothing>()
        @Suppress("UNCHECKED_CAST")
        operator fun <@Treapable E> invoke(): EmptyTreapSet<E> = instance as EmptyTreapSet<E>
    }
}