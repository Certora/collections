package com.certora.collect

/** Provides a base implementation of Map.Entry and/or MutableMap.Entry. */
public abstract class AbstractMapEntry<K, V> : Map.Entry<K, V> {
    override fun toString(): String = "$key=$value"
    override fun hashCode(): Int = hashCode(key, value)
    override fun equals(other: Any?): Boolean {
        if (other !is Map.Entry<*, *>) {
            return false
        }
        return other.key == this.key && this.value == other.value
    }
    public companion object {
        public fun hashCode(key: Any?, value: Any?): Int = key.hashCode() xor value.hashCode()
    }
}
