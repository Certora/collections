package com.certora.common.collect

import com.certora.common.utils.*
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.PersistentSet




interface TreapSet<out E> : PersistentSet<E> {
    override fun add(element: @UnsafeVariance E): TreapSet<E>
    override fun addAll(elements: Collection<@UnsafeVariance E>): TreapSet<E>
    override fun remove(element: @UnsafeVariance E): TreapSet<E>
    override fun removeAll(elements: Collection<@UnsafeVariance E>): TreapSet<E>
    override fun removeAll(predicate: (E) -> Boolean): TreapSet<E>
    override fun retainAll(elements: Collection<@UnsafeVariance E>): TreapSet<E>
    override fun clear(): TreapSet<E>

    interface Builder<E> : PersistentSet.Builder<E> {
        override fun build(): TreapSet<E>
    }

    override fun builder(): Builder<@UnsafeVariance E>
}


fun <@WithStableHashCodeIfSerialized T : Comparable<T>> treapSetOf(): PersistentSet<T> = SortedTreapSet.emptyOf<T>()
fun <@WithStableHashCodeIfSerialized T : Comparable<T>> treapSetOf(element: T): PersistentSet<T> = treapSetOf<T>().add(element)
fun <@WithStableHashCodeIfSerialized T : Comparable<T>> treapSetOf(vararg elements: T): PersistentSet<T> = treapSetOf<T>().mutate { it.addAll(elements) }

fun <@WithStableHashCodeIfSerialized T> hashTreapSetOf(): PersistentSet<T> = HashTreapSet.emptyOf<T>()
fun <@WithStableHashCodeIfSerialized T> hashTreapSetOf(element: T): PersistentSet<T> = hashTreapSetOf<T>().add(element)
fun <@WithStableHashCodeIfSerialized T> hashTreapSetOf(vararg elements: T): PersistentSet<T> = hashTreapSetOf<T>().mutate { it.addAll(elements) }

