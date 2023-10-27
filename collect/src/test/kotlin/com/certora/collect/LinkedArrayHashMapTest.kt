package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.DeserializationStrategy

/** Tests for [LinkedArrayHashMap]. */
class LinkedArrayHashMapTest: MapTest() {
    override fun makeKey(value: Int, code: Int) = HashTestKey(value, code)
    override fun makeMap(): MutableMap<TestKey?, Any?> = LinkedArrayHashMap<TestKey?, Any?>()
    override fun makeBaseline(): MutableMap<TestKey?, Any?> = LinkedHashMap()
    override fun makeMap(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = makeMap().apply { putAll(other) }
    override fun makeBaseline(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = LinkedHashMap(other)
    override fun makeMapOfInts(): TreapMap<Int?, Int?> = treapMapOf<Int?, Int?>()
    override fun makeMapOfInts(other: Map<Int?, Int?>) = makeMapOfInts().apply { putAll(other) }
    override fun makeBaselineOfInts(): MutableMap<Int?, Int?> = HashMap()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
