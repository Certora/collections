/*
 * Modified from the kotlinx.collections.immutable sources, which contained the following notice:
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.*
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class Add {
    @Param(KOTLIN_IMPL, TREAP_IMPL, JAVA_IMPL)
    var implementation = ""

    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    var initial = persistentListOf<String>()

    @Setup
    fun prepare() {
        initial = persistentListAdd(implementation, size)
    }

    @Benchmark
    fun addLast(): ImmutableList<String> {
        return initial.add("another element")
    }

    /**
     * Adds [size] - 1 elements to an empty persistent list
     * and then inserts one element at the beginning.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun addFirst(): ImmutableList<String> {
        return initial.add(0, "another element")
    }

    /**
     * Adds [size] - 1 elements to an empty persistent list
     * and then inserts one element at the middle.
     *
     * Measures mean time and memory spent per `add` operation.
     *
     * Expected time: nearly constant.
     * Expected memory: nearly constant.
     */
    @Benchmark
    fun addMiddle(): ImmutableList<String> {
        return initial.add(initial.size / 2, "another element")
    }
}
