package com.certora.common.collect.impl

import parallel.forkjoin.*

/*
    Treaps are probabilistically balanced binary search trees.  We use these to implement efficient persistent
    Set and Map data structures.  For background reading, see:

    "Randomized Search Trees", Seidel and Aragon, 1996:
        https://www.researchgate.net/publication/221498261_Randomized_Search_Trees

    "Fast Set Operations Using Treaps", Belloch and Reid-Miller, 1998:
        https://www.cs.cmu.edu/~scandal/papers/treaps-spaa98.pdf

    A Treap is a binary search tree (BST) where each node has two properties: its "key" value, and its "priority."
    Keys are organized according to the usual BST rule (lower values on the left, higher on the right).  To balance
    the tree, nodes are additionally organized by priority, with higher-priority nodes appearing "above" lower-
    priority nodes, a la heaps.  Priorities are assigned "randomly" (more on that later), so that the tree ends up
    probabilistically balanced, such that the expected depth of the tree is O(log(N)).

    In the original Treap paper, the idea was to assign nodes random priorities with an RNG.  However, in the footnotes
    it was suggested that a hash of the node's key could be used instead.  We have taken this suggestion.  Also, when
    comparing two nodes' priorities, we break ties by comparing the nodes' keys.  Thus we have a total ordering
    on priority.  This priority scheme - hashing plus total ordering - has a couple of huge advantages: First, it allows
    us to balance the tree without storing any additional data, since we can recompute the priorities on-demand from the
    key values.  Second, it means that two treaps containing the same keys, and balanced with the same hash function,
    will have identical shapes.  This makes set operators more efficient, and allows for efficient "merging" of map
    data.

    We also modify the Set operations from Belloch and Reid-Miller, to add an additional invariant that Set operations
    whose result is equal to the left hand side of the operation, return the same Set reference that was passed to the
    LHS of the operator.  E.g., (A union B) === A, if B is a subset of A.  This allows us to efficiently detect
    modification of sets, which has performance benefits all over the place, and it allows greater re-use of the memory
    already allocated.

    As a BST, Treaps are well-suited to keys with a defined total ordering.  For values which implement Comparable,
    we use the value itself as the Treap key.  However, we need to be able to use arbitrary types as elements of Set,
    or as keys in Maps.  For types that do not implement Comparable, we use the object's hashCode() function to generate
    a Treap key.  We thus sort all elements/keys by their hash codes.  We resolve collisions by chaining off of the
    Treap node.  This results in a bifurcation of Set and Map implementations, into SortedTreapSet and HashTreapSet,
    SortedTreapMap and HashTreapMap.  To make this more manageable at use sites, we We provide AutoPersistentSet and
    AutoPersistentMap, which are empty set/map objects that expand into the appropriate sorted/hashed structure based
    on the actual key types encountered.

    Some other notes:

    - We save quite a bit of memory by deriving our Map and Set classes directly from Treap, rather han encapsulating
      the Treap node objects.  This means that Treap instances are exposed directly to code using Set and Map objects.
      This is why these are in their own package (so that they're less visible).  It's also important that Treap
      not implement any public interfaces that the Map and Set objects should not implement.  For example, it might
      be tempting to make Treap implement Comparable, but that would make all derived Set and Map types comparable,
      which is probably not wise.

    - All Treap instances must implement TreapKey, allowing them to be used in key comparisons.  Other objects also
      implement TreapKey, for example when adding a new element to a set, we allocate a new, temporary, TreapKey
      with a precomputed priority, to speed up priority computations.

    - To ensure "persistence" of these data structures, all instances of Treap are deeply immutable.  "mutation" is
      always done by making a new instance.  We go to lengths to avoid this, and also to throw out any copies if they
      turn out to be unnecessary, so that we maximize our chances of reusing existing memory allocations, in the hope
      of keeping memory small.  Typically a union of two Sets, for example, will reuse a substantial portion of both
      sets.

    - Treap instances are Java-serializable, but not Kotlin-serializable.  The JVM object serialization preserves object
      identity, which is very important because we rely heavily on object sharing to reduce memory usage.  Kotlin
      serialization to, say, JSON, would result in separate copies of all Treap nodes that are shared between different
      Treaps.  On deserialization this would expand to much larger memory usage, as the object sharing would be lost.
      It would be very easy to serialize data structures that could not be deserialized into physical memory.  We do
      have PersistentSetSerializer and PersistentMapSerializer availble, but these must be used explicitly, with
      explicit opt-in, in cases where memory consumption is not a problem.

    - Most Treap operations are written as extension methods, so that they can deal with null "this" values.  This
      makes things much more convenient, but is a bit odd, and is another reason this stuff is isolated in its own
      package.

    - We do some tricks with @JvmField and "inline" annotations to improve performance.  These are based on profiling
      data; please don't modify them without profile data that shows no significant regressions.

    - Most Treap operations are defined recursively.  This should not lead to stack overflow, as Treaps are very likely
      to be mostly balanced.  The expected depth of a Treap is O(log2(N)), so we don't expect to see Treaps that are
      much more than, say, 64 nodes deep.

    - We take advantage of the total ordering we impose on Treap priorities, to avoid additional key comparisons.
      We never need to check if two keys are equal, if their nodes' priorities have already been compared.
*/
internal abstract class Treap(
    @JvmField val left: Treap?,
    @JvmField val right: Treap?
) : TreapKey, java.io.Serializable {

    /**
     * Produces a copy of this node, but with differen left and/or right nodes.
     */
    @Suppress("NOTHING_TO_INLINE")
    inline fun with(left: Treap? = this.left, right: Treap? = this.right): Treap = when {
        left === this.left && right === this.right -> this
        else -> copyWith(left, right)
    }

    fun getSingleElement(): Any? = when {
        left === null && right === null -> shallowGetSingleElement()
        else -> null
    }

    /**
     * Derived classes make an appropriate copy of themselves, with the specified right/left nodes.
     */
    abstract fun copyWith(left: Treap?, right: Treap?): Treap

    /**
     * Derived classes use this to compare key/value data appropriately.  For example, hashed sets and maps
     * need to compare all keys/values with the same hash code.
     */
    abstract fun shallowEquals(that: Treap): Boolean

    /**
     * Derived classes use this to report the number of logical items stored in this node due to hash collisions.
     */
    abstract val shallowSize: Int

    /**
     * Derived classes use these to perform these operations in the face of hash collisions.
     */
    abstract infix fun shallowAdd(that: Treap): Treap
    abstract infix fun shallowUnion(that: Treap): Treap
    abstract infix fun shallowDifference(that: Treap): Treap?
    abstract infix fun shallowIntersect(that: Treap): Treap?
    abstract fun shallowContainsAll(elements: Treap): Boolean
    abstract fun shallowContainsAny(elements: Treap): Boolean
    abstract fun shallowRemove(element: Any?): Treap?
    abstract fun shallowRemoveEntry(key: Any?, value: Any?): Treap?
    abstract fun shallowRemoveAll(predicate: (Any?) -> Boolean): Treap?
    abstract fun shallowComputeHashCode(): Int
    abstract fun shallowGetSingleElement(): Any?

    /**
     * This gets called by the JVM deserializer, after deserialization.  It's our chance to validate our invariants,
     * in case any hash functions have changed since this Treap was serialized.
     */
    protected fun readResolve(): Any? {
        if (left != null) {
            check(left.compareKeyTo(this) < 0) { "Treap key comparison logic changed: ${left.treapKey} >= ${this.treapKey}" }
            check(left.comparePriorityTo(this) < 0) { "Treap key priority hash logic changed: ${left.treapKey} >= ${this.treapKey} "}
        }
        if (right != null) {
            check(right.compareKeyTo(this) > 0) { "Treap key comparison logic changed: ${right.treapKey} <= ${this.treapKey}" }
            check(right.comparePriorityTo(this) < 0) { "Treap key priority hash logic changed: ${right.treapKey} >= ${this.treapKey} "}
        }
        return this
    }

    abstract fun shallowUpdate(entryKey: Any?, toUpdate: Any?, merger: (Any?, Any?) -> Any?): Treap?
}

