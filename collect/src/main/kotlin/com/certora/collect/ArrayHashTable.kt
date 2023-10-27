@file:Suppress("NOTHING_TO_INLINE")

package com.certora.collect

import java.util.BitSet
import kotlin.math.ceil
import kotlin.math.max

/**
 * ArrayHashTable:
 *
 * This is the underlying implementation of ArrayHashMap, ArrayHashSet, LinkedArrayHashMap, and
 * LinkedArrayHashSet. ArrayHashTable's main advantage over the built-in HashSet, HashMap, etc., is
 * that it does not allocate an individual "node" object for each key. Instead, all data is stored
 * in a few arrays. All keys are stored in one array, values in another array, and "pointers" to
 * keys and values are stored in arrays of integer indices.
 *
 * This array-based organization has a few advantages:
 *
 * 1) We don't have the time overhead of allocating and initializing separate "node" objects
 *    individually.  We just have to allocate a few arrays to hold many, many objects.
 * 2) We don't have the space overhead of individual node objects. Every heap-allocated object
 *    carries quite a bit of overhead (such as the "object header" and class pointer).  Arrays
 *    hold references more compactly.
 * 3) Arrays are contiguous in memory, resulting in better spatial locality for the stored
 *    elements.  For algorithms that operate over many elements of a set or map, this can increase
 *    performance due to better CPU cache usage.
 * 4) The GC scans array elements much faster than it can walk a tree of individually allocated
 *    node objects.
 * 5) Indexes can sometimes be stored more compactly than object references, as we can choose to
 *    store them in 8- or 16-bit integers when possible.
 *
 * All of this results in less time spent in the GC for set and map operations, along with lower
 * steady-state memory usage, and often much faster set operations.
 */
@PublishedApi internal abstract class ArrayHashTable(val container: ArrayHashTableContainer, val capacity: Int) {
    //
    // Hashing: we use a 32-bit fibonacci hash.  See
    // https://en.wikipedia.org/wiki/Hash_function#Fibonacci_hashing.
    // This gives a nice distribution over n buckets, where n is a power of two.  We don't need to
    // worry about finding
    // prime numbers, expensive division operations, etc.
    //
    internal fun hashToBucket(hash: Int): Int =
            ((hash.toUInt() * 2654435769u) shr (32 - bucketCountLog2)).toInt()

    private fun calcBucketCountLog2(): Int {
        //
        // Find a bucket count that is a power of two >= the min bucket count
        // needed to meet the container's desired load factor
        val minBucketCount = max(2, kotlin.math.ceil(capacity / container.loadFactor).toInt())
        return (32 - (minBucketCount - 1).countLeadingZeroBits())
    }
    private val bucketCountLog2 = calcBucketCountLog2()
    internal val bucketCount get() = 1 shl bucketCountLog2

    var count = 0 // The number of keys in the table

    var mutationCount = 0 // Incremented for each modification to the table.  Used for consistency checks.

    //
    // Keys and values are stored in the following arrays.  Indexes into these are 1-based, allowing
    // us to reserve slot 0 for our free list.  values[i] is the value associated with keys[i], but
    // only if the container actually holds values (it's a map and not a set).
    //
    @PublishedApi internal val keys: Array<Any?> = arrayOfNulls<Any?>(capacity + 1)
    @PublishedApi internal val values: Array<Any?> = objectArrayOrEmpty(container.hasValues, capacity + 1)

    //
    // This is a "poor man's bloom filters" for each hash bucket.  (Don't google that; I just made
    // it up.)  This gives us a quick idea of whether a given key is already in the bucket.  Each
    // element is a bitwise or of the hash codes of every key that has been stored in the bucket.
    // If you have an object whose hash code contains bits not set in blooms[bucket], then that
    // object is definitely not in the bucket yet. This lets us avoid walking the bucket for many
    // lookups.
    //
    internal val blooms = IntArray(bucketCount)

