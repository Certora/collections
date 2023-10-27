package com.certora.collect

import kotlinx.serialization.Serializable

/** Document this. */
@Suppress("UNCHECKED_CAST")
@Serializable(with = LinkedArrayHashMapSerializer::class)
public class LinkedArrayHashMap<K, V>(
        initialCapacity: Int = ArrayHashTable.defaultCapacity,
        private var loadFactor: Float = ArrayHashTable.defaultLoadFactor
) : AbstractMutableMap<K, V>(), java.io.Serializable {

    private var hashTableContainer = HashTableContainer()

    private inner class HashTableContainer : ArrayHashTableContainer {
        override val isOrdered: Boolean get() = true
        override val hasValues: Boolean get() = true
        override val loadFactor: Float get() = this@LinkedArrayHashMap.loadFactor
        override var hashTable: ArrayHashTable
            get() = this@LinkedArrayHashMap.hashTable
            set(value: ArrayHashTable) { this@LinkedArrayHashMap.hashTable = value }
    }
    @PublishedApi internal var hashTable: ArrayHashTable = createArrayHashTable(hashTableContainer, initialCapacity)

    public constructor(other: Map<out K, V>) : this(other.size) {
        hashTable._initFromOrdered(other)
    }

    public companion object {
        private const val serialVersionUID = mapSerialVersionUID
    }

    @Throws(java.io.IOException::class)
    private fun writeObject(out: java.io.ObjectOutputStream) {
        out.writeMap(this, loadFactor)
    }

    @Throws(java.io.IOException::class, ClassNotFoundException::class)
    private fun readObject(inn: java.io.ObjectInputStream) {
        inn.readMap { c, l ->
            loadFactor = l
            hashTableContainer = HashTableContainer()
            hashTable = createArrayHashTable(hashTableContainer, c)
            this
        }
    }

    public override val size: Int get() = hashTable.count
    public override fun isEmpty(): Boolean = hashTable.count == 0
    public override fun clear() { hashTable.reset() }
    public override fun put(key: K, value: V): V? = hashTable._addValueOrdered(key, value) as V?
    public override fun containsKey(key: K): Boolean = hashTable._containsKey(key)
    public override fun containsValue(value: V): Boolean = hashTable._containsValue(value)
    public override fun get(key: K): V? = hashTable._getValueOrDefault(key, null) as V?
    public override fun getOrDefault(key: K, defaultValue: @UnsafeVariance V): V =
            hashTable._getValueOrDefault(key, defaultValue) as V
    public override fun remove(key: K): V? = hashTable._removeValueOrdered(key) as V?
    public override fun remove(key: K, value: V): Boolean = hashTable._removeValueOrdered(key, value)

    private inner class EntryIterator : MutableIterator<MutableMap.MutableEntry<K, V>> {
        val keyIterator = hashTable._orderedKeyIterator()
        override fun hasNext() = keyIterator.hasNext()
        override fun next() = MutableMapEntry(this@LinkedArrayHashMap, keyIterator.next() as K)
        override fun remove() = keyIterator.remove()
    }

    private inner class EntrySet : AbstractMutableSet<MutableMap.MutableEntry<K, V>>() {
        override val size
            get() = hashTable.count
        override fun clear() = hashTable.reset()
        override fun add(element: MutableMap.MutableEntry<K, V>): Boolean = throw UnsupportedOperationException()
        override fun iterator(): MutableIterator<MutableMap.MutableEntry<K, V>> = EntryIterator()
    }

    public override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = EntrySet()

    public inline fun forEachEntry(action: (key: K, value: V) -> Unit): Unit =
        hashTable._forEachValueOrdered { k, v ->
            action(k as K, v as V)
        }

    public fun putIfAbsent(key: K, value: V): V? = hashTable._addValueOrderedIfAbsent(key, value) as V?
}