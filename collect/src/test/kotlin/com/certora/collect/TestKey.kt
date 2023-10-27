package com.certora.collect

import com.certora.collect.*
import kotlinx.serialization.Serializable

/** Type to use as a key for Treap tests. */
@Treapable
@Serializable
sealed class TestKey : java.io.Serializable

/** Type to use as a key for Treap tests.  Allows tests to control exact hash codes. */
@Serializable
class HashTestKey(val value: Int, val code: Int = value.hashCode()) : TestKey() {
    private constructor() : this(0, 0) // for serialization
    override fun hashCode() = code
    override fun equals(other: Any?) = other is HashTestKey && other.value == this.value
}

/** Adds Comparable. */
@Serializable
class ComparableTestKey(private val value: Int, private val code: Int = value.hashCode()) : TestKey(), Comparable<ComparableTestKey> {
    private constructor() : this(0, 0) // for serialization
    override fun hashCode() = code
    override fun equals(other: Any?) = other is ComparableTestKey && other.value == this.value
    override fun compareTo(other: ComparableTestKey): Int {
        val v = this.value
        val ov = other.value
        return when {
            v < ov -> -1
            v > ov -> 1
            else -> 0
        }
    }
}