    //
    // The following are abstract functions for maniuplating the various arrays of key/value
    // indices. These are logically arrays of Ints, but we store them more compactly when possible.
    // We have separate implementations of these for 8-bit, 16-bit, and 32-bit indices.  We go to
    // some length, using inline functions, to eliminate the virtual call overhead when accessing
    // these.
    //
    // bucketStart[i] is the index of the first key in bucket i, or zero if the bucket is empty.
    //
    @PublishedApi internal abstract fun bucketStart(i: Int): Int
    @PublishedApi internal abstract fun setBucketStart(i: Int, value: Int)
    //
    // bucketNext[i] is the index of the next key in the same bucket as the i'th key, or zero if
    // there are no more keys in the bucket.
    // bucketNext[0] is the index of the first "free" (unused) key index (and
    // bucketNext[bucketNext[0]] is the next free index, etc.)
    //
    @PublishedApi internal abstract fun bucketNext(i: Int): Int
    @PublishedApi internal abstract fun setBucketNext(i: Int, value: Int)
    //
    // orderNext[i] is the index of the next key, in insertion order, after the i'th key.
    // orderNext is only available for ordered collections (LinkedArrayHashMap, etc.).
    //
    @PublishedApi internal abstract fun orderNext(i: Int): Int
    @PublishedApi internal abstract fun setOrderNext(i: Int, value: Int)
    //
    // orderPrev[i] is the index of the previous key, in insertion order, before the i'th key.
    // orderPrev is only available for ordered collections (LinkedArrayHashMap, etc.).
    //
    @PublishedApi internal abstract fun orderPrev(i: Int): Int
    @PublishedApi internal abstract fun setOrderPrev(i: Int, value: Int)

    //
    // These are the primitives that collections use to implement sets and maps.  These have
    // concrete implementations for each index size.  Those are, in turn, implemented by
    // generic inline functions to avoid repeating the implementation logic for each index
    // size.
    //
    abstract fun _containsValue(value: Any?): Boolean
    abstract fun _containsKey(key: Any?): Boolean
    abstract fun _addKey(key: Any?): Boolean
    abstract fun _addKeyOrdered(key: Any?): Boolean
    abstract fun _addAllKeys(elements: Collection<Any?>): Boolean
    abstract fun _addValue(key: Any?, value: Any?): Any?
    abstract fun _addValueIfAbsent(key: Any?, value: Any?): Any?
    abstract fun _addValueOrderedIfAbsent(key: Any?, value: Any?): Any?
    abstract fun _addValueOrdered(key: Any?, value: Any?): Any?
    abstract fun _getValueOrDefault(key: Any?, default: Any?): Any?
    abstract fun _removeKey(key: Any?): Boolean
    abstract fun _removeKeyOrdered(key: Any?): Boolean
    abstract fun _removeValue(key: Any?): Any?
    abstract fun _removeValueOrdered(key: Any?): Any?
    abstract fun _removeValue(key: Any?, value: Any?): Boolean
    abstract fun _removeValueOrdered(key: Any?, value: Any?): Boolean
    abstract fun _removeAllKeys(elements: Collection<Any?>): Boolean
    abstract fun _removeAllKeysExcept(exceptions: Collection<Any?>): Boolean
    abstract fun _initFrom(map: Map<*, *>)
    abstract fun _initFromOrdered(map: Map<*, *>)
    abstract fun _initFrom(keys: Collection<*>)
    abstract fun _initFromOrdered(keys: Collection<*>)
    abstract fun _keyIterator(): MutableIterator<Any?>
    abstract fun _orderedKeyIterator(): MutableIterator<Any?>

    inline fun _forEachKey(action: (key: Any?) -> Unit) = forEachKey(action)
    inline fun _forEachValue(action: (key: Any?, value: Any?) -> Unit) = forEachValue(action)
    inline fun _forEachKeyOrdered(action: (key: Any?) -> Unit) = forEachKeyOrdered(action)
    inline fun _forEachValueOrdered(action: (key: Any?, value: Any?) -> Unit) = forEachValueOrdered(action)

    open fun reset() {
        for (i in keys.indices) {
            keys[i] = null
        }
        for (i in values.indices) {
            values[i] = null
        }
        for (i in blooms.indices) {
            blooms[i] = 0
        }
        count = 0
        ++mutationCount
    }

    companion object {
        internal @JvmStatic val emptyByteArray = ByteArray(0)
        internal @JvmStatic val emptyShortArray = ShortArray(0)
        internal @JvmStatic val emptyIntArray = IntArray(0)
        internal @JvmStatic val emptyObjectArray = emptyArray<Any?>()
        const val defaultCapacity = 8
        const val defaultLoadFactor = 2.0f
        internal const val geometricCapacityGrowthLimit = 1024 * 1024
        internal const val geometricCapacityGrowthFactor = 4
        internal const val linearCapacityGrowthIncrement = 1024 * 1024
    }

