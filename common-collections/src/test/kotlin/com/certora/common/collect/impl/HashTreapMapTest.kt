package com.certora.common.collect

import com.certora.common.collect.*
import kotlinx.serialization.DeserializationStrategy

/** Tests for [HashTreapMap]. */
class HashTreapMapTest: TreapMapTest() {
    override fun makeMap(): MutableMap<TestKey?, Any?> = hashTreapMapOf<TestKey?, Any?>().builder()
    override fun makeBaseline(): MutableMap<TestKey?, Any?> = HashMap()
    override fun makeMap(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = makeMap().apply { putAll(other) }
    override fun makeBaseline(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = HashMap(other)
    override fun makeMapOfInts(): TreapMap<Int?, Int?> = hashTreapMapOf<Int?, Int?>()
    override fun makeMapOfInts(other: Map<Int?, Int?>) = makeMapOfInts().apply { putAll(other) }
    override fun makeBaselineOfInts(): MutableMap<Int?, Int?> = HashMap()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
