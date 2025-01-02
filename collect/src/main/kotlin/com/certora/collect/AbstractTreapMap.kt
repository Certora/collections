package com.certora.collect

import com.certora.forkjoin.*
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet

/**
    Base class for TreapMap implementations.  Provides the Map operations; derived classes deal with type-specific
    behavior such as hash collisions.  See [Treap] for an overview of all of this.
 */
internal sealed class AbstractTreapMap<@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>>(
    left: S?,
    right: S?
) : TreapMap<K, V>, Treap<K, S>(left, right) {

    /**
        Derived classes override to create an apropriate node containing the given entry.
     */
    abstract fun new(key: K, value: V): S

    /**
        Converts the given Map to a AbstractTreapMap, if the Collection is already a AbstractTreapMap of the same type
        as 'this' AbstractTreapMap.  For example, if this is a HashTreapMap, and so is the supplied collection.
        Otherwise returns null.
     */
    abstract fun Map<out K, V>.toTreapMapOrNull(): AbstractTreapMap<K, V, S>?

    /**
        Given a map, calls the supplied `action` if the collection is a Treap of the same type as this Treap, otherwise
        calls `fallback.`  Used to implement optimized operations over two compatible Treaps, with a fallback when
        needed.
     */
    private inline fun <R> Map<out K, V>.useAsTreap(action: (S) -> R, fallback: () -> R): R {
        val treapMap = this.toTreapMapOrNull()
        return if (treapMap != null) {
            action(treapMap.self)
        } else {
            fallback()
        }
    }

    /**
        Gets a sequence of map entries just in this Treap node.
     */
    abstract fun shallowEntrySequence(): Sequence<Map.Entry<K, V>>

    /**
        Converts the supplied map key to a TreapKey appropriate to this type of AbstractTreapMap (sorted vs. hashed)
     */
    abstract fun K.toTreapKey(): TreapKey<K>?

    /**
        Does this node contain an entry with the given map key?
     */
    abstract fun shallowContainsKey(key: K): Boolean

    /**
        Gets the value of the entry with the given key, *in this node only*.
     */
    abstract fun shallowGetValue(key: K): V?

    abstract fun shallowRemoveEntry(key: K, value: V): S?
    abstract fun <U> shallowUpdate(entryKey: K, toUpdate: U, merger: (V?, U) -> V?): S?
    abstract fun <R : Any> shallowMapReduce(map: (K, V) -> R, reduce: (R, R) -> R): R

    /**
        Applies a merge function to all entries in this Treap node.
     */
    abstract fun getShallowMerger(merger: (K, V?, V?) -> V?): (S?, S?) -> S?
    abstract fun getShallowUnionMerger(merger: (K, V, V) -> V): (S, S) -> S
    abstract fun getShallowIntersectMerger(merger: (K, V, V) -> V): (S, S) -> S?

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
        Gets a sequence of all map entries in this Map.
     */
    fun entrySequence() = asTreapSequence().flatMap { it.shallowEntrySequence() }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Map opreations start here

    override fun toString(): String = entries.joinToString(", ", "{", "}") { toString(it) }

    private fun toString(entry: Map.Entry<K, V>): String = toString(entry.key) + "=" + toString(entry.value)

    private fun toString(o: Any?): String = if (o === this) { "(this Map)" } else { o.toString() }

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?) : Boolean {
        val otherMap = other as? Map<K, V>
        return when {
            otherMap == null -> false
            otherMap === this -> true
            otherMap.isEmpty() -> false // NB AbstractTreapMap always contains at least one entry
            else -> otherMap.useAsTreap(
                { otherTreap -> this.self.deepEquals(otherTreap) },
                { other.size == this.size && other.entries.all { this.containsEntry(it) }}
            )
        }
    }

    override val size: Int get() = computeSize()
    override fun isEmpty(): Boolean = false

    // NB AbstractTreapMap always contains at least one entry
    override fun single() = singleOrNull() ?: throw IllegalArgumentException("Map contains more than one entry")

    override fun containsKey(key: K) =
        key.toTreapKey()?.let { self.find(it) }?.shallowContainsKey(key) ?: false

    override fun containsValue(value: V) = values.contains(value)

    override fun get(key: K): V? =
        key.toTreapKey()?.let { self.find(it) }?.shallowGetValue(key)

    override fun putAll(m: Map<out K, V>): TreapMap<K, V> =
        m.entries.fold(this as TreapMap<K, V>) { t, e -> t.put(e.key, e.value) }

    override fun remove(key: K): TreapMap<K, V> =
        key.toTreapKey()?.let { self.remove(it, key) ?: clear() } ?: this

    override fun remove(key: K, value: V): TreapMap<K, V> =
        key.toTreapKey()?.let { self.removeEntry(it, key, value) ?: clear() } ?: this

    override fun clear(): TreapMap<K, V> = treapMapOf<K, V>()

    override fun builder(): TreapMapBuilder<K, V> = TreapMapBuilder(self)

    override val entries: ImmutableSet<Map.Entry<K, V>>
        get() = object : AbstractSet<Map.Entry<K, V>>(), ImmutableSet<Map.Entry<K, V>> {
            override val size get() = this@AbstractTreapMap.size
            override fun isEmpty() = this@AbstractTreapMap.isEmpty()
            override fun iterator() = entrySequence().iterator()
        }

    override val values: ImmutableCollection<V>
        get() = object: AbstractCollection<V>(), ImmutableCollection<V> {
            override val size get() = this@AbstractTreapMap.size
            override fun isEmpty() = this@AbstractTreapMap.isEmpty()
            override operator fun iterator() = entrySequence().map { it.value }.iterator()
        }

    override fun union(m: Map<K, V>, merger: (K, V, V) -> V): TreapMap<K, V> =
        m.useAsTreap(
            { otherTreap -> self.unionWith(otherTreap, getShallowUnionMerger(merger)) ?: clear() },
            { fallbackUnion(m, merger) }
        )

    override fun parallelUnion(m: Map<K, V>, parallelThresholdLog2: Int, merger: (K, V, V) -> V): TreapMap<K, V> =
        m.useAsTreap(
            { otherTreap -> self.parallelUnionWith(otherTreap, parallelThresholdLog2, getShallowUnionMerger(merger)) ?: clear() },
            { fallbackUnion(m, merger) }
        )

    private fun fallbackUnion(m: Map<K, V>, merger: (K, V, V) -> V): TreapMap<K, V> {
        var newThis = this as TreapMap<K, V>
        for ((k, v) in m.entries) {
            if (k in this) {
                newThis = newThis + (k to merger(k, this[k]!!, v))
            } else {
                newThis = newThis + (k to v)
            }
        }
        return newThis
    }

    override fun intersect(m: Map<K, V>, merger: (K, V, V) -> V): TreapMap<K, V> =
        m.useAsTreap(
            { otherTreap -> self.intersectWith(otherTreap, getShallowIntersectMerger(merger)) ?: clear() },
            { fallbackIntersect(m, merger) }
        )

    override fun parallelIntersect(m: Map<K, V>, parallelThresholdLog2: Int, merger: (K, V, V) -> V): TreapMap<K, V> =
        m.useAsTreap(
            { otherTreap -> self.parallelIntersectWith(otherTreap, parallelThresholdLog2, getShallowIntersectMerger(merger)) ?: clear() },
            { fallbackIntersect(m, merger) }
        )

    private fun fallbackIntersect(m: Map<K, V>, merger: (K, V, V) -> V): TreapMap<K, V> {
        var newThis = clear()
        for ((k, v) in m.entries) {
            if (k in this) {
                newThis = newThis + (k to merger(k, this[k]!!, v))
            }
        }
        return newThis
    }

    /**
        Merges the entries in `m` with the entries in this AbstractTreapMap, applying the "merger" function to get the
        new values for each key.
     */
    override fun merge(m: Map<K, V>, merger: (K, V?, V?) -> V?): TreapMap<K, V> =
        m.useAsTreap(
            { otherTreap -> self.mergeWith(otherTreap, getShallowMerger(merger)) ?: clear() },
            { fallbackMerge(m, merger) }
        )

    /**
        Merges the entries in `m` with the entries in this AbstractTreapMap, applying the "merger" function to get the
        new values or each key, processing multiple entries in parallel.

        @param[parallelThresholdLog2] The minimum number of entries to process in parallel, expressed as a power of 2.
        If a subtree is estimated to have fewer entries than this, it will be processed sequentially.

        @param[merger] The merge function to apply to each pair of entries.  Must be pure and thread-safe.
     */
    override fun parallelMerge(m: Map<K, V>, parallelThresholdLog2: Int, merger: (K, V?, V?) -> V?): TreapMap<K, V> =
        m.useAsTreap(
            { otherTreap -> self.parallelMergeWith(otherTreap, parallelThresholdLog2, getShallowMerger(merger)) ?: clear() },
            { fallbackMerge(m, merger) }
        )

    private fun fallbackMerge(m: Map<K, V>, merger: (K, V?, V?) -> V?): TreapMap<K, V> {
        var newThis = clear()
        for (k in this.keys.asSequence() + m.keys.asSequence()) {
            if (k !in newThis) {
                newThis = when (val merged = merger(k, this[k], m[k])) {
                    null -> newThis.remove(k)
                    else -> newThis.put(k, merged)
                }
            }
        }
        return newThis
    }

    /**
        Applies a transform to each entry, producing new values.
     */
    @Suppress("UNCHECKED_CAST", "Treapability")
    override fun <R : Any> updateValues(transform: (K, V) -> R?): TreapMap<K, R> =
        (this as AbstractTreapMap<Any?, Any?, *>).updateValuesErasedTypes(
            transform as (Any?, Any?) -> Any?
        ) as TreapMap<K, R>

    private fun updateValuesErasedTypes(transform: (K, V) -> V?): TreapMap<K, V> = when {
        isEmpty() -> self
        else -> notForking(this) {
            updateValuesImpl(transform) ?: clear()
        }
    }

    /**
        Applies a transform to each entry, producing new values, processing multiple entries in parallel.

        @param[parallelThresholdLog2] The minimum number of entries to process in parallel, expressed as a power of 2.
        If a subtree is estimated to have fewer entries than this, it will be processed sequentially.

        @param[transform] The transform to apply to each entry.  Must be pure and thread-safe.
     */
    @Suppress("UNCHECKED_CAST", "Treapability")
    override fun <R : Any> parallelUpdateValues(parallelThresholdLog2: Int, transform: (K, V) -> R?): TreapMap<K, R> =
        (this as AbstractTreapMap<Any?, Any?, *>).parallelUpdateValuesErasedTypes(
            parallelThresholdLog2,
            transform as (Any?, Any?) -> Any?
        ) as TreapMap<K, R>

    private fun parallelUpdateValuesErasedTypes(parallelThresholdLog2: Int, transform: (K, V) -> V?): TreapMap<K, V> = when {
        isEmpty() -> self
        else -> maybeForking(self, threshold = { it.isApproximatelySmallerThanLog2(parallelThresholdLog2) }) {
            updateValuesImpl(transform) ?: clear()
        }
    }

    context(ThresholdForker<S>)
    private fun updateValuesImpl(transform: (K, V) -> V?): S? {
        val (newLeft, newRight, newThis) = fork(
            self,
            { left?.updateValuesImpl(transform) },
            { right?.updateValuesImpl(transform) },
            { shallowUpdateValues(transform) }
        )
        return newThis?.with(newLeft, newRight) ?: (newLeft join newRight)
    }

    abstract fun shallowUpdateValues(transform: (K, V) -> V?): S?


    /**
        Insert, update, or remove an entry at key [key] based on the auxiliary data [value] passed in and the supplied
        [merger] function. The [merger] function takes the current value associated with [key] (if it exists) and the
        value being updated and produces the new value for [key], or null if the binding should be removed.

        More specifically, if [key] exists in the map, then the current value associate with [key] is passed into merger
        as the first argument, along with [value] as the second argument. If the merger function then returns null, the
        key is removed from the mapping, otherwise the value associated with [key] is updated to the value returned by
        merger.

        If [key] does not exist in the map, then [merger] is called with null as the first argument and [value] as the
        second argument. If [merger] then returns null, the map is unchanged, otherwise, [key] is inserted into the map.

        In pseudo-code, this function is equivalent to the following:
        ```
        if(key in this) {
          val merged = merger(this.get(key)!!, value)
          if(merged == null) {
             this.removeKey(key)
          } else {
             this.put(key, merged)
          }
       } else {
          val gen = merger(null, value)
          if(gen == null) {
             this
          } else {
             this.put(key, gen)
          }
       }
       ```
     */
    override fun <U> updateEntry(key: K, value: U, merger: (V?, U) -> V?): TreapMap<K, V> {
        val treapKey = key.toTreapKey()?.precompute()
        return if (treapKey == null) {
            // The key is not compatible with this map type, so it's definitely not in the map.
            merger(null, value)?.let { put(key, it) } ?: this
        } else {
            self.updateEntry(treapKey, key, value, merger, ::new) ?: clear()
        }
    }

    /**
        Produces a sequence from the entries of this map and another map.  For each key, the result is an entry mapping
        the key to a pair of (possibly null) values.
     */
    override fun zip(m: Map<out K, V>) = sequence<Map.Entry<K, Pair<V?, V?>>> {
        fun <T> Iterator<T>.nextOrNull() = if (hasNext()) { next() } else { null }

        val sequences = getTreapSequencesIfSameType(m)
        if (sequences != null) {
            // Fast case for when the maps are the same type
            val thisIt = sequences.first.iterator()
            val thatIt = sequences.second.iterator()

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
        } else {
            // Slower fallback for maps of different types
            for ((k, v) in entries) {
                yield(MapEntry(k, v to m[k]))
            }
            for ((k, v) in m.entries) {
                if (k !in this@AbstractTreapMap) {
                    yield(MapEntry(k, null to v))
                }
            }
        }
    }

    private fun shallowZipThisOnly() = shallowEntrySequence().map { MapEntry(it.key, it.value to null) }
    private fun shallowZipThatOnly() = shallowEntrySequence().map { MapEntry(it.key, null to it.value) }
    protected abstract fun shallowZip(that: S): Sequence<Map.Entry<K, Pair<V?, V?>>>
    protected abstract fun getTreapSequencesIfSameType(that: Map<out K, V>): Pair<Sequence<S>, Sequence<S>>?


    override fun <R : Any> mapReduce(map: (K, V) -> R, reduce: (R, R) -> R): R =
        notForking(self) { mapReduceImpl(map, reduce) }

    override fun <R : Any> parallelMapReduce(map: (K, V) -> R, reduce: (R, R) -> R, parallelThresholdLog2: Int): R =
        maybeForking(self, threshold = { it.isApproximatelySmallerThanLog2(parallelThresholdLog2) }) {
            mapReduceImpl(map, reduce)
        }

    context(ThresholdForker<S>)
    private fun <R : Any> mapReduceImpl(map: (K, V) -> R, reduce: (R, R) -> R): R {
        val (left, middle, right) = fork(
            self,
            { left?.mapReduceImpl(map, reduce) },
            { shallowMapReduce(map, reduce) },
            { right?.mapReduceImpl(map, reduce) }
        )
        val leftAndMiddle = left?.let { reduce(it, middle) } ?: middle
        return right?.let { reduce(leftAndMiddle, it) } ?: leftAndMiddle
    }
}

