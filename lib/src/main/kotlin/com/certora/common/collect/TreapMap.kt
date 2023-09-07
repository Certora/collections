package com.certora.common.collect

import com.certora.common.collect.*
import com.certora.common.forkjoin.*
import com.certora.common.utils.*
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentMap

/**
 * Base class for Treap-based PersistentMap implementations.  Provides the Map operations; derived classes deal
 * with type-specific behavior such as hash collisions.  See [Treap] for an overview of all of this.
 */
internal abstract class TreapMap<@WithStableHashCodeIfSerialized K, V>(
    left: Treap?,
    right: Treap?
) : PersistentMap<K, V>, Treap(left, right) {

    /**
     * In order to reduce heap space usage, we derive from Treap.  That makes it tricky to have a TreapMap representing
     * an empty map.  To handle that, we create special subclasses to represent empty maps, and distinguish them
     * via this property.  `treap` returns `this` if this is a "real" node containing data, and `null` if this is
     * an empty node.  We do it this way because it works out very nicely with interacting with the base Treap
     * methods, which use `null` Treaps to represent "nothing."
     */
    abstract val treap: Treap?

    /**
     * Derived classes override to create an apropriate node containing the given entry
     */
    abstract fun new(key: K, value: V): TreapMap<K, V>

    /**
     * Converts the given Map to a TreapMap, if the Collection is already a TreapMap of the same type as 'this'
     * TreapMap.  For example, if this is a HashTreapMap, and so is the supplied collection.  Otherwise returns null.
     */
    abstract fun Map<out K, V>.toTreapMapOrNull(): TreapMap<K, V>?

    /**
     * Converts the given Map to a TreapMap of the same type as 'this'.  May copy the map.
     */
    fun Map<out K, V>.toTreapMap(): TreapMap<K, V> = toTreapMapOrNull() ?: this@TreapMap.clear().putAll(this)

    /**
     * Given a map, calls the supplied `action` if the collection is a Treap of the same type as this Treap,
     * otherwise calls `fallback.`  Used to implement optimized operations over two compatible Treaps, with a fallback
     * when needed.
     */
    private inline fun <R> Map<out K, V>.useAsTreap(action: (Treap?) -> R, fallback: () -> R): R {
        val treapMap = this.toTreapMapOrNull()
        return if (treapMap != null) {
            action(treapMap.treap)
        } else {
            fallback()
        }
    }

    /**
     * Gets a sequence of map entries just in this Treap node
     */
    abstract fun shallowEntrySequence(): Sequence<Map.Entry<K, V>>

    /**
     * Converts the supplied map key to a TreapKey appropriate to this type of TreapMap (sorted vs. hashed)
     */
    abstract fun K.toTreapKey(): TreapKey

    /**
     * Does this node contain an entry with the given map key?
     */
    abstract fun shallowContainsKey(key: K): Boolean

    /**
     * Gets the value of the entry with the given key, *in this node only*
     */
    abstract fun shallowGetValue(key: K): V?

    /**
     * Applies a merge function to all entries in this Treap node
     */
    abstract fun getShallowMerger(merger: (K, V?, V?) -> V?): (Treap?, Treap?) -> Treap?

    // Maps don't have these operations
    override fun shallowContainsAll(elements: Treap): Boolean = throw UnsupportedOperationException()
    override fun shallowContainsAny(elements: Treap): Boolean = throw UnsupportedOperationException()
    override fun shallowUnion(that: Treap): Treap = throw UnsupportedOperationException()
    override fun shallowIntersect(that: Treap): Treap? = throw UnsupportedOperationException()
    override fun shallowDifference(that: Treap): Treap? = throw UnsupportedOperationException()
    override fun shallowGetSingleElement(): Any? = throw UnsupportedOperationException()

    private fun containsEntry(entry: Map.Entry<K, V>): Boolean {
        val key = entry.key
        val value = entry.value
        return when {
            this.get(key) != value -> false
            value != null -> true // get(key) returned a non-null value, and it matched
            else -> this.containsKey(key) // get(key) returned null; is the key in the map or not?
        }
    }

    /**
     * Gets a sequence of all map entries in this Map.
     */
    fun entrySequence() = treap.asSequence().flatMap { it.uncheckedAs<TreapMap<K, V>>().shallowEntrySequence() }


    private fun Treap?.asMap() = this.uncheckedAs<TreapMap<K, V>?>() ?: clear()


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Map opreations start here

    override fun toString(): String = entries.joinToString(", ", "{", "}") { toString(it) }

    private fun toString(entry: Map.Entry<K, V>): String = toString(entry.key) + "=" + toString(entry.value)

    private fun toString(o: Any?): String = if (o === this) { "(this Map)" } else { o.toString() }

    override fun hashCode() = treap.computeHashCode()

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?) : Boolean {
        val otherMap = other as? Map<K, V>
        return when {
            otherMap == null -> false
            otherMap === this -> true
            else -> otherMap.useAsTreap(
                { otherTreap -> this.treap.deepEquals(otherTreap) },
                { other.size == this.size && other.entries.all { this.containsEntry(it) }}
            )
        }
    }

    override val size: Int get() = treap.computeSize()
    override fun isEmpty(): Boolean = treap == null

    override fun containsKey(key: K) =
        treap.find(key.toTreapKey()).uncheckedAs<TreapMap<K, V>?>()?.shallowContainsKey(key) ?: false

    override fun containsValue(value: V) = values.contains(value)

    override fun get(key: K): V? =
        treap.find(key.toTreapKey()).uncheckedAs<TreapMap<K, V>?>()?.shallowGetValue(key)

    override fun put(key: K, value: V): TreapMap<K, V> =
        treap.add(new(key, value)).asMap()

    override fun putAll(m: Map<out K, V>): TreapMap<K, V> =
        m.entries.fold(this) { t, e -> t.put(e.key, e.value) }

    override fun remove(key: K): PersistentMap<K, V> =
        treap.remove(key.toTreapKey(), key).asMap()

    override fun remove(key: K, value: V): PersistentMap<K, V> =
        (treap difference new(key, value)).asMap()

    // Require clear() to return a TreapMap
    override abstract fun clear(): TreapMap<K, V>

    override fun builder(): PersistentMap.Builder<K, V> = SimplePersistentMapBuilder(this)

    override val entries: ImmutableSet<Map.Entry<K, V>>
        get() = object : AbstractSet<Map.Entry<K, V>>(), ImmutableSet<Map.Entry<K, V>> {
            override val size get() = this@TreapMap.size
            override fun isEmpty() = this@TreapMap.isEmpty()
            override fun iterator() = entrySequence().iterator()
        }

    override val keys: ImmutableSet<K>
        get() = object: AbstractSet<K>(), ImmutableSet<K> {
            override val size get() = this@TreapMap.size
            override fun isEmpty() = this@TreapMap.isEmpty()
            override operator fun contains(element: K) = containsKey(element)
            override operator fun iterator() = entrySequence().map { it.key }.iterator()
        }

    override val values: ImmutableCollection<V>
        get() = object: AbstractCollection<V>(), ImmutableCollection<V> {
            override val size get() = this@TreapMap.size
            override fun isEmpty() = this@TreapMap.isEmpty()
            override operator fun iterator() = entrySequence().map { it.value }.iterator()
        }

    /**
     * Merges the entries in `m` with the entries in this TreapMap, applying the "merger" function to get the new values
     * for each key.
     */
    fun merge(m: Map<out K, V>, merger: (K, V?, V?) -> V?): PersistentMap<K, V> =
        m.useAsTreap(
            { otherTreap -> this.treap.mergeWith(otherTreap, getShallowMerger(merger)).asMap() },
            { fallbackMerge(m, merger) }
        )

    /**
      Merges the entries in `m` with the entries in this TreapMap, applying the "merger" function to get the new values
      or each key, processing multiple entries in parallel.

      @param[parallelThresholdLog2] The minimum number of entries to process in parallel, expressed as a power of 2. If
      a subtree is estimated to have fewer entries than this, it will be processed sequentially.

      @param[merger] The merge function to apply to each pair of entries.  Must be pure and thread-safe.
     */
    fun parallelMerge(m: Map<out K, V>, parallelThresholdLog2: Int, merger: (K, V?, V?) -> V?): PersistentMap<K, V> =
        m.useAsTreap(
            { otherTreap -> this.treap.parallelMergeWith(otherTreap, parallelThresholdLog2, getShallowMerger(merger)).asMap() },
            { fallbackMerge(m, merger) }
        )

    private fun fallbackMerge(m: Map<out K, V>, merger: (K, V?, V?) -> V?): PersistentMap<K, V> {
        val newThis = clear().builder()
        for (k in this.keys.asSequence() + m.keys.asSequence()) {
            if (k !in newThis) {
                when (val merged = merger(k, this[k], m[k])) {
                    null -> newThis.remove(k)
                    else -> newThis.put(k, merged)
                }
            }
        }
        return newThis.build()
    }

    /**
     * Applies a transform to each entry, producing new values.
     */
    fun updateValues(transform: (K, V) -> V?): TreapMap<K, V> = when {
        isEmpty() -> this
        else -> notForking(this) {
            updateValuesImpl(transform).asMap()
        }
    }

    /**
      Applies a transform to each entry, producing new values, processing multiple entries in parallel.

      @param[parallelThresholdLog2] The minimum number of entries to process in parallel, expressed as a power of 2. If a
      subtree is estimated to have fewer entries than this, it will be processed sequentially.

      @param[transform] The transform to apply to each entry.  Must be pure and thread-safe.
     */
    fun parallelUpdateValues(parallelThresholdLog2: Int, transform: (K, V) -> V?): PersistentMap<K, V> = when {
        isEmpty() -> this
        else -> maybeForking(this, threshold = { it.isApproximatelySmallerThanLog2(parallelThresholdLog2) }) {
            updateValuesImpl(transform).asMap()
        }
    }

    context(ThresholdForker<TreapMap<K, V>>)
    private fun updateValuesImpl(transform: (K, V) -> V?): Treap? {
        val (newLeft, newRight, newThis) = fork(
            this,
            { left.uncheckedAs<TreapMap<K, V>?>()?.updateValuesImpl(transform) },
            { right.uncheckedAs<TreapMap<K, V>?>()?.updateValuesImpl(transform) },
            { shallowUpdateValues(transform) }
        )
        return newThis?.with(newLeft, newRight) ?: (newLeft join newRight)
    }

    abstract fun shallowUpdateValues(transform: (K, V) -> V?): Treap?


    /**
     * Insert, update, or remove an entry at key [key] based on the auxiliary data [value] passed in and the supplied
     * [merger] function. The [merger] function takes the current value associated with [key] (if it exists) and the value
     * being updated and produces the new value for [key], or null if the binding should be removed.
     *
     * More specifically, if [key] exists in the map, then the current value associate with [key] is passed into merger
     * as the first argument, along with [value] as the second argument. If the merger function then returns null,
     * the key is removed from the mapping, otherwise the value associated with [key] is updated to the value returned by merger.
     *
     * If [key] does not exist in the map, then [merger] is called with null as the first argument and [value] as the second
     * argument. If [merger] then returns null, the map is unchanged, otherwise, [key] is inserted into the map.
     *
     * In pseudo-code, this function is equivalent to the following:
     * ```
     * if(key in this) {
     *    val merged = merger(this.get(key)!!, value)
     *    if(merged == null) {
     *       this.removeKey(key)
     *    } else {
     *       this.put(key, merged)
     *    }
     * } else {
     *    val gen = merger(null, value)
     *    if(gen == null) {
     *       this
     *    } else {
     *       this.put(key, gen)
     *    }
     * }
     * ```
     */
    fun <U> updateEntry(key: K, value: U?, merger: (V?, U?) -> V?) : PersistentMap<K, V> {
        return this.treap.updateEntry(key.toTreapKey().precompute(), key, value, merger.uncheckedAs(), this::new.uncheckedAs()).uncheckedAs() ?: clear()
    }

    /**
     * Produces a sequence from the entries of this map and another map.  For each key, the result is an entry mapping
     * the key to a pair of (possibly null) values.
     */
    fun zip(m: Map<out K, V>) = sequence<Map.Entry<K, Pair<V?, V?>>> {
        // Iterate over the two maps' treap sequences.  We ensure that each sequence uses the same key ordering, by
        // converting `m` to a TreapMap of this map's type, if necessary.
        // Note that we can't use entrySequence, because HashTreapMap's entrySequence is only partially ordered.
        val thisIt = treap.asSequence().uncheckedAs<Sequence<TreapMap<K, V>>>().iterator()
        val thatIt = m.toTreapMap().treap.asSequence().uncheckedAs<Sequence<TreapMap<K, V>>>().iterator()

        var thisCurrent = thisIt.nextOrNull()
        var thatCurrent = thatIt.nextOrNull()

        while (thisCurrent != null && thatCurrent != null) {
            val c = thisCurrent.compareKeyTo(thatCurrent)
            when {
                c < 0 -> {
                    yieldAll(thisCurrent.shallowZipThisOnly())
                    thisCurrent = thisIt.nextOrNull()
                }
                c > 0 -> {
                    yieldAll(thatCurrent.shallowZipThatOnly())
                    thatCurrent = thatIt.nextOrNull()
                }
                else -> {
                    yieldAll(thisCurrent.shallowZip(thatCurrent))
                    thisCurrent = thisIt.nextOrNull()
                    thatCurrent = thatIt.nextOrNull()
                }
            }
        }
        while (thisCurrent != null) {
            yieldAll(thisCurrent.shallowZipThisOnly())
            thisCurrent = thisIt.nextOrNull()
        }
        while (thatCurrent != null) {
            yieldAll(thatCurrent.shallowZipThatOnly())
            thatCurrent = thatIt.nextOrNull()
        }
    }

    private fun shallowZipThisOnly() = shallowEntrySequence().map { MapEntry(it.key, it.value to null) }
    private fun shallowZipThatOnly() = shallowEntrySequence().map { MapEntry(it.key, null to it.value) }
    protected abstract fun shallowZip(that: TreapMap<K, V>): Sequence<Map.Entry<K, Pair<V?, V?>>>
}
