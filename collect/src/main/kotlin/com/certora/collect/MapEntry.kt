package com.certora.collect

/** Simple implementation of Map.Entry. */
public class MapEntry<K, V>(
    override val key: K,
    override val value: V
) : AbstractMapEntry<K, V>()
