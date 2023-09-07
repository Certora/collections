package com.certora.common.collect

import com.certora.common.utils.*
import kotlinx.collections.immutable.PersistentMap

/**
 * A AbstractTreapMap for map keys that do not have a total ordering defined by implementing Comparable.  For those,
 * we use the map keys' hash codes as Treap keys, and deal with collisions by chaining multiple map entries from a
 * single Treap node.
 *
 * The HashTreapMap instance itself stores the first entry, and additional entries are chained via MoreKeyValuePairs.
 * This is just a simple linked list, so operations on it are either O(N) or O(N^2), but collisions are assumed to be
 * rare enough that these lists will be very small - usually just one element.
 */
internal sealed class HashTreapMap<@Treapable K, V> private constructor(
    left: HashTreapMap<K, V>?,
    right: HashTreapMap<K, V>?
) : AbstractTreapMap<K, V, HashTreapMap<K, V>>(left, right), TreapKey.Hashed<K> {

    override fun K.toTreapKey() = TreapKey.Hashed.FromKey(this)
    override fun new(key: K, value: V): HashTreapMap<K, V> = Node(key, value)
    override fun clear() = emptyOf<K, V>()

    @Suppress("UNCHECKED_CAST")
    override fun Map<out K, V>.toTreapMapOrNull() =
        this as? HashTreapMap<K, V>
        ?: (this as? PersistentMap.Builder<K, V>)?.build() as? HashTreapMap<K, V>

    override fun getShallowMerger(merger: (K, V?, V?) -> V?): (HashTreapMap<K, V>?, HashTreapMap<K, V>?) -> HashTreapMap<K, V>? = { t1, t2 ->
        val e1 = t1 as Node<K, V>?
        val e2 = t2 as Node<K, V>?
        var newPairs: MoreKeyValuePairs<K, V>? = null
        e1?.forEachPair { (k, v1) ->
            val v2 = e2?.shallowGetValue(k)
            val v = merger(k, v1, v2)
            if (v != null) {
                newPairs = MoreKeyValuePairs(k, v, newPairs)
            }
        }
        e2?.forEachPair { (k, v2) ->
            val v1 = e1?.shallowGetValue(k)
            if (v1 == null) {
                val v = merger(k, v1, v2)
                if (v != null) {
                    newPairs = MoreKeyValuePairs(k, v, newPairs)
                }
            }
        }
        val firstPair = newPairs
        when {
            firstPair == null -> null
            t1 != null -> {
                val newNode = Node(firstPair.key, firstPair.value, firstPair.next, t1.left, t1.right)
                if (newNode.shallowEquals(t1)) { t1 } else { newNode }
            }
            t2 != null -> {
                val newNode = Node(firstPair.key, firstPair.value, firstPair.next, t2.left, t2.right)
                if (newNode.shallowEquals(t2)) { t2 } else { newNode }
            }
            else -> throw IllegalArgumentException("shallow merge with no treaps")
        }
    }

    protected interface KeyValuePairList<K, V> {
        abstract val key: K
        abstract val value: V
        abstract val next: MoreKeyValuePairs<K, V>?
        operator fun component1() = key
        operator fun component2() = value
    }

    protected class MoreKeyValuePairs<K, V>(
        override val key: K,
        override val value: V,
        override val next: MoreKeyValuePairs<K, V>?
    ) : KeyValuePairList<K, V>, java.io.Serializable

    protected inline fun KeyValuePairList<K, V>?.forEachPair(action: (KeyValuePairList<K, V>) -> Unit) {
        var current = this
        while (current != null) {
            action(current)
            current = current.next
        }
    }

    protected fun KeyValuePairList<K, V>?.shallowContainsPair(key: K, value: V): Boolean {
        forEachPair {
            if (it.key == key && it.value == value) {
                return true
            }
        }
        return false
    }

    protected fun KeyValuePairList<K, V>?.shallowContainsKey(key: K) : Boolean {
        forEachPair {
            if (it.key == key) {
                return true
            }
        }
        return false
    }

    override fun shallowZip(that: HashTreapMap<K, V>): Sequence<Map.Entry<K, Pair<V?, V?>>> = sequence {
        val thisNode = this@HashTreapMap as Node<K, V>
        val thatNode = that as Node<K, V>

        thisNode.forEachPair {
            yield(MapEntry(it.key, it.value to thatNode.shallowGetValue(it.key)))
        }
        thatNode.forEachPair {
            if (!thisNode.containsKey(it.key)) {
                yield(MapEntry(it.key, null to it.value))
            }
        }
    }

    private class Node<@Treapable K, V>(
        override val key: K,
        override val value: V,
        override val next: MoreKeyValuePairs<K, V>? = null,
        left: HashTreapMap<K, V>? = null,
        right: HashTreapMap<K, V>? = null
    ) : HashTreapMap<K, V>(left, right), KeyValuePairList<K, V> {
        override val treap get() = this
        override val self get() = this
        override val treapKey get() = key

        override fun copyWith(left: HashTreapMap<K, V>?, right: HashTreapMap<K, V>?): HashTreapMap<K, V> = Node(key, value, next, left, right)

        override fun shallowEntrySequence(): Sequence<Map.Entry<K, V>> = sequence {
            forEachPair { (k, v) ->
                yield (MapEntry(k, v))
            }
        }

        override fun shallowContainsKey(key: K): Boolean = (this as KeyValuePairList<K, V>).shallowContainsKey(key)

        override fun shallowGetValue(key: K): V? {
            forEachPair {
                if (it.key == key) {
                    return it.value
                }
            }
            return null
        }

        override fun shallowAdd(that: HashTreapMap<K, V>): HashTreapMap<K, V> {
            val thatNode = that as Node<K, V>
            check(thatNode.next == null) { "Add with mulple map entries?" }
            return when {
                !shallowContainsKey(thatNode.key) -> {
                    Node(this.key, this.value, MoreKeyValuePairs(thatNode.key, thatNode.value, this.next), this.left, this.right)
                }
                this.shallowGetValue(thatNode.key) == thatNode.value -> {
                    this
                }
                else -> {
                    var newPairs: MoreKeyValuePairs<K, V>? = null
                    this.forEachPair {
                        if (it.key == thatNode.key) {
                            newPairs = MoreKeyValuePairs(it.key, thatNode.value, newPairs)
                        } else {
                            newPairs = MoreKeyValuePairs(it.key, it.value, newPairs)
                        }
                    }
                    val firstPair = newPairs!!
                    Node(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
                }
            }
        }

        override fun shallowRemoveEntry(key: K, value: V): HashTreapMap<K, V>? {
            return when {
                !this.shallowContainsPair(key, value) -> this
                else -> {
                    var newPairs: MoreKeyValuePairs<K, V>? = null
                    this.forEachPair {
                        if (it.key != key || it.value != value) {
                            newPairs = MoreKeyValuePairs(it.key, it.value, newPairs)
                        }
                    }
                    val firstPair = newPairs
                    if (firstPair == null) {
                        null
                    } else {
                        Node(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
                    }
                }
            }
        }

        override fun shallowRemoveAll(predicate: (K) -> Boolean): HashTreapMap<K, V>? {
            var newPairs: MoreKeyValuePairs<K, V>? = null
            var removed = false
            this.forEachPair {
                if (predicate(it.key)) {
                    removed = true
                } else {
                    newPairs = MoreKeyValuePairs(it.key, it.value, newPairs)
                }
            }
            val firstPair = newPairs
            return when {
                !removed -> this
                firstPair == null -> null
                else -> Node(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
            }
        }

        override fun <U> shallowUpdate(entryKey: K, toUpdate: U, merger: (V?, U?) -> V?): HashTreapMap<K, V>? {
            return when (this.key) {
                entryKey -> {
                    val newValue = merger(this.value, toUpdate)
                    if(newValue == null) {
                        if(this.next == null) {
                            return null
                        } else {
                            Node(this.next.key, this.next.value, this.next.next, this.left, this.right)
                        }
                    } else if(newValue == value) {
                        this
                    } else {
                        Node(this.key, newValue, this.next, this.left, this.right)
                    }
                }
                else -> {
                    // look for entryKey in the buckets off of this one
                    var newPairs: MoreKeyValuePairs<K, V>? = null
                    var found = false
                    var it = this.next
                    while(it != null) {
                        if(it.key == entryKey) {
                            val upd = merger(it.value, toUpdate)
                            found = true
                            if(upd != null && upd == it.value) {
                                return this
                            } else if(upd != null) {
                                newPairs = MoreKeyValuePairs(it.key, upd, newPairs)
                            }
                        } else {
                            newPairs = MoreKeyValuePairs(it.key, it.value, newPairs)
                        }
                        it = it.next
                    }
                    if(!found) {
                        val deNovoMerge = merger(null, toUpdate) ?: return this
                        newPairs = MoreKeyValuePairs(entryKey, deNovoMerge, this.next)
                    }
                    Node(this.key, this.value, newPairs, this.left, this.right)
                }
            }
        }

        override fun shallowRemove(element: K): HashTreapMap<K, V>? {
            if (!this.shallowContainsKey(element)) {
                return this
            } else {
                var newPairs: MoreKeyValuePairs<K, V>? = null
                this.forEachPair {
                    if (it.key != element) {
                        newPairs = MoreKeyValuePairs(it.key, it.value, newPairs)
                    }
                }
                val firstPair = newPairs
                return if (firstPair == null) {
                    null
                } else {
                    Node(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
                }
            }
        }

        override val shallowSize: Int get() {
            var count = 0
            forEachPair {
                ++count
            }
            return count
        }

        override fun shallowEquals(that: HashTreapMap<K, V>): Boolean {
            val thatNode = that as Node<K, V>
            forEachPair {
                if (!thatNode.shallowContainsPair(it.key, it.value)) {
                    return false
                }
            }
            return this.shallowSize == that.shallowSize
        }

        override fun shallowUpdateValues(transform: (K, V) -> V?): HashTreapMap<K, V>? {
            return when {
                next == null -> {
                    val newValue = transform(key, value)
                    when {
                        newValue == null -> null
                        newValue === value -> this
                        else -> Node(key, newValue, null, left, right)
                    }
                }
                else -> {
                    var newPairs: MoreKeyValuePairs<K, V>? = null
                    this.forEachPair { (k, v) ->
                        val newValue = transform(k, v)
                        if (newValue != null) {
                            newPairs = MoreKeyValuePairs(k, newValue, newPairs)
                        }
                    }
                    val firstPair = newPairs
                    when {
                        firstPair == null -> null
                        else -> {
                            val newEntry = Node(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
                            if (newEntry.shallowEquals(this)) { this } else { newEntry }
                        }
                    }
                }
            }
        }

        override fun shallowComputeHashCode(): Int {
            var h = 0
            forEachPair { (k, v) -> h += AbstractMapEntry.hashCode(k, v) }
            return h
        }
    }

    private class Empty<@Treapable K, V> : HashTreapMap<K, V>(null, null) {
        // `Empty<E>` is just a placeholder, and should not be used as a treap
        override val treap get() = null
        override val self get() = this

        override fun shallowEntrySequence() = sequenceOf<Map.Entry<K, V>>()

        override val treapKey get() = throw UnsupportedOperationException()
        override fun copyWith(left: HashTreapMap<K, V>?, right: HashTreapMap<K, V>?) = throw UnsupportedOperationException()
        override val shallowSize get() = throw UnsupportedOperationException()
        override fun shallowEquals(that: HashTreapMap<K, V>) = throw UnsupportedOperationException()
        override fun shallowAdd(that: HashTreapMap<K, V>) = throw UnsupportedOperationException()
        override fun shallowRemove(element: K) = throw UnsupportedOperationException()
        override fun shallowRemoveEntry(key: K, value: V) = throw UnsupportedOperationException()
        override fun shallowRemoveAll(predicate: (K) -> Boolean) = throw UnsupportedOperationException()
        override fun <U> shallowUpdate(entryKey: K, toUpdate: U, merger: (V?, U?) -> V?) = throw UnsupportedOperationException()
        override fun shallowContainsKey(key: K) = throw UnsupportedOperationException()
        override fun shallowGetValue(key: K) = throw UnsupportedOperationException()
        override fun shallowUpdateValues(transform: (K, V) -> V?) = throw UnsupportedOperationException()
        override fun shallowComputeHashCode() = throw UnsupportedOperationException()
    }

    companion object {
        private val _empty = Empty<Nothing, Nothing>()
        @Suppress("UNCHECKED_CAST")
        fun <@Treapable K, V> emptyOf() = _empty as HashTreapMap<K, V>
    }
}
