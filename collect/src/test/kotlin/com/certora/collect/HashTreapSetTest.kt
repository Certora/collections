package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.DeserializationStrategy
import kotlin.test.*

/** Tests for [HashTreapSet]. */
class HashTreapSetTest : TreapSetTest() {
    override fun makeKey(value: Int, code: Int) = HashTestKey(value, code)
    override fun makeBaseline(): MutableSet<TestKey?> = HashSet()
    override fun getBaseDeserializer(): DeserializationStrategy<*>? = null
    override fun getDeserializer(): DeserializationStrategy<*>? = null

    @Test
    fun hashCollisions() {
        val a = HashTestKey(1, code = 7)
        val b = HashTestKey(2, code = 7)
        val c = HashTestKey(3, code = 8)
        val d = HashTestKey(4, code = 7)
        val left = treapSetOf(a, b, c)
        val right = treapSetOf(b, c, d)
        assertEquals(setOf(a, b, c, d), left union right)
        assertEquals(setOf(b, c), left intersect right)
        assertEquals(setOf(a), left subtract right)
        assertTrue(left.containsAny(listOf(d, b)))
        assertSame(b, left.findEqual(HashTestKey(2, code = 7)))
        assertFalse(b in left.remove(b))
        assertEquals(setOf(a, c), left.removeAll { it == b })
    }
}
