package com.certora.common.collect

import com.certora.common.collect.*
import com.certora.common.utils.*
import kotlinx.collections.immutable.PersistentSet

/**
 * A TreapSet specific to Comparable elements.  Iterates in the order defined by the objects.  We store one element
 * per Treap node, with the element itself as the Treap key.
 */
internal sealed class SortedTreapSet<@WithStableHashCodeIfSerialized E : Comparable<E>> private constructor(
    left: Treap? = null,
    right: Treap? = null
) : TreapSet<E>(left, right), TreapKey.Sorted<E> {

    override fun E.toTreapKey() = TreapKey.Sorted.FromKey(this)
    override fun clear() = emptyOf<E>()
    override fun new(element: E): TreapSet<E> = Node(element)

    override fun Iterable<E>.toTreapSetOrNull(): TreapSet<E>? =
        (this as? SortedTreapSet<E>)
        ?: (this as? PersistentSet.Builder<E>)?.build() as? SortedTreapSet<E>

    private class Node<@WithStableHashCodeIfSerialized E : Comparable<E>>(
        override val treapKey: E,
        left: Treap? = null,
        right: Treap? = null
    ) : SortedTreapSet<E>(left, right) {
        override val treap get() = this
        override fun iterator(): Iterator<E> = treap.asSequence().map { it.treapKey.uncheckedAs<E>() }.iterator()

        override fun shallowEquals(that: Treap): Boolean = this.compareKeyTo(that) == 0
        override val shallowSize: Int get() = 1

        override fun copyWith(left: Treap?, right: Treap?): Treap = Node(treapKey, left, right)

        // Since these are only called for Treap nodes with the same key, and each of our nodes stores a single element,
        // these are trivial.
        override fun shallowContains(element: E) = true
        override fun shallowContainsAll(elements: Treap) = true
        override fun shallowContainsAny(elements: Treap) = true
        override fun shallowFindEqual(element: E) = treapKey.takeIf { it == element }
        override fun shallowAdd(that: Treap) = this
        override fun shallowUnion(that: Treap) = this
        override fun shallowDifference(that: Treap) = null
        override fun shallowIntersect(that: Treap) = this
        override fun shallowRemove(element: Any?): Treap? = null
        override fun shallowRemoveAll(predicate: (Any?) -> Boolean): Treap? = this.takeIf { !predicate(treapKey) }
        override fun shallowComputeHashCode(): Int = treapKey.hashCode()
        override fun shallowGetSingleElement(): E = treapKey
        override fun shallowForEach(action: (element: E) -> Unit): Unit { action(treapKey) }
    }

    private class Empty<@WithStableHashCodeIfSerialized E : Comparable<E>> : SortedTreapSet<E>(null, null) {
        override fun iterator(): Iterator<E> = emptySet<E>().iterator()

        // `Empty<E>` is just a placeholder, and should not be used as a treap
        override val treap: Treap? get() = null

        override val treapKey: E get() = throw UnsupportedOperationException()
        override fun copyWith(left: Treap?, right: Treap?): Treap = throw UnsupportedOperationException()
        override val shallowSize: Int get() = throw UnsupportedOperationException()
        override fun shallowEquals(that: Treap): Boolean = throw UnsupportedOperationException()
        override fun shallowContains(element: E): Boolean = throw UnsupportedOperationException()
        override fun shallowContainsAll(elements: Treap): Boolean = throw UnsupportedOperationException()
        override fun shallowContainsAny(elements: Treap): Boolean = throw UnsupportedOperationException()
        override fun shallowFindEqual(element: E): E? = throw UnsupportedOperationException()
        override fun shallowAdd(that: Treap): Treap = throw UnsupportedOperationException()
        override fun shallowUnion(that: Treap): Treap = throw UnsupportedOperationException()
        override fun shallowDifference(that: Treap): Treap? = throw UnsupportedOperationException()
        override fun shallowIntersect(that: Treap): Treap? = throw UnsupportedOperationException()
        override fun shallowRemove(element: Any?): Treap? = throw UnsupportedOperationException()
        override fun shallowRemoveAll(predicate: (Any?) -> Boolean): Treap? = throw UnsupportedOperationException()
        override fun shallowComputeHashCode(): Int = throw UnsupportedOperationException()
        override fun shallowGetSingleElement(): E? = throw UnsupportedOperationException()
        override fun shallowForEach(action: (element: E) -> Unit): Unit = throw UnsupportedOperationException()
    }

    companion object {
        private val _empty = Empty<Nothing>()
        fun <@WithStableHashCodeIfSerialized E : Comparable<E>> emptyOf() = _empty.uncheckedAs<SortedTreapSet<E>>()
    }
}