    private fun objectArrayOrEmpty(needArray: Boolean, count: Int) =
            if (needArray) {
                arrayOfNulls<Any?>(count)
            } else {
                emptyObjectArray
            }
}

/**
 * Creates a new hash table instance, with an implementation appropriate to the desired capacity.
 */
internal fun createArrayHashTable(container: ArrayHashTableContainer, initialCapacity: Int): ArrayHashTable {
    require(initialCapacity >= 0) { "initialCapacity must be positive. Was $initialCapacity." }
    val capacity = max(initialCapacity, 1)

    return when {
        capacity <= Byte.MAX_VALUE.toInt() -> ArrayHashTable_Capacity8Bits(container, capacity)
        capacity <= Short.MAX_VALUE.toInt() -> ArrayHashTable_Capacity16Bits(container, capacity)
        else -> ArrayHashTable_Capacity32Bits(container, capacity)
    }
}

//
// Below are the implementations of the hash table operations.  These are implemented as inline
// generic extension methods to allow the Kotlin compiler and the JVM JIT compiler maximum ability
// to eliminate as many virtual calls as possible.  In particular, we always know the actual
// implementation of the index arrays, so can inline all array accesses, rather than calling
// through virtual dispatch.
//

internal inline fun <T : ArrayHashTable> T.containsValue(value: Any?): Boolean {
    return values.contains(value)
}

internal inline fun <T : ArrayHashTable> T.containsKey(key: Any?): Boolean {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { _, _ ->
        return true
    }
    return false
}

internal inline fun <T : ArrayHashTable> T.findKey(
        key: Any?,
        hash: Int,
        bucket: Int,
        found: (i: Int, prev: Int) -> Unit
) {
    val bloom = blooms[bucket]
    if ((bloom or hash) == bloom) {
        var prev = 0
        var i = bucketStart(bucket)
        while (i != 0) {
            if (keys[i] == key) {
                return found(i, prev)
            }
            prev = i
            i = bucketNext(i)
        }
    }
}

// This needs to return two values; instead of allocating an object to hold them, we use an
// inline lambda.
internal inline fun <T : ArrayHashTable> T.addKey(
        key: Any?,
        returnResults: ArrayHashTable.(added: Boolean, i: Int) -> Nothing
): Nothing {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { i, _ -> this.returnResults(false, i) }

    if (count < capacity) {
        this.returnResults(true, addUniqueKey(key, hash, bucket))
    } else {
        grow(count + 1) {
            // Note that we need to recompute the bucket, due to the resize.
            this.returnResults(true, addUniqueKey(key, hash, hashToBucket(hash)))
        }
    }
}

internal inline fun <T : ArrayHashTable> T.addKey(key: Any?): Boolean {
    addKey(key) { added, _ ->
        return added
    }
}

internal inline fun <T : ArrayHashTable> T.addKeyOrdered(key: Any?): Boolean {
    addKey(key) { added, i ->
        if (added) {
            makeLast(i)
        }
        return added
    }
}

internal inline fun <T : ArrayHashTable> T.addValue(key: Any?, value: Any?): Any? {
    addKey(key) { _, i ->
        val old = values[i]
        values[i] = value
        return old
    }
}

internal inline fun <T : ArrayHashTable> T.addValueOrdered(key: Any?, value: Any?): Any? {
    addKey(key) { added, i ->
        val old = values[i]
        values[i] = value
        if (added) {
            makeLast(i)
        }
        return old
    }
}

internal inline fun <T : ArrayHashTable> T.addValueIfAbsent(key: Any?, value: Any?): Any? {
    addKey(key) { added, i ->
        if (added) {
            values[i] = value
            return null
        } else {
            return values[i]
        }
    }
}

internal inline fun <T : ArrayHashTable> T.addValueOrderedIfAbsent(key: Any?, value: Any?): Any? {
    addKey(key) { added, i ->
        if (added) {
            values[i] = value
            makeLast(i)
            return null
        } else {
            return values[i]
        }
    }
}

