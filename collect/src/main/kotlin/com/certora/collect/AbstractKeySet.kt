package com.certora.collect

/**
    Presents the keys of a [TreapMap] as a [TreapSet].

    The idea here is that a `TreapMap<K, *>` is stored with the same Treap structure as a `TreapSet<K>`, so we can very
    quickly create the corresponding `TreapSet<K>` when needed, in O(1) time.

    We lazily initialize the set, so that we don't create it until we need it.  For many operations, we can avoid
    creating the set entirely, and just use the map directly.  However, many operations, e.g. [addAll]/[union] and
    [retainAll/intersect], are much more efficient when we have a [TreapSet], so we create it when needed.

    Note: It would be really nice if we could just treat the [TreapMap] objects themselves as [TreapSet] objects, but
    this presents some problems.  The most fundamental problem is that the [TreapMap] and [TreapSet] interfaces have
    methods that are not compatible; for example, they both implement [Iterable], but with different element types.
 */
internal abstract class AbstractKeySet<@Treapable K, S : TreapSet<K>> : TreapSet<K> {
    /**
        The map whose keys we are presenting as a set.  We prefer to use the map directly when possible, so we don't
        need to create the set.
     */
    abstract val map: AbstractTreapMap<K, *, *>
    /**
        The set of keys.  This is a lazy property so that we don't create the set until we need it.
     */
    abstract val keys: Lazy<S>

    @Suppress("Treapability")
    override fun hashCode() = keys.value.hashCode()
    override fun equals(other: Any?) = keys.value.equals(other)
    override fun toString() = keys.value.toString()

    override val size get() = map.size
    override fun isEmpty() = map.isEmpty()
    override fun clear() = treapSetOf<K>()

    override operator fun contains(element: K) = map.containsKey(element)
    override operator fun iterator() = map.entrySequence().map { it.key }.iterator()

    override fun add(element: K) = keys.value.add(element)
    override fun addAll(elements: Collection<K>) = keys.value.addAll(elements)
    override fun remove(element: K) = keys.value.remove(element)
    override fun removeAll(elements: Collection<K>) = keys.value.removeAll(elements)
    override fun removeAll(predicate: (K) -> Boolean) = keys.value.removeAll(predicate)
    override fun retainAll(elements: Collection<K>) = keys.value.retainAll(elements)

    override fun single() = map.single().key
    override fun singleOrNull() = map.singleOrNull()?.key
    override fun arbitraryOrNull() = map.arbitraryOrNull()?.key

    override fun containsAny(elements: Iterable<K>) = keys.value.containsAny(elements)
    override fun containsAny(predicate: (K) -> Boolean) = (this as Iterable<K>).any(predicate)
    override fun containsAll(elements: Collection<K>) = keys.value.containsAll(elements)
    override fun findEqual(element: K) = keys.value.findEqual(element)

    override fun forEachElement(action: (K) -> Unit) = map.forEachEntry { action(it.key) }

    override fun <R : Any> mapReduce(map: (K) -> R, reduce: (R, R) -> R) =
        this.map.mapReduce({ k, _ -> map(k) }, reduce)
    override fun <R : Any> parallelMapReduce(map: (K) -> R, reduce: (R, R) -> R, parallelThresholdLog2: Int) =
        this.map.parallelMapReduce({ k, _ -> map(k) }, reduce, parallelThresholdLog2)
}
