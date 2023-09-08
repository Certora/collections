package com.certora.common.collect

import com.certora.common.forkjoin.*
import kotlinx.collections.immutable.ImmutableCollection
import kotlinx.collections.immutable.ImmutableSet

/**
    Base class for TreapMap implementations.  Provides the Map operations; derived classes deal with type-specific
    behavior such as hash collisions.  See [Treap] for an overview of all of this.
 */
internal abstract class AbstractTreapMap<@Treapable K, V, S : AbstractTreapMap<K, V, S>>(
    left: S?,
    right: S?
) : TreapMap<K, V>, Treap<K, S>(left, right) {

    /**
        Derived classes override to create an apropriate node containing the given entry
     */
    abstract fun new(key: K, value: V): S

    /**
        Converts the given Map to a AbstractTreapMap, if the Collection is already a AbstractTreapMap of the same type
        as 'this' AbstractTreapMap.  For example, if this is a HashTreapMap, and so is the supplied collection.
        Otherwise returns null.
     */
    abstract fun Map<out K, V>.toTreapMapOrNull(): AbstractTreapMap<K, V, S>?

    /**
        Converts the given Map to a AbstractTreapMap of the same type as 'this'.  May copy the map.
     */
    fun Map<out K, V>.toTreapMap(): AbstractTreapMap<K, V, S> = toTreapMapOrNull() ?: this@AbstractTreapMap.clear().putAll(this)

    /**
        Given a map, calls the supplied `action` if the collection is a Treap of the same type as this Treap, otherwise
        calls `fallback.`  Used to implement optimized operations over two compatible Treaps, with a fallback when
        needed.
     */
    private inline fun <R> Map<out K, V>.useAsTreap(action: (S?) -> R, fallback: () -> R): R {
        val treapMap = this.toTreapMapOrNull()
        return if (treapMap != null) {
            action(treapMap.selfNotEmpty)
        } else {
            fallback()
        }
    }

    /**
        Gets a sequence of map entries just in this Treap node
     */
    abstract fun shallowEntrySequence(): Sequence<Map.Entry<K, V>>

    /**
        Converts the supplied map key to a TreapKey appropriate to this type of AbstractTreapMap (sorted vs. hashed)
     */
    abstract fun K.toTreapKey(): TreapKey<K>

    /**
        Does this node contain an entry with the given map key?
     */
    abstract fun shallowContainsKey(key: K): Boolean

    /**
        Gets the value of the entry with the given key, *in this node only*
     */
    abstract fun shallowGetValue(key: K): V?

    abstract fun shallowRemoveEntry(key: K, value: V): S?
    abstract fun <U> shallowUpdate(entryKey: K, toUpdate: U, merger: (V?, U?) -> V?): S?

    /**
        Applies a merge function to all entries in this Treap node
     */
    abstract fun getShallowMerger(merger: (K, V?, V?) -> V?): (S?, S?) -> S?

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
    fun entrySequence() = selfNotEmpty.asSequence().flatMap { it.shallowEntrySequence() }


    ////////////////////////////////////////////////////////////////////////////////////////////////////
    // Map opreations start here

    override fun toString(): String = entries.joinToString(", ", "{", "}") { toString(it) }

    private fun toString(entry: Map.Entry<K, V>): String = toString(entry.key) + "=" + toString(entry.value)

    private fun toString(o: Any?): String = if (o === this) { "(this Map)" } else { o.toString() }

    override fun hashCode() = selfNotEmpty.computeHashCode()

    @Suppress("UNCHECKED_CAST")
    override fun equals(other: Any?) : Boolean {
        val otherMap = other as? Map<K, V>
        return when {
            otherMap == null -> false
            otherMap === this -> true
            else -> otherMap.useAsTreap(
                { otherTreap -> this.selfNotEmpty.deepEquals(otherTreap) },
                { other.size == this.size && other.entries.all { this.containsEntry(it) }}
            )
        }
    }

    override val size: Int get() = selfNotEmpty.computeSize()
    override fun isEmpty(): Boolean = selfNotEmpty == null

    override fun containsKey(key: K) =
        selfNotEmpty.find(key.toTreapKey())?.shallowContainsKey(key) ?: false

    override fun containsValue(value: V) = values.contains(value)

    override fun get(key: K): V? =
        selfNotEmpty.find(key.toTreapKey())?.shallowGetValue(key)

    override fun put(key: K, value: V): S =
        selfNotEmpty.add(new(key, value))

    override fun putAll(m: Map<out K, V>): S =
        m.entries.fold(self) { t, e -> t.put(e.key, e.value) }

    override fun remove(key: K): S =
        selfNotEmpty.remove(key.toTreapKey(), key) ?: clear()

    override fun remove(key: K, value: V): S =
        selfNotEmpty.removeEntry(key.toTreapKey(), key, value) ?: clear()

    override abstract fun clear(): S

    override fun builder(): TreapMapBuilder<K, V, S> = TreapMapBuilder<K, V, S>(self)

    override val entries: ImmutableSet<Map.Entry<K, V>>
        get() = object : AbstractSet<Map.Entry<K, V>>(), ImmutableSet<Map.Entry<K, V>> {
            override val size get() = this@AbstractTreapMap.size
            override fun isEmpty() = this@AbstractTreapMap.isEmpty()
            override fun iterator() = entrySequence().iterator()
        }

    override val keys: ImmutableSet<K>
        get() = object: AbstractSet<K>(), ImmutableSet<K> {
            override val size get() = this@AbstractTreapMap.size
            override fun isEmpty() = this@AbstractTreapMap.isEmpty()
            override operator fun contains(element: K) = containsKey(element)
            override operator fun iterator() = entrySequence().map { it.key }.iterator()
        }

    override val values: ImmutableCollection<V>
        get() = object: AbstractCollection<V>(), ImmutableCollection<V> {
            override val size get() = this@AbstractTreapMap.size
            override fun isEmpty() = this@AbstractTreapMap.isEmpty()
            override operator fun iterator() = entrySequence().map { it.value }.iterator()
        }

    /**
        Merges the entries in `m` with the entries in this AbstractTreapMap, applying the "merger" function to get the
        new values for each key.
     */
    override fun merge(m: Map<out K, V>, merger: (K, V?, V?) -> V?): S =
        m.useAsTreap(
            { otherTreap -> selfNotEmpty.mergeWith(otherTreap, getShallowMerger(merger)) ?: clear() },
            { fallbackMerge(m, merger) }
        )

    /**
        Merges the entries in `m` with the entries in this AbstractTreapMap, applying the "merger" function to get the
        new values or each key, processing multiple entries in parallel.

        @param[parallelThresholdLog2] The minimum number of entries to process in parallel, expressed as a power of 2.
        If a subtree is estimated to have fewer entries than this, it will be processed sequentially.

        @param[merger] The merge function to apply to each pair of entries.  Must be pure and thread-safe.
     */
    override fun parallelMerge(m: Map<out K, V>, parallelThresholdLog2: Int, merger: (K, V?, V?) -> V?): S =
        m.useAsTreap(
            { otherTreap -> selfNotEmpty.parallelMergeWith(otherTreap, parallelThresholdLog2, getShallowMerger(merger)) ?: clear() },
            { fallbackMerge(m, merger) }
        )

    private fun fallbackMerge(m: Map<out K, V>, merger: (K, V?, V?) -> V?): S {
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
        Applies a transform to each entry, producing new values.
     */
    override fun updateValues(transform: (K, V) -> V?): S = when {
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
    override fun parallelUpdateValues(parallelThresholdLog2: Int, transform: (K, V) -> V?): S = when {
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
    override fun <U> updateEntry(key: K, value: U?, merger: (V?, U?) -> V?): S {
        return selfNotEmpty.updateEntry(key.toTreapKey().precompute(), key, value, merger, ::new) ?: clear()
    }

    /**
        Produces a sequence from the entries of this map and another map.  For each key, the result is an entry mapping
        the key to a pair of (possibly null) values.
     */
    override fun zip(m: Map<out K, V>) = sequence<Map.Entry<K, Pair<V?, V?>>> {
        fun <T> Iterator<T>.nextOrNull() = if (hasNext()) { next() } else { null }

        // Iterate over the two maps' treap sequences.  We ensure that each sequence uses the same key ordering, by
        // converting `m` to a AbstractTreapMap of this map's type, if necessary. Note that we can't use entrySequence,
        // because HashTreapMap's entrySequence is only partially ordered.
        val thisIt = selfNotEmpty.asSequence().iterator()
        val thatIt = m.toTreapMap().selfNotEmpty.asSequence().iterator()

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
    protected abstract fun shallowZip(that: S): Sequence<Map.Entry<K, Pair<V?, V?>>>
}

/**
    Removes a map entry (`entryKey`, `entryValue`) with key `key`
 */
internal fun <@Treapable K, V, S : AbstractTreapMap<K, V, S>> S?.removeEntry(key: TreapKey<K>, entryKey: K, entryValue: V): S? = when {
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

internal fun <@Treapable K, V, U, S : AbstractTreapMap<K, V, S>> S?.updateEntry(
    thatKey: TreapKey<K>, 
    entryKey: K, 
    toUpdate: U?, 
    merger: (V?, U?) -> V?, 
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
internal fun <@Treapable K, V, S : AbstractTreapMap<K, V, S>> S?.mergeWith(that: S?, shallowMerge: (S?, S?) -> S?): S? =
    notForking(this to that) {
        mergeWithImpl(that, shallowMerge)
    }

internal fun <@Treapable K, V, S : AbstractTreapMap<K, V, S>> S?.parallelMergeWith(that: S?, parallelThresholdLog2: Int, shallowMerge: (S?, S?) -> S?): S? =
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
private fun <@Treapable K, V, S : AbstractTreapMap<K, V, S>> S?.mergeWithImpl(that: S?, shallowMerge: (S?, S?) -> S?): S? {
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

