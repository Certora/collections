/*
 * Modified from the kotlinx.collections.immutable sources, which contained the following notice:
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks

open class ObjectWrapper<K: Comparable<K>>(
        val obj: K,
        val hashCode: Int
) {
    override fun hashCode(): Int {
        return hashCode
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ObjectWrapper<*>) {
            return false
        }
        return obj == other.obj
    }
}

class ComparableObjectWrapper<K : Comparable<K>>(
        obj: K,
        hashCode: Int
) : ObjectWrapper<K>(obj, hashCode), Comparable<ObjectWrapper<K>> {
    override fun compareTo(other: ObjectWrapper<K>): Int {
        return obj.compareTo(other.obj)
    }
}

typealias IntWrapper = ObjectWrapper<Int>
typealias ComparableIntWrapper = ComparableObjectWrapper<Int>
