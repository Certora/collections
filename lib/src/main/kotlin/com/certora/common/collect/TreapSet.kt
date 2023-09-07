package com.certora.common.collect

import com.certora.common.utils.*
import kotlinx.collections.immutable.PersistentSet

/**
 * Base class for Treap-based PersistentSet implementations.  Provides the Set operations; derived classes deal
 * with type-specific behavior such as hash collisions.  See `Treap` for an overview of all of this.
 */
internal abstract class TreapSet<E, S : TreapSet<E, S>>(
    left: S?, 
    right: S?
) : PersistentSet<E>, InternSet<E>, Treap<S>(left, right) {
    /**
     * In order to reduce heap space usage, we derive from Treap.  That makes it tricky to have a TreapSet representing
     * an empty set.  To handle that, we create special subclasses to represent empty sets, and distinguish them
     * via this property.  `treap` returns `this` if this is a "real" node containing data, and `null` if this is
     * an empty node.  We do it this way because it works out very nicely with interacting with the base Treap
     * methods, which use `null` Treaps to represent "nothing."
     */
    abstract val treap: S?

    /**
     * Derived classes override to create an apropriate node containing the given element
     */
    abstract fun new(element: E): S

    /**
     * Casts the given Collection to a TreapSet, if the Collection is already a TreapSet of the same type as 'this'
     * TreapSet.  For example, if this is a HashTreapSet, and so is the supplied collection.  Otherwise returns null.
     */
    abstract fun Iterable<E>.toTreapSetOrNull(): S?

    /**
     * Given a collection, calls the supplied `action` if the collection is a Treap of the same type as this Treap,
     * otherwise calls `fallback.`  Used to implement optimized operations over two compatible Treaps, with a fallback
     * when needed.
     */
    private inline fun <R> Iterable<E>.useAsTreap(action: (S?) -> R, fallback: () -> R): R {
        val treapSet = this.toTreapSetOrNull()
        return if (treapSet != null) {
            action(treapSet.treap)
        } else {
            fallback()
        }
    }

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

    abstract fun shallowGetSingleElement(): Any?

    abstract infix fun shallowUnion(that: S): S
    abstract infix fun shallowIntersect(that: S): S?
    abstract fun shallowContainsAll(elements: S): Boolean
    abstract fun shallowContainsAny(elements: S): Boolean


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Set opreations start here

    override val size: Int get() = treap.computeSize()

    override fun isEmpty(): Boolean = treap == null

    override fun builder(): TreapSetBuilder<E, S> = TreapSetBuilder(self)

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
        treap.find(element.toTreapKey())?.shallowContains(element) ?: false

    override fun containsAll(elements: Collection<E>): Boolean = elements.useAsTreap(
        { elementsTreap -> this.treap.containsAll(elementsTreap) },
        { elements.all { this.contains(it) }}
    )

    fun containsAny(elements: Iterable<E>): Boolean = elements.useAsTreap(
        { elementsTreap -> this.treap.containsAny(elementsTreap) },
        { elements.any { this.contains(it) }}
    )

    override fun add(element: E): S = treap.add(new(element))

    override fun addAll(elements: Collection<E>): S = elements.useAsTreap(
        { elementsTreap -> (this.treap union elementsTreap) ?: clear() },
        { elements.fold(self) { t, e -> t.add(e)}}
    )

    override fun remove(element: E): S =
        treap.remove(element.toTreapKey(), element) ?: clear()

    override fun removeAll(elements: Collection<E>): S = elements.useAsTreap(
        { elementsTreap -> (this.treap difference elementsTreap) ?: clear() },
        { elements.fold(self) { t, e -> t.remove(e) }}
    )

    override fun removeAll(predicate: (E) -> Boolean): S =
        treap.removeAll(predicate.uncheckedAs<(Any?) -> Boolean>()) ?: clear()

    // Require clear() to return a TreapSet
    override abstract fun clear(): S

    override fun retainAll(elements: Collection<E>): S = elements.useAsTreap(
        { elementsTreap -> (this.treap intersect elementsTreap) ?: clear() },
        { this.removeAll { it !in elements } }
    )

    override fun findEqual(element: E): E? =
        treap.find(element.toTreapKey())?.shallowFindEqual(element)

    fun single(): E = treap?.getSingleElement()?.uncheckedAs() ?: when {
        isEmpty() -> throw NoSuchElementException("Set is empty")
        size > 1 -> throw IllegalArgumentException("Set has more than one element")
        else -> null.uncheckedAs<E>()
    }

    fun singleOrNull(): E? = treap?.getSingleElement().uncheckedAs()

    fun forEachElement(action: (element: E) -> Unit): Unit {
        if (treap != null) { shallowForEach(action) }
        left?.forEachElement(action)
        right?.forEachElement(action)
    }

    fun getSingleElement(): Any? = when {
        left === null && right === null -> shallowGetSingleElement()
        else -> null
    }
}

