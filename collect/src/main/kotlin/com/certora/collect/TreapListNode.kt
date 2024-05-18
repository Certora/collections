package com.certora.collect

import com.certora.forkjoin.*
import kotlin.random.Random
import java.lang.Math.addExact

/**
    A [kotlinx.collections.immutable.PersistentList] implemented as a [treap](https://en.wikipedia.org/wiki/Treap).

    See [Treap] for an overview of the "treap" data structure.  This implementation does *not* derive from [Treap], as
    [TreapList]'s requirements are somewhat different from [TreapSet] and [TreapMap], but the description of [Treap] is
    useful background for understanding the design of [TreapList].

    Here we use the treap idea a little differently.  "Left" and "right" are simply the list's insertion order.
    "Priority" is randomly assigned for each new node, as in the original Treap literature.  This allows our trees to
    contain multiple copies of the same value, and yet remain balanced.

    To facilitate log-time indexing into the list, each node tracks the size of the sub-list represented by that node.
    A given node's index in its own sub-list is thus the size of it's left-hand subtree.  We do this rather than storing
    indexes directly so that lists can be freely inserted into other lists without having to rewrite all of the indices.
 */
internal class TreapListNode<E> private constructor(
    private val elem: E,
    private val priority: Int,
    private val left: TreapListNode<E>? = null,
    private val right: TreapListNode<E>? = null,
    override val size: Int = addExact(1, addExact(left?.size?:0, right?.size?:0))
) : TreapList<E>, java.io.Serializable {

    constructor(elem: E) : this(elem, priority = Random.Default.nextInt(), size = 1)

    override fun hashCode() = computeHashCode(initial = 1)
    override fun toString() = joinToString(", ", "[", "]")

    override fun equals(other: Any?): Boolean = when {
        this === other -> true
        other is List<*> -> this.size == other.size && this.zip(other).all { it.first == it.second }
        else -> false
    }

    override fun isEmpty(): Boolean = false

    override fun builder(): TreapList.Builder<E> = TreapListBuilder(this)

    override fun clear() = EmptyTreapList<E>()

    override fun addFirst(element: E): TreapListNode<E> = TreapListNode(element) append this
    override fun addLast(element: E): TreapListNode<E> = this append TreapListNode(element)

    override fun addAll(elements: Collection<E>): TreapList<E> = when {
        elements.isEmpty() -> this
        else -> this append elements.toTreapList() as TreapListNode<E>
    }

    override fun add(index: Int, element: E): TreapList<E> = validPosition(index).let {
        when {
            index == size -> addLast(element)
            index == 0 -> addFirst(element)
            else -> split(index).let { (l, r) -> l!!.addLast(element) append r!! }
        }
    }

    override fun addAll(index: Int, c: Collection<E>): TreapList<E> = validPosition(index).let {
        when {
            index == size -> addAll(c)
            c.isEmpty() -> this
            c is TreapListNode<E> -> when {
                index == 0 -> c append this
                else -> split(index).let { (left, right) -> left!! append c append right!! }
            }
            else -> split(index).let { (left, right) ->
                left.orEmpty().addAll(c).addAll(right!!)
            }
        }
    }

    override fun remove(element: E): TreapList<E> = removeNode(element).orEmpty()
    override fun removeFirst(): TreapList<E> = removeFirstNode().orEmpty()
    override fun removeLast(): TreapList<E> = removeLastNode().orEmpty()
    override fun removeAt(index: Int): TreapList<E> = removeNodeAt(validIndex(index)).orEmpty()
    override fun removeAll(elements: Collection<E>): TreapList<E> = removeAll { it in elements }
    override fun removeAll(predicate: (E) -> Boolean): TreapList<E> = removeAllNodes(predicate).orEmpty()
    override fun retainAll(elements: Collection<E>): TreapList<E> = removeAll { it !in elements }

    override fun get(index: Int): E = when (index) {
        0 -> first()
        size - 1 -> last()
        else -> getNodeAt(validIndex(index)).elem
    }

    override fun set(index: Int, element: E): TreapList<E> = setNodeAt(validIndex(index), element)

    override fun first(): E = firstNode().elem
    override fun firstOrNull(): E? = first()
    override fun last(): E = lastNode().elem
    override fun lastOrNull(): E? = last()

    override fun indexOf(element: E): Int = indexOfFirstNode(rootIndex(), element) ?: -1
    override fun lastIndexOf(element: E): Int = indexOfLastNode(rootIndex(), element) ?: -1

    override fun contains(element: E): Boolean =
        this.elem == element || (left?.contains(element) == true) || (right?.contains(element) == true)

    override fun containsAll(elements: Collection<E>): Boolean = elements.all { contains(it) }

    override fun subList(fromIndex: Int, toIndex: Int): TreapList<E> = when {
        validPosition(fromIndex) > validPosition(toIndex) ->
            throw IndexOutOfBoundsException("fromIndex $fromIndex > toIndex $toIndex")
        fromIndex == 0 -> split(toIndex).first.orEmpty()
        toIndex == size -> split(fromIndex).second.orEmpty()
        fromIndex == toIndex -> clear()
        else -> split(fromIndex).second?.split(toIndex - fromIndex)?.first.orEmpty()
    }

    override fun iterator(): Iterator<E> = listIterator()
    override fun listIterator(): ListIterator<E> = listIterator(0)

    override fun listIterator(index: Int): ListIterator<E> = object : ListIterator<E> {
        /** stores the path back to the original root of the tree. */
        val stack = ArrayDeque<TreapListNode<E>>()

        var current: TreapListNode<E>? = null
        var currentIndex = 0

        fun setPosition(index: Int) {
            stack.clear()
            currentIndex = index
            if (index < size) {
                fun TreapListNode<E>.buildStack(i: Int) {
                    stepToIndex(
                        i,
                        found = { current = this },
                        goLeft = {
                            stack.addLast(this)
                            left!!.buildStack(it)
                        },
                        goRight = {
                            stack.addLast(this)
                            right!!.buildStack(it)
                        },
                    )
                }
                buildStack(index)
            } else {
                current = null
            }
        }

        override fun hasNext() = current != null
        override fun hasPrevious() = currentIndex > 0

        override fun nextIndex() = currentIndex
        override fun previousIndex() = currentIndex - 1

        override fun next(): E {
            val result = current ?: throw NoSuchElementException()
            currentIndex++
            if (current!!.right != null) {
                stack.addLast(current!!)
                current = current!!.right
                while (current!!.left != null) {
                    stack.addLast(current!!)
                    current = current!!.left
                }
            } else {
                while (stack.lastOrNull()?.right === current) {
                    current = stack.removeLast()
                }
                current = stack.removeLastOrNull()
            }
            return result.elem
        }

        override fun previous(): E {
            if (currentIndex == 0) {
                throw NoSuchElementException()
            }
            currentIndex--
            if (current == null) {
                setPosition(currentIndex)
            } else if (current!!.left != null) {
                stack.addLast(current!!)
                current = current!!.left
                while (current!!.right != null) {
                    stack.addLast(current!!)
                    current = current!!.right
                }
            } else {
                while (stack.lastOrNull()?.left === current) {
                    current = stack.removeLast()
                }
                current = stack.removeLastOrNull()
            }
            return current!!.elem
        }
    }.apply { setPosition(validPosition(index)) }

    override fun forEachElement(action: (E) -> Unit) = forEachNode(action)
    override fun forEachElementIndexed(action: (Int, E) -> Unit) = forEachNodeIndexed(rootIndex(), action)

    override fun updateElements(transform: (E) -> E?): TreapList<E> =
        updateNodes(transform).orEmpty()

    override fun updateElementsIndexed(transform: (Int, E) -> E?): TreapList<E> =
        updateNodesIndexed(rootIndex(), transform).orEmpty()

    private fun with(
        left: TreapListNode<E>? = this.left,
        right: TreapListNode<E>? = this.right,
        elem: E = this.elem
    ) = when {
        left === this.left && right === this.right && elem === this.elem -> this
        else -> TreapListNode<E>(
            elem = elem,
            left = left,
            right = right,
            priority = this.priority,
        )
    }

    private fun validIndex(index: Int) = when {
        index < 0 || index >= size ->
            throw IndexOutOfBoundsException("Index $index is out of bounds for list of size $size")
        else -> index
    }

    private fun validPosition(index: Int) = when {
        index < 0 || index > size ->
            throw IndexOutOfBoundsException("Index $index is out of bounds for list of size $size")
        else -> index
    }

    private inline fun <T> stepToIndex(
        index: Int,
        found: () -> T,
        goLeft: (Int) -> T,
        goRight: (Int) -> T
    ) : T {
        val leftSize = left?.size ?: 0
        return when {
            index < leftSize -> goLeft(index)
            index == leftSize -> found()
            else -> goRight(index - leftSize - 1)
        }
    }

    private fun computeHashCode(initial: Int): Int {
        // We use recursive descent rather than `forEachNode` to avoid allocating a closure in this perf-critical
        // method.
        var code = left?.computeHashCode(initial) ?: initial
        code = 31 * code + elem.hashCode()
        if (right != null) {
            code = right.computeHashCode(code)
        }
        return code
    }

    private infix fun append(that: TreapListNode<E>): TreapListNode<E> = when {
        that.priority > this.priority -> that.with(left = this.append(that.left))
        else -> this.with(right = this.right append that)
    }

    /**
        Splits the list into two lists.  This first list contains all elements before [index]; the second list contains
        the [index]th element and beyond.
     */
    private fun split(index: Int): Pair<TreapListNode<E>?, TreapListNode<E>?> = stepToIndex(
        index,
        found = { left to this.with(left = null) },
        goLeft = { left!!.split(it).let { it.first to this.with(left = it.second) }},
        goRight = { right!!.split(it).let { this.with(right = it.first) to it.second }},
    )

    private fun removeNode(element: E): TreapListNode<E>? {
        val newLeft = left?.removeNode(element)
        return when {
            newLeft !== left -> this.with(left = newLeft)
            element == this.elem -> left append right
            else -> this.with(right = this.right?.removeNode(element))
        }
    }

    private fun removeFirstNode(): TreapListNode<E>? = when {
        left != null -> this.with(left = left.removeFirstNode())
        else -> right
    }

    private fun removeLastNode(): TreapListNode<E>? = when {
        right != null -> this.with(right = right.removeLastNode())
        else -> left
    }

    private fun removeNodeAt(index: Int): TreapListNode<E>? = stepToIndex(
        index,
        found = { left append right },
        goLeft = { this.with(left = left!!.removeNodeAt(it)) },
        goRight = { this.with(right = right!!.removeNodeAt(it)) },
    )

    private fun getNodeAt(index: Int): TreapListNode<E> = stepToIndex(
        index,
        found = { this },
        goLeft = { left!!.getNodeAt(it) },
        goRight = { right!!.getNodeAt(it) },
    )

    private fun firstNode(): TreapListNode<E> = left?.firstNode() ?: this
    private fun lastNode(): TreapListNode<E> = right?.lastNode() ?: this

    private fun leftIndex(parentIndex: Int) = parentIndex - 1 - (right?.size ?: 0)
    private fun rightIndex(parentIndex: Int) = parentIndex + 1 + (left?.size ?: 0)
    private fun rootIndex() = leftIndex(size)

    private fun indexOfFirstNode(thisIndex: Int, element: E): Int? =
        left?.indexOfFirstNode(left.leftIndex(thisIndex), element)
        ?: thisIndex.takeIf { this.elem == element }
        ?: right?.indexOfFirstNode(right.rightIndex(thisIndex), element)

    private fun indexOfLastNode(thisIndex: Int, element: E): Int? =
        right?.indexOfLastNode(right.rightIndex(thisIndex), element)
        ?: thisIndex.takeIf { this.elem == element }
        ?: left?.indexOfLastNode(left.leftIndex(thisIndex), element)

    private fun setNodeAt(index: Int, element: E): TreapListNode<E> = stepToIndex(
        index,
        found = { this.with(elem = element) },
        goLeft = { this.with(left = left!!.setNodeAt(it, element)) },
        goRight = { this.with(right = right!!.setNodeAt(it, element)) },
    )

    private fun removeAllNodes(predicate: (E) -> Boolean): TreapListNode<E>? = when {
        predicate(this.elem) -> left?.removeAllNodes(predicate) append right?.removeAllNodes(predicate)
        else -> this.with(left = left?.removeAllNodes(predicate), right = right?.removeAllNodes(predicate))
    }

    private fun updateNodes(transform: (E) -> E?): TreapListNode<E>? {
        val newLeft = left?.updateNodes(transform)
        val newElem = transform(elem)
        val newRight = right?.updateNodes(transform)
        return when {
            newElem == null -> newLeft append newRight
            else -> this.with(left = newLeft, right = newRight, elem = newElem)
        }
    }

    private fun updateNodesIndexed(thisIndex: Int, transform: (Int, E) -> E?): TreapListNode<E>? {
        val newLeft = left?.updateNodesIndexed(left.leftIndex(thisIndex), transform)
        val newElem = transform(thisIndex, elem)
        val newRight = right?.updateNodesIndexed(right.rightIndex(thisIndex), transform)
        return when {
            newElem == null -> newLeft append newRight
            else -> this.with(left = newLeft, right = newRight, elem = newElem)
        }
    }

    private fun forEachNode(action: (E) -> Unit) {
        left?.forEachNode(action)
        action(elem)
        right?.forEachNode(action)
    }

    private fun forEachNodeIndexed(thisIndex: Int, action: (Int, E) -> Unit) {
        left?.forEachNodeIndexed(left.leftIndex(thisIndex), action)
        action(thisIndex, elem)
        right?.forEachNodeIndexed(right.rightIndex(thisIndex), action)
    }


    override fun <R : Any> mapReduce(map: (E) -> R, reduce: (R, R) -> R): R =
        notForking(this) { mapReduceImpl(map, reduce) }

    override fun <R : Any> parallelMapReduce(map: (E) -> R, reduce: (R, R) -> R, parallelThresholdLog2: Int): R =
        maybeForking(this, threshold = { it.isApproximatelySmallerThanLog2(parallelThresholdLog2) }) {
            mapReduceImpl(map, reduce)
        }

    context(ThresholdForker<TreapListNode<E>>)
    private fun <R : Any> mapReduceImpl(map: (E) -> R, reduce: (R, R) -> R): R {
        val (left, middle, right) = fork(
            this,
            { left?.mapReduceImpl(map, reduce) },
            { map(elem) },
            { right?.mapReduceImpl(map, reduce) }
        )
        val leftAndMiddle = left?.let { reduce(it, middle) } ?: middle
        return right?.let { reduce(leftAndMiddle, it) } ?: leftAndMiddle
    }

    companion object {
        private infix fun <E> TreapListNode<E>?.append(that: TreapListNode<E>?): TreapListNode<E>? = when {
            this == null -> that
            that == null -> this
            else -> this.append(that)
        }

        /**
            Given a non-empty iterator, produces a TreapList from the elements in O(N) time.
         */
        fun <E> fromIterator(elems: Iterator<E>): TreapListNode<E> {

            class Result(val node: TreapListNode<E>, val hasNext: Boolean, val nextElem: E?, val nextPri: Int) {
                constructor(node: TreapListNode<E>) : this(node, false, null, 0)
                constructor(node: TreapListNode<E>, nextElem: E, nextPri: Int) : this(node, true, nextElem, nextPri)
            }

            /*
                Builds a TreapListNode containing consecutive elements pulled from [elems] until one is assigned a
                higher priority than [upperPri].  Returns the node and optionally the higher-priority element.
             */
            fun buildLowerPri(upperPri: Int, initialElem: E, initialPri: Int): Result {
                var thisNode = TreapListNode(initialElem, initialPri)

                // If there are no more elements, we're done
                if (!elems.hasNext()) {
                    return Result(thisNode)
                }

                // Start the next element, and loop until we find one with a higher priority than [upperPri]
                var nextElem = elems.next()
                var nextPri = Random.Default.nextInt()
                while (true) {
                    when {
                        nextPri > upperPri -> {
                            // Return this up the recursion stack; [thisNode] will end up on the left of the next node.
                            return Result(thisNode, nextElem, nextPri)
                        }
                        nextPri > thisNode.priority -> {
                            // The priority below the node above us, but above the current node.  Push the current node
                            // to the left
                            thisNode = TreapListNode(nextElem, nextPri, left = thisNode)
                            if (!elems.hasNext()) {
                                return Result(thisNode)
                            }
                            nextElem = elems.next()
                            nextPri = Random.Default.nextInt()
                        }
                        else -> {
                            // The priority is below the current node; construct a subtree on the right.
                            val lowerResult = buildLowerPri(thisNode.priority, nextElem, nextPri)
                            thisNode = thisNode.with(right = lowerResult.node)
                            if (!lowerResult.hasNext) {
                                return Result(thisNode)
                            }
                            @Suppress("unchecked_cast")
                            nextElem = lowerResult.nextElem as E
                            nextPri = lowerResult.nextPri
                        }
                    }
                }
            }

            // Build the whole list
            return buildLowerPri(Int.MAX_VALUE, elems.next(), Random.Default.nextInt()).node
        }

        internal tailrec fun <E> TreapListNode<E>?.isApproximatelySmallerThanLog2(sizeLog2: Int): Boolean = when {
            sizeLog2 < 0 -> throw IllegalArgumentException("sizeLog2 must be positive")
            this == null -> true
            sizeLog2 == 0 -> false
            else -> this.left.isApproximatelySmallerThanLog2(sizeLog2 - 1)
        }
    }
}
