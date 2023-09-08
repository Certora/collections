package com.certora.collect

import kotlinx.collections.immutable.PersistentSet

/**
    A [PersistentSet] implemented as a [Treap](https://en.wikipedia.org/wiki/Treap) - a kind of balanced binary tree.    
 */
public interface TreapSet<@Treapable out E> : PersistentSet<E> {
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
    public interface Builder<@Treapable E> : PersistentSet.Builder<E> {
        override fun build(): TreapSet<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>

    public fun findEqual(element: @UnsafeVariance E): E?
}


public fun <@Treapable T : Comparable<T>> treapSetOf(): TreapSet<T> = SortedTreapSet.emptyOf<T>()
public fun <@Treapable T : Comparable<T>> treapSetOf(element: T): TreapSet<T> = treapSetOf<T>().add(element)
public fun <@Treapable T : Comparable<T>> treapSetOf(vararg elements: T): TreapSet<T> = treapSetOf<T>().mutate { it.addAll(elements) }

public fun <@Treapable T> hashTreapSetOf(): TreapSet<T> = HashTreapSet.emptyOf<T>()
public fun <@Treapable T> hashTreapSetOf(element: T): TreapSet<T> = hashTreapSetOf<T>().add(element)
public fun <@Treapable T> hashTreapSetOf(vararg elements: T): TreapSet<T> = hashTreapSetOf<T>().mutate { it.addAll(elements) }

public fun <@Treapable T : Comparable<T>> Iterable<T>.toTreapSet(): TreapSet<T> =
    this as? SortedTreapSet<T>
    ?: (this as? TreapSet.Builder<T>)?.build() as? SortedTreapSet<T>
    ?: treapSetOf<T>() + this

public fun <@Treapable T> Iterable<T>.toHashTreapSet(): TreapSet<T> =
    this as? HashTreapSet<T>
    ?: (this as? TreapSet.Builder<T>)?.build() as? HashTreapSet<T>
    ?: hashTreapSetOf<T>() + this

public fun <@Treapable T : Comparable<T>> Sequence<T>.toTreapSet(): TreapSet<T> = treapSetOf<T>() + this
public fun <@Treapable T : Comparable<T>> Sequence<@Treapable T>.toHashTreapSet(): TreapSet<T> = hashTreapSetOf<T>() + this

public fun CharSequence.toTreapSet(): TreapSet<Char> = treapSetOf<Char>().mutate { this.toCollection(it) }
public fun CharSequence.toHashTreapSet(): TreapSet<Char> = hashTreapSetOf<Char>().mutate { this.toCollection(it) }


public inline fun <@Treapable T> TreapSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): TreapSet<T> = builder().apply(mutator).build()

public operator fun <@Treapable E> TreapSet<E>.plus(element: E): TreapSet<E> = add(element)

public operator fun <@Treapable E> TreapSet<E>.minus(element: E): TreapSet<E> = remove(element)

public operator fun <@Treapable E> TreapSet<E>.plus(elements: Iterable<E>): TreapSet<E> = 
    if (elements is Collection) { addAll(elements) } else { mutate { it.addAll(elements) }}

public operator fun <@Treapable E> TreapSet<E>.plus(elements: Array<out E>): TreapSet<E> =
    mutate { it.addAll(elements) }

public operator fun <@Treapable E> TreapSet<E>.plus(elements: Sequence<E>): TreapSet<E> =
    mutate { it.addAll(elements) }

public operator fun <@Treapable E> TreapSet<E>.minus(elements: Iterable<E>): TreapSet<E> =
    if (elements is Collection) { removeAll(elements) } else { mutate { it.removeAll(elements) }}

public operator fun <@Treapable E> TreapSet<E>.minus(elements: Array<out E>): TreapSet<E> =
    mutate { it.removeAll(elements) }

public operator fun <@Treapable E> TreapSet<E>.minus(elements: Sequence<E>): TreapSet<E> =
    mutate { it.removeAll(elements) }

public infix fun <@Treapable E> TreapSet<E>.intersect(elements: Iterable<E>): TreapSet<E> =
    if (elements is Collection) { retainAll(elements) } else { mutate { it.retainAll(elements) } }
