package com.certora.common.collect

@PublishedApi
internal abstract class AbstractMapEntry<K, V> : Map.Entry<K, V> {
    override fun toString(): String = "$key=$value"
    override fun hashCode(): Int = hashCode(key, value)
    override fun equals(other: Any?): Boolean {
        if (other !is Map.Entry<*, *>) {
            return false
        }
        return other.key == this.key && this.value == other.value
    }
    companion object {
        fun hashCode(key: Any?, value: Any?) = key.hashCode() xor value.hashCode()
    }
}
