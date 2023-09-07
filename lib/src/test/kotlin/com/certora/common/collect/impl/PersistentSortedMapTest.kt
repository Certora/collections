package com.certora.common.collect

import com.certora.common.collect.*
import com.certora.common.utils.*
import kotlinx.serialization.DeserializationStrategy
import java.util.TreeMap

@Suppress("UNCHECKED_CAST")
class PersistentSortedMapTest: MapTest() {
    override val allowNullKeys = false
    override fun makeMap(): MutableMap<HashTestObject?, Any?> = treapMapOf<HashTestObject, Any?>().builder() as MutableMap<HashTestObject?, Any?>
    override fun makeBaseline(): MutableMap<HashTestObject?, Any?> = TreeMap()
    override fun makeMap(other: Map<HashTestObject?,Any?>): MutableMap<HashTestObject?, Any?> = makeMap().apply { putAll(other) }
    override fun makeBaseline(other: Map<HashTestObject?,Any?>): MutableMap<HashTestObject?, Any?> = TreeMap(other)
    override fun makeMapOfInts(): TreapMap<Int?, Int?> = treapMapOf<Int, Int?>() as TreapMap<Int?, Int?>
    override fun makeMapOfInts(other: Map<Int?, Int?>) = makeMapOfInts().apply { putAll(other) }
    override fun makeBaselineOfInts(): MutableMap<Int?, Int?> = TreeMap()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
