package com.certora.collect

import kotlinx.collections.immutable.PersistentSet

/**
    A TreapSet specific to Comparable elements.  Iterates in the order defined by the objects.  We store one element per
    Treap node, with the element itself as the Treap key.
 */
internal abstract class AbstractSortedTreapSet<@Treapable E>
    : AbstractTreapSet<E, AbstractSortedTreapSet<E>>(), TreapKey.Sorted<E> {

    override fun hashCode(): Int = computeHashCode()

    override fun E.toTreapKey() = TreapKey.Sorted.fromKey(this)
    override fun new(element: E): SortedTreapSet<E> = SortedTreapSet(element)

    override fun add(element: E): TreapSet<E> = when(element) {
        !is Comparable<*>?, is PrefersHashTreap -> HashTreapSet(element) + this
        else -> self.add(new(element))
    }

    override fun Iterable<E>.toTreapSetOrNull(): SortedTreapSet<E>? =
        (this as? SortedTreapSet<E>)
        ?: (this as? PersistentSet.Builder<E>)?.build() as? SortedTreapSet<E>

    override val self get() = this
    override fun iterator(): Iterator<E> = this.asTreapSequence().map { it.treapKey }.iterator()

    override fun shallowEquals(that: AbstractSortedTreapSet<E>): Boolean = this.compareKeyTo(that) == 0
    override val shallowSize: Int get() = 1

    override fun copyWith(left: AbstractSortedTreapSet<E>?, right: AbstractSortedTreapSet<E>?) =
        SortedTreapSet(treapKey, left, right)

    // Since these are only called for Treap nodes with the same key, and each of our nodes stores a single element,
    // these are trivial.
    override fun shallowContains(element: E) = true
    override fun shallowContainsAll(elements: AbstractSortedTreapSet<E>) = true
    override fun shallowContainsAny(elements: AbstractSortedTreapSet<E>) = true
    override fun shallowFindEqual(element: E) = treapKey.takeIf { it == element }
    override fun shallowAdd(that: AbstractSortedTreapSet<E>) = this
    override fun shallowUnion(that: AbstractSortedTreapSet<E>) = this
    override fun shallowDifference(that: AbstractSortedTreapSet<E>) = null
    override fun shallowIntersect(that: AbstractSortedTreapSet<E>) = this
    override fun shallowRemove(element: E) = null
    override fun shallowRemoveAll(predicate: (E) -> Boolean) = this.takeIf { !predicate(treapKey) }
    override fun shallowComputeHashCode() = treapKey.hashCode()
    override fun shallowGetSingleElement() = treapKey
    override fun arbitraryOrNull() = treapKey
    override fun shallowForEach(action: (element: E) -> Unit) { action(treapKey) }
    override fun <R : Any> shallowMapReduce(map: (E) -> R, reduce: (R, R) -> R) = map(treapKey)

    override fun containsAny(predicate: (E) -> Boolean): Boolean =
        predicate(treapKey) || left?.containsAny(predicate) == true || right?.containsAny(predicate) == true
}

internal class SortedTreapSet<@Treapable E>(
    override val treapKey: E,
    override val left: AbstractSortedTreapSet<E>? = null,
    override val right: AbstractSortedTreapSet<E>? = null
) : AbstractSortedTreapSet<E>()
