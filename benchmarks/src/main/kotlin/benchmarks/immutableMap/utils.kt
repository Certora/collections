/*
 * Modified from the kotlinx.collections.immutable sources, which contained the following notice:
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableMap

import benchmarks.*
import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlin.math.ceil
import kotlin.math.log


fun <K, V> emptyPersistentMap(implementation: String): PersistentMap<K, V> = when (implementation) {
    ORDERED_HAMT_IMPL -> persistentMapOf()
    HAMT_IMPL -> persistentHashMapOf()
    TREAP_IMPL -> treapMapOf()
    HASH_MAP_IMPL -> fakePersistentMapOf()
    else -> throw AssertionError("Unknown PersistentMap implementation: $implementation")
}

fun <K> persistentMapPut(implementation: String, keys: List<K>): PersistentMap<K, String> {
    var map = emptyPersistentMap<K, String>(implementation)
    for (key in keys) {
        map = map.put(key, "some value")
    }
    return map
}

fun <K> treapMapPut(keys: List<K>): TreapMap<K, String> {
    var map = treapMapOf<K, String>()
    for (key in keys) {
        map = map.put(key, "some value")
    }
    return map
}

fun <K> persistentMapRemove(persistentMap: PersistentMap<K, String>, keys: List<K>): PersistentMap<K, String> {
    var map = persistentMap
    for (key in keys) {
        map = map.remove(key)
    }
    return map
}