/**
    Removes a map entry (`entryKey`, `entryValue`) with key `key`.
 */
internal fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.removeEntry(
    key: TreapKey<K>,
    entryKey: K,
    entryValue: V
): S? = when {
    this == null -> null
    key.comparePriorityTo(this) > 0 -> this
    else -> {
        val c = key.compareKeyTo(this)
        when {
            c < 0 -> this.with(left = left.removeEntry(key, entryKey, entryValue))
            c > 0 -> this.with(right = right.removeEntry(key, entryKey, entryValue))
            else -> this.shallowRemoveEntry(entryKey, entryValue) ?: (this.left join this.right)
        }
    }
}

internal fun <@Treapable K, V, U, @Treapable S : AbstractTreapMap<K, V, S>> S?.updateEntry(
    thatKey: TreapKey<K>,
    entryKey: K,
    toUpdate: U,
    merger: (V?, U) -> V?,
    new: (K, V) -> S
): S? = when {
    this == null -> {
        val generated = merger(null, toUpdate)
        if(generated == null) {
            null
        } else {
            new(entryKey, generated)
        }
    }
    else -> {
        val c = thatKey.comparePriorityTo(this)
        when {
            c == 0 -> this.shallowUpdate(entryKey, toUpdate, merger) ?: (left join right)
            c > 0 -> {
                val merged = merger(null, toUpdate)
                if(merged == null) {
                    self
                } else {
                    val newRoot = new(entryKey, merged)
                    this.split(thatKey).let { split ->
                        newRoot.with(left = split.left, right = split.right)
                    }
                }
            }
            thatKey.compareKeyTo(this) < 0 -> this.with(left = this.left.updateEntry(thatKey, entryKey, toUpdate, merger, new))
            else -> this.with(right = this.right.updateEntry(thatKey, entryKey, toUpdate, merger, new))
        }
    }
}

