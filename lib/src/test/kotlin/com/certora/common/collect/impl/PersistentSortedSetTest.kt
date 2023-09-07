package com.certora.common.collect

import com.certora.common.collect.*
import com.certora.common.utils.*
import kotlinx.serialization.DeserializationStrategy
import java.util.TreeSet

class PersistentSortedSetTest : SetTest() {
    override val nullKeysAllowed get() = false
    override fun makeSet(): MutableSet<HashTestObject?> = persistentSortedSetOf<HashTestObject>().builder().uncheckedAs<MutableSet<HashTestObject?>>()
    override fun makeBaseline(): MutableSet<HashTestObject?> = TreeSet()
    override fun makeSet(other: Collection<HashTestObject?>): MutableSet<HashTestObject?> =
            makeSet().apply { addAll(other) }
    override fun makeBaseline(other: Collection<HashTestObject?>): MutableSet<HashTestObject?> =
        TreeSet(other)
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
