package com.certora.common.collect

import com.certora.common.collect.*
import kotlinx.serialization.DeserializationStrategy
import java.util.TreeSet

@Suppress("UNCHECKED_CAST")
class PersistentSortedSetTest : SetTest() {
    override val nullKeysAllowed get() = false
    override fun makeSet(): MutableSet<HashTestObject?> = treapSetOf<HashTestObject>().builder() as MutableSet<HashTestObject?>
    override fun makeBaseline(): MutableSet<HashTestObject?> = TreeSet()
    override fun makeSet(other: Collection<HashTestObject?>): MutableSet<HashTestObject?> =
            makeSet().apply { addAll(other) }
    override fun makeBaseline(other: Collection<HashTestObject?>): MutableSet<HashTestObject?> =
        TreeSet(other)
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