/**
 * Splits this treap into two treaps, one with keys less than `key`, and one greater.  Returns both,
 * and if there was a node with the same key, returns that too.  This is a basic building block of other Treap
 * operations.
 */
internal fun Treap?.split(key: TreapKey): Split = when {
    this == null -> Split(left = null, right = null, duplicate = null)
    else -> {
        val c = this.compareKeyTo(key)
        when {
            c > 0 -> {
                // Split our left.  The RHS of that split is greater than `key`, and less than `this`.  Insert `this`
                // as the new RHS of the split.
                left.split(key).also { it.right = this.with(left = it.right) }
            }
            c < 0 -> {
                // Split our right.  The LHS of that split is less than `key`, and greater than `this`.  Insert `this`
                // as the new LHS of the split.
                right.split(key).also { it.left = this.with(right = it.left) }
            }
            else -> {
                // The keys are equal.  Both keys have the same Treap coordinates (priority and sort order), so there's
                // nothing to split.
                Split(left = left, right = right, duplicate = this)
            }
        }
    }
}
internal class Split(var left: Treap?, var right: Treap?, var duplicate: Treap?)


/**
 * Converse of `split.`  Combines this treap with another treap whose keys are all greater than any key in this treap.
 * Another basic building block.
 */
internal infix fun Treap?.join(greater: Treap?): Treap? = when {
    this == null -> greater
    greater == null -> this
    this.comparePriorityTo(greater) < 0 -> greater.with(left = this join greater.left)
    else -> this.with(right = this.right join greater) // note that the priorities will never be equal, because the keys can't be equal.
}


