package com.certora.collect

/**
    Provides properties and methods over Treap keys.  See `Treap` for an overview.  This is an interface rather than an
    abstract class so that it can be "mixed into" other classes with their own bases.

    A "treap key" is a value that locates a treap node in a treap.  The location has two coordinates: sort order, and
    priority.  Sort order is the position in the treap from left to right.  Priority is top to bottom.

    The treap key is related to, but not the same as, the key used for map entries or set elements.  For map/set keys
    that implement Comparable, the map/set key is used as the treap key directly.  The ordering defined by the key type
    determines the treap sort order; priority is determined by a hash of the key's hashCode.  For map/set keys that
    don't implement a total ordering via Comparable, we bucket keys by hash code.  Buckets sort from left to right
    according to their hashCode values; priorities are again assigned by hashing the hashCode.  Within the buckets, we
    use chaining to maintain distinct map/set keys.
 */
internal interface TreapKey<@Treapable K> {
    /**
        Get's the value of this key.
     */
    abstract val treapKey: K

    /**
        Compares two keys for treap sort order.  Returns:

            result < 0 : `this` should be to the left of `that`
            result > 0 : `this` should be to the right of `that`
            result == 0 : `this` and `that` appear in the same position in the treap.

        If result == 0, we require that the two keys have equal hashCodes as well, so that they will have the same
        priority in the treap.
     */
    abstract fun compareKeyTo(that: TreapKey<K>): Int

    open val treapKeyHashCode: Int get() = treapKey.hashCode()

    open val treapPriority: Int get() {
        // The goal is to produce a seemingly random number using the object's hash code as a seed.  We borrow the
        // proven hash finalization mixing function from MurmurHash: https://en.wikipedia.org/wiki/MurmurHash
        var h = treapKey.hashCode()
        h = h xor (h ushr 16)
        h = (h * 0x85ebca6b).toInt()
        h = h xor (h ushr 13)
        h = (h * 0xc2b2ae35).toInt()
        h = h xor (h ushr 16)
        return h
    }

    fun comparePriorityTo(that: TreapKey<K>) : Int {
        val thisPri = this.treapPriority
        val thatPri = that.treapPriority
        return when {
            thisPri < thatPri -> -1
            thisPri > thatPri -> 1
            else -> this.compareKeyTo(that)
        }
    }

    /**
        Produces an equivalent TreapKey with the hashes precomputed.  Used in Treap.add for extra speed.
     */
    abstract fun precompute(): TreapKey<K>

    /**
        A TreapKey whose underlying key implement Comparable.  This allows us to sort the Treap naturally.
     */
    interface Sorted<@Treapable K : Comparable<K>> : TreapKey<K> {
        abstract override val treapKey: K

        // Note that we must never compare a Hashed key with a Sorted key.  We'd check that here, but this is extremely
        // perf-critical code.
        override fun compareKeyTo(that: TreapKey<K>) = this.treapKey.compareTo(that.treapKey)

        override fun precompute() = FromKey(treapKey)

        class FromKey<@Treapable K : Comparable<K>>(override val treapKey: K) : Sorted<K> {
            override val treapPriority = super.treapPriority // precompute the priority
        }
    }

    /**
        A TreapKey whose underlying key is not Comparable.  We have to use its hash code instead.  Derived classes will
        need to deal with hash collisions.
     */
    interface Hashed<@Treapable K> : TreapKey<K> {
        abstract override val treapKey: K

        // Note that we must never compare a Hashed key with a Sorted key.  We'd check that here, but this is extremely
        // perf-critical code. On that note, it might be tempting to just subtract the hashes here, but that doesn't
        // work due to integer over/underflow.
        override fun compareKeyTo(that: TreapKey<K>): Int {
            val thisHash = this.treapKeyHashCode
            val thatHash = that.treapKeyHashCode
            return when {
                thisHash < thatHash -> -1
                thisHash > thatHash -> 1
                else -> 0
            }
        }

        override fun precompute() = FromKey(treapKey)

        class FromKey<@Treapable K>(override val treapKey: K) : Hashed<K> {
            override val treapKeyHashCode = treapKey.hashCode() // precompute the hash code
            override val treapPriority = super.treapPriority // precompute the priority
        }
    }
}