internal inline fun <T : ArrayHashTable> T.getValueOrDefault(key: Any?, default: Any?): Any? {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { i, _ ->
        return values[i]
    }
    return default
}

internal inline fun <T : ArrayHashTable> T.removeKey(key: Any?): Boolean {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { i, prev ->
        removeKeyAtIndex(bucket, i, prev)
        return true
    }
    return false
}

internal inline fun <T : ArrayHashTable> T.removeKeyOrdered(key: Any?): Boolean {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { i, prev ->
        removeKeyAtIndex(bucket, i, prev)
        removeFromOrder(i)
        return true
    }
    return false
}

internal inline fun <T : ArrayHashTable> T.removeValue(key: Any?): Any? {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { i, prev ->
        removeKeyAtIndex(bucket, i, prev)
        val old = values[i]
        values[i] = null
        return old
    }
    return null
}

internal inline fun <T : ArrayHashTable> T.removeValueOrdered(key: Any?): Any? {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { i, prev ->
        removeKeyAtIndex(bucket, i, prev)
        removeFromOrder(i)
        val old = values[i]
        values[i] = null
        return old
    }
    return null
}

internal inline fun <T : ArrayHashTable> T.removeValue(key: Any?, value: Any?): Boolean {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { i, prev ->
        val old = values[i]
        if (old == value) {
            removeKeyAtIndex(bucket, i, prev)
            values[i] = null
            return true
        }
    }
    return false
}

internal inline fun <T : ArrayHashTable> T.removeValueOrdered(key: Any?, value: Any?): Boolean {
    val hash = key.hashCode()
    val bucket = hashToBucket(hash)
    findKey(key, hash, bucket) { i, prev ->
        val old = values[i]
        if (old == value) {
            removeKeyAtIndex(bucket, i, prev)
            removeFromOrder(i)
            values[i] = null
            return true
        }
    }
    return false
}

internal inline fun <T : ArrayHashTable> T.removeKeyAtIndex(bucket: Int, i: Int, prev: Int) {

    ++mutationCount

    keys[i] = null

    // remove from this bucket list
    val next = bucketNext(i)
    if (prev == 0) {
        setBucketStart(bucket, next)
    } else {
        setBucketNext(prev, next)
    }

    // add to the free list
    setBucketNext(i, bucketNext(0))
    setBucketNext(0, i)

    --count
}

internal inline fun <T : ArrayHashTable> T.removeAllKeys(keys: Collection<Any?>) =
    when {
        keys === container -> { reset(); true }
        keys.isEmpty() -> false
        keys is Set<Any?> -> removeAllKeysInSet(keys)
        else -> removeAllKeysInCollection(keys)
    }

internal inline fun <T : ArrayHashTable> T.removeAllKeysInSet(keys: Set<Any?>): Boolean {
    var removed = false
    if (container.isOrdered) {
        keys.forEach {
            removed = removeKeyOrdered(it) || removed
        }
    } else {
        keys.forEach {
            removed = removeKey(it) || removed
        }
    }
    return removed
}

internal inline fun <T : ArrayHashTable> T.removeAllKeysInCollection(keys: Collection<Any?>): Boolean {
    var removed = false
    if (container.isOrdered) {
        keys.forEach {
            removed = removeKeyOrdered(it) || removed
        }
    } else {
        keys.forEach {
            removed = removeKey(it) || removed
        }
    }
    return removed
}


internal inline fun <T : ArrayHashTable> T.addAllKeys(keys: Collection<Any?>) =
    when {
        keys === container -> false
        keys.isEmpty() -> false
        keys is Set<Any?> -> addAllKeysInSet(keys)
        else -> addAllKeysInCollection(keys)
    }

internal inline fun <T : ArrayHashTable> T.addAllKeysInSet(keys: Set<Any?>): Boolean {
    var added = false
    if (container.isOrdered) {
        keys.forEach {
            added = container.hashTable._addKeyOrdered(it) || added
        }
    }
    else {
        keys.forEach {
            added = container.hashTable._addKey(it) || added
        }
    }
    return added
}

internal inline fun <T : ArrayHashTable> T.addAllKeysInCollection(keys: Collection<Any?>): Boolean {
    var added = false
    if (container.isOrdered) {
        keys.forEach {
            added = container.hashTable._addKeyOrdered(it) || added
        }
    }
    else {
        keys.forEach {
            added = container.hashTable._addKey(it) || added
        }
    }
    return added
}

