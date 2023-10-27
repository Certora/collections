package com.certora.collect

/**
 * Implemented by ArrayHashMap, etc.  This allows ArrayHashTable to query information
 * about the particular collection type, as well as to update the hash table itself
 * when resizing.
 */
internal interface ArrayHashTableContainer {
    var hashTable: ArrayHashTable
    val hasValues: Boolean
    val isOrdered: Boolean
    val loadFactor: Float
}
