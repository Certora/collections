package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.Serializable

/** Type to use as a key for Treap tests.  Allows tests to control exact hash codes. */
@Serializable
@Treapable
class TestKey(val value: Int, val code: Int = value.hashCode()) : java.io.Serializable, Comparable<TestKey> {
    private constructor() : this(0, 0)
    override fun hashCode() = code
    override fun equals(other: Any?) = other is TestKey && other.value == this.value
    override fun compareTo(other: TestKey): Int {
        val v = this.value
        val ov = other.value
        return when {
            v < ov -> -1
            v > ov -> 1
            else -> 0
        }
    }
}
