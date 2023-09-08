package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.DeserializationStrategy

/** Tests for [HashTreapSet]. */
class HashTreapSetTest : TreapSetTest() {
    override fun makeSet(): MutableSet<TestKey?> = hashTreapSetOf<TestKey?>().builder()
    override fun makeBaseline(): MutableSet<TestKey?> = HashSet()
    override fun makeSet(other: Collection<TestKey?>): MutableSet<TestKey?> =
            makeSet().apply { addAll(other) }
    override fun makeBaseline(other: Collection<TestKey?>): MutableSet<TestKey?> =
        HashSet(other)
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
