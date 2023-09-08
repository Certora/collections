package com.certora.common.collect

import com.certora.common.collect.*
import kotlinx.serialization.DeserializationStrategy
import java.util.TreeSet

/** Tests for [SortedTreapSet]. */
@Suppress("UNCHECKED_CAST")
class SortedTreapSetTest : TreapSetTest() {
    override val nullKeysAllowed get() = false
    override fun makeSet(): MutableSet<TestKey?> = treapSetOf<TestKey>().builder() as MutableSet<TestKey?>
    override fun makeBaseline(): MutableSet<TestKey?> = TreeSet()
    override fun makeSet(other: Collection<TestKey?>): MutableSet<TestKey?> =
            makeSet().apply { addAll(other) }
    override fun makeBaseline(other: Collection<TestKey?>): MutableSet<TestKey?> =
        TreeSet(other)
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
