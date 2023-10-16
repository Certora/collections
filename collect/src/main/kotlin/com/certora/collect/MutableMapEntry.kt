package com.certora.collect

/** Simple implementation of MutableMap.Entry. */
public class MutableMapEntry<K, V>(
    private val map: MutableMap<K, V>,
    override val key: K
) : AbstractMapEntry<K, V>(), MutableMap.MutableEntry<K, V> {
    @Suppress("UNCHECKED_CAST")
    override val value: @UnsafeVariance V get() = map.get(key) as V
    @Suppress("UNCHECKED_CAST")
    override fun setValue(newValue: V): @UnsafeVariance V = map.put(key, newValue) as V
}
