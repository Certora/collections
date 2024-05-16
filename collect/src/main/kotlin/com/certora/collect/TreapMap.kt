package com.certora.collect

import kotlinx.collections.immutable.PersistentMap

/**
    A [PersistentMap] implemented as a [Treap](https://en.wikipedia.org/wiki/Treap) - a kind of balanced binary tree.
 */
@Treapable
public sealed interface TreapMap<K, V> : PersistentMap<K, V> {
    override fun put(key: K, value: @UnsafeVariance V): TreapMap<K, V>
    override fun remove(key: K): TreapMap<K, V>
    override fun remove(key: K, value: @UnsafeVariance V): TreapMap<K, V>
    override fun putAll(m: Map<out K, @UnsafeVariance V>): TreapMap<K, V>
    override fun clear(): TreapMap<K, V>

    /**
        A [PersistentMap.Builder] that produces a [TreapMap].
    */
    public interface Builder<K, V>: PersistentMap.Builder<K, V> {
        override fun build(): TreapMap<K, V>
    }

    @Suppress("Treapability")
    override fun builder(): Builder<K, @UnsafeVariance V> = TreapMapBuilder(this)

    public fun merge(
        m: Map<K, V>,
        merger: (K, V?, V?) -> V?
    ): TreapMap<K, V>

    public fun parallelMerge(
        m: Map<K, V>,
        parallelThresholdLog2: Int = 4,
        merger: (K, V?, V?) -> V?
    ): TreapMap<K, V>

    public fun <R : Any> updateValues(
        transform: (K, V) -> R?
    ): TreapMap<K, R>

    public fun <R : Any> parallelUpdateValues(
        parallelThresholdLog2: Int = 5,
        transform: (K, V) -> R?
    ): TreapMap<K, R>

    public fun <U> updateEntry(
        key: K,
        value: U,
        merger: (V?, U) -> V?
    ): TreapMap<K, V>

    public fun zip(
        m: Map<out K, V>
    ): Sequence<Map.Entry<K, Pair<V?, V?>>>

    public fun <R : Any> mapReduce(map: (K, V) -> R, reduce: (R, R) -> R): R?
    public fun <R : Any> parallelMapReduce(map: (K, V) -> R, reduce: (R, R) -> R, parallelThresholdLog2: Int = 5): R?
}

public fun <@Treapable K, V> treapMapOf(): TreapMap<K, V> = EmptyTreapMap<K, V>()
public fun <@Treapable K, V> treapMapOf(vararg pairs: Pair<K, V>): TreapMap<K, V> = treapMapOf<K,V>().mutate { it += pairs }

public fun <@Treapable K, V> treapMapBuilderOf(): TreapMap.Builder<K, V> = treapMapOf<K, V>().builder()
public fun <@Treapable K, V> treapMapBuilderOf(vararg pairs: Pair<K, V>): TreapMap.Builder<K, V> = treapMapOf<K, V>().builder().apply { putAll(pairs) }

public fun <@Treapable K, V> Map<out K, V>.toTreapMap(): TreapMap<K, V> = treapMapOf<K, V>() + this
public fun <@Treapable K, V> Collection<Pair<K, V>>.toTreapMap(): TreapMap<K, V> = treapMapOf(*this.toTypedArray())

public fun <@Treapable K, V> TreapMap<K, V>?.orEmpty(): TreapMap<K, V> = this ?: treapMapOf()

@Suppress("UNCHECKED_CAST")
public inline fun <K, V> TreapMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): TreapMap<K, V> =
    (this as TreapMap<K, V>).builder().apply(mutator).build()

@Suppress("UNCHECKED_CAST")
public operator fun <K, V> TreapMap<out K, V>.plus(pair: Pair<K, V>): TreapMap<K, V> =
    (this as TreapMap<K, V>).put(pair.first, pair.second)

