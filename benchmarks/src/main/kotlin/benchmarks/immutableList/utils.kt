/*
 * Modified from the kotlinx.collections.immutable sources, which contained the following notice:
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import com.certora.collect.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf

fun <E> emptyPersistentList(implementation: String): PersistentList<E> = when (implementation) {
    KOTLIN_IMPL -> persistentListOf()
    TREAP_IMPL -> treapListOf()
    JAVA_IMPL -> fakePersistentListOf()
    else -> throw AssertionError("Unknown PersistentList implementation: $implementation")
}

fun persistentListAdd(implementation: String, size: Int): PersistentList<String> {
    var list = emptyPersistentList<String>(implementation)
    repeat(times = size) {
        list = list.add("some element")
    }
    return list
}

@Suppress("UNCHECKED_CAST")
fun <T> PersistentList<T>.removeLast(): PersistentList<T> = when(this) {
    is TreapList<*> -> this.removeLast() as PersistentList<T>
    else -> this.removeAt(this.size - 1)
}
