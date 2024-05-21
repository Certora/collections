package com.certora.collect

import kotlinx.collections.immutable.PersistentSet

/**
    A TreapSet specific to Comparable elements.  Iterates in the order defined by the objects.  We store one element per
    Treap node, with the element itself as the Treap key.
 */
internal class SortedTreapSet<@Treapable E : Comparable<E>>(
    override val treapKey: E,
    left: SortedTreapSet<E>? = null,
    right: SortedTreapSet<E>? = null
) : AbstractTreapSet<E, SortedTreapSet<E>>(left, right), TreapKey.Sorted<E> {

    override fun hashCode(): Int = computeHashCode()

    override fun E.toTreapKey() = TreapKey.Sorted.FromKey(this)
    override fun new(element: E): SortedTreapSet<E> = SortedTreapSet(element)

    override fun add(element: E): TreapSet<E> = when {
        element is PrefersHashTreap -> HashTreapSet(element as E) + this
        else -> self.add(new(element))
    }

    override fun Iterable<E>.toTreapSetOrNull(): SortedTreapSet<E>? =
        (this as? SortedTreapSet<E>)
        ?: (this as? PersistentSet.Builder<E>)?.build() as? SortedTreapSet<E>

    override val self get() = this
    override fun iterator(): Iterator<E> = this.asTreapSequence().map { it.treapKey }.iterator()

    override fun shallowEquals(that: SortedTreapSet<E>): Boolean = this.compareKeyTo(that) == 0
    override val shallowSize: Int get() = 1

    override fun copyWith(left: SortedTreapSet<E>?, right: SortedTreapSet<E>?): SortedTreapSet<E> = SortedTreapSet(treapKey, left, right)

    // Since these are only called for Treap nodes with the same key, and each of our nodes stores a single element,
    // these are trivial.
    override fun shallowContains(element: E) = true
    override fun shallowContainsAll(elements: SortedTreapSet<E>) = true
    override fun shallowContainsAny(elements: SortedTreapSet<E>) = true
    override fun shallowFindEqual(element: E) = treapKey.takeIf { it == element }
    override fun shallowAdd(that: SortedTreapSet<E>) = this
    override fun shallowUnion(that: SortedTreapSet<E>) = this
    override fun shallowDifference(that: SortedTreapSet<E>) = null
    override fun shallowIntersect(that: SortedTreapSet<E>) = this
    override fun shallowRemove(element: E): SortedTreapSet<E>? = null
    override fun shallowRemoveAll(predicate: (E) -> Boolean): SortedTreapSet<E>? = this.takeIf { !predicate(treapKey) }
    override fun shallowComputeHashCode(): Int = treapKey.hashCode()
    override fun shallowGetSingleElement(): E = treapKey
    override fun shallowForEach(action: (element: E) -> Unit): Unit { action(treapKey) }
    override fun <R : Any> shallowMapReduce(map: (E) -> R, reduce: (R, R) -> R): R = map(treapKey)
}
