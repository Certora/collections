package com.certora.collect

import kotlin.test.*

/** Tests for map entries. */
class MapEntryTest {
    @Test
    fun getValue() {
        val map = treapMapBuilderOf(1 to 2, 3 to 4)
        val e = map.entries.first()
        assertEquals(1, e.key)
        assertEquals(2, e.value)
        map.remove(1)
        assertEquals(1, e.key)
        assertFailsWith<IllegalStateException> { e.value }
    }

    @Test
    fun setValue() {
        val map = treapMapBuilderOf(1 to 2, 3 to 4)
        val e = map.entries.first()
        assertEquals(2, e.setValue(5))
        assertEquals(1, e.key)
        assertEquals(5, e.value)
        assertEquals(5, map[1])
        map.remove(1)
        assertEquals(1, e.key)
        assertFailsWith<IllegalStateException> { e.setValue(10) }
    }

    @Test
    fun getAndSetNullValue() {
        val map = treapMapBuilderOf(1 to null, 3 to 4)
        val e = map.entries.first()
        assertEquals(1, e.key)
        assertEquals(null, e.value)
        assertEquals(null, e.setValue(5))
        assertEquals(5, e.value)
        assertEquals(5, map[1])
        assertEquals(5, e.setValue(null))
        assertEquals(null, e.value)
        assertEquals(null, map[1])
    }
}
