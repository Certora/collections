package com.certora.collect

internal class ArrayHashTable_Capacity8Bits(
        container: ArrayHashTableContainer,
        capacity: Int
) : ArrayHashTable(container, capacity) {

    private val bucketStarts = indexArray(bucketCount)
    private val bucketNexts = indexArray(capacity + 1) { it + 1 }
    private val orderNexts = indexArrayOrEmpty(container.isOrdered, capacity + 1)
    private val orderPrevs = indexArrayOrEmpty(container.isOrdered, capacity + 1)

    override fun bucketStart(i: Int): Int = bucketStarts[i].toInt()
    override fun setBucketStart(i: Int, value: Int) {
        bucketStarts[i] = value.toIndex()
    }
    override fun bucketNext(i: Int): Int = bucketNexts[i].toInt()
    override fun setBucketNext(i: Int, value: Int) {
        bucketNexts[i] = value.toIndex()
    }
    override fun orderNext(i: Int): Int = orderNexts[i].toInt()
    override fun setOrderNext(i: Int, value: Int) {
        orderNexts[i] = value.toIndex()
    }
    override fun orderPrev(i: Int): Int = orderPrevs[i].toInt()
    override fun setOrderPrev(i: Int, value: Int) {
        orderPrevs[i] = value.toIndex()
    }

    override fun _containsValue(value: Any?): Boolean = containsValue(value)
    override fun _containsKey(key: Any?): Boolean = containsKey(key)
    override fun _addKey(key: Any?): Boolean = addKey(key)
    override fun _addKeyOrdered(key: Any?): Boolean = addKeyOrdered(key)
    override fun _addValue(key: Any?, value: Any?): Any? = addValue(key, value)
    override fun _addValueOrdered(key: Any?, value: Any?): Any? = addValueOrdered(key, value)
    override fun _addValueIfAbsent(key: Any?, value: Any?): Any? = addValueIfAbsent(key, value)
    override fun _addValueOrderedIfAbsent(key: Any?, value: Any?): Any? = addValueOrderedIfAbsent(key, value)
    override fun _getValueOrDefault(key: Any?, default: Any?): Any? = getValueOrDefault(key, default)
    override fun _removeKey(key: Any?): Boolean = removeKey(key)
    override fun _removeKeyOrdered(key: Any?): Boolean = removeKeyOrdered(key)
    override fun _removeValue(key: Any?): Any? = removeValue(key)
    override fun _removeValueOrdered(key: Any?): Any? = removeValueOrdered(key)
    override fun _removeValue(key: Any?, value: Any?): Boolean = removeValue(key, value)
    override fun _removeValueOrdered(key: Any?, value: Any?): Boolean = removeValueOrdered(key, value)
    override fun _removeAllKeysExcept(exceptions: Collection<Any?>): Boolean = removeAllKeysExcept(exceptions)
    override fun _initFrom(map: Map<*, *>) = initFrom(map)
    override fun _initFromOrdered(map: Map<*, *>) = initFromOrdered(map)
    override fun _initFrom(keys: Collection<*>) = initFrom(keys)
    override fun _initFromOrdered(keys: Collection<*>) = initFromOrdered(keys)
    override fun _keyIterator(): MutableIterator<Any?> = keyIterator()
    override fun _orderedKeyIterator(): MutableIterator<Any?> = orderedKeyIterator()
    override fun _addAllKeys(elements: Collection<Any?>) = addAllKeys(elements)
    override fun _removeAllKeys(elements: Collection<Any?>) = removeAllKeys(elements)

    override fun reset() {
        super.reset()

        for (i in bucketStarts.indices) {
            bucketStarts[i] = 0.toIndex()
        }
        for (i in bucketNexts.indices) {
            bucketNexts[i] = (i + 1).toIndex()
        }
        for (i in orderNexts.indices) {
            orderNexts[i] = 0.toIndex()
        }
        for (i in orderPrevs.indices) {
            orderPrevs[i] = 0.toIndex()
        }
    }

    private fun indexArrayOrEmpty(needArray: Boolean, count: Int) =
            if (needArray) {
                indexArray(count)
            } else {
                emptyByteArray
            }

    private fun indexArray(count: Int) = ByteArray(count)

    private inline fun indexArray(count: Int, init: (Int) -> Int) =
            ByteArray(count) { init(it).toIndex() }

    private fun Int.toIndex() = toByte()
}
