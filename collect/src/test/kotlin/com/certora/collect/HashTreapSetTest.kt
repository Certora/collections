package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.DeserializationStrategy

/** Tests for [HashTreapSet]. */
class HashTreapSetTest : TreapSetTest() {
    override fun makeKey(value: Int, code: Int) = HashTestKey(value, code)
    override fun makeBaseline(): MutableSet<TestKey?> = HashSet()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null
}
