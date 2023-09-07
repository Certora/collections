package com.certora.common.collect

import kotlinx.collections.immutable.PersistentMap

fun <@WithStableHashCodeIfSerialized K : Comparable<K>, V> treapMapOf(): TreapMap<K, V> = 
    SortedTreapMap.emptyOf<K, V>()

fun <@WithStableHashCodeIfSerialized K, V> hashTreapMapOf(): TreapMap<K, V> = 
    HashTreapMap.emptyOf<K, V>()


interface TreapMap<@WithStableHashCodeIfSerialized K, V> : PersistentMap<K, V> {
    interface Builder<@WithStableHashCodeIfSerialized K, V> : PersistentMap.Builder<K, V> {
        override fun build(): TreapMap<K, V>
    }

    override fun builder(): Builder<K, V>

    override fun get(key: K): V?
    override fun put(key: K, value: @UnsafeVariance V): TreapMap<K, V>
    override fun putAll(m: Map<out K, @UnsafeVariance V>): TreapMap<K, V>
    override fun remove(key: K): TreapMap<K, V>
    override fun remove(key: K, value: @UnsafeVariance V): TreapMap<K, V>
    override fun clear(): TreapMap<K, V>

    fun merge(
        m: Map<out K, V>, 
        merger: (K, V?, V?) -> V?
    ): TreapMap<K, V>
    
    fun parallelMerge(
        m: Map<out K, V>, 
        parallelThresholdLog2: Int = 4, 
        merger: (K, V?, V?) -> V?
    ): TreapMap<K, V>

    fun updateValues(
        transform: (K, V) -> V?
    ): TreapMap<K, V>

    fun parallelUpdateValues(
        parallelThresholdLog2: Int = 5, 
        transform: (K, V) -> V?
    ): TreapMap<K, V> 

    fun <U> updateEntry(
        key: K, 
        value: U?, 
        merger: (V?, U?) -> V?
    ) : TreapMap<K, V>

    fun zip(
        m: Map<out K, V>
    ): Sequence<Map.Entry<K, Pair<V?, V?>>>
}

fun <@WithStableHashCodeIfSerialized K, V : Any> TreapMap<K, V>.retainAll(predicate: (Map.Entry<K, V>) -> Boolean): TreapMap<K, V> =
    this.updateValues { k, v -> if (predicate(MapEntry(k, v))) { v } else { null } }

fun <@WithStableHashCodeIfSerialized K, V : Any> TreapMap<K, V>.retainAllKeys(predicate: (K) -> Boolean): TreapMap<K, V> =
    this.updateValues { k, v -> if (predicate(k)) { v } else { null } }

fun <@WithStableHashCodeIfSerialized K, V : Any> TreapMap<K, V>.retainAllValues(predicate: (V) -> Boolean): TreapMap<K, V> =
    this.updateValues { _, v -> if (predicate(v)) { v } else { null } }

fun <@WithStableHashCodeIfSerialized K, V : Any> TreapMap<K, V>.removeAll(predicate: (Map.Entry<K, V>) -> Boolean): TreapMap<K, V> =
    this.retainAll { !predicate(it) }

fun <@WithStableHashCodeIfSerialized K, V : Any> TreapMap<K, V>.removeAllKeys(predicate: (K) -> Boolean): TreapMap<K, V> =
    this.retainAllKeys { !predicate(it) }

fun <@WithStableHashCodeIfSerialized K, V : Any> TreapMap<K, V>.removeAllValues(predicate: (V) -> Boolean): TreapMap<K, V> =
    this.retainAllValues { !predicate(it) }
