package com.certora.common.collect

import com.certora.common.utils.*
import kotlinx.collections.immutable.PersistentMap

/**
 * A AbstractTreapMap specific to Comparable keys.  Iterates in the order defined by the objects.  We store one element
 * per Treap node, with the map key itself as the Treap key, and an additional `value` field
 */
internal sealed class SortedTreapMap<@Treapable K : Comparable<K>, V> private constructor(
    left: SortedTreapMap<K, V>?,
    right: SortedTreapMap<K, V>?
) : AbstractTreapMap<K, V, SortedTreapMap<K, V>>(left, right), TreapKey.Sorted<K> {

    override fun K.toTreapKey() = TreapKey.Sorted.FromKey(this)

    override fun new(key: K, value: V): SortedTreapMap<K, V> = Node(key, value)
    override fun clear() = emptyOf<K, V>()

    abstract override fun shallowGetValue(key: K): V

    @Suppress("UNCHECKED_CAST")
    override fun Map<out K, V>.toTreapMapOrNull() =
        this as? SortedTreapMap<K, V>
        ?: (this as? PersistentMap.Builder<K, V>)?.build() as? SortedTreapMap<K, V>

    override fun getShallowMerger(merger: (K, V?, V?) -> V?): (SortedTreapMap<K, V>?, SortedTreapMap<K, V>?) -> SortedTreapMap<K, V>? = { t1, t2 ->
        val e1 = t1.uncheckedAs<Node<K, V>?>()
        val e2 = t2.uncheckedAs<Node<K, V>?>()
        val k = e1?.key ?: e2?.key as K
        val v1 = e1?.value
        val v2 = e2?.value
        val v = merger(k, v1, v2)
        when {
            v == null -> null
            t1 != null -> if (v == v1) { t1 } else { Node<K, V>(k, v, t1.left, t1.right) }
            t2 != null -> if (v == v2) { t2 } else { Node<K, V>(k, v, t2.left, t2.right) }
            else -> throw IllegalArgumentException("shallow merge with no treaps")
        }
    }

    override fun shallowZip(that: SortedTreapMap<K, V>): Sequence<Map.Entry<K, Pair<V, V>>> {
        val thisNode = this.uncheckedAs<Node<K, V>>()
        val thatNode = that.uncheckedAs<Node<K, V>>()
        return sequenceOf(MapEntry(thisNode.key, thisNode.value to thatNode.value))
    }

    private class Node<@Treapable K : Comparable<K>, V>(
        val key: K,
        val value: V,
        left: SortedTreapMap<K, V>? = null,
        right: SortedTreapMap<K, V>? = null
    ) : SortedTreapMap<K, V>(left, right) {
        override val treap get() = this
        override val self get() = this
        override val treapKey get() = key

        override fun shallowEntrySequence(): Sequence<Map.Entry<K, V>> = sequenceOf(MapEntry(key, value))

        override fun shallowContainsKey(key: K) = true
        override val shallowSize get() = 1
        override fun shallowRemove(element: K): SortedTreapMap<K, V>? = null
        override fun shallowRemoveEntry(key: K, value: V): SortedTreapMap<K, V>? = this.takeIf { this.value != value }
        override fun shallowRemoveAll(predicate: (K) -> Boolean): SortedTreapMap<K, V>? = this.takeIf { !predicate(this.key) }
        override fun shallowGetValue(key: K): V = value
        override fun shallowEquals(that: SortedTreapMap<K, V>): Boolean = this.value == that.uncheckedAs<Node<K, V>>().value
        override fun shallowComputeHashCode(): Int = AbstractMapEntry.hashCode(key, value)

        override fun copyWith(left: SortedTreapMap<K, V>?, right: SortedTreapMap<K, V>?) = Node(key, value, left, right)

        override fun shallowAdd(that: SortedTreapMap<K, V>): SortedTreapMap<K, V> {
            return if (this.shallowGetValue(treapKey) == that.shallowGetValue(treapKey)) {
                this
            } else {
                Node(treapKey, that.shallowGetValue(treapKey), left, right)
            }
        }

        override fun shallowUpdateValues(transform: (K, V) -> V?): SortedTreapMap<K, V>? {
            val newValue = transform(key, value)
            return when {
                newValue == null -> null
                newValue === value -> this
                else -> Node(key, newValue, left, right)
            }
        }

        override fun <U> shallowUpdate(entryKey: K, toUpdate: U, merger: (V?, U?) -> V?): SortedTreapMap<K, V>? {
            val newValue = merger(value, toUpdate)
            return when {
                newValue == null -> null
                newValue === value -> this
                else -> Node(key, newValue, left, right)
            }
        }
    }

    private class Empty<@Treapable K : Comparable<K>, V> : SortedTreapMap<K, V>(null, null) {
         // `Empty<E>` is just a placeholder, and should not be used as a treap
         override val treap get() = null
         override val self get() = this

         override fun shallowEntrySequence(): Sequence<Map.Entry<K, V>> = sequenceOf()

         override val treapKey get() = throw UnsupportedOperationException()
         override fun copyWith(left: SortedTreapMap<K, V>?, right: SortedTreapMap<K, V>?) = throw UnsupportedOperationException()
         override val shallowSize get() = throw UnsupportedOperationException()
         override fun shallowEquals(that: SortedTreapMap<K, V>) = throw UnsupportedOperationException()
         override fun shallowAdd(that: SortedTreapMap<K, V>) = throw UnsupportedOperationException()
         override fun shallowRemove(element: K) = throw UnsupportedOperationException()
         override fun shallowRemoveEntry(key: K, value: V) = throw UnsupportedOperationException()
         override fun shallowRemoveAll(predicate: (K) -> Boolean) = throw UnsupportedOperationException()
         override fun shallowContainsKey(key: K) = throw UnsupportedOperationException()
         override fun shallowGetValue(key: K) = throw UnsupportedOperationException()
         override fun shallowUpdateValues(transform: (K, V) -> V?) = throw UnsupportedOperationException()
         override fun <U> shallowUpdate(entryKey: K, toUpdate: U, merger: (V?, U?) -> V?) = throw UnsupportedOperationException()
         override fun shallowComputeHashCode() = throw UnsupportedOperationException()
    }

    companion object {
        private val _empty = Empty<Nothing, Nothing>()
        fun <@Treapable K : Comparable<K>, V> emptyOf() = _empty.uncheckedAs<SortedTreapMap<K, V>>()
    }
}
