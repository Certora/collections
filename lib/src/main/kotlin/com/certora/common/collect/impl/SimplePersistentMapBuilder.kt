package com.certora.common.collect.impl

import kotlinx.collections.immutable.PersistentMap

// A "builder" for an arbitrary PersistentMap.  Assumes that all PersistentMap operations
// maintain object identity for updates which have no effect.  E.g., a.put(k, v) === a, iff a[k] == v

internal class SimplePersistentMapBuilder<K, V>(
    private var map: PersistentMap<K, V>
) : AbstractMutableMap<K, V>(), PersistentMap.Builder<K, V>, java.io.Serializable {

    override val size get() = map.size
    override fun isEmpty() = map.isEmpty()

    override fun hashCode() = map.hashCode()
    override fun equals(other: Any?) = map.equals(other)

    override fun containsKey(key: K): Boolean = map.containsKey(key)
    override fun containsValue(value: V): Boolean = map.containsValue(value)
    override fun get(key: K): V? = map.get(key)
    override fun getOrDefault(key: K, defaultValue: @UnsafeVariance V): V = map.getOrDefault(key, defaultValue)

    override fun remove(key: K): V? {
        val oldMap = map
        map = map.remove(key)
        return oldMap.get(key)
    }

    override fun remove(key: K, value: V): Boolean {
        val oldMap = map
        val newMap = map.remove(key, value)
        if (newMap !== oldMap) {
            map = newMap
            return true
        } else {
            return false
        }
    }

    override fun put(key: K, value: V): V? {
        val oldMap = map
        map = map.put(key, value)
        return oldMap.get(key)
    }

    override fun build(): PersistentMap<K, V> = map

    private inner class EntryIterator : MutableIterator<MutableMap.MutableEntry<K, V>> {
        val mapIterator = map.entries.iterator()
        var currentKey: K? = null
        var haveCurrent = false

        override fun hasNext() = mapIterator.hasNext()

        override fun next(): MutableMap.MutableEntry<K, V> {
            val nextKey = mapIterator.next().key
            currentKey = nextKey
            haveCurrent = true
            return MutableMapEntry(this@SimplePersistentMapBuilder, nextKey)
        }

        override fun remove() {
            if (!haveCurrent) {
                throw IllegalStateException("Iterator is not positioned on an entry")
            }
            remove(currentKey)
            haveCurrent = false
        }
    }

    private inner class EntrySet : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size get() = this@SimplePersistentMapBuilder.size
        override fun clear() = this@SimplePersistentMapBuilder.clear()
        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = throw UnsupportedOperationException()
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = EntryIterator()
    }

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>> get() = EntrySet()
}
