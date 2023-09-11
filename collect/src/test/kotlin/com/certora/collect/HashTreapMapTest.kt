package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.DeserializationStrategy

/** Tests for [HashTreapMap]. */
class HashTreapMapTest: TreapMapTest() {
    override fun makeKey(value: Int, code: Int) = HashTestKey(value, code)
    override fun makeMap(): MutableMap<TestKey?, Any?> = treapMapOf<TestKey?, Any?>().builder()
    override fun makeBaseline(): MutableMap<TestKey?, Any?> = HashMap()
    override fun makeMap(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = makeMap().apply { putAll(other) }
    override fun makeBaseline(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = HashMap(other)
    override fun makeMapOfInts(): TreapMap<Int?, Int?> = treapMapOf<Int?, Int?>()
    override fun makeMapOfInts(other: Map<Int?, Int?>) = makeMapOfInts().apply { putAll(other) }
    override fun makeBaselineOfInts(): MutableMap<Int?, Int?> = HashMap()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