internal inline fun <T : ArrayHashTable> T.removeAllKeysExcept(exceptions: Collection<Any?>) =
    when {
        count == 0 -> false
        exceptions === container -> false
        exceptions.isEmpty() -> { reset(); true }
        exceptions is Set<Any?> -> removeAllKeysExceptSet(exceptions)
        else -> removeAllKeysExceptCollection(exceptions)
    }


internal inline fun <T : ArrayHashTable> T.removeAllKeysExceptSet(exceptions: Set<Any?>): Boolean {
    return removeKeysIf { !exceptions.contains(keys[it]) }
}

internal inline fun <T : ArrayHashTable> T.removeAllKeysExceptCollection(
        exceptions: Collection<Any?>
): Boolean {
    val exceptionSet = BitSet(count)
    for (key in exceptions) {
        val hash = key.hashCode()
        val bucket = hashToBucket(hash)
        findKey(key, hash, bucket) { i, _ -> exceptionSet.set(i) }
    }
    return removeKeysIf { !exceptionSet.get(it) }
}

internal inline fun <T : ArrayHashTable> T.removeKeysIf(pred: (i: Int) -> Boolean): Boolean {
    ++mutationCount
    var removed = false
    var bucket = -1
    var current = 0
    var prev = 0
    val isOrdered = container.isOrdered
    for (i in 1..count) {
        while (current == 0) {
            ++bucket
            current = bucketStart(bucket)
            prev = 0
        }

        val next = bucketNext(current)

        if (pred(current)) {
            removeKeyAtIndex(bucket, current, prev)
            if (isOrdered) {
                removeFromOrder(current)
            }
            removed = true
            current = next
        } else {
            prev = current
            current = next
        }
    }
    return removed
}

internal inline fun <T : ArrayHashTable> T.addUniqueKey(key: Any?, hash: Int, bucket: Int): Int {
    // Get a free index
    val i = bucketNext(0)
    setBucketNext(0, bucketNext(i))

    // Add to the bucket list
    keys[i] = key
    setBucketNext(i, bucketStart(bucket))
    setBucketStart(bucket, i)

    // Update bloom filter
    blooms[bucket] = blooms[bucket] or hash

    ++count
    ++mutationCount
    return i
}

internal inline fun <T : ArrayHashTable> T.addUniqueKeyOrdered(
        key: Any?,
        hash: Int,
        bucket: Int
): Int {
    val i = addUniqueKey(key, hash, bucket)
    makeLast(i)
    return i
}

internal inline fun <T : ArrayHashTable> T.addUniqueValue(
        key: Any?,
        hash: Int,
        bucket: Int,
        value: Any?
): Int {
    val i = addUniqueKey(key, hash, bucket)
    values[i] = value
    return i
}

internal inline fun <T : ArrayHashTable> T.addUniqueValueOrdered(
        key: Any?,
        hash: Int,
        bucket: Int,
        value: Any?
): Int {
    val i = addUniqueValue(key, hash, bucket, value)
    makeLast(i)
    return i
}

internal inline fun <T : ArrayHashTable> T.initFrom(map: Map<*, *>) {
    check(capacity >= map.size) { "Not enough room for map entries.  initFrom requires pre-initialization of capacity." }
    check(container.hasValues) { "Attempted to initialize set with a map." }
    check(!container.isOrdered) { "initFrom called on ordered map; use initFromOrdered instead." }
    map.forEachEntry { (key, value) ->
        val hash = key.hashCode()
        val bucket = hashToBucket(hash)
        addUniqueValue(key, hash, bucket, value)
    }
}

internal inline fun <T : ArrayHashTable> T.initFromOrdered(map: Map<*, *>) {
    check(capacity >= map.size) { "Not enough room for map entries.  initFrom requires pre-initialization of capacity." }
    check(container.hasValues) { "Attempted to initialize set with a map." }
    check(container.isOrdered) { "initFromOrdered called on unordered map; use initFrom instead." }
    map.forEachEntry { (key, value) ->
        val hash = key.hashCode()
        val bucket = hashToBucket(hash)
        addUniqueValueOrdered(key, hash, bucket, value)
    }
}