public operator fun <K, V> TreapMap<out K, V>.plus(map: Map<out K, V>): TreapMap<K, V> = putAll(map)
public operator fun <K, V> TreapMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): TreapMap<K, V> = putAll(pairs)
public operator fun <K, V> TreapMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): TreapMap<K, V> = putAll(pairs)
public operator fun <K, V> TreapMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): TreapMap<K, V> = putAll(pairs)


@Suppress("UNCHECKED_CAST")
public fun <K, V> TreapMap<out K, V>.putAll(map: Map<out K, V>): TreapMap<K, V> =
    (this as TreapMap<K, V>).putAll(map)

public fun <K, V> TreapMap<out K, V>.putAll(pairs: Iterable<Pair<K, V>>): TreapMap<K, V> =
    mutate { it.putAll(pairs) }

public fun <K, V> TreapMap<out K, V>.putAll(pairs: Array<out Pair<K, V>>): TreapMap<K, V> =
    mutate { it.putAll(pairs) }

public fun <K, V> TreapMap<out K, V>.putAll(pairs: Sequence<Pair<K, V>>): TreapMap<K, V> =
    mutate { it.putAll(pairs) }


@Suppress("UNCHECKED_CAST")
public operator fun <K, V> TreapMap<out K, V>.minus(key: K): TreapMap<K, V> =
    (this as TreapMap<K, V>).remove(key)

public operator fun <K, V> TreapMap<out K, V>.minus(keys: Iterable<K>): TreapMap<K, V> = removeAll(keys)
public operator fun <K, V> TreapMap<out K, V>.minus(keys: Array<out K>): TreapMap<K, V> = removeAll(keys)
public operator fun <K, V> TreapMap<out K, V>.minus(keys: Sequence<K>): TreapMap<K, V> = removeAll(keys)

public fun <K, V : Any> TreapMap<K, V>.retainAll(predicate: (Map.Entry<K, V>) -> Boolean): TreapMap<K, V> =
    this.updateValues { k, v -> if (predicate(MapEntry(k, v))) { v } else { null } }

public fun <K, V : Any> TreapMap<K, V>.retainAllKeys(predicate: (K) -> Boolean): TreapMap<K, V> =
    this.updateValues { k, v -> if (predicate(k)) { v } else { null } }

public fun <K, V : Any> TreapMap<K, V>.retainAllValues(predicate: (V) -> Boolean): TreapMap<K, V> =
    this.updateValues { _, v -> if (predicate(v)) { v } else { null } }

public fun <K, V> TreapMap<out K, V>.removeAll(keys: Iterable<K>): TreapMap<K, V> =
    mutate { it.minusAssign(keys) }

public fun <K, V> TreapMap<out K, V>.removeAll(keys: Array<out K>): TreapMap<K, V> =
    mutate { it.minusAssign(keys) }

public fun <K, V> TreapMap<out K, V>.removeAll(keys: Sequence<K>): TreapMap<K, V> =
    mutate { it.minusAssign(keys) }

public fun <K, V : Any> TreapMap<K, V>.removeAll(predicate: (Map.Entry<K, V>) -> Boolean): TreapMap<K, V> =
    this.retainAll { !predicate(it) }

public fun <K, V : Any> TreapMap<K, V>.removeAllKeys(predicate: (K) -> Boolean): TreapMap<K, V> =
    this.retainAllKeys { !predicate(it) }

public fun <K, V : Any> TreapMap<K, V>.removeAllValues(predicate: (V) -> Boolean): TreapMap<K, V> =
    this.retainAllValues { !predicate(it) }

public inline fun <K, V, @Treapable R> TreapMap<out K, V>.mapKeys(transform: (Map.Entry<K, V>) -> R): TreapMap<R, V> =
    mapKeysTo(treapMapOf<R, V>().builder(), transform).build()

