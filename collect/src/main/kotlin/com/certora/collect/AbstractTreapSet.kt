package com.certora.collect

/**
    Base class for TreapSet implementations.  Provides the Set operations; derived classes deal with type-specific
    behavior such as hash collisions.  See `Treap` for an overview of all of this.
 */
internal abstract class AbstractTreapSet<@Treapable E, S : AbstractTreapSet<E, S>>(
    left: S?, 
    right: S?
) : TreapSet<E>, Treap<E, S>(left, right) {
    /**
        Derived classes override to create an apropriate node containing the given element.
     */
    abstract fun new(element: E): S

    /**
        Casts the given Collection to a AbstractTreapSet, if the Collection is already a AbstractTreapSet of the same
        type as 'this' AbstractTreapSet.  For example, if this is a HashTreapSet, and so is the supplied collection.
        Otherwise returns null.
     */
    abstract fun Iterable<E>.toTreapSetOrNull(): S?

    /**
        Given a collection, calls the supplied `action` if the collection is a Treap of the same type as this Treap,
        otherwise calls `fallback.`  Used to implement optimized operations over two compatible Treaps, with a fallback
        when needed.
     */
    private inline fun <R> Iterable<E>.useAsTreap(action: (S?) -> R, fallback: () -> R): R {
        val treapSet = this.toTreapSetOrNull()
        return if (treapSet != null) {
            action(treapSet.selfNotEmpty)
        } else {
            fallback()
        }
    }

    /**
        Converts the supplied set element to a TreapKey appropriate to this type of AbstractTreapSet (sorted vs. hashed)
     */
    abstract fun E.toTreapKey(): TreapKey<E>

    /**
        Does this node contain the element?
     */
    abstract fun shallowContains(element: E): Boolean

    /**
        Get an element from this node that is equal to the given object.
     */
    abstract fun shallowFindEqual(element: E): E?

    /**
        Apply the action to each element in this node.
     */
    abstract fun shallowForEach(action: (element: E) -> Unit): Unit

    abstract fun shallowGetSingleElement(): E?

    abstract infix fun shallowUnion(that: S): S
    abstract infix fun shallowIntersect(that: S): S?
    abstract infix fun shallowDifference(that: S): S?
    abstract fun shallowContainsAll(elements: S): Boolean
    abstract fun shallowContainsAny(elements: S): Boolean


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Set opreations start here

    override val size: Int get() = selfNotEmpty.computeSize()

    override fun isEmpty(): Boolean = selfNotEmpty == null

    override fun builder(): TreapSet.Builder<E> = TreapSetBuilder(self)

    override fun toString(): String = joinToString(", ", "[", "]") {
        if (it === this) { "(this Collection)" } else { it.toString() }
    }

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?): Boolean = when {
        other == null -> false
        this === other -> true
        other !is Set<*> -> false
        else -> (other as Set<E>).useAsTreap(
            { otherTreap -> this.selfNotEmpty.deepEquals(otherTreap) },
            { this.size == other.size && this.containsAll(other) }
        )
    }

    override fun hashCode(): Int = selfNotEmpty.computeHashCode()

    override fun contains(element: E): Boolean =
        selfNotEmpty.find(element.toTreapKey())?.shallowContains(element) ?: false

    override fun containsAll(elements: Collection<E>): Boolean = elements.useAsTreap(
        { elementsTreap -> selfNotEmpty.containsAll(elementsTreap) },
        { elements.all { this.contains(it) }}
    )

    override fun containsAny(elements: Iterable<E>): Boolean = elements.useAsTreap(
        { elementsTreap -> selfNotEmpty.containsAny(elementsTreap) },
        { elements.any { this.contains(it) }}
    )

    override fun add(element: E): S = selfNotEmpty.add(new(element))

    override fun addAll(elements: Collection<E>): S = elements.useAsTreap(
        { elementsTreap -> (selfNotEmpty union elementsTreap) ?: clear() },
        { elements.fold(self) { t, e -> t.add(e)}}
    )

    override fun remove(element: E): S =
        selfNotEmpty.remove(element.toTreapKey(), element) ?: clear()

    override fun removeAll(elements: Collection<E>): S = elements.useAsTreap(
        { elementsTreap -> (selfNotEmpty difference elementsTreap) ?: clear() },
        { elements.fold(self) { t, e -> t.remove(e) }}
    )

    override fun removeAll(predicate: (E) -> Boolean): S =
        selfNotEmpty.removeAll(predicate) ?: clear()

    // Require clear() to return a AbstractTreapSet
    override abstract fun clear(): S

    override fun retainAll(elements: Collection<E>): S = elements.useAsTreap(
        { elementsTreap -> (selfNotEmpty intersect elementsTreap) ?: clear() },
        { this.removeAll { it !in elements } }
    )

    override fun findEqual(element: E): E? =
        selfNotEmpty.find(element.toTreapKey())?.shallowFindEqual(element)

    @Suppress("UNCHECKED_CAST")
    override fun single(): E = selfNotEmpty?.getSingleElement() ?: when {
        isEmpty() -> throw NoSuchElementException("Set is empty")
        size > 1 -> throw IllegalArgumentException("Set has more than one element")
        else -> null as E // The single element must have been null!
    }

    override fun singleOrNull(): E? = selfNotEmpty?.getSingleElement()

    override fun forEachElement(action: (element: E) -> Unit): Unit {
        if (selfNotEmpty != null) { 
            shallowForEach(action) 
            left?.forEachElement(action)
            right?.forEachElement(action)
        }
    }

    internal fun getSingleElement(): E? = when {
        left === null && right === null -> shallowGetSingleElement()
        else -> null
    }
}

