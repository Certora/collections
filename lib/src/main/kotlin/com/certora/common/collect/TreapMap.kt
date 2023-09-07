package com.certora.common.collect

import com.certora.common.utils.*
import kotlinx.collections.immutable.PersistentMap

interface TreapMap<@Treapable K, V> : PersistentMap<K, V> {
    override fun put(key: K, value: @UnsafeVariance V): TreapMap<K, V>
    override fun remove(key: K): TreapMap<K, V>
    override fun remove(key: K, value: @UnsafeVariance V): TreapMap<K, V>
    override fun putAll(m: Map<out K, @UnsafeVariance V>): TreapMap<K, V>
    override fun clear(): TreapMap<K, V>

    interface Builder<@Treapable K, V>: PersistentMap.Builder<K, V> {
        override fun build(): TreapMap<K, V>
    }

    override fun builder(): Builder<K, @UnsafeVariance V>

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

fun <@Treapable K : Comparable<K>, V> treapMapOf(): TreapMap<K, V> = SortedTreapMap.emptyOf<K, V>()
fun <@Treapable K : Comparable<K>, V> treapMapOf(vararg pairs: Pair<K, V>): TreapMap<K, V> = treapMapOf<K,V>().mutate { it += pairs }

fun <@Treapable K, V> hashTreapMapOf(): TreapMap<K, V> = HashTreapMap.emptyOf<K, V>()
fun <@Treapable K, V> hashTreapMapOf(vararg pairs: Pair<K, V>): TreapMap<K, V> = hashTreapMapOf<K,V>().mutate { it += pairs }


fun <@Treapable K : Comparable<K>, V> Map<K, V>.toTreapMap(): TreapMap<K, V> =
    this as? SortedTreapMap<K, V>
    ?: (this as? TreapMap.Builder<K, V>)?.build() as? SortedTreapMap<K, V>
    ?: treapMapOf<K, V>().putAll(this)

fun <@Treapable K, V> Map<K, V>.toHashTreapMap(): TreapMap<K, V> =
    this as? HashTreapMap<K, V>
    ?: (this as? TreapMap.Builder<K, V>)?.build() as? HashTreapMap<K, V>
    ?: hashTreapMapOf<K, V>().putAll(this)


@Suppress("UNCHECKED_CAST")
inline fun <@Treapable K, V> TreapMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): TreapMap<K, V> =
    (this as TreapMap<K, V>).builder().apply(mutator).build()

@Suppress("UNCHECKED_CAST")
operator fun <@Treapable K, V> TreapMap<out K, V>.plus(pair: Pair<K, V>): TreapMap<K, V> =
    (this as TreapMap<K, V>).put(pair.first, pair.second)

operator fun <@Treapable K, V> TreapMap<out K, V>.plus(pairs
: Iterable<Pair<K, V>>): TreapMap<K, V> = putAll(pairs)

operator fun <@Treapable K, V> TreapMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): TreapMap<K, V> = putAll(pairs)

operator fun <@Treapable K, V> TreapMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): TreapMap<K, V> = putAll(pairs)

operator fun <@Treapable K, V> TreapMap<out K, V>.plus(map: Map<out K, V>): TreapMap<K, V> = putAll(map)


@Suppress("UNCHECKED_CAST")
public fun <@Treapable K, V> TreapMap<out K, V>.putAll(map: Map<out K, V>): TreapMap<K, V> =
    (this as TreapMap<K, V>).putAll(map)

public fun <@Treapable K, V> TreapMap<out K, V>.putAll(pairs: Iterable<Pair<K, V>>): TreapMap<K, V> =
    mutate { it.putAll(pairs) }

public fun <@Treapable K, V> TreapMap<out K, V>.putAll(pairs: Array<out Pair<K, V>>): TreapMap<K, V> =
    mutate { it.putAll(pairs) }

public fun <@Treapable K, V> TreapMap<out K, V>.putAll(pairs: Sequence<Pair<K, V>>): TreapMap<K, V> =
    mutate { it.putAll(pairs) }


@Suppress("UNCHECKED_CAST")
public operator fun <@Treapable K, V> TreapMap<out K, V>.minus(key: K): TreapMap<K, V> =
    (this as TreapMap<K, V>).remove(key)

public operator fun <@Treapable K, V> TreapMap<out K, V>.minus(keys: Iterable<K>): TreapMap<K, V> =
    mutate { it.minusAssign(keys) }

public operator fun <@Treapable K, V> TreapMap<out K, V>.minus(keys: Array<out K>): TreapMap<K, V> =
    mutate { it.minusAssign(keys) }

public operator fun <@Treapable K, V> TreapMap<out K, V>.minus(keys: Sequence<K>): TreapMap<K, V> =
    mutate { it.minusAssign(keys) }

fun <@Treapable K, V : Any> TreapMap<K, V>.retainAll(predicate: (Map.Entry<K, V>) -> Boolean): TreapMap<K, V> =
    this.updateValues { k, v -> if (predicate(MapEntry(k, v))) { v } else { null } }

fun <@Treapable K, V : Any> TreapMap<K, V>.retainAllKeys(predicate: (K) -> Boolean): TreapMap<K, V> =
    this.updateValues { k, v -> if (predicate(k)) { v } else { null } }

fun <@Treapable K, V : Any> TreapMap<K, V>.retainAllValues(predicate: (V) -> Boolean): TreapMap<K, V> =
    this.updateValues { _, v -> if (predicate(v)) { v } else { null } }

fun <@Treapable K, V : Any> TreapMap<K, V>.removeAll(predicate: (Map.Entry<K, V>) -> Boolean): TreapMap<K, V> =
    this.retainAll { !predicate(it) }

fun <@Treapable K, V : Any> TreapMap<K, V>.removeAllKeys(predicate: (K) -> Boolean): TreapMap<K, V> =
    this.retainAllKeys { !predicate(it) }

fun <@Treapable K, V : Any> TreapMap<K, V>.removeAllValues(predicate: (V) -> Boolean): TreapMap<K, V> =
    this.retainAllValues { !predicate(it) }
