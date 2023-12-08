package com.certora.collect

import kotlinx.collections.immutable.PersistentList

/**
    A PersistentList implemented as a [Treap](https://en.wikipedia.org/wiki/Treap).
 */
@Treapable
public interface TreapList<E> : PersistentList<E> {
    override fun add(element: E): TreapList<E> = addLast(element)
    override fun addAll(elements: Collection<E>): TreapList<E>
    override fun remove(element: E): TreapList<E>
    override fun removeAll(elements: Collection<E>): TreapList<E>
    override fun removeAll(predicate: (E) -> Boolean): TreapList<E>
    override fun retainAll(elements: Collection<E>): TreapList<E>
    override fun clear(): TreapList<E>
    override fun addAll(index: Int, c: Collection<E>): TreapList<E>
    override fun set(index: Int, element: E): TreapList<E>
    override fun add(index: Int, element: E): TreapList<E>
    override fun removeAt(index: Int): TreapList<E>
    override fun subList(fromIndex: Int, toIndex: Int): TreapList<E>

    public fun forEachElement(action: (E) -> Unit)
    public fun forEachElementIndexed(action: (Int, E) -> Unit)

    public fun first(): E
    public fun firstOrNull(): E?
    public fun last(): E
    public fun lastOrNull(): E?

    public fun addFirst(element: E): TreapList<E>
    public fun addLast(element: E): TreapList<E>

    public fun removeFirst(): TreapList<E>
    public fun removeLast(): TreapList<E>

    public fun updateElements(transform: (E) -> E?): TreapList<E>
    public fun updateElementsIndexed(transform: (Int, E) -> E?): TreapList<E>

    /**
        A [PersistentList.Builder] that produces a [TreapList].
    */
    public interface Builder<E>: MutableList<E>, PersistentList.Builder<E> {
        override fun build(): TreapList<E>

        public fun first(): E
        public fun firstOrNull(): E?
        public fun last(): E
        public fun lastOrNull(): E?

        public fun addFirst(element: E)
        public fun addLast(element: E)
        public fun removeFirst(): E
        public fun removeLast(): E
    }

    override fun builder(): Builder<E>
}

public fun <T> treapListOf(): TreapList<T> = EmptyTreapList<T>()
public fun <T> treapListOf(element: T): TreapList<T> = treapListOf<T>().add(element)
public fun <T> treapListOf(vararg elements: T): TreapList<T> = elements.asIterable().toTreapList()

public inline fun <T> TreapList<T>.mutate(mutator: (MutableList<T>) -> Unit): TreapList<T> = builder().apply(mutator).build()

public operator fun <E> TreapList<E>.plus(element: E): TreapList<E> = add(element)
public operator fun <E> TreapList<E>.minus(element: E): TreapList<E> = remove(element)
public operator fun <E> TreapList<E>.plus(elements: Iterable<E>): TreapList<E> = addAll(elements.toTreapList())
public operator fun <E> TreapList<E>.plus(elements: Array<out E>): TreapList<E> = addAll(elements.asIterable().toTreapList())
public operator fun <E> TreapList<E>.plus(elements: Sequence<E>): TreapList<E> = addAll(elements.asIterable().toTreapList())

public operator fun <E> TreapList<E>.minus(elements: Iterable<E>): TreapList<E> =
    if (elements is Collection) { removeAll(elements) } else { mutate { it.removeAll(elements) }}
public operator fun <E> TreapList<E>.minus(elements: Array<out E>): TreapList<E> = mutate { it.removeAll(elements) }
public operator fun <E> TreapList<E>.minus(elements: Sequence<E>): TreapList<E> = minus(elements.asIterable())

public fun <T> Iterable<T>.toTreapList(): TreapList<T> = when(this) {
    is TreapList<T> -> this
    is TreapList.Builder<T> -> this.build()
    else -> {
        val iterator = this.iterator()
        when {
            iterator.hasNext() -> TreapListNode.fromIterator(iterator)
            else -> EmptyTreapList()
        }
    }
}

public fun <T> Sequence<T>.toTreapList(): TreapList<T> = asIterable().toTreapList()

public fun CharSequence.toTreapList(): TreapList<Char> =
    treapListOf<Char>().mutate { this.toCollection(it) }

public fun <T> TreapList<T>?.orEmpty(): TreapList<T> = this ?: EmptyTreapList<T>()
