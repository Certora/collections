package com.certora.common.collect

import com.certora.common.utils.*
import kotlinx.collections.immutable.PersistentMap

/**
 * A TreapMap specific to Comparable keys.  Iterates in the order defined by the objects.  We store one element
 * per Treap node, with the map key itself as the Treap key, and an additional `value` field
 */
internal sealed class SortedTreapMap<@WithStableHashCodeIfSerialized K : Comparable<K>, V> private constructor(
    left: Treap?,
    right: Treap?
) : TreapMap<K, V>(left, right), TreapKey.Sorted<K> {

    override fun K.toTreapKey() = TreapKey.Sorted.FromKey(this)

    override fun new(key: K, value: V): TreapMap<K, V> = Node(key, value)
    override fun clear() = emptyOf<K, V>()

    @Suppress("UNCHECKED_CAST")
    override fun Map<out K, V>.toTreapMapOrNull() =
        this as? SortedTreapMap<K, V>
        ?: (this as? PersistentMap.Builder<K, V>)?.build() as? SortedTreapMap<K, V>

    override fun getShallowMerger(merger: (K, V?, V?) -> V?): (Treap?, Treap?) -> Treap? = { t1, t2 ->
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

    override fun shallowZip(that: TreapMap<K, V>): Sequence<Map.Entry<K, Pair<V, V>>> {
        val thisNode = this.uncheckedAs<Node<K, V>>()
        val thatNode = that.uncheckedAs<Node<K, V>>()
        return sequenceOf(MapEntry(thisNode.key, thisNode.value to thatNode.value))
    }

    private class Node<@WithStableHashCodeIfSerialized K : Comparable<K>, V>(
        val key: K,
        val value: V,
        left: Treap? = null,
        right: Treap? = null
    ) : SortedTreapMap<K, V>(left, right) {
        override val treap get() = this
        override val treapKey get() = key

        override fun shallowEntrySequence(): Sequence<Map.Entry<K, V>> = sequenceOf(MapEntry(key, value))

        override fun shallowContainsKey(key: K) = true
        override val shallowSize get() = 1
        override fun shallowRemove(element: Any?): Treap? = null
        override fun shallowRemoveEntry(key: Any?, value: Any?): Treap? = this.takeIf { this.value != value }
        override fun shallowRemoveAll(predicate: (Any?) -> Boolean): Treap? = this.takeIf { !predicate(this.value) }
        override fun shallowGetValue(key: K): V? = value
        override fun shallowEquals(that: Treap): Boolean = this.value == that.uncheckedAs<Node<K, V>>().value
        override fun shallowComputeHashCode(): Int = AbstractMapEntry.hashCode(key, value)

        override fun copyWith(left: Treap?, right: Treap?) = Node(key, value, left, right)

        override fun shallowAdd(that: Treap): Treap {
            val thatMap = that.uncheckedAs<SortedTreapMap<K, V>>()
            return if (this.shallowGetValue(treapKey) == thatMap.shallowGetValue(treapKey)) {
                this
            } else {
                Node(treapKey, thatMap.shallowGetValue(treapKey), left, right)
            }
        }

        override fun shallowUpdateValues(transform: (K, V) -> V?): Treap? {
            val newValue = transform(key, value)
            return when {
                newValue == null -> null
                newValue === value -> this
                else -> Node(key, newValue, left, right)
            }
        }

        override fun shallowUpdate(entryKey: Any?, toUpdate: Any?, merger: (Any?, Any?) -> Any?): Treap? {
            val newValue = merger(value, toUpdate)
            return when {
                newValue == null -> null
                newValue === value -> this
                else -> Node(key, newValue, left, right)
            }
        }
    }

    private class Empty<@WithStableHashCodeIfSerialized K : Comparable<K>, V> : SortedTreapMap<K, V>(null, null) {
         // `Empty<E>` is just a placeholder, and should not be used as a treap
         override val treap: Treap? get() = null

         override fun shallowEntrySequence(): Sequence<Map.Entry<K, V>> = sequenceOf()

         override val treapKey: K get() = throw UnsupportedOperationException()
         override fun copyWith(left: Treap?, right: Treap?): Treap = throw UnsupportedOperationException()
         override val shallowSize: Int get() = throw UnsupportedOperationException()
         override fun shallowEquals(that: Treap): Boolean = throw UnsupportedOperationException()
         override fun shallowAdd(that: Treap): Treap = throw UnsupportedOperationException()
         override fun shallowDifference(that: Treap): Treap = throw UnsupportedOperationException()
         override fun shallowRemove(element: Any?): Treap = throw UnsupportedOperationException()
         override fun shallowRemoveEntry(key: Any?, value: Any?): Treap = throw UnsupportedOperationException()
         override fun shallowRemoveAll(predicate: (Any?) -> Boolean): Treap? = throw UnsupportedOperationException()
         override fun shallowContainsKey(key: K): Boolean = throw UnsupportedOperationException()
         override fun shallowGetValue(key: K): V = throw UnsupportedOperationException()
         override fun shallowUpdateValues(transform: (K, V) -> V?): Treap? = throw UnsupportedOperationException()
         override fun shallowUpdate(entryKey: Any?, toUpdate: Any?, merger: (Any?, Any?) -> Any?): Treap = throw UnsupportedOperationException()
         override fun shallowComputeHashCode(): Int = throw UnsupportedOperationException()
    }

    companion object {
        private val _empty = Empty<Nothing, Nothing>()
        fun <@WithStableHashCodeIfSerialized K : Comparable<K>, V> emptyOf() = _empty.uncheckedAs<TreapMap<K, V>>()
    }
}
