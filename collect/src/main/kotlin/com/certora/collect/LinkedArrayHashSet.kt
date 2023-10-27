package com.certora.collect

import kotlinx.serialization.Serializable

/** Document this. */
@Suppress("UNCHECKED_CAST")
@Serializable(with = LinkedArrayHashSetSerializer::class)
public class LinkedArrayHashSet<E>(
        initialCapacity: Int = ArrayHashTable.defaultCapacity,
        private var loadFactor: Float = ArrayHashTable.defaultLoadFactor
) : AbstractMutableSet<E>(), java.io.Serializable {

    private var hashTableContainer = HashTableContainer()

    private inner class HashTableContainer : ArrayHashTableContainer {
        override val isOrdered: Boolean get() = true
        override val hasValues: Boolean get() = false
        override val loadFactor: Float get() = this@LinkedArrayHashSet.loadFactor
        override var hashTable: ArrayHashTable
            get() = this@LinkedArrayHashSet.hashTable
            set(value: ArrayHashTable) { this@LinkedArrayHashSet.hashTable = value }
    }
    @PublishedApi internal var hashTable: ArrayHashTable = createArrayHashTable(hashTableContainer, initialCapacity)

    public constructor(other: Collection<E>) : this(other.size) {
        hashTable._initFromOrdered(other)
    }

    public companion object {
        private const val serialVersionUID = setSerialVersionUID
    }

    @Throws(java.io.IOException::class)
    private fun writeObject(out: java.io.ObjectOutputStream) {
        out.writeSet(this, loadFactor)
    }

    @Throws(java.io.IOException::class, ClassNotFoundException::class)
    private fun readObject(inn: java.io.ObjectInputStream) {
        inn.readSet { c, l ->
            loadFactor = l
            hashTableContainer = HashTableContainer()
            hashTable = createArrayHashTable(hashTableContainer, c)
            this
        }
    }

    public override val size: Int get() = hashTable.count
    public override fun isEmpty(): Boolean = hashTable.count == 0
    public override fun clear() { hashTable.reset() }
    public override fun iterator(): MutableIterator<E> = hashTable._orderedKeyIterator() as MutableIterator<E>
    public override fun add(element: E): Boolean = hashTable._addKeyOrdered(element)
    public override fun contains(element: E): Boolean = hashTable._containsKey(element)
    public override fun remove(element: E): Boolean = hashTable._removeKeyOrdered(element)

    public override fun addAll(elements: Collection<E>): Boolean = hashTable._addAllKeys(elements)
    public override fun removeAll(elements: Collection<E>): Boolean = hashTable._removeAllKeys(elements)
    public override fun retainAll(elements: Collection<E>): Boolean = hashTable._removeAllKeysExcept(elements)

    public inline fun forEach(action: (element: E) -> Unit) {
        hashTable._forEachKeyOrdered {
            action(it as E)
        }
    }
}
