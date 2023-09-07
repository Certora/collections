package com.certora.common.collect

import com.certora.common.collect.*
import kotlinx.serialization.DeserializationStrategy

class PersistentHashMapTest: MapTest() {
    override fun makeMap(): MutableMap<HashTestObject?, Any?> = persistentHashMapOf<HashTestObject?, Any?>().builder()
    override fun makeBaseline(): MutableMap<HashTestObject?, Any?> = HashMap()
    override fun makeMap(other: Map<HashTestObject?,Any?>): MutableMap<HashTestObject?, Any?> = makeMap().apply { putAll(other) }
    override fun makeBaseline(other: Map<HashTestObject?,Any?>): MutableMap<HashTestObject?, Any?> = HashMap(other)
    override fun makeMapOfInts(): MutableMap<Int?, Int?> = persistentHashMapOf<Int?, Int?>().builder()
    override fun makeMapOfInts(other: Map<Int?, Int?>) = makeMapOfInts().apply { putAll(other) }
    override fun makeBaselineOfInts(): MutableMap<Int?, Int?> = HashMap()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
