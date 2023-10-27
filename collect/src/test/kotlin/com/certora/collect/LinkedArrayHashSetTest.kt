package com.certora.collect

import kotlinx.serialization.*

/** Test for [LinkedArrayHashSet]. */
class LinkedArrayHashSetTest : SetTest() {
    override fun makeSet(): MutableSet<TestKey?> = LinkedArrayHashSet()
    override fun makeKey(value: Int, code: Int) = HashTestKey(value, code)
    override fun makeBaseline(): MutableSet<TestKey?> = LinkedHashSet()
    override fun getBaseDeserializer(): DeserializationStrategy<*> = serializer<HashSet<TestKey?>>()
    override fun getDeserializer(): DeserializationStrategy<*> = serializer<LinkedArrayHashSet<TestKey?>>()
}
