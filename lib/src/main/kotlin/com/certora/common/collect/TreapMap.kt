package com.certora.common.collect

import kotlinx.collections.immutable.PersistentMap

interface TreapMap<@WithStableHashCodeIfSerialized K, V> : PersistentMap<K, V> {
    override fun put(key: K, value: @UnsafeVariance V): TreapMap<K, V>
    override fun remove(key: K): TreapMap<K, V>
    override fun remove(key: K, value: @UnsafeVariance V): TreapMap<K, V>
    override fun putAll(m: Map<out K, @UnsafeVariance V>): TreapMap<K, V>
    override fun clear(): TreapMap<K, V>

    interface Builder<@WithStableHashCodeIfSerialized K, V>: PersistentMap.Builder<K, V> {
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

fun <@WithStableHashCodeIfSerialized K : Comparable<K>, V> treapMapOf(): TreapMap<K, V> = 
    SortedTreapMap.emptyOf<K, V>()

fun <@WithStableHashCodeIfSerialized K, V> hashTreapMapOf(): TreapMap<K, V> = 
    HashTreapMap.emptyOf<K, V>()


/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Entries of the map are iterated in the order they were specified.
 */
fun <K, V> persistentMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = PersistentOrderedMap.emptyOf<K,V>().mutate { it += pairs }

/**
 * Returns an empty persistent map.
 */
fun <K, V> persistentMapOf(): PersistentMap<K, V> = PersistentOrderedMap.emptyOf()


/**
 * Returns a new persistent map with the specified contents, given as a list of pairs
 * where the first component is the key and the second is the value.
 *
 * If multiple pairs have the same key, the resulting map will contain the value from the last of those pairs.
 *
 * Order of the entries in the returned map is unspecified.
 */
fun <K, V> persistentHashMapOf(vararg pairs: Pair<K, V>): PersistentMap<K, V> = PersistentHashMap.emptyOf<K,V>().mutate { it += pairs }

/**
 * Returns an empty persistent map.
 */
fun <K, V> persistentHashMapOf(): PersistentMap<K, V> = PersistentHashMap.emptyOf()


/**
 * Returns a persistent map containing all entries from this map.
 *
 * If the receiver is already a persistent map, returns it as is.
 * If the receiver is a persistent map builder, calls `build` on it and returns the result.
 *
 * Entries of the returned map are iterated in the same order as in this map.
 */
fun <K, V> Map<K, V>.toPersistentMap(): PersistentMap<K, V>
    = this as? PersistentOrderedMap<K, V>
        ?: (this as? PersistentOrderedMapBuilder<K, V>)?.build()
        ?: PersistentOrderedMap.emptyOf<K, V>().putAll(this)

/**
 * Returns an immutable map containing all entries from this map.
 *
 * If the receiver is already a persistent hash map, returns it as is.
 * If the receiver is a persistent hash map builder, calls `build` on it and returns the result.
 *
 * Order of the entries in the returned map is unspecified.
 */
fun <K, V> Map<K, V>.toPersistentHashMap(): PersistentMap<K, V>
        = this as? PersistentHashMap
        ?: (this as? PersistentHashMapBuilder<K, V>)?.build()
        ?: PersistentHashMap.emptyOf<K, V>().putAll(this)

inline fun <K, V> TreapMap<out K, V>.mutate(mutator: (MutableMap<K, V>) -> Unit): TreapMap<K, V> =
    (this as PersistentMap<K, V>).builder().apply(mutator).build()

/**
 * Returns the result of adding an entry to this map from the specified key-value [pair].
 *
 * If this map already contains a mapping for the key,
 * the old value is replaced by the value from the specified [pair].
 *
 * @return a new persistent map with an entry from the specified key-value [pair] added;
 * or this instance if no modifications were made in the result of this operation.
 */
@Suppress("UNCHECKED_CAST")
inline operator fun <K, V> PersistentMap<out K, V>.plus(pair: Pair<K, V>): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).put(pair.first, pair.second)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Array<out Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
inline operator fun <K, V> PersistentMap<out K, V>.plus(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V> = putAll(pairs)

/**
 * Returns the result of merging the specified [map] with this map.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * @return a new persistent map with keys and values from the specified [map] associated;
 * or this instance if no modifications were made in the result of this operation.
 */
inline operator fun <K, V> PersistentMap<out K, V>.plus(map: Map<out K, V>): PersistentMap<K, V> = putAll(map)


/**
 * Returns the result of merging the specified [map] with this map.
 *
 * The effect of this call is equivalent to that of calling `put(k, v)` once for each
 * mapping from key `k` to value `v` in the specified map.
 *
 * @return a new persistent map with keys and values from the specified [map] associated;
 * or this instance if no modifications were made in the result of this operation.
 */
@Suppress("UNCHECKED_CAST")
public fun <K, V> PersistentMap<out K, V>.putAll(map: Map<out K, V>): PersistentMap<K, V> =
        (this as PersistentMap<K, V>).putAll(map)

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Iterable<Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Array<out Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }

/**
 * Returns the result of replacing or adding entries to this map from the specified key-value pairs.
 *
 * @return a new persistent map with entries from the specified key-value pairs added;
 * or this instance if no modifications were made in the result of this operation.
 */
public fun <K, V> PersistentMap<out K, V>.putAll(pairs: Sequence<Pair<K, V>>): PersistentMap<K, V>
        = mutate { it.putAll(pairs) }


/**
 * Returns the result of removing the specified [key] and its corresponding value from this map.
 *
 * @return a new persistent map with the specified [key] and its corresponding value removed;
 * or this instance if it contains no mapping for the key.
 */
@Suppress("UNCHECKED_CAST")
public operator fun <K, V> PersistentMap<out K, V>.minus(key: K): PersistentMap<K, V>
        = (this as PersistentMap<K, V>).remove(key)

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 * or this instance if no modifications were made in the result of this operation.
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Iterable<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 * or this instance if no modifications were made in the result of this operation.
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Array<out K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }

/**
 * Returns the result of removing the specified [keys] and their corresponding values from this map.
 *
 * @return a new persistent map with the specified [keys] and their corresponding values removed;
 * or this instance if no modifications were made in the result of this operation.
 */
public operator fun <K, V> PersistentMap<out K, V>.minus(keys: Sequence<K>): PersistentMap<K, V>
        = mutate { it.minusAssign(keys) }



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