internal inline fun <T : ArrayHashTable> T.initFrom(keys: Collection<*>) {
    check(capacity >= keys.size) { "Not enough room for map entries.  initFrom requires pre-initialization of capacity." }
    check(!container.hasValues) { "Attempted to initialize map with a set." }
    check(!container.isOrdered) { "initFrom called on ordered set; use initFromOrdered instead." }
    if (keys is Set<*>) {
        // If it's a set, we know all the elements are already unique
        keys.forEach { key ->
            val hash = key.hashCode()
            val bucket = hashToBucket(hash)
            addUniqueKey(key, hash, bucket)
        }
    } else {
        for (key in keys) {
            addKey(key)
        }
    }
}

internal inline fun <T : ArrayHashTable> T.initFromOrdered(keys: Collection<*>) {
    check(capacity >= keys.size) { "Not enough room for map entries.  initFrom requires pre-initialization of capacity." }
    check(!container.hasValues) { "Attempted to initialize map with a set." }
    check(container.isOrdered) { "initFromOrdered called on unordered set; use initFrom instead." }
    if (keys is Set<*>) {
        // If it's a set, we know all the elements are already unique
        keys.forEach { key ->
            val hash = key.hashCode()
            val bucket = hashToBucket(hash)
            addUniqueKeyOrdered(key, hash, bucket)
        }
    } else {
        for (key in keys) {
            addKeyOrdered(key)
        }
    }
}

internal inline fun <T : ArrayHashTable> T.makeLast(i: Int) {
    setOrderNext(i, 0)
    setOrderPrev(i, orderPrev(0))
    setOrderNext(orderPrev(0), i)
    setOrderPrev(0, i)
}

internal inline fun <T : ArrayHashTable> T.removeFromOrder(i: Int) {
    val prev = orderPrev(i)
    val next = orderNext(i)
    setOrderNext(prev, next)
    setOrderPrev(next, prev)
    // Don't set prev and next for i.  Removal from an iterator depends on these
    // remaining intact during iteration.
}

@PublishedApi
internal inline fun <T: ArrayHashTable> T.forEachIndex(action: (i: Int) -> Unit): Unit {
    var bucket = -1
    var i = 0
    var remaining = count
    while (remaining > 0) {
        while (i == 0) {
            bucket++
            i = bucketStart(bucket)
        }

        action(i)

        i = bucketNext(i)
        remaining--
    }
}

@PublishedApi
internal inline fun <T: ArrayHashTable> T.forEachIndexOrdered(action: (i: Int) -> Unit): Unit {
    var i = orderNext(0)
    while (i != 0) {
        action(i)
        i = orderNext(i)
    }
}

@PublishedApi
internal inline fun <T: ArrayHashTable> T.forEachKey(action: (key: Any?) -> Unit) {
    forEachIndex {
        action(keys[it])
    }
}

@PublishedApi
internal inline fun <T: ArrayHashTable> T.forEachValue(action: (key: Any?, value: Any?) -> Unit) {
    forEachIndex {
        action(keys[it], values[it])
    }
}

@PublishedApi
internal inline fun <T: ArrayHashTable> T.forEachKeyOrdered(action: (key: Any?) -> Unit) {
    forEachIndexOrdered {
        action(keys[it])
    }
}

@PublishedApi
internal inline fun <T: ArrayHashTable> T.forEachValueOrdered(action: (key: Any?, value: Any?) -> Unit) {
    forEachIndexOrdered {
        action(keys[it], values[it])
    }
}


//
// grow() increases the capacity of the table, by allocating a new table and
// rehashing existing keys into the new table, and then updating the table
// reference in the container.
//
// grow() retuns "nothing" so that the caller cannot proceed to use the old
// table by mistake.  The new table is passed to the "then" lambda.
//
internal inline fun <T : ArrayHashTable> T.grow(
        minCapacity: Int,
        then: ArrayHashTable.() -> Nothing
): Nothing {
    ++mutationCount
    check(minCapacity > capacity) { "grow() can only increase capacity.  minCapacity=$minCapacity capacity=$capacity" }
    var newCapacity = capacity
    while (newCapacity < minCapacity) {
        if (newCapacity < ArrayHashTable.geometricCapacityGrowthLimit) {
            newCapacity *= ArrayHashTable.geometricCapacityGrowthFactor
        } else {
            newCapacity += ArrayHashTable.linearCapacityGrowthIncrement
        }
    }

    val newTable = createArrayHashTable(container, newCapacity)
    container.hashTable = newTable
    newTable.rehashFrom(this, then)
}

