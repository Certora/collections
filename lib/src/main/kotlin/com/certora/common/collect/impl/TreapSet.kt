package com.certora.common.collect.impl

import com.certora.common.collect.*
import com.certora.common.utils.internal.*
import kotlinx.collections.immutable.PersistentSet

/**
 * Base class for Treap-based PersistentSet implementations.  Provides the Set operations; derived classes deal
 * with type-specific behavior such as hash collisions.  See `Treap` for an overview of all of this.
 */
internal abstract class TreapSet<E>(left: Treap?, right: Treap?) : PersistentSet<E>, InternSet<E>, Treap(left, right) {
    /**
     * In order to reduce heap space usage, we derive from Treap.  That makes it tricky to have a TreapSet representing
     * an empty set.  To handle that, we create special subclasses to represent empty sets, and distinguish them
     * via this property.  `treap` returns `this` if this is a "real" node containing data, and `null` if this is
     * an empty node.  We do it this way because it works out very nicely with interacting with the base Treap
     * methods, which use `null` Treaps to represent "nothing."
     */
    abstract val treap: Treap?

    /**
     * Derived classes override to create an apropriate node containing the given element
     */
    abstract fun new(element: E): TreapSet<E>

    /**
     * Casts the given Collection to a TreapSet, if the Collection is already a TreapSet of the same type as 'this'
     * TreapSet.  For example, if this is a HashTreapSet, and so is the supplied collection.  Otherwise returns null.
     */
    abstract fun Iterable<E>.toTreapSetOrNull(): TreapSet<E>?

    /**
     * Given a collection, calls the supplied `action` if the collection is a Treap of the same type as this Treap,
     * otherwise calls `fallback.`  Used to implement optimized operations over two compatible Treaps, with a fallback
     * when needed.
     */
    private inline fun <R> Iterable<E>.useAsTreap(action: (Treap?) -> R, fallback: () -> R): R {
        val treapSet = this.toTreapSetOrNull()
        return if (treapSet != null) {
            action(treapSet.treap)
        } else {
            fallback()
        }
    }

    private val leftSet get() = left.uncheckedAs<TreapSet<E>?>()
    private val rightSet get() = right.uncheckedAs<TreapSet<E>?>()
    private fun Treap?.asSet() = uncheckedAs<PersistentSet<E>?>() ?: clear()

    /**
     * Converts the supplied set element to a TreapKey appropriate to this type of TreapSet (sorted vs. hashed)
     */
    abstract fun E.toTreapKey(): TreapKey

    /**
     * Does this node contain the element?
     */
    abstract fun shallowContains(element: E): Boolean

    /**
     * Get an element from this node that is equal to the given object
     */
    abstract fun shallowFindEqual(element: E): E?

    /**
     * Apply the action to each element in this node
     */
    abstract fun shallowForEach(action: (element: E) -> Unit): Unit

    /**
     * TreapSet doesn't support these operations
     */
    override fun shallowRemoveEntry(key: Any?, value: Any?): Treap? = throw UnsupportedOperationException()
    override fun shallowUpdate(entryKey: Any?, toUpdate: Any?, merger: (Any?, Any?) -> Any?): Treap = throw UnsupportedOperationException()

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Set opreations start here

    override val size: Int get() = treap.computeSize()

    override fun isEmpty(): Boolean = treap == null

    override fun builder(): PersistentSet.Builder<E> = SimplePersistentSetBuilder(this)

    override fun toString(): String = joinToString(", ", "[", "]") {
        if (it === this) { "(this Collection)" } else { it.toString() }
    }

    override fun equals(other: Any?): Boolean = when {
        other == null -> false
        this === other -> true
        other !is Set<*> -> false
        else -> other.uncheckedAs<Set<E>>().useAsTreap(
            { otherTreap -> this.treap.deepEquals(otherTreap) },
            { this.size == other.size && this.containsAll(other) }
        )
    }

    override fun hashCode(): Int = treap.computeHashCode()

    override fun contains(element: E): Boolean =
        treap.find(element.toTreapKey()).uncheckedAs<TreapSet<E>?>()?.shallowContains(element) ?: false

    override fun containsAll(elements: Collection<E>): Boolean = elements.useAsTreap(
        { elementsTreap -> this.treap.containsAll(elementsTreap) },
        { elements.all { this.contains(it) }}
    )

    fun containsAny(elements: Iterable<E>): Boolean = elements.useAsTreap(
        { elementsTreap -> this.treap.containsAny(elementsTreap) },
        { elements.any { this.contains(it) }}
    )

    override fun add(element: E): PersistentSet<E> = treap.add(new(element)).uncheckedAs<PersistentSet<E>>()

    override fun addAll(elements: Collection<E>): PersistentSet<E> = elements.useAsTreap(
        { elementsTreap -> (this.treap union elementsTreap).asSet() },
        { elements.fold(this as PersistentSet<E>, { t, e -> t.add(e)} ) }
    )

    override fun remove(element: E): PersistentSet<E> =
        treap.remove(element.toTreapKey(), element).asSet()

    override fun removeAll(elements: Collection<E>): PersistentSet<E> = elements.useAsTreap(
        { elementsTreap -> (this.treap difference elementsTreap).asSet() },
        { elements.fold(this as PersistentSet<E>, { t, e -> t.remove(e) }) }
    )

    override fun removeAll(predicate: (E) -> Boolean): PersistentSet<E> =
        treap.removeAll(predicate.uncheckedAs<(Any?) -> Boolean>()).asSet()

    override fun retainAll(elements: Collection<E>): PersistentSet<E> = elements.useAsTreap(
        { elementsTreap -> (this.treap intersect elementsTreap).asSet() },
        { this.removeAll { it !in elements } }
    )

    override fun findEqual(element: E): E? =
        treap.find(element.toTreapKey()).uncheckedAs<TreapSet<E>?>()?.shallowFindEqual(element)

    fun single(): E = treap?.getSingleElement()?.uncheckedAs() ?: when {
        isEmpty() -> throw NoSuchElementException("Set is empty")
        size > 1 -> throw IllegalArgumentException("Set has more than one element")
        else -> null.uncheckedAs<E>()
    }

    fun singleOrNull(): E? = treap?.getSingleElement().uncheckedAs()

    fun forEachElement(action: (element: E) -> Unit): Unit {
        if (treap != null) { shallowForEach(action) }
        leftSet?.forEachElement(action)
        rightSet?.forEachElement(action)
    }
}