public inline fun <@Treapable K, V, R> TreapMap<out K, V>.mapValues(transform: (Map.Entry<K, V>) -> R): TreapMap<K, R> =
    mapValuesTo(treapMapOf<K, R>().builder(), transform).build()

/**
    Returns a key-value mapping associated with the greatest key less than or equal to the given key, or null if there
    is no such key.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.floorEntry(key: K): Map.Entry<K, V>? = when (this) {
    is EmptyTreapMap<K, V> -> null
    is SortedTreapMap<K, V> -> floorEntry(key)
    // Shouldn't happen due to static Comparable constraint on K
    is HashTreapMap<K, V> -> throw UnsupportedOperationException("floorEntry is not supported for hashed treap maps")
}

/**
    Returns the greatest key less than or equal to the given key, or null if there is no such key.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.floorKey(key: K): K? = floorEntry(key)?.key

/**
    Returns a key-value mapping associated with the least key greater than or equal to the given key, or null if there
    is no such key.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.ceilingEntry(key: K): Map.Entry<K, V>? = when (this) {
    is EmptyTreapMap<K, V> -> null
    is SortedTreapMap<K, V> -> ceilingEntry(key)
    // Shouldn't happen due to static Comparable constraint on K
    is HashTreapMap<K, V> -> throw UnsupportedOperationException("ceilingEntry is not supported for hashed treap maps")
}

/**
    Returns the least key greater than or equal to the given key, or null if there is no such key.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.ceilingKey(key: K): K? = ceilingEntry(key)?.key

/**
    Returns a key-value mapping associated with the greatest key strictly less than the given key, or null if there is
    no such key.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.lowerEntry(key: K): Map.Entry<K, V>? = when (this) {
    is EmptyTreapMap<K, V> -> null
    is SortedTreapMap<K, V> -> lowerEntry(key)
    // Shouldn't happen due to static Comparable constraint on K
    is HashTreapMap<K, V> -> throw UnsupportedOperationException("lowerEntry is not supported for hashed treap maps")
}

/**
    Returns the greatest key strictly less than the given key, or null if there is no such key.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.lowerKey(key: K): K? = lowerEntry(key)?.key


/**
    Returns a key-value mapping associated with the least key strictly greater than the given key, or null if there is no
    such key.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.higherEntry(key: K): Map.Entry<K, V>? = when (this) {
    is EmptyTreapMap<K, V> -> null
    is SortedTreapMap<K, V> -> higherEntry(key)
    // Shouldn't happen due to static Comparable constraint on K
    is HashTreapMap<K, V> -> throw UnsupportedOperationException("higherEntry is not supported for hashed treap maps")
}

/**
    Returns the least key strictly greater than the given key, or null if there is no such key.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.higherKey(key: K): K? = higherEntry(key)?.key

/**
    Returns a key-value mapping associated with the least key in this map, or null if the map is empty.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.firstEntry(): Map.Entry<K, V>? = when (this) {
    is EmptyTreapMap<K, V> -> null
    is SortedTreapMap<K, V> -> firstEntry()
    // Shouldn't happen due to static Comparable constraint on K
    is HashTreapMap<K, V> -> throw UnsupportedOperationException("firstEntry is not supported for hashed treap maps")
}

/**
    Returns the least key in this map, or null if the map is empty.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.firstKey(): K? = firstEntry()?.key


/**
    Returns a key-value mapping associated with the greatest key in this map, or null if the map is empty.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.lastEntry(): Map.Entry<K, V>? = when (this) {
    is EmptyTreapMap<K, V> -> null
    is SortedTreapMap<K, V> -> lastEntry()
    // Shouldn't happen due to static Comparable constraint on K
    is HashTreapMap<K, V> -> throw UnsupportedOperationException("lastEntry is not supported for hashed treap maps")
}

/**
    Returns the greatest key in this map, or null if the map is empty.
 */
public fun <@Treapable K : Comparable<K>, V> TreapMap<K, V>.lastKey(): K? = lastEntry()?.key