internal inline fun <TOld : ArrayHashTable> ArrayHashTable.rehashFrom(
        old: TOld,
        then: ArrayHashTable.() -> Nothing
): Nothing {

    val isOrdered = container.isOrdered
    val hasValues = container.hasValues

    // Rehash old elements, maintaining ordering and values
    when {
        hasValues && isOrdered ->
            old.forEachValueOrdered { key, value ->
                val hash = key.hashCode()
                val bucket = hashToBucket(hash)
                addUniqueValueOrdered(key, hash, bucket, value)
            }
        hasValues && !isOrdered ->
            old.forEachValue { key, value ->
                val hash = key.hashCode()
                val bucket = hashToBucket(hash)
                addUniqueValue(key, hash, bucket, value)
            }
        !hasValues && isOrdered ->
            old.forEachKeyOrdered { key ->
                val hash = key.hashCode()
                val bucket = hashToBucket(hash)
                addUniqueKeyOrdered(key, hash, bucket)
            }
        !hasValues && !isOrdered ->
            old.forEachKey { key ->
                val hash = key.hashCode()
                val bucket = hashToBucket(hash)
                addUniqueKey(key, hash, bucket)
            }
    }

    this.then()
}

internal inline fun <T : ArrayHashTable> T.keyIterator(): MutableIterator<Any?> {
    check(!container.isOrdered) { "keyIterator() is for unordered tables; use orderedKeyIterator instead." }

    return object : MutableIterator<Any?> {
        val originalCount = count
        var itCount = 0
        var bucket = -1
        var current = 0
        var prev = 0 // the previous index in the current bucket
        var removed = false
        var currentMutationCount = mutationCount

        override fun hasNext(): Boolean = (itCount < originalCount)

        override fun next(): Any? {
            check(mutationCount == currentMutationCount) { "Table was modified during iteration " }
            check(container.hashTable === this@keyIterator) { "Table was rehashed during iteration "}
            if (itCount >= originalCount) { throw NoSuchElementException("No more elements") }

            do {
                if (removed) {
                    if (prev == 0) {
                        current = bucketStart(bucket)
                    } else {
                        current = bucketNext(prev)
                    }
                    removed = false
                } else if (current == 0) {
                    prev = 0
                    ++bucket
                    current = bucketStart(bucket)
                } else {
                    prev = current
                    current = bucketNext(current)
                }
            } while (current == 0)

            ++itCount

            return keys[current]
        }

        override fun remove() {
            check(!removed) { "Already removed this element" }
            check(current != 0) { "remove() called before next()" }
            check(mutationCount == currentMutationCount) { "Table was modified during iteration " }

            removeKeyAtIndex(bucket, current, prev)
            if (container.hasValues) {
                values[current] = null
            }
            removed = true
            currentMutationCount = mutationCount
        }
    }
}

internal inline fun <T : ArrayHashTable> T.orderedKeyIterator(): MutableIterator<Any?> {
    check(container.isOrdered) { "orderedKeyIterator() is for ordered tables; use keyIterator instead." }

    return object : MutableIterator<Any?> {
        var current = 0
        var removed = false
        var currentMutationCount = mutationCount

        override fun hasNext(): Boolean = (orderNext(current) != 0)

        override fun next(): Any? {
            check(mutationCount == currentMutationCount) { "Table was modified during iteration " }
            check(container.hashTable === this@orderedKeyIterator) { "Table was rehashed during iteration "}
            val next = orderNext(current)
            if (next == 0) { throw NoSuchElementException("No more elements") }
            current = next
            removed = false
            return keys[current]
        }

        override fun remove() {
            check(!removed) { "Already removed this element" }
            check(current != 0) { "remove() called before next()" }
            check(mutationCount == currentMutationCount) { "Table was modified during iteration " }

            if (container.hasValues) {
                removeValueOrdered(keys[current])
            } else {
                removeKeyOrdered(keys[current])
            }
            removed = true
            currentMutationCount = mutationCount
        }
    }
}