/**
 * Computes the union of two TreapSets.  When nodes have equal keys, they are combined by calling `shallowUnion`, which
 * derived classes use to, e.g, merge hash buckets. Note that we always prefer to return 'this' over 'that', to
 * preserve the object identity invariant described in the `Treap` summary.
 */
internal infix fun <E, S : TreapSet<E, S>> S?.union(that: S?): S? = when {
    this == null -> that
    that == null -> this
    this === that -> this
    that.getSingleElement() != null -> add(that)
    else -> {
        // remember, a.comparePriorityTo(b)==0 <=> a.compareKeyTo(b)==0
        val c = this.comparePriorityTo(that)
        when {
            c > 0 -> unionMerge(this, that)
            c < 0 -> unionMerge(that, this)
            else -> unionMerge(this shallowUnion that, that)
        }
    }
}

private fun <E, S : TreapSet<E, S>> unionMerge(higher: Treap<S>, lower: Treap<S>) =
    // Note that the "higher" key can not occur in "lower", because if it did it wouldn't have a higher priority.
    // We don't need to worry about the split's `duplicate` field.
    lower.split(higher).let { lowerSplit ->
        higher.with(higher.left union lowerSplit.left, higher.right union lowerSplit.right)
    }

/**
 * Computes the intersection of two treaps. Note that we always prefer to return 'this' over 'that', to preserve the
 * object identity invariant described in the `Treap` summary.
 */
internal infix fun <E, S : TreapSet<E, S>> S?.intersect(that: S?): S? = when {
    this == null -> null
    that == null -> null
    this === that -> this
    else -> {
        // remember, a.comparePriorityTo(b)==0 <=> a.compareKeyTo(b)==0
        val c = this.comparePriorityTo(that)
        when {
            c > 0 -> intersectMerge(this, that)
            c < 0 -> intersectMerge(that, this)
            else -> {
                val newLeft = this.left intersect that.left
                val newRight = this.right intersect that.right
                val newThis = this.shallowIntersect(that)
                when {
                    newThis == null -> newLeft join newRight
                    else -> newThis.with(newLeft, newRight)
                }
            }
        }
    }
}

private fun <E, S : TreapSet<E, S>> intersectMerge(higher: Treap<S>, lower: Treap<S>) =
    // Note that the "higher" key can not occur in "lower", because if it did it wouldn't have a higher priority.
    // We don't need to worry about the split's `duplicate` field.
    lower.split(higher).let { lowerSplit ->
        (higher.left intersect lowerSplit.left) join (higher.right intersect lowerSplit.right)
    }

/**
 * Checks if this Treap contains all items in another Treap.  This should be equivalent to:
 *
 *   (this union that) === this
 *
 * ...except that we don't want to do all of the work that would imply, if we can avoid it.
 */
internal fun <E, S : TreapSet<E, S>> S?.containsAll(that: S?): Boolean = when {
    that == null -> true
    this == null -> false
    else -> {
        this.split(that).let {
            it.duplicate?.shallowContainsAll(that) == true &&
            it.left.containsAll(that.left) &&
            it.right.containsAll(that.right)
        }
    }
}

/**
 * Checks if this Treap contains any of the items in another Treap.  This should be equivalent to:
 *
 *   (this difference that) !== this
 *
 * ...except that we don't want to do all of the work that would imply, if we can avoid it.
 */
internal fun <E, S : TreapSet<E, S>> S?.containsAny(that: S?): Boolean = when {
    that == null -> false
    this == null -> false
    else -> {
        that.split(this).let {
            it.duplicate?.let { this.shallowContainsAny(it) } == true ||
            this.left.containsAny(it.left) ||
            this.right.containsAny(it.right)
        }
    }
}

