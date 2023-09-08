package com.certora.collect

@PublishedApi
internal class MapEntry<K, V>(
    override val key: K,
    override val value: V
) : AbstractMapEntry<K, V>()