/**
    Merges two treaps, using a supplied merge function.  This is used to implement our Map<K, V>.merge() function.  It's
    distinct from `union` because it needs to call the merge function even in cases where the key only exists in one of
    the Treaps, to support the semantics of the higher-level Map.merge() function.  Note that we always prefer to return
    'this' over 'that', to preserve the object identity invariant described in the `Treap` summary.
 */
internal fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.mergeWith(
    that: S?,
    shallowMerge: (S?, S?) -> S?
): S? =
    notForking(this to that) {
        mergeWithImpl(that, shallowMerge)
    }

internal fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.parallelMergeWith(
    that: S?,
    parallelThresholdLog2: Int,
    shallowMerge: (S?, S?) -> S?
): S? =
    maybeForking(
        this to that,
        {
            it.first.isApproximatelySmallerThanLog2(parallelThresholdLog2 - 1) &&
            it.second.isApproximatelySmallerThanLog2(parallelThresholdLog2 - 1)
        }
    ) {
        mergeWithImpl(that, shallowMerge)
    }

context(ThresholdForker<Pair<S?, S?>>)
private fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.mergeWithImpl(
    that: S?,
    shallowMerge: (S?, S?) -> S?
): S? {
    val (newLeft, newRight, newThis) = when {
        this == null && that == null -> {
            return null
        }
        this == null || that == null -> {
            fork(
                this to that,
                { this?.left.mergeWithImpl(that?.left, shallowMerge) },
                { this?.right.mergeWithImpl(that?.right, shallowMerge) },
                { shallowMerge(this, that) }
            )
        }
        this.comparePriorityTo(that) >= 0 -> {
            val thatSplit = that.split(this)
            fork(
                this to that,
                { this.left.mergeWithImpl(thatSplit.left, shallowMerge) },
                { this.right.mergeWithImpl(thatSplit.right, shallowMerge) },
                { shallowMerge(this, thatSplit.duplicate) }
            )
        }
        else -> {
            // remember, a.comparePriorityTo(b)==0 <=> a.compareKeyTo(b)==0
            val thisSplit = this.split(that)
            fork(
                this to that,
                { thisSplit.left.mergeWithImpl(that.left, shallowMerge) },
                { thisSplit.right.mergeWithImpl(that.right, shallowMerge) },
                { shallowMerge(thisSplit.duplicate, that) }
            )
        }
    }
    return newThis?.with(newLeft, newRight) ?: (newLeft join newRight)
}

