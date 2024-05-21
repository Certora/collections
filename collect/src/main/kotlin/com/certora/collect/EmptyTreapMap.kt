package com.certora.collect

import kotlinx.collections.immutable.*

internal class EmptyTreapMap<@Treapable K, V> private constructor() : TreapMap<K, V>, java.io.Serializable {
    override val size get() = 0
    override fun isEmpty() = true

    override fun hashCode() = 0
    override fun equals(other: Any?) = (other is Map<*, *>) && (other.isEmpty())
    override fun toString() = "{}"

    override fun clear(): TreapMap<K, V> = this

    override fun containsKey(key: K): Boolean = false
    override fun containsValue(value: V): Boolean = false
    override fun get(key: K): V? = null

    override fun remove(key: K): TreapMap<K, V> = this
    override fun remove(key: K, value: V): TreapMap<K, V> = this

    override fun <R : Any> updateValues(
        transform: (K, V) -> R?
    ): TreapMap<K, R> = treapMapOf()

    override fun <R : Any> parallelUpdateValues(
        parallelThresholdLog2: Int,
        transform: (K, V) -> R?
    ): TreapMap<K, R> = treapMapOf()

    override fun <R : Any> mapReduce(map: (K, V) -> R, reduce: (R, R) -> R): R? = null
    override fun <R : Any> parallelMapReduce(map: (K, V) -> R, reduce: (R, R) -> R, parallelThresholdLog2: Int): R? = null

    override fun <U> updateEntry(key: K, value: U, merger: (V?, U) -> V?): TreapMap<K, V> =
        when (val v = merger(null, value)) {
            null -> this
            else -> put(key, v)
        }

    override fun merge(m: Map<K, V>, merger: (K, V?, V?) -> V?): TreapMap<K, V> {
        var map: TreapMap<K, V> = this
        for ((key, value) in m) {
            val v = merger(key, null, value)
            if (v != null) {
                map = map.put(key, v)
            }
        }
        return map
    }


    override fun parallelMerge(m: Map<K, V>, parallelThresholdLog2: Int, merger: (K, V?, V?) -> V?): TreapMap<K, V> =
        merge(m, merger)

    override fun zip(m: Map<out K, V>): Sequence<Map.Entry<K, Pair<V?, V?>>> =
        m.asSequence().map { MapEntry(it.key, null to it.value) }

    override val entries: ImmutableSet<Map.Entry<K, V>> get() = persistentSetOf<Map.Entry<K, V>>()
    override val keys: ImmutableSet<K> get() = persistentSetOf<K>()
    override val values: ImmutableCollection<V> get() = persistentSetOf<V>()

    @Suppress("Treapability", "UNCHECKED_CAST")
    override fun put(key: K, value: V): TreapMap<K, V> = when (key) {
        is PrefersHashTreap -> HashTreapMap(key, value)
        is Comparable<*> ->
            SortedTreapMap<Comparable<Comparable<*>>, V>(key as Comparable<Comparable<*>>, value) as TreapMap<K, V>
        else -> HashTreapMap(key, value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun putAll(m: Map<out K, V>): TreapMap<K, V> = when {
        m.isEmpty() -> this
        m is TreapMap<*, *> -> m as TreapMap<K, V>
        m is PersistentMap.Builder<*, *> -> m.build() as TreapMap<K, V>
        else -> m.entries.fold(this as TreapMap<K, V>) { map, (key, value) -> map.put(key, value) }
    }

    companion object {
        private val instance = EmptyTreapMap<Nothing, Nothing>()
        @Suppress("UNCHECKED_CAST")
        operator fun <@Treapable K, V> invoke(): EmptyTreapMap<K, V> = instance as EmptyTreapMap<K, V>
    }
}