/**
 * Adds a single Treap node to this Treap, if its key does not already exist.  We precompute the key hashes for extra
 * speed.  To add multiple nodes, use `union`.
 */
internal fun Treap?.add(that: Treap): Treap = add(that, that.precompute())


internal fun Treap?.updateEntry(thatKey: TreapKey, entryKey: Any?, toUpdate: Any?, merger: (Any?, Any?) -> Any?, generate: (Any?, Any?) -> Treap): Treap? = when {
    this == null -> {
        val generated = merger(null, toUpdate)
        if(generated == null) {
            null
        } else {
            generate(entryKey, generated)
        }
    }
    else -> {
        val c = thatKey.comparePriorityTo(this)
        when {
            c == 0 -> this.shallowUpdate(entryKey, toUpdate, merger) ?: (left join right)
            c > 0 -> {
                val merged = merger(null, toUpdate)
                if(merged == null) {
                    this
                } else {
                    val newRoot = generate(entryKey, merged)
                    this.split(thatKey).let { split ->
                        newRoot.with(left = split.left, right = split.right)
                    }
                }
            }
            thatKey.compareKeyTo(this) < 0 -> this.with(left = this.left.updateEntry(thatKey, entryKey, toUpdate, merger, generate))
            else -> this.with(right = this.right.updateEntry(thatKey, entryKey, toUpdate, merger, generate))
        }
    }
}

private fun Treap?.add(that: Treap, thatKey: TreapKey): Treap = when {
    that.left != null || that.right != null -> throw IllegalArgumentException("add requires a single treap node")
    this == null -> that
    else -> {
        // remember, a.comparePriorityTo(b)==0 <=> a.compareKeyTo(b)==0
        // and therefore if priorities are different, then there will be no duplicate from `split`
        val c = thatKey.comparePriorityTo(this)
        when {
            c == 0 -> this shallowAdd that
            c > 0 -> this.split(thatKey).let { split -> that.with(split.left, split.right) }
            thatKey.compareKeyTo(this) < 0 -> this.with(left = this.left.add(that, thatKey))
            else -> this.with(right = this.right.add(that, thatKey))
        }
    }
}

/**
 * Removes `element` with key `key`
 */
internal fun Treap?.remove(key: TreapKey, element: Any?): Treap? = when {
    this == null -> null
    key.comparePriorityTo(this) > 0 -> this
    else -> {
        val c = key.compareKeyTo(this)
        when {
            c < 0 -> this.with(left = left.remove(key, element))
            c > 0 -> this.with(right = right.remove(key, element))
            else -> this.shallowRemove(element) ?: (this.left join this.right)
        }
    }
}

/**
 * Removes a map entry (`entryKey`, `entryValue`) with key `key`
 */
internal fun Treap?.removeEntry(key: TreapKey, entryKey: Any?, entryValue: Any?): Treap? = when {
    this == null -> null
    key.comparePriorityTo(this) > 0 -> this
    else -> {
        val c = key.compareKeyTo(this)
        when {
            c < 0 -> this.with(left = left.removeEntry(key, entryKey, entryValue))
            c > 0 -> this.with(right = right.removeEntry(key, entryKey, entryValue))
            else -> this.shallowRemoveEntry(entryKey, entryValue) ?: (this.left join this.right)
        }
    }
}

/**
 * Computes the union of two treaps.  When nodes have equal keys, they are combined by calling `shallowUnion`, which
 * derived classes use to, e.g, merge hash buckets. Note that we always prefer to return 'this' over 'that', to
 * preserve the object identity invariant described in the `Treap` summary.
 */
