package com.certora.common.collect

import com.certora.common.collect.*
import kotlinx.serialization.DeserializationStrategy
import java.util.TreeMap

/** Tests for [SortedTreapMap]. */
@Suppress("UNCHECKED_CAST")
class SortedTreapMapTest: TreapMapTest() {
    override val allowNullKeys = false
    override fun makeMap(): MutableMap<TestKey?, Any?> = treapMapOf<TestKey, Any?>().builder() as MutableMap<TestKey?, Any?>
    override fun makeBaseline(): MutableMap<TestKey?, Any?> = TreeMap()
    override fun makeMap(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = makeMap().apply { putAll(other) }
    override fun makeBaseline(other: Map<TestKey?,Any?>): MutableMap<TestKey?, Any?> = TreeMap(other)
    override fun makeMapOfInts(): TreapMap<Int?, Int?> = treapMapOf<Int, Int?>() as TreapMap<Int?, Int?>
    override fun makeMapOfInts(other: Map<Int?, Int?>) = makeMapOfInts().apply { putAll(other) }
    override fun makeBaselineOfInts(): MutableMap<Int?, Int?> = TreeMap()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
