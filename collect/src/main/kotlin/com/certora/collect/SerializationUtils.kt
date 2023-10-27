package com.certora.collect

internal const val setSerialVersionUID: Long = 1
internal const val mapSerialVersionUID: Long = 1

internal fun <E> java.io.ObjectOutputStream.writeSet(set: Set<E>, loadFactor: Float) {
    writeInt(set.size)
    writeFloat(loadFactor)
    for (e in set) {
        writeObject(e)
    }
}

internal fun <E> java.io.ObjectInputStream.readSet(builder: (Int, Float) -> MutableSet<E>) {
    val size = readInt()
    val loadFactor = readFloat()
    val set = builder(size, loadFactor)
    repeat(size) {
        @Suppress("UNCHECKED_CAST")
        set.add(readObject() as E)
    }
}

internal fun <K, V> java.io.ObjectOutputStream.writeMap(map: Map<K, V>, loadFactor: Float) {
    writeInt(map.size)
    writeFloat(loadFactor)
    for (e in map.entries) {
        writeObject(e.key)
        writeObject(e.value)
    }
}

internal fun <K, V> java.io.ObjectInputStream.readMap(builder: (capacity: Int, loadFactor: Float) -> MutableMap<K, V>) {
    val size = readInt()
    val loadFactor = readFloat()
    val map = builder(size, loadFactor)
    repeat(size) {
        @Suppress("UNCHECKED_CAST")
        val key = readObject() as K;
        @Suppress("UNCHECKED_CAST")
        val value = readObject() as V;

        map.put(key, value)
    }
}