internal infix fun Treap?.union(that: Treap?): Treap? = when {
    this == null -> that
    that == null -> this
    this === that -> this
    that.getSingleElement() != null -> add(that)
    else -> {
        // remember, a.comparePriorityTo(b)==0 <=> a.compareKeyTo(b)==0
        val c = this.comparePriorityTo(that)
        when {
            c > 0 -> unionMerge(this, that)
            c < 0 -> unionMerge(that, this)
            else -> unionMerge(this shallowUnion that, that)
        }
    }
}

private fun unionMerge(higher: Treap, lower: Treap) =
    // Note that the "higher" key can not occur in "lower", because if it did it wouldn't have a higher priority.
    // We don't need to worry about the split's `duplicate` field.
    lower.split(higher).let { lowerSplit ->
        higher.with(higher.left union lowerSplit.left, higher.right union lowerSplit.right)
    }

/**
 * Merges two treaps, using a supplied merge function.  This is used to implement our Map<K, V>.merge() function.  It's
 * distinct from `union` because it needs to call the merge function even in cases where the key only exists in
 * one of the Treaps, to support the semantics of the higher-level Map.merge() function.  Note that we always prefer to
 * return 'this' over 'that', to preserve the object identity invariant described in the `Treap` summary.
 */
internal fun Treap?.mergeWith(that: Treap?, shallowMerge: (Treap?, Treap?) -> Treap?): Treap? =
    notForking(this to that) {
        mergeWithImpl(that, shallowMerge)
    }

internal fun Treap?.parallelMergeWith(that: Treap?, parallelThresholdLog2: Int, shallowMerge: (Treap?, Treap?) -> Treap?): Treap? =
    maybeForking(
        this to that,
        {
            it.first.isApproximatelySmallerThanLog2(parallelThresholdLog2 - 1) &&
            it.second.isApproximatelySmallerThanLog2(parallelThresholdLog2 - 1)
        }
    ) {
        mergeWithImpl(that, shallowMerge)
    }

context(ThresholdForker<Pair<Treap?, Treap?>>)
private fun Treap?.mergeWithImpl(that: Treap?, shallowMerge: (Treap?, Treap?) -> Treap?): Treap? {
    val (newLeft, newRight, newThis) = when {
        this == null && that == null -> {
            return null
        }
        this == null || that == null -> {
            fork(
                this to that,
                { this?.left.mergeWithImpl(that?.left, shallowMerge) },
                { this?.right.mergeWithImpl(that?.right, shallowMerge) },
                { shallowMerge(this, that) }
            )
        }
        this.comparePriorityTo(that) >= 0 -> {
            val thatSplit = that.split(this)
            fork(
                this to that,
                { this.left.mergeWithImpl(thatSplit.left, shallowMerge) },
                { this.right.mergeWithImpl(thatSplit.right, shallowMerge) },
                { shallowMerge(this, thatSplit.duplicate) }
            )
        }
        else -> {
            // remember, a.comparePriorityTo(b)==0 <=> a.compareKeyTo(b)==0
            val thisSplit = this.split(that)
            fork(
                this to that,
                { thisSplit.left.mergeWithImpl(that.left, shallowMerge) },
                { thisSplit.right.mergeWithImpl(that.right, shallowMerge) },
                { shallowMerge(thisSplit.duplicate, that) }
            )
        }
    }
    return newThis?.with(newLeft, newRight) ?: (newLeft join newRight)
}

/**
 * Computes the intersection of two treaps. Note that we always prefer to return 'this' over 'that', to preserve the
 * object identity invariant described in the `Treap` summary.
 */
internal infix fun Treap?.intersect(that: Treap?): Treap? = when {
    this == null -> null
    that == null -> null
    this === that -> this
    else -> {
        // remember, a.comparePriorityTo(b)==0 <=> a.compareKeyTo(b)==0
        val c = this.comparePriorityTo(that)
        when {
            c > 0 -> intersectMerge(this, that)
            c < 0 -> intersectMerge(that, this)
            else -> {
                val newLeft = this.left intersect that.left
                val newRight = this.right intersect that.right
                val newThis = this.shallowIntersect(that)
                when {
                    newThis == null -> newLeft join newRight
                    else -> newThis.with(newLeft, newRight)
                }
            }
        }
    }
}

private fun intersectMerge(higher: Treap, lower: Treap) =
    // Note that the "higher" key can not occur in "lower", because if it did it wouldn't have a higher priority.
    // We don't need to worry about the split's `duplicate` field.
    lower.split(higher).let { lowerSplit ->
        (higher.left intersect lowerSplit.left) join (higher.right intersect lowerSplit.right)
    }

