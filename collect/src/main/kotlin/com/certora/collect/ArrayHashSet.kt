package com.certora.collect

import kotlinx.serialization.Serializable

/** Document this. */
@Suppress("UNCHECKED_CAST")
@Serializable(with = ArrayHashSetSerializer::class)
public class ArrayHashSet<E>(
        initialCapacity: Int = ArrayHashTable.defaultCapacity,
        private var loadFactor: Float = ArrayHashTable.defaultLoadFactor
) : AbstractMutableSet<E>(), java.io.Serializable {

    private var hashTableContainer = HashTableContainer()

    private inner class HashTableContainer : ArrayHashTableContainer {
        override val isOrdered: Boolean get() = false
        override val hasValues: Boolean get() = false
        override val loadFactor: Float get() = this@ArrayHashSet.loadFactor
        override var hashTable: ArrayHashTable
            get() = this@ArrayHashSet.hashTable
            set(value: ArrayHashTable) { this@ArrayHashSet.hashTable = value }
    }
    @PublishedApi internal var hashTable: ArrayHashTable = createArrayHashTable(hashTableContainer, initialCapacity)

    public constructor(other: Collection<E>) : this(other.size) {
        hashTable._initFrom(other)
    }

    public companion object {
        private const val serialVersionUID = setSerialVersionUID
    }

    private fun writeObject(out: java.io.ObjectOutputStream) {
        out.writeSet(this, loadFactor)
    }

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
    public override fun iterator(): MutableIterator<E> = hashTable._keyIterator() as MutableIterator<E>
    public override fun add(element: E): Boolean = hashTable._addKey(element)
    public override fun contains(element: E): Boolean = hashTable._containsKey(element)
    public override fun remove(element: E): Boolean = hashTable._removeKey(element)

    public override fun addAll(elements: Collection<E>): Boolean = hashTable._addAllKeys(elements)
    public override fun removeAll(elements: Collection<E>): Boolean = hashTable._removeAllKeys(elements)
    public override fun retainAll(elements: Collection<E>): Boolean = hashTable._removeAllKeysExcept(elements)

    public inline fun forEach(action: (element: E) -> Unit) {
        hashTable._forEachKey {
            action(it as E)
        }
    }
}
