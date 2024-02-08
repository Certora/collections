package com.certora.collect

internal class TreapListBuilder<E>(
    private var list: TreapList<E>
) : TreapList.Builder<E>, java.io.Serializable, AbstractMutableList<E>() {
    override val size: Int = list.size
    override fun isEmpty(): Boolean = list.isEmpty()

    override fun get(index: Int): E = list[index]

    override fun indexOf(element: E): Int = list.indexOf(element)
    override fun lastIndexOf(element: E): Int = list.lastIndexOf(element)

    override fun contains(element: E): Boolean = list.contains(element)
    override fun containsAll(elements: Collection<E>): Boolean = list.containsAll(elements)

    override fun add(element: E): Boolean = update(list.add(element))
    override fun add(index: Int, element: E) { update(list.add(index, element)) }
    override fun addAll(elements: Collection<E>): Boolean = update(list.addAll(elements))
    override fun addAll(index: Int, elements: Collection<E>): Boolean = update(list.addAll(index, elements))

    override fun remove(element: E): Boolean = update(list.remove(element))
    override fun removeAll(elements: Collection<E>): Boolean = update(list.removeAll(elements))
    override fun removeAt(index: Int): E = get(index).also { list = list.removeAt(index) }

    override fun retainAll(elements: Collection<E>): Boolean = update(list.retainAll(elements))

    override fun set(index: Int, element: E): E = list.get(index).also { list = list.set(index, element) }

    override fun first(): E = list.first()
    override fun firstOrNull(): E? = list.firstOrNull()
    override fun last(): E = list.last()
    override fun lastOrNull(): E? = list.lastOrNull()

    override fun addFirst(element: E) { list = list.addFirst(element) }
    override fun addLast(element: E) { list = list.addLast(element) }
    override fun removeFirst(): E = list.first().also { list = list.removeFirst() }
    override fun removeLast(): E = list.last().also { list = list.removeLast() }

    override fun clear() { list = EmptyTreapList() }

    override fun iterator(): MutableIterator<E> = listIterator()
    override fun listIterator(): MutableListIterator<E> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<E> = object : MutableListIterator<E> {
        var listIterator = list.listIterator(index)
        var lastReturnedIndex = -1

        override fun hasNext(): Boolean = listIterator.hasNext()
        override fun hasPrevious(): Boolean = listIterator.hasPrevious()
        override fun next(): E = listIterator.next().also { lastReturnedIndex = listIterator.previousIndex() }
        override fun previous(): E = listIterator.previous().also { lastReturnedIndex = listIterator.nextIndex() }
        override fun nextIndex(): Int = listIterator.nextIndex()
        override fun previousIndex(): Int = listIterator.previousIndex()

        override fun add(element: E) {
            val i = nextIndex()
            add(i, element)
            listIterator = list.listIterator(i + 1)
        }

        override fun set(element: E) {
            val i = lastReturnedIndex
            if (i == -1) {
                throw IllegalStateException("set() called before next()")
            }
            list = list.set(i, element)
        }

        override fun remove() {
            val i = lastReturnedIndex
            if (i == -1) {
                throw IllegalStateException("remove() called before next()")
            }
            removeAt(i)
            listIterator = list.listIterator(i)
        }
    }

    override fun build(): TreapList<E> = list

    private fun update(newList: TreapList<E>): Boolean {
        if (newList !== list) {
            list = newList
            return true
        } else {
            return false
        }
    }
}