/**
 * Removes the items in `that` from `this`.
 */
internal infix fun Treap?.difference(that: Treap?): Treap? = when {
    this == null -> null
    that == null -> this
    this === that -> null
    else -> {
        val thatSplit = that.split(this)
        val newLeft = this.left difference thatSplit.left
        val newRight = this.right difference thatSplit.right
        val newThis = when {
            thatSplit.duplicate == null -> this
            else -> this.shallowDifference(thatSplit.duplicate!!)
        }
        when {
            newThis == null -> newLeft join newRight
            else -> newThis.with(newLeft, newRight)
        }
    }
}

internal infix fun Treap?.removeAll(predicate: (Any?) -> Boolean): Treap? = when {
    this == null -> null
    else -> {
        val newThis = this.shallowRemoveAll(predicate)
        val newLeft = left.removeAll(predicate)
        val newRight = right.removeAll(predicate)
        newThis?.with(newLeft, newRight) ?: (newLeft join newRight)
    }
}

/**
 * Counts the items stored in this Treap.
 */
internal fun Treap?.computeSize(): Int = when {
    this == null -> 0
    else -> shallowSize + left.computeSize() + right.computeSize()
}

/**
 * Finds a given key in this Treap, and returns the Treap node.  Takes advantage of tail-recursion for speed.
 */
internal tailrec fun Treap?.find(key: TreapKey): Treap? = when {
    this == null -> null
    this.comparePriorityTo(key) < 0 -> null
    else -> {
        val c = key.compareKeyTo(this)
        when {
            c < 0 -> left.find(key)
            c > 0 -> right.find(key)
            else -> this
        }
    }
}

internal fun Treap?.containsKey(key: TreapKey): Boolean = (find(key) != null)

/**
 * Checks if this Treap contains all items in another Treap.  This should be equivalent to:
 *
 *   (this union that) === this
 *
 * ...except that we don't want to do all of the work that would imply, if we can avoid it.
 */
internal fun Treap?.containsAll(that: Treap?): Boolean = when {
    that == null -> true
    this == null -> false
    else -> {
        this.split(that).let {
            it.duplicate?.shallowContainsAll(that) == true &&
            it.left.containsAll(that.left) &&
            it.right.containsAll(that.right)
        }
    }
}

/**
 * Checks if this Treap contains any of the items in another Treap.  This should be equivalent to:
 *
 *   (this difference that) !== this
 *
 * ...except that we don't want to do all of the work that would imply, if we can avoid it.
 */
internal fun Treap?.containsAny(that: Treap?): Boolean = when {
    that == null -> false
    this == null -> false
    else -> {
        that.split(this).let {
            it.duplicate?.let { this.shallowContainsAny(it) } == true ||
            this.left.containsAny(it.left) ||
            this.right.containsAny(it.right)
        }
    }
}

/**
 * Produces a sequence of all nodes in this Treap, which we use at a higher level to enumerate elements/entries.
 */
internal fun Treap?.asSequence(): Sequence<Treap> = when {
    this == null -> sequenceOf<Treap>()
    else -> sequence<Treap> {
        val stack = ArrayDeque<Treap>()
        var current: Treap? = this@asSequence
        while (stack.isNotEmpty() || current != null) {
            if (current != null) {
                stack.addFirst(current)
                current = current.left
            } else {
                current = stack.removeFirst()
                yield(current)
                current = current.right
            }
        }
    }
}

/**
 * Compares two treaps for equality, according to the derived class' definition.
 */
internal fun Treap?.deepEquals(that: Treap?): Boolean = when {
    this === that -> true
    this == null -> that == null
    that == null -> false
    this.compareKeyTo(that) != 0 -> false
    !this.shallowEquals(that) -> false
    else -> this.left.deepEquals(that.left) && this.right.deepEquals(that.right)
}

internal fun Treap?.computeHashCode(): Int = when {
    this == null -> 0
    else -> shallowComputeHashCode() + left.computeHashCode() + right.computeHashCode()
}

/**
  Quickly estimates if this Treap is smaller than a given size, without actually counting the nodes.  Probes the depth
  along a single path, under the assumption that the tree is balanced.
 */
internal tailrec fun Treap?.isApproximatelySmallerThanLog2(sizeLog2: Int): Boolean = when {
    this == null -> true
    sizeLog2 <= 0 -> false
    else -> this.left.isApproximatelySmallerThanLog2(sizeLog2 - 1)
}
