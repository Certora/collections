package com.certora.collect

import kotlinx.collections.immutable.PersistentMap

/**
    A TreapMap for map keys that do not have a total ordering defined by implementing Comparable.  For those, we use the
    map keys' hash codes as Treap keys, and deal with collisions by chaining multiple map entries from a single Treap
    node.

    The HashTreapMap instance itself stores the first entry, and additional entries are chained via KeyValuePairList.
    This is just a simple linked list, so operations on it are either O(N) or O(N^2), but collisions are assumed to be
    rare enough that these lists will be very small - usually just one element.
 */
internal class HashTreapMap<@Treapable K, V>(
    override val key: K,
    override val value: V,
    override val next: KeyValuePairList.More<K, V>? = null,
    left: HashTreapMap<K, V>? = null,
    right: HashTreapMap<K, V>? = null
) : AbstractTreapMap<K, V, HashTreapMap<K, V>>(left, right), TreapKey.Hashed<K>, KeyValuePairList<K, V> {

    override fun hashCode() = computeHashCode()

    override fun K.toTreapKey() = TreapKey.Hashed.FromKey(this)
    override fun new(key: K, value: V): HashTreapMap<K, V> = HashTreapMap(key, value)

    override fun put(key: K, value: V): TreapMap<K, V> = self.add(new(key, value))

    @Suppress("UNCHECKED_CAST")
    override fun Map<out K, V>.toTreapMapOrNull() =
        this as? HashTreapMap<K, V>
        ?: (this as? PersistentMap.Builder<K, V>)?.build() as? HashTreapMap<K, V>

    override fun getShallowMerger(merger: (K, V?, V?) -> V?): (HashTreapMap<K, V>?, HashTreapMap<K, V>?) -> HashTreapMap<K, V>? = { t1, t2 ->
        var newPairs: KeyValuePairList.More<K, V>? = null
        t1?.forEachPair { (k, v1) ->
            val v2 = t2?.shallowGetValue(k)
            val v = merger(k, v1, v2)
            if (v != null) {
                newPairs = KeyValuePairList.More(k, v, newPairs)
            }
        }
        t2?.forEachPair { (k, v2) ->
            val v1 = t1?.shallowGetValue(k)
            if (v1 == null) {
                val v = merger(k, v1, v2)
                if (v != null) {
                    newPairs = KeyValuePairList.More(k, v, newPairs)
                }
            }
        }
        val firstPair = newPairs
        when {
            firstPair == null -> null
            t1 != null -> {
                val newNode = HashTreapMap(firstPair.key, firstPair.value, firstPair.next, t1.left, t1.right)
                if (newNode.shallowEquals(t1)) { t1 } else { newNode }
            }
            t2 != null -> {
                val newNode = HashTreapMap(firstPair.key, firstPair.value, firstPair.next, t2.left, t2.right)
                if (newNode.shallowEquals(t2)) { t2 } else { newNode }
            }
            else -> throw IllegalArgumentException("shallow merge with no treaps")
        }
    }

    private inline fun KeyValuePairList<K, V>?.forEachPair(action: (KeyValuePairList<K, V>) -> Unit) {
        var current = this
        while (current != null) {
            action(current)
            current = current.next
        }
    }

    private fun KeyValuePairList<K, V>?.shallowContainsPair(key: K, value: V): Boolean {
        forEachPair {
            if (it.key == key && it.value == value) {
                return true
            }
        }
        return false
    }

    private fun KeyValuePairList<K, V>?.shallowContainsKey(key: K) : Boolean {
        forEachPair {
            if (it.key == key) {
                return true
            }
        }
        return false
    }

    protected override fun getTreapSequencesIfSameType(
        that: Map<out K, V>
    ): Pair<Sequence<HashTreapMap<K, V>>, Sequence<HashTreapMap<K, V>>>? {
        @Suppress("UNCHECKED_CAST")
        return (that as? HashTreapMap<K, V>)?.let {
            this.asTreapSequence() to it.asTreapSequence()
        }
    }

    override fun shallowZip(that: HashTreapMap<K, V>): Sequence<Map.Entry<K, Pair<V?, V?>>> = sequence {
        forEachPair {
            yield(MapEntry(it.key, it.value to that.shallowGetValue(it.key)))
        }
        that.forEachPair {
            if (!containsKey(it.key)) {
                yield(MapEntry(it.key, null to it.value))
            }
        }
    }

    override val self get() = this
    override val treapKey get() = key

    override fun copyWith(left: HashTreapMap<K, V>?, right: HashTreapMap<K, V>?): HashTreapMap<K, V> = HashTreapMap(key, value, next, left, right)

    override fun shallowEntrySequence(): Sequence<Map.Entry<K, V>> = sequence {
        forEachPair { (k, v) ->
            yield (MapEntry(k, v))
        }
    }

    override fun shallowContainsKey(key: K): Boolean = (this as KeyValuePairList<K, V>).shallowContainsKey(key)

    override fun shallowGetValue(key: K): V? {
        forEachPair {
            if (it.key == key) {
                return it.value
            }
        }
        return null
    }

    override fun shallowAdd(that: HashTreapMap<K, V>): HashTreapMap<K, V> {
        check(that.next == null) { "Add with mulple map entries?" }
        return when {
            !shallowContainsKey(that.key) -> {
                HashTreapMap(this.key, this.value, KeyValuePairList.More(that.key, that.value, this.next), this.left, this.right)
            }
            this.shallowGetValue(that.key) == that.value -> {
                this
            }
            else -> {
                var newPairs: KeyValuePairList.More<K, V>? = null
                this.forEachPair {
                    if (it.key == that.key) {
                        newPairs = KeyValuePairList.More(it.key, that.value, newPairs)
                    } else {
                        newPairs = KeyValuePairList.More(it.key, it.value, newPairs)
                    }
                }
                val firstPair = newPairs!!
                HashTreapMap(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
            }
        }
    }

    override fun shallowRemoveEntry(key: K, value: V): HashTreapMap<K, V>? {
        return when {
            !this.shallowContainsPair(key, value) -> this
            else -> {
                var newPairs: KeyValuePairList.More<K, V>? = null
                this.forEachPair {
                    if (it.key != key || it.value != value) {
                        newPairs = KeyValuePairList.More(it.key, it.value, newPairs)
                    }
                }
                val firstPair = newPairs
                if (firstPair == null) {
                    null
                } else {
                    HashTreapMap(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
                }
            }
        }
    }

    override fun <U> shallowUpdate(entryKey: K, toUpdate: U, merger: (V?, U) -> V?): HashTreapMap<K, V>? {
        return when (this.key) {
            entryKey -> {
                val newValue = merger(this.value, toUpdate)
                if(newValue == null) {
                    if(this.next == null) {
                        return null
                    } else {
                        HashTreapMap(this.next.key, this.next.value, this.next.next, this.left, this.right)
                    }
                } else if(newValue == value) {
                    this
                } else {
                    HashTreapMap(this.key, newValue, this.next, this.left, this.right)
                }
            }
            else -> {
                // look for entryKey in the buckets off of this one
                var newPairs: KeyValuePairList.More<K, V>? = null
                var found = false
                var it = this.next
                while(it != null) {
                    if(it.key == entryKey) {
                        val upd = merger(it.value, toUpdate)
                        found = true
                        if(upd != null && upd == it.value) {
                            return this
                        } else if(upd != null) {
                            newPairs = KeyValuePairList.More(it.key, upd, newPairs)
                        }
                    } else {
                        newPairs = KeyValuePairList.More(it.key, it.value, newPairs)
                    }
                    it = it.next
                }
                if(!found) {
                    val deNovoMerge = merger(null, toUpdate) ?: return this
                    newPairs = KeyValuePairList.More(entryKey, deNovoMerge, this.next)
                }
                HashTreapMap(this.key, this.value, newPairs, this.left, this.right)
            }
        }
    }

    override fun shallowRemove(element: K): HashTreapMap<K, V>? {
        if (!this.shallowContainsKey(element)) {
            return this
        } else {
            var newPairs: KeyValuePairList.More<K, V>? = null
            this.forEachPair {
                if (it.key != element) {
                    newPairs = KeyValuePairList.More(it.key, it.value, newPairs)
                }
            }
            val firstPair = newPairs
            return if (firstPair == null) {
                null
            } else {
                HashTreapMap(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
            }
        }
    }

    override val shallowSize: Int get() {
        var count = 0
        forEachPair {
            ++count
        }
        return count
    }

    override fun shallowEquals(that: HashTreapMap<K, V>): Boolean {
        forEachPair {
            if (!that.shallowContainsPair(it.key, it.value)) {
                return false
            }
        }
        return this.shallowSize == that.shallowSize
    }

    override fun shallowUpdateValues(transform: (K, V) -> V?): HashTreapMap<K, V>? {
        return when {
            next == null -> {
                val newValue = transform(key, value)
                when {
                    newValue == null -> null
                    newValue === value -> this
                    else -> HashTreapMap(key, newValue, null, left, right)
                }
            }
            else -> {
                var newPairs: KeyValuePairList.More<K, V>? = null
                this.forEachPair { (k, v) ->
                    val newValue = transform(k, v)
                    if (newValue != null) {
                        newPairs = KeyValuePairList.More(k, newValue, newPairs)
                    }
                }
                val firstPair = newPairs
                when {
                    firstPair == null -> null
                    else -> {
                        val newEntry = HashTreapMap(firstPair.key, firstPair.value, firstPair.next, this.left, this.right)
                        if (newEntry.shallowEquals(this)) { this } else { newEntry }
                    }
                }
            }
        }
    }

    override fun shallowComputeHashCode(): Int {
        var h = 0
        forEachPair { (k, v) -> h += AbstractMapEntry.hashCode(k, v) }
        return h
    }

    override fun <R : Any> shallowMapReduce(map: (K, V) -> R, reduce: (R, R) -> R): R {
        var result: R? = null
        forEachPair {
            val mapped = map(it.key, it.value)
            result = result?.let { result -> reduce(result, mapped) } ?: mapped
        }
        return result!!
    }
}

internal interface KeyValuePairList<K, V> {
    abstract val key: K
    abstract val value: V
    abstract val next: More<K, V>?
    operator fun component1() = key
    operator fun component2() = value

    class More<K, V>(
        override val key: K,
        override val value: V,
        override val next: More<K, V>?
    ) : KeyValuePairList<K, V>, java.io.Serializable
}


