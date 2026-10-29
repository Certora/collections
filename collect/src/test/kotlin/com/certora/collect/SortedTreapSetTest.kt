package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.DeserializationStrategy
import java.util.TreeSet
import kotlin.test.*

/** Tests for [SortedTreapSet]. */
@Suppress("UNCHECKED_CAST")
class SortedTreapSetTest : TreapSetTest() {
    override val nullKeysAllowed get() = false
    override fun makeKey(value: Int, code: Int) = ComparableTestKey(value, code)
    override fun makeBaseline(): MutableSet<TestKey?> = TreeSet()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null

    @Test
    fun iteratesInSortedOrder() {
        val set = treapSetOf(makeKey(3), makeKey(1), makeKey(4), makeKey(2))
        assertEquals(listOf(1, 2, 3, 4).map { makeKey(it) }, set.toList())
    }
}
