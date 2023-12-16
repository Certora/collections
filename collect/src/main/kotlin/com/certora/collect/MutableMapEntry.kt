package com.certora.collect

/** Simple implementation of MutableMap.Entry. */
public class MutableMapEntry<K, V>(
    private val map: MutableMap<K, V>,
    override val key: K
) : AbstractMapEntry<K, V>(), MutableMap.MutableEntry<K, V> {

    override val value: @UnsafeVariance V get() {
        return map.get(key) ?: run {
            check(key in map) { "Key '$key' was removed from the map" }
            @Suppress("UNCHECKED_CAST")
            null as V
        }
    }

    override fun setValue(newValue: V): @UnsafeVariance V {
        check(key in map) { "Key '$key' was removed from the map" }
        @Suppress("UNCHECKED_CAST")
        return map.put(key, newValue) as V
    }
}
