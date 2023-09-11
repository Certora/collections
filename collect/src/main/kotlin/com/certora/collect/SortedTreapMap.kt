package com.certora.collect

import kotlinx.collections.immutable.PersistentMap

/**
    A TreapMap specific to Comparable keys.  Iterates in the order defined by the objects.  We store one element per
    Treap node, with the map key itself as the Treap key, and an additional `value` field
 */
internal class SortedTreapMap<@Treapable K : Comparable<K>, V>(
    val key: K,
    val value: V,
    left: SortedTreapMap<K, V>? = null,
    right: SortedTreapMap<K, V>? = null
) : AbstractTreapMap<K, V, SortedTreapMap<K, V>>(left, right), TreapKey.Sorted<K> {

    override fun K.toTreapKey() = TreapKey.Sorted.FromKey(this)

    override fun new(key: K, value: V): SortedTreapMap<K, V> = SortedTreapMap(key, value)

    @Suppress("UNCHECKED_CAST")
    override fun Map<out K, V>.toTreapMapOrNull() =
        this as? SortedTreapMap<K, V>
        ?: (this as? PersistentMap.Builder<K, V>)?.build() as? SortedTreapMap<K, V>

    override fun getShallowMerger(merger: (K, V?, V?) -> V?): (SortedTreapMap<K, V>?, SortedTreapMap<K, V>?) -> SortedTreapMap<K, V>? = { t1, t2 ->
        val k = t1?.key ?: t2?.key as K
        val v1 = t1?.value
        val v2 = t2?.value
        val v = merger(k, v1, v2)
        when {
            v == null -> null
            t1 != null -> if (v == v1) { t1 } else { SortedTreapMap<K, V>(k, v, t1.left, t1.right) }
            t2 != null -> if (v == v2) { t2 } else { SortedTreapMap<K, V>(k, v, t2.left, t2.right) }
            else -> throw IllegalArgumentException("shallow merge with no treaps")
        }
    }

    override fun shallowZip(that: SortedTreapMap<K, V>): Sequence<Map.Entry<K, Pair<V, V>>> =
        sequenceOf(MapEntry(this.key, this.value to that.value))

    override val self get() = this
    override val treapKey get() = key

    override fun shallowEntrySequence(): Sequence<Map.Entry<K, V>> = sequenceOf(MapEntry(key, value))

    override fun shallowContainsKey(key: K) = true
    override val shallowSize get() = 1
    override fun shallowRemove(element: K): SortedTreapMap<K, V>? = null
    override fun shallowRemoveEntry(key: K, value: V): SortedTreapMap<K, V>? = this.takeIf { this.value != value }
    override fun shallowGetValue(key: K): V = value
    override fun shallowEquals(that: SortedTreapMap<K, V>): Boolean = this.value == that.value
    override fun shallowComputeHashCode(): Int = AbstractMapEntry.hashCode(key, value)

    override fun copyWith(left: SortedTreapMap<K, V>?, right: SortedTreapMap<K, V>?) = SortedTreapMap(key, value, left, right)

    override fun shallowAdd(that: SortedTreapMap<K, V>): SortedTreapMap<K, V> {
        return if (this.shallowGetValue(treapKey) == that.shallowGetValue(treapKey)) {
            this
        } else {
            SortedTreapMap(treapKey, that.shallowGetValue(treapKey), left, right)
        }
    }

    override fun shallowUpdateValues(transform: (K, V) -> V?): SortedTreapMap<K, V>? {
        val newValue = transform(key, value)
        return when {
            newValue == null -> null
            newValue === value -> this
            else -> SortedTreapMap(key, newValue, left, right)
        }
    }

    override fun <U> shallowUpdate(entryKey: K, toUpdate: U, merger: (V?, U?) -> V?): SortedTreapMap<K, V>? {
        val newValue = merger(value, toUpdate)
        return when {
            newValue == null -> null
            newValue === value -> this
            else -> SortedTreapMap(key, newValue, left, right)
        }
    }
}