/**
    Computes the union of two TreapSets.  When nodes have equal keys, they are combined by calling `shallowUnion`, which
    derived classes use to, e.g, merge hash buckets. Note that we always prefer to return 'this' over 'that', to
    preserve the object identity invariant described in the `Treap` summary.
 */
internal infix fun <@Treapable E, S : AbstractTreapSet<E, S>> S?.union(that: S?): S? = when {
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

private fun <@Treapable E, S : AbstractTreapSet<E, S>> unionMerge(higher: AbstractTreapSet<E, S>, lower: AbstractTreapSet<E, S>) =
    // Note that the "higher" key can not occur in "lower", because if it did it wouldn't have a higher priority. We
    // don't need to worry about the split's `duplicate` field.
    lower.split(higher).let { lowerSplit ->
        higher.with(higher.left union lowerSplit.left, higher.right union lowerSplit.right)
    }

/**
    Computes the intersection of two treaps. Note that we always prefer to return 'this' over 'that', to preserve the
    object identity invariant described in the `Treap` summary.
 */
internal infix fun <@Treapable E, S : AbstractTreapSet<E, S>> S?.intersect(that: S?): S? = when {
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

private fun <@Treapable E, S : AbstractTreapSet<E, S>> intersectMerge(higher: AbstractTreapSet<E, S>, lower: AbstractTreapSet<E, S>) =
    // Note that the "higher" key can not occur in "lower", because if it did it wouldn't have a higher priority. We
    // don't need to worry about the split's `duplicate` field.
    lower.split(higher).let { lowerSplit ->
        (higher.left intersect lowerSplit.left) join (higher.right intersect lowerSplit.right)
    }

/**
    Removes the items in `that` from `this`.
 */
internal infix fun <@Treapable E, S : AbstractTreapSet<E, S>> S?.difference(that: S?): S? = when {
    this == null -> null
    that == null -> this
    this === that -> null
    else -> {
        val thatSplit = that.split(this)
        val newLeft = this.left difference thatSplit.left
        val newRight = this.right difference thatSplit.right
        val newThis = when {
            thatSplit.duplicate == null -> this
            else -> this.shallowDifference(thatSplit.duplicate!!)
        }
        when {
            newThis == null -> newLeft join newRight
            else -> newThis.with(newLeft, newRight)
        }
    }
}


/**
    Checks if this Treap contains all items in another Treap.  This should be equivalent to:

        (this union that) === this

    ...except that we don't want to do all of the work that would imply, if we can avoid it.
 */
internal fun <@Treapable E, S : AbstractTreapSet<E, S>> S?.containsAll(that: S?): Boolean = when {
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
    Checks if this Treap contains any of the items in another Treap.  This should be equivalent to:

        (this difference that) !== this

    ...except that we don't want to do all of the work that would imply, if we can avoid it.
 */
internal fun <@Treapable E, S : AbstractTreapSet<E, S>> S?.containsAny(that: S?): Boolean = when {
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

