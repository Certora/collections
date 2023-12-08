package benchmarks

import kotlinx.collections.immutable.*

class FakePersistentList<T>(val value: List<T>) : PersistentList<T>, List<T> by value {
    class Builder<T>(val value: MutableList<T>) : PersistentList.Builder<T>, MutableList<T> by value {
        override fun equals(other: Any?) = value == other
        override fun hashCode() = value.hashCode()
        override fun build() = FakePersistentList(value)
    }

    override fun equals(other: Any?) = value == other
    override fun hashCode() = value.hashCode()

    override fun builder() = Builder(value.toMutableList())
    override fun clear() = FakePersistentList(emptyList<T>())

    override fun add(element: T): PersistentList<T> = FakePersistentList(value + element)
    override fun add(index: Int, element: T): PersistentList<T> = FakePersistentList(value.toMutableList().apply { add(index, element) })
    override fun addAll(elements: Collection<T>): PersistentList<T> = FakePersistentList(value + elements)
    override fun addAll(index: Int, c: Collection<T>): PersistentList<T> = FakePersistentList(value.toMutableList().apply { addAll(index, c) })
    override fun remove(element: T): PersistentList<T> = FakePersistentList(value - element)
    override fun removeAll(predicate: (T) -> Boolean): PersistentList<T> = FakePersistentList(value.filterNot(predicate))
    override fun removeAll(elements: Collection<T>): PersistentList<T> = FakePersistentList(value - elements)
    override fun removeAt(index: Int): PersistentList<T> = FakePersistentList(value.toMutableList().apply { removeAt(index) })
    override fun retainAll(elements: Collection<T>): PersistentList<T> = FakePersistentList(value.filter { it !in elements})
    override fun set(index: Int, element: T): PersistentList<T> = FakePersistentList(value.toMutableList().apply { set(index, element) })

    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<T> =
        super<PersistentList>.subList(fromIndex, toIndex)
}

fun <T> fakePersistentListOf(): PersistentList<T> = FakePersistentList(emptyList<T>())