internal fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.unionWith(
    that: S?,
    shallowUnion: (S, S) -> S
): S? =
    notForking(this to that) {
        unionWithImpl(that, shallowUnion)
    }

internal fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.parallelUnionWith(
    that: S?,
    parallelThresholdLog2: Int,
    shallowUnion: (S, S) -> S
): S? =
    maybeForking(
        this to that,
        {
            it.first.isApproximatelySmallerThanLog2(parallelThresholdLog2 - 1) &&
            it.second.isApproximatelySmallerThanLog2(parallelThresholdLog2 - 1)
        }
    ) {
        unionWithImpl(that, shallowUnion)
    }

context(ThresholdForker<Pair<S?, S?>>)
private fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.unionWithImpl(
    that: S?,
    shallowUnion: (S, S) -> S
): S? {
    val (newLeft, newRight, newThis) = when {
        this == null -> return that
        that == null -> return this
        this.comparePriorityTo(that) >= 0 -> {
            val thatSplit = that.split(this)
            fork(
                this to that,
                { this.left.unionWithImpl(thatSplit.left, shallowUnion) },
                { this.right.unionWithImpl(thatSplit.right, shallowUnion) },
                { thatSplit.duplicate?.let { shallowUnion(this, it) } ?: this }
            )
        }
        else -> {
            val thisSplit = this.split(that)
            fork(
                this to that,
                { thisSplit.left.unionWithImpl(that.left, shallowUnion) },
                { thisSplit.right.unionWithImpl(that.right, shallowUnion) },
                { thisSplit.duplicate?.let { shallowUnion(it, that) } ?: that }
            )
        }
    }
    return newThis.with(newLeft, newRight)
}

