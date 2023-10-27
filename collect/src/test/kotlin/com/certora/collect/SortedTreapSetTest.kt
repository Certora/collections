package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.DeserializationStrategy
import java.util.TreeSet

/** Tests for [SortedTreapSet]. */
@Suppress("UNCHECKED_CAST")
class SortedSetTest : SetTest() {
    override val nullKeysAllowed get() = false
    override fun makeSet(): MutableSet<TestKey?> = treapSetOf<TestKey?>().builder()
    override fun makeKey(value: Int, code: Int) = ComparableTestKey(value, code)
    override fun makeBaseline(): MutableSet<TestKey?> = TreeSet()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
