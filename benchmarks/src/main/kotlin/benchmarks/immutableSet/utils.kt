/*
 * Modified from the kotlinx.collections.immutable sources, which contained the following notice:
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet

import benchmarks.*
import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlin.math.ceil
import kotlin.math.log


fun <E> emptyPersistentSet(implementation: String): PersistentSet<E> = when (implementation) {
    ORDERED_HAMT_IMPL -> persistentSetOf()
    HAMT_IMPL -> persistentHashSetOf()
    TREAP_IMPL -> treapSetOf()
    HASH_MAP_IMPL -> fakePersistentSetOf()
    else -> throw AssertionError("Unknown PersistentSet implementation: $implementation")
}

fun <E> persistentSetAdd(implementation: String, elements: List<E>): PersistentSet<E> {
    var set = emptyPersistentSet<E>(implementation)
    for (element in elements) {
        set = set.add(element)
    }
    return set
}

fun <E> treapSetAdd(elements: List<E>): TreapSet<E> {
    var set = treapSetOf<E>()
    for (element in elements) {
        set = set.add(element)
    }
    return set
}

fun <E> persistentSetRemove(persistentSet: PersistentSet<E>, elements: List<E>): PersistentSet<E> {
    var set = persistentSet
    for (element in elements) {
        set = set.remove(element)
    }
    return set
}

