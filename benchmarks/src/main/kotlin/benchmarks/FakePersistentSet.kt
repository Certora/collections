package benchmarks

import kotlinx.collections.immutable.*

class FakePersistentSet<T>(val value: Set<T>) : PersistentSet<T>, Set<T> by value {
    class Builder<T>(val value: MutableSet<T>) : PersistentSet.Builder<T>, MutableSet<T> by value {
        override fun build() = FakePersistentSet(value)
    }

    override fun builder() = Builder(value.toMutableSet())
    override fun clear() = FakePersistentSet(emptySet<T>())
    override fun add(element: T) = FakePersistentSet(value + element)
    override fun addAll(elements: Collection<T>) = FakePersistentSet(value + elements)
    override fun remove(element: T) = FakePersistentSet(value - element)
    override fun removeAll(predicate: (T) -> Boolean) = FakePersistentSet(value.filterNot(predicate).toSet())
    override fun removeAll(elements: Collection<T>) = FakePersistentSet(value - elements)
    override fun retainAll(elements: Collection<T>) = FakePersistentSet(value.intersect(elements))
}

fun <T> fakePersistentSetOf(): PersistentSet<T> = FakePersistentSet(emptySet<T>())
