package com.certora.common.collect.impl

import com.certora.common.collect.*
import kotlinx.serialization.Serializable

@Serializable
class HashTestObject(val value: Int, val code: Int = value.hashCode()) : java.io.Serializable, Comparable<HashTestObject>, StableHashCode {
    private constructor() : this(0, 0)
    override fun hashCode() = code
    override fun equals(other: Any?) = other is HashTestObject && other.value == this.value
    override fun compareTo(other: HashTestObject): Int {
        val v = this.value
        val ov = other.value
        return when {
            v < ov -> -1
            v > ov -> 1
            else -> 0
        }
    }
}
