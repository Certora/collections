package com.certora.collect

import kotlinx.collections.immutable.PersistentSet

/**
    A [PersistentSet] implemented as a [Treap](https://en.wikipedia.org/wiki/Treap) - a kind of balanced binary tree.    
 */
public interface TreapSet<out E> : PersistentSet<E> {
    override fun add(element: @UnsafeVariance E): TreapSet<E>
    override fun addAll(elements: Collection<@UnsafeVariance E>): TreapSet<E>
    override fun remove(element: @UnsafeVariance E): TreapSet<E>
    override fun removeAll(elements: Collection<@UnsafeVariance E>): TreapSet<E>
    override fun removeAll(predicate: (E) -> Boolean): TreapSet<E>
    override fun retainAll(elements: Collection<@UnsafeVariance E>): TreapSet<E>
    override fun clear(): TreapSet<E>

    /**
        A [PersistentSet.Builder] that produces a [TreapSet].    
    */
    public interface Builder<E> : PersistentSet.Builder<E> {
        override fun build(): TreapSet<E>
    }

    @Suppress("Treapability")
    override fun builder(): Builder<@UnsafeVariance E> = TreapSetBuilder(this)

    public fun containsAny(elements: Iterable<@UnsafeVariance E>): Boolean
    public fun single(): E
    public fun singleOrNull(): E?
    public fun findEqual(element: @UnsafeVariance E): E?
    public fun forEachElement(action: (element: E) -> Unit): Unit
}


public fun <@Treapable T> treapSetOf(): TreapSet<T> = EmptyTreapSet()
public fun <@Treapable T> treapSetOf(element: T): TreapSet<T> = treapSetOf<T>().add(element)
public fun <@Treapable T> treapSetOf(vararg elements: T): TreapSet<T> = treapSetOf<T>().mutate { it.addAll(elements) }

public fun <@Treapable T> Iterable<T>.toTreapSet(): TreapSet<T> = treapSetOf<T>() + this
public fun <@Treapable T> Sequence<T>.toTreapSet(): TreapSet<T> = treapSetOf<T>() + this

public fun CharSequence.toTreapSet(): TreapSet<Char> = treapSetOf<Char>().mutate { this.toCollection(it) }

public inline fun <T> TreapSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): TreapSet<T> = builder().apply(mutator).build()

public operator fun <E> TreapSet<E>.plus(element: E): TreapSet<E> = add(element)

public operator fun <E> TreapSet<E>.minus(element: E): TreapSet<E> = remove(element)

public operator fun <E> TreapSet<E>.plus(elements: Iterable<E>): TreapSet<E> = 
    if (elements is Collection) { addAll(elements) } else { mutate { it.addAll(elements) }}

public operator fun <E> TreapSet<E>.plus(elements: Array<out E>): TreapSet<E> =
    mutate { it.addAll(elements) }

public operator fun <E> TreapSet<E>.plus(elements: Sequence<E>): TreapSet<E> =
    mutate { it.addAll(elements) }

public operator fun <E> TreapSet<E>.minus(elements: Iterable<E>): TreapSet<E> =
    if (elements is Collection) { removeAll(elements) } else { mutate { it.removeAll(elements) }}

public operator fun <E> TreapSet<E>.minus(elements: Array<out E>): TreapSet<E> =
    mutate { it.removeAll(elements) }

public operator fun <E> TreapSet<E>.minus(elements: Sequence<E>): TreapSet<E> =
    mutate { it.removeAll(elements) }

public infix fun <E> TreapSet<E>.intersect(elements: Iterable<E>): TreapSet<E> =
    if (elements is Collection) { retainAll(elements) } else { mutate { it.retainAll(elements) } }
