package com.certora.common.collect

import com.certora.common.collect.*
import kotlinx.serialization.DeserializationStrategy

class PersistentHashSetTest : SetTest() {
    override fun makeSet(): MutableSet<HashTestObject?> = persistentHashSetOf<HashTestObject?>().builder()
    override fun makeBaseline(): MutableSet<HashTestObject?> = HashSet()
    override fun makeSet(other: Collection<HashTestObject?>): MutableSet<HashTestObject?> =
            makeSet().apply { addAll(other) }
    override fun makeBaseline(other: Collection<HashTestObject?>): MutableSet<HashTestObject?> =
        HashSet(other)
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
