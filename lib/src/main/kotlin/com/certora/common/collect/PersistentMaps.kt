package com.certora.common.collect

import com.certora.common.collect.impl.*
import com.certora.common.utils.internal.*
import kotlinx.collections.immutable.PersistentMap

fun <@WithStableHashCodeIfSerialized K : Comparable<K>, V> persistentSortedMapOf(): PersistentMap<K, V> = SortedTreapMap.emptyOf<K, V>()
fun <@WithStableHashCodeIfSerialized K, V> persistentHashMapOf(): PersistentMap<K, V> = HashTreapMap.emptyOf<K, V>()

fun <@WithStableHashCodeIfSerialized K, V : Any> Map<out K, V>.merge(m: Map<out K, V>, merger: (K, V?, V?) -> V?): PersistentMap<K, V> =
    this.toTreapMap().merge(m, merger)

fun <@WithStableHashCodeIfSerialized K, V : Any> PersistentMap<out K, V>.parallelMerge(m: PersistentMap<out K, V>, parallelThresholdLog2: Int = 4, merger: (K, V?, V?) -> V?): PersistentMap<K, V> =
    this.toTreapMap().parallelMerge(m, parallelThresholdLog2, merger)

fun <@WithStableHashCodeIfSerialized K, V : Any> Map<out K, V>.updateValues(transform: (K, V) -> V?): PersistentMap<K, V> =
    this.toTreapMap().updateValues(transform)

fun <@WithStableHashCodeIfSerialized K, V : Any> Map<out K, V>.parallelUpdateValues(parallelThresholdLog2: Int = 5, transform: (K, V) -> V?): PersistentMap<K, V> =
    this.toTreapMap().parallelUpdateValues(parallelThresholdLog2, transform)

fun <@WithStableHashCodeIfSerialized K, V : Any, U : Any> Map<out K, V>.updateEntry(key: K, item: U?, merger: (V?, U?) -> V?) : PersistentMap<K, V> =
    this.toTreapMap().updateEntry(key, item, merger)

infix fun <@WithStableHashCodeIfSerialized K, V> Map<out K, V>.zip(m: Map<out K, V>): Sequence<Map.Entry<K, Pair<V?, V?>>> =
    this.toTreapMap().zip(m)

fun <@WithStableHashCodeIfSerialized K, V : Any> PersistentMap<K, V>.retainAll(predicate: (Map.Entry<K, V>) -> Boolean): PersistentMap<K, V> =
    this.updateValues { k, v -> if (predicate(MapEntry(k, v))) { v } else { null } }

fun <@WithStableHashCodeIfSerialized K, V : Any> PersistentMap<K, V>.retainAllKeys(predicate: (K) -> Boolean): PersistentMap<K, V> =
    this.updateValues { k, v -> if (predicate(k)) { v } else { null } }

fun <@WithStableHashCodeIfSerialized K, V : Any> PersistentMap<K, V>.retainAllValues(predicate: (V) -> Boolean): PersistentMap<K, V> =
    this.updateValues { _, v -> if (predicate(v)) { v } else { null } }

fun <@WithStableHashCodeIfSerialized K, V : Any> PersistentMap<K, V>.removeAll(predicate: (Map.Entry<K, V>) -> Boolean): PersistentMap<K, V> =
    this.retainAll { !predicate(it) }

fun <@WithStableHashCodeIfSerialized K, V : Any> PersistentMap<K, V>.removeAllKeys(predicate: (K) -> Boolean): PersistentMap<K, V> =
    this.retainAllKeys { !predicate(it) }

fun <@WithStableHashCodeIfSerialized K, V : Any> PersistentMap<K, V>.removeAllValues(predicate: (V) -> Boolean): PersistentMap<K, V> =
    this.retainAllValues { !predicate(it) }

@Suppress("UNCHECKED_CAST")
private fun <@WithStableHashCodeIfSerialized K, V> Map<out K, V>.toTreapMap(): TreapMap<K, V> =
    this as? TreapMap<K, V> ?:
    (this as? PersistentMap.Builder<K, V>)?.build() as? TreapMap<K, V> ?:
    HashTreapMap.emptyOf<K, V>().putAll(this)
