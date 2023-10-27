package com.certora.collect

import kotlinx.serialization.*

/** Test for [ArrayHashSet]. */
class ArrayHashSetTest : SetTest() {
    override fun makeSet(): MutableSet<TestKey?> = ArrayHashSet()
    override fun makeKey(value: Int, code: Int) = HashTestKey(value, code)
    override fun makeBaseline(): MutableSet<TestKey?> = HashSet()
    override fun getBaseDeserializer(): DeserializationStrategy<*> = serializer<HashSet<TestKey?>>()
    override fun getDeserializer(): DeserializationStrategy<*> = serializer<ArrayHashSet<TestKey?>>()
}
