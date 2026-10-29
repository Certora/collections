package com.certora.collect

import kotlin.reflect.KClass
import kotlin.test.*

/** Tests for [treapHash] and [treapHashObject]. */
class TreapableHashCodeTest {
    private enum class Color { RED, BLUE }

    @Test
    fun mixesIntsEnumsClassesAndObjects() {
        assertEquals(Color.RED.name.hashCode(), treapHash { it + Color.RED })
        assertEquals(31 * Color.RED.name.hashCode() + 4, treapHash { it + Color.RED + 4 })
        assertEquals(String::class.java.name.hashCode(), treapHash { it + String::class.java })
        assertEquals(String::class.java.name.hashCode(), treapHash { it + String::class })
        val obj = object : Any() {}
        assertEquals(obj::class.java.name.hashCode(), treapHashObject(obj))
    }

    @Test
    fun nullOperandsHashAsZero() {
        assertEquals(treapHash { it + 0 }, treapHash { it + (null as Color?) })
        assertEquals(treapHash { it + 0 }, treapHash { it + (null as Class<*>?) })
        assertEquals(treapHash { it + 0 }, treapHash { it + (null as KClass<*>?) })
        assertEquals(treapHash { it + 0 }, treapHash { it + (null as String?) })
    }
}