internal fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.intersectWith(
    that: S?,
    shallowIntersect: (S, S) -> S?
): S? =
    notForking(this to that) {
        intersectWithImpl(that, shallowIntersect)
    }

internal fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.parallelIntersectWith(
    that: S?,
    parallelThresholdLog2: Int,
    shallowIntersect: (S, S) -> S?
): S? =
    maybeForking(
        this to that,
        {
            it.first.isApproximatelySmallerThanLog2(parallelThresholdLog2 - 1) &&
            it.second.isApproximatelySmallerThanLog2(parallelThresholdLog2 - 1)
        }
    ) {
        intersectWithImpl(that, shallowIntersect)
    }

context(ThresholdForker<Pair<S?, S?>>)
private fun <@Treapable K, V, @Treapable S : AbstractTreapMap<K, V, S>> S?.intersectWithImpl(
    that: S?,
    shallowIntersect: (S, S) -> S?
): S? {
    val (newLeft, newRight, newThis) = when {
        this == null || that == null -> return null
        this.comparePriorityTo(that) >= 0 -> {
            val thatSplit = that.split(this)
            fork(
                this to that,
                { this.left.intersectWithImpl(thatSplit.left, shallowIntersect) },
                { this.right.intersectWithImpl(thatSplit.right, shallowIntersect) },
                { thatSplit.duplicate?.let { shallowIntersect(this, it) } }
            )
        }
        else -> {
            val thisSplit = this.split(that)
            fork(
                this to that,
                { thisSplit.left.intersectWithImpl(that.left, shallowIntersect) },
                { thisSplit.right.intersectWithImpl(that.right, shallowIntersect) },
                { thisSplit.duplicate?.let { shallowIntersect(it, that) } }
            )
        }
    }
    return newThis?.with(newLeft, newRight) ?: (newLeft join newRight)
}
