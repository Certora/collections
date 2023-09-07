package com.certora.common.collect

import com.certora.common.utils.*

@PublishedApi
internal class MutableMapEntry<K, V>(
    private val map: MutableMap<K, V>,
    override val key: K
) : AbstractMapEntry<K, V>(), MutableMap.MutableEntry<K, V> {
    override val value: @UnsafeVariance V get() = map.get(key).uncheckedAs<V>()
    override fun setValue(newValue: V): @UnsafeVariance V = map.put(key, newValue).uncheckedAs<V>()
}
