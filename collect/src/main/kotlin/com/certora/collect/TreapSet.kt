package com.certora.collect

import kotlinx.collections.immutable.PersistentSet

/**
    A [PersistentSet] implemented as a [Treap](https://en.wikipedia.org/wiki/Treap) - a kind of balanced binary tree.
 */
@Treapable
public sealed interface TreapSet<out T> : PersistentSet<T> {
    override fun add(element: @UnsafeVariance T): TreapSet<T>
    override fun addAll(elements: Collection<@UnsafeVariance T>): TreapSet<T>
    override fun remove(element: @UnsafeVariance T): TreapSet<T>
    override fun removeAll(elements: Collection<@UnsafeVariance T>): TreapSet<T>
    override fun removeAll(predicate: (T) -> Boolean): TreapSet<T>
    override fun retainAll(elements: Collection<@UnsafeVariance T>): TreapSet<T>
    override fun clear(): TreapSet<T>

    /**
        A [PersistentSet.Builder] that produces a [TreapSet].
    */
    public interface Builder<T> : PersistentSet.Builder<T> {
        override fun build(): TreapSet<T>
    }

    @Suppress("Treapability")
    override fun builder(): Builder<@UnsafeVariance T> = TreapSetBuilder(this)

    /**
        Checks if this set contains any of the given [elements].  This is equivalent to, but more efficient than,
        `this.intersect(elements).isNotEmpty()`.
     */
    public fun containsAny(elements: Iterable<@UnsafeVariance T>): Boolean

    /**
        If this set contains exactly one element, returns that element.  Otherwise, throws [NoSuchElementException].
     */
    public fun single(): T

    /**
        If this set contains exactly one element, returns that element.  Otherwise, returns null.
     */
    public fun singleOrNull(): T?

    /**
        If this set contains an element that compares equal to the specified [element], returns that element instance.
        This is useful for implementing intern tables, for example.
     */
    public fun findEqual(element: @UnsafeVariance T): T?

    /**
        Calls [action] for each element in the set.  This traverses the treap without allocating temporary storage,
        which may be more efficient than [forEach].
     */
    public fun forEachElement(action: (element: T) -> Unit): Unit
}

/**
    Returns an empty [TreapSet<T>].
 */
public fun <@Treapable T> treapSetOf(): TreapSet<T> = EmptyTreapSet()

/**
    Returns a new [TreapSet<T>] containing the given element.
 */
public fun <@Treapable T> treapSetOf(element: T): TreapSet<T> = treapSetOf<T>().add(element)

/**
    Returns a new [TreapSet<T>] containing the given elements.
 */
public fun <@Treapable T> treapSetOf(vararg elements: T): TreapSet<T> = treapSetOf<T>().mutate { it.addAll(elements) }

public fun <@Treapable T> treapSetOfNotNull(element: T?): TreapSet<T> = if (element != null) { treapSetOf(element) } else { treapSetOf() }
public fun <@Treapable T> treapSetOfNotNull(vararg elements: T?): TreapSet<T> = treapSetOf<T>() + elements.filterNotNull()

public fun <@Treapable T> treapSetBuilderOf(): TreapSet.Builder<T> = treapSetOf<T>().builder()
public fun <@Treapable T> treapSetBuilderOf(vararg elements: T): TreapSet.Builder<T> = treapSetOf<T>().builder().apply { addAll(elements) }

public fun <@Treapable T> Iterable<T>.toTreapSet(): TreapSet<T> = treapSetOf<T>() + this
public fun <@Treapable T> Sequence<T>.toTreapSet(): TreapSet<T> = treapSetOf<T>() + this

public fun CharSequence.toTreapSet(): TreapSet<Char> = treapSetOf<Char>().mutate { this.toCollection(it) }

public fun <@Treapable T> TreapSet<T>?.orEmpty(): TreapSet<T> = this ?: treapSetOf()

public inline fun <T> TreapSet<T>.mutate(mutator: (MutableSet<T>) -> Unit): TreapSet<T> = builder().apply(mutator).build()

public infix fun <T> TreapSet<T>.union(other: TreapSet<T>): TreapSet<T> = addAll(other)
public infix fun <T> TreapSet<T>.union(elements: Iterable<T>): TreapSet<T> =
    if (elements is Collection) { addAll(elements) } else { mutate { it.addAll(elements) }}


public infix fun <T> TreapSet<T>.intersect(other: TreapSet<T>): TreapSet<T> = retainAll(other)
public infix fun <T> TreapSet<T>.intersect(elements: Iterable<T>): TreapSet<T> =
    if (elements is Collection) { retainAll(elements) } else { mutate { it.retainAll(elements) } }

public infix fun <T> TreapSet<T>.subtract(other: TreapSet<T>): TreapSet<T> = removeAll(other)
public infix fun <T> TreapSet<T>.subtract(elements: Iterable<T>): TreapSet<T> =
    if (elements is Collection) { removeAll(elements) } else { mutate { it.removeAll(elements) }}

public operator fun <T> TreapSet<T>.plus(element: T): TreapSet<T> = add(element)
public operator fun <T> TreapSet<T>.plus(other: TreapSet<T>): TreapSet<T> = union(other)
public operator fun <T> TreapSet<T>.plus(elements: Iterable<T>): TreapSet<T> = union(elements)
public operator fun <T> TreapSet<T>.plus(elements: Array<out T>): TreapSet<T> = mutate { it.addAll(elements) }
public operator fun <T> TreapSet<T>.plus(elements: Sequence<T>): TreapSet<T> = mutate { it.addAll(elements) }

public operator fun <T> TreapSet<T>.minus(element: T): TreapSet<T> = remove(element)
public operator fun <T> TreapSet<T>.minus(other: TreapSet<T>): TreapSet<T> = subtract(other)
public operator fun <T> TreapSet<T>.minus(elements: Iterable<T>): TreapSet<T> = subtract(elements)
public operator fun <T> TreapSet<T>.minus(elements: Array<out T>): TreapSet<T> = mutate { it.removeAll(elements) }
public operator fun <T> TreapSet<T>.minus(elements: Sequence<T>): TreapSet<T> = mutate { it.removeAll(elements) }

public fun <T> TreapSet<T>.retainAll(predicate: (T) -> Boolean): TreapSet<T> =
    this.removeAll { !predicate(it) }

@Suppress("UNCHECKED_CAST")
public inline fun <reified R> TreapSet<*>.filterIsInstance(): TreapSet<R> =
    this.retainAll { it is R } as TreapSet<R>
