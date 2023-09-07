package com.certora.common.collect

import com.certora.common.utils.*
import kotlinx.collections.immutable.PersistentSet

// A "builder" for an arbitrary PersistentSet.  Assumes that all PersistentSet operations
// maintain object identity for updates which have no effect.  E.g., a.add(b) === a, iff b is a subset of a.

internal class TreapSetBuilder<E, S : TreapSet<E, S>>(
    private var set: S
) : AbstractMutableSet<E>(), PersistentSet.Builder<E>, java.io.Serializable {

    override fun hashCode() = set.hashCode()
    override fun equals(other: Any?) = set.equals(other)
    override val size get() = set.size
    override fun isEmpty() = set.isEmpty()

    override fun clear() { update(set.clear()) }

    override fun contains(element: E): Boolean = set.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = set.containsAll(elements)

    override fun add(element: E): Boolean = update(set.add(element))
    override fun addAll(elements: Collection<E>): Boolean = update(set.addAll(elements))
    override fun remove(element: E): Boolean = update(set.remove(element))
    override fun removeAll(elements: Collection<E>): Boolean = update(set.removeAll(elements))
    override fun retainAll(elements: Collection<E>): Boolean = update(set.retainAll(elements))

    override fun build() = set

    override fun iterator() = object : MutableIterator<E> {
        val setIterator = set.iterator()
        var current: E? = null
        var haveCurrent = false

        override fun hasNext() = setIterator.hasNext()

        override fun next(): E {
            val next = setIterator.next()
            current = next
            haveCurrent = true
            return next
        }

        override fun remove() {
            check(haveCurrent) { "remove() called with no current item" }
            remove(current.uncheckedAs<E>())
            haveCurrent = false
        }
    }

    private fun update(newSet: S): Boolean {
        if (newSet !== set) {
            set = newSet
            return true
        } else {
            return false
        }
    }
}
