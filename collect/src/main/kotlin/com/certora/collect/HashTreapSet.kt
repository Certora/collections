package com.certora.collect

/**
    A TreapSet for elements that do not have a total ordering defined by implementing Comparable.  For those, we use the
    objects' hash codes as Treap keys, and deal with collisions by chaining multiple elements from a single Treap node.

    The HashTreapSet instance itself stores the first element, and additional elements are chained via More.
    This is just a simple linked list, so operations on it are either O(N) or O(N^2), but collisions are assumed to be
    rare enough that these lists will be very small - usually just one element.
 */
internal class HashTreapSet<@Treapable E>(
    override val element: E,
    override val next: ElementList.More<E>? = null,
    left: HashTreapSet<E>? = null,
    right: HashTreapSet<E>? = null
) : AbstractTreapSet<E, HashTreapSet<E>>(left, right), TreapKey.Hashed<E>, ElementList<E> {

    override fun hashCode(): Int = computeHashCode()

    override fun E.toTreapKey() = TreapKey.Hashed.fromKey(this)
    override fun new(element: E): HashTreapSet<E> = HashTreapSet(element)

    override fun add(element: E): TreapSet<E> = self.add(new(element))

    override fun Iterable<E>.toTreapSetOrNull(): HashTreapSet<E>? =
        (this as? HashTreapSet<E>)
        ?: (this as? TreapSet.Builder<E>)?.build() as? HashTreapSet<E>
        ?: (this as? HashTreapMap<E, *>.KeySet)?.keys?.value

    private inline fun ElementList<E>?.forEachNodeElement(action: (E) -> Unit) {
        var current = this
        while (current != null) {
            action(current.element)
            current = current.next
        }
    }

    override val self get() = this
    override val treapKey get() = element

    override val shallowSize: Int get() {
        var count = 0
        forEachNodeElement {
            ++count
        }
        return count
    }

    override fun copyWith(left: HashTreapSet<E>?, right: HashTreapSet<E>?): HashTreapSet<E> =
        HashTreapSet(element, next, left, right)

    fun withElement(element: E) = when {
        this.shallowContains(element) -> this
        else -> HashTreapSet(this.element, ElementList.More(element, this.next), this.left, this.right)
    }

    override fun shallowEquals(that: HashTreapSet<E>): Boolean {
        forEachNodeElement {
            if (!that.shallowContains(it)) {
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

    override fun shallowContainsAll(elements: HashTreapSet<E>): Boolean {
        elements.forEachNodeElement {
            if (!this.shallowContains(it)) {
                return false
            }
        }
        return true
    }

    override fun shallowContainsAny(elements: HashTreapSet<E>): Boolean {
        elements.forEachNodeElement {
            if (this.shallowContains(it)) {
                return true
            }
        }
        return false
    }

    override fun shallowAdd(that: HashTreapSet<E>): HashTreapSet<E> {
        // add is only called with a single element
        check (that.next == null) { "add with multiple elements?" }
        return this.withElement(that.element)
    }

    override fun shallowUnion(that: HashTreapSet<E>): HashTreapSet<E> {
        var result = this
        that.forEachNodeElement {
            result = result.withElement(it)
        }
        return result
    }

    override fun shallowDifference(that: HashTreapSet<E>): HashTreapSet<E>? {
        // Fast path for the most common case
        if (this.next == null) {
            if (that.shallowContains(this.element)) {
                return null
            } else {
                return this
            }
        }

        var result: HashTreapSet<E>? = null
        var changed = false
        this.forEachNodeElement {
            if (!that.shallowContains(it)) {
                result = result?.withElement(it) ?: HashTreapSet(it, null, left, right)
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

    override fun shallowIntersect(that: HashTreapSet<E>): HashTreapSet<E>? {
        // Fast path for the most common case
        if (this.next == null) {
            if (that.shallowContains(this.element)) {
                return this
            } else {
                return null
            }
        }

        var result: HashTreapSet<E>? = null
        var changed = false
        this.forEachNodeElement {
            if (that.shallowContains(it)) {
                result = result?.withElement(it) ?: HashTreapSet(it, null, left, right)
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

    override fun shallowRemove(element: E): HashTreapSet<E>? {
        // Fast path for the most common case
        if (this.next == null) {
            if (this.element == element) {
                return null
            } else {
                return this
            }
        }

        var result: HashTreapSet<E>? = null
        var changed = false
        this.forEachNodeElement {
            if (it != element) {
                result = result?.withElement(it) ?: HashTreapSet(it, null, left, right)
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

    override fun shallowRemoveAll(predicate: (E) -> Boolean): HashTreapSet<E>? {
        var result: HashTreapSet<E>? = null
        var removed = false
        this.forEachNodeElement {
            if (predicate(it)) {
                removed = true
            } else {
                result = result?.withElement(it) ?: HashTreapSet(it, null, left, right)
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
        self.asTreapSequence().forEach { node ->
            node.forEachNodeElement {
                yield(it)
            }
        }
    }.iterator()

    override fun singleOrNull(): E? = element.takeIf { next == null && left == null && right == null }
    override fun single(): E = element.also {
        if (next != null || left != null || right != null) { throw IllegalArgumentException("Set contains more than one element") }
    }
    override fun arbitraryOrNull(): E? = element

    override fun <R : Any> shallowMapReduce(map: (E) -> R, reduce: (R, R) -> R): R {
        var result: R? = null
        forEachNodeElement {
            val mapped = map(it)
            result = result?.let { result -> reduce(result, mapped) } ?: mapped
        }
        return result!!
    }

    override fun containsAny(predicate: (E) -> Boolean): Boolean {
        forEachNodeElement {
            if (predicate(it)) {
                return true
            }
        }
        return left?.containsAny(predicate) == true || right?.containsAny(predicate) == true
    }
}

internal interface ElementList<E> {
    val element: E
    val next: More<E>?

    class More<E>(
        override val element: E,
        override val next: More<E>?
    ) : ElementList<E>, java.io.Serializable
}
