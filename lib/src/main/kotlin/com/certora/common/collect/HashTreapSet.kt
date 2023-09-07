package com.certora.common.collect

import com.certora.common.collect.*
import com.certora.common.utils.*
import kotlinx.collections.immutable.PersistentSet

/**
 * A TreapSet for elements that do not have a total ordering defined by implementing Comparable.  For those,
 * we use the objects' hash codes as Treap keys, and deal with collisions by chaining multiple elements from a single
 * Treap node.
 *
 * The HashTreapSet instance itself stores the first element, and additional elements are chained via MoreElements. This
 * is just a simple linked list, so operations on it are either O(N) or O(N^2), but collisions are assumed to be rare
 * enough that these lists will be very small - usually just one element.
 */
internal sealed class HashTreapSet<@WithStableHashCodeIfSerialized E> private constructor(
    left: Treap? = null,
    right: Treap? = null
) : TreapSet<E>(left, right), TreapKey.Hashed<E> {

    override fun E.toTreapKey() = TreapKey.Hashed.FromKey(this)
    override fun clear() = emptyOf<E>()
    override fun new(element: E): TreapSet<E> = Node(element)

    override fun Iterable<E>.toTreapSetOrNull(): HashTreapSet<E>? =
        (this as? HashTreapSet<E>)
        ?: (this as? PersistentSet.Builder<E>)?.build() as? HashTreapSet<E>

    protected interface ElementList<E> {
        val element: E
        val next: MoreElements<E>?
    }

    protected class MoreElements<E>(
        override val element: E,
        override val next: MoreElements<E>?
    ) : ElementList<E>, java.io.Serializable

    protected inline fun ElementList<E>?.forEachNodeElement(action: (E) -> Unit) {
        var current = this
        while (current != null) {
            action(current.element)
            current = current.next
        }
    }

    private class Node<@WithStableHashCodeIfSerialized E>(
        override val element: E,
        override val next: MoreElements<E>? = null,
        left: Treap? = null,
        right: Treap? = null
    ) : HashTreapSet<E>(left, right), ElementList<E> {
        override val treap get() = this
        override val treapKey get() = element

        override val shallowSize: Int get() {
            var count = 0
            forEachNodeElement {
                ++count
            }
            return count
        }

        override fun copyWith(left: Treap?, right: Treap?): Treap = Node(element, next, left, right)

        fun withElement(element: E) = when {
            this.shallowContains(element) -> this
            else -> Node(this.element, MoreElements(element, this.next), this.left, this.right)
        }

        override fun shallowEquals(that: Treap): Boolean {
            val thatSet = that.uncheckedAs<Node<E>>()
            forEachNodeElement {
                if (!thatSet.shallowContains(it)) {
                    return false
                }
            }
            return this.shallowSize == that.shallowSize
        }

        override fun shallowFindEqual(element: E): E? {
            forEachNodeElement {
                if (it == element) {
                    return it
                }
            }
            return null
        }

        override fun shallowForEach(action: (element: E) -> Unit): Unit {
            forEachNodeElement { action(it) }
        }

        override fun shallowContains(element: E): Boolean {
            forEachNodeElement {
                if (it == element) {
                    return true
                }
            }
            return false
        }

        override fun shallowContainsAll(elements: Treap): Boolean {
            elements.uncheckedAs<Node<E>>().forEachNodeElement {
                if (!this.shallowContains(it)) {
                    return false
                }
            }
            return true
        }

        override fun shallowContainsAny(elements: Treap): Boolean {
            elements.uncheckedAs<Node<E>>().forEachNodeElement {
                if (this.shallowContains(it)) {
                    return true
                }
            }
            return false
        }

        override fun shallowAdd(that: Treap): Treap {
            // add is only called with a single element
            val thatNode = that.uncheckedAs<Node<E>>()
            check (thatNode.next == null) { "add with multiple elements?" }
            return this.withElement(thatNode.element)
        }

        override fun shallowUnion(that: Treap): Treap {
            var result = this
            that.uncheckedAs<Node<E>>().forEachNodeElement {
                result = result.withElement(it)
            }
            return result
        }

        override fun shallowDifference(that: Treap): Treap? {
            val thatSet = that.uncheckedAs<Node<E>>()

            // Fast path for the most common case
            if (this.next == null) {
                if (thatSet.shallowContains(this.element)) {
                    return null
                } else {
                    return this
                }
            }

            var result: Node<E>? = null
            var changed = false
            this.forEachNodeElement {
                if (!thatSet.shallowContains(it)) {
                    result = result?.withElement(it) ?: Node(it, null, left, right)
                } else {
                    changed = true
                }
            }

            if (changed) {
                return result
            } else {
                return this
            }
        }

        override fun shallowIntersect(that: Treap): Treap? {
            val thatSet = that.uncheckedAs<Node<E>>()

            // Fast path for the most common case
            if (this.next == null) {
                if (thatSet.shallowContains(this.element)) {
                    return this
                } else {
                    return null
                }
            }

            var result: Node<E>? = null
            var changed = false
            this.forEachNodeElement {
                if (thatSet.shallowContains(it)) {
                    result = result?.withElement(it) ?: Node(it, null, left, right)
                } else {
                    changed = true
                }
            }

            if (changed) {
                return result
            } else {
                return this
            }
        }

        override fun shallowRemove(element: Any?): Treap? {
            // Fast path for the most common case
            if (this.next == null) {
                if (this.element == element) {
                    return null
                } else {
                    return this
                }
            }

            var result: Node<E>? = null
            var changed = false
            this.forEachNodeElement {
                if (it != element) {
                    result = result?.withElement(it) ?: Node(it, null, left, right)
                } else {
                    changed = true
                }
            }

            if (changed) {
                return result
            } else {
                return this
            }
        }

        override fun shallowRemoveAll(predicate: (Any?) -> Boolean): Treap? {
            var result: Node<E>? = null
            var removed = false
            this.forEachNodeElement {
                if (predicate(it)) {
                    removed = true
                } else {
                    result = result?.withElement(it) ?: Node(it, null, left, right)
                }
            }
            if (removed) {
                return result
            } else {
                return this
            }
        }

        override fun shallowComputeHashCode(): Int {
            var h = 0
            forEachNodeElement { h += it.hashCode() }
            return h
        }

        override fun iterator(): Iterator<E> = sequence {
            treap.asSequence().forEach { node ->
                node.uncheckedAs<Node<E>>().forEachNodeElement {
                    yield(it)
                }
            }
        }.iterator()

        override fun shallowGetSingleElement(): E? = element.takeIf { next == null }
    }

    private class Empty<@WithStableHashCodeIfSerialized E> : HashTreapSet<E>(null, null) {
        override fun iterator(): Iterator<E> = emptySet<E>().iterator()

        // `Empty<E>` is just a placeholder, and should not be used as a treap
        override val treap: Treap? get() = null

        override val treapKey: E get() = throw UnsupportedOperationException()
        override fun copyWith(left: Treap?, right: Treap?): Treap = throw UnsupportedOperationException()
        override val shallowSize: Int get() = throw UnsupportedOperationException()
        override fun shallowEquals(that: Treap): Boolean = throw UnsupportedOperationException()
        override fun shallowFindEqual(element: E): E? = throw UnsupportedOperationException()
        override fun shallowContains(element: E): Boolean = throw UnsupportedOperationException()
        override fun shallowContainsAll(elements: Treap): Boolean = throw UnsupportedOperationException()
        override fun shallowContainsAny(elements: Treap): Boolean = throw UnsupportedOperationException()
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
        fun <@WithStableHashCodeIfSerialized E> emptyOf() = _empty.uncheckedAs<HashTreapSet<E>>()
    }
}

