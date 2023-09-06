package com.certora.common.collect

import com.certora.common.collect.impl.*
import com.certora.common.utils.internal.*
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.PersistentSet

fun <@WithStableHashCodeIfSerialized T : Comparable<T>> persistentSortedSetOf(): PersistentSet<T> = SortedTreapSet.emptyOf<T>()
fun <@WithStableHashCodeIfSerialized T : Comparable<T>> persistentSortedSetOf(element: T): PersistentSet<T> = persistentSortedSetOf<T>().add(element)
fun <@WithStableHashCodeIfSerialized T : Comparable<T>> persistentSortedSetOf(vararg elements: T): PersistentSet<T> = persistentSortedSetOf<T>().mutate { it.addAll(elements) }

fun <@WithStableHashCodeIfSerialized T> persistentHashSetOf(): PersistentSet<T> = HashTreapSet.emptyOf<T>()
fun <@WithStableHashCodeIfSerialized T> persistentHashSetOf(element: T): PersistentSet<T> = persistentHashSetOf<T>().add(element)
fun <@WithStableHashCodeIfSerialized T> persistentHashSetOf(vararg elements: T): PersistentSet<T> = persistentHashSetOf<T>().mutate { it.addAll(elements) }

