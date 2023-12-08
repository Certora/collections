/*
 * Modified from the kotlinx.collections.immutable sources, which contained the following notice:
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class AddAll {
    @Param(KOTLIN_IMPL, TREAP_IMPL, JAVA_IMPL)
    var implementation = ""

    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    private var initialHalf = persistentListOf<String>()
    private var initialTwoThirds = persistentListOf<String>()

    private var listToAdd = emptyList<String>()
    private var halfList = emptyList<String>()
    private var oneThirdList = emptyList<String>()

    @Setup
    fun prepare() {
        listToAdd = persistentListAdd(implementation, size)
        halfList = persistentListAdd(implementation, size / 2)
        initialHalf = persistentListAdd(implementation, size - halfList.size)
        oneThirdList = persistentListAdd(implementation, size / 3)
        initialTwoThirds = persistentListAdd(implementation, size - oneThirdList.size)
    }

    // Results of the following benchmarks do not indicate memory or time spent per operation,
    // however regressions there do indicate changes.
    //
    // the benchmarks measure mean time and memory spent per added element.
    //
    // Expected time: nearly constant.
    // Expected memory: nearly constant.

    /**
     * Adds [size] elements to an empty persistent list using `addAll` operation.
     */
    @Benchmark
    fun addAllLast(): ImmutableList<String> {
        return emptyPersistentList<String>(implementation).addAll(listToAdd)
    }

    /**
     * Adds `size / 2` elements to an empty persistent list
     * and then adds `size - size / 2` elements using `addAll` operation.
     */
    @Benchmark
    fun addAllLast_Half(): ImmutableList<String> {
        return initialHalf.addAll(halfList)
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list
     * and then adds `size / 3` elements using `addAll` operation.
     */
    @Benchmark
    fun addAllLast_OneThird(): ImmutableList<String> {
        return initialTwoThirds.addAll(oneThirdList)
    }

    /**
     * Adds `size / 2` elements to an empty persistent list
     * and then inserts `size - size / 2` elements at the beginning using `addAll` operation.
     */
    @Benchmark
    fun addAllFirst_Half(): ImmutableList<String> {
        return initialHalf.addAll(0, halfList)
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list
     * and then inserts `size / 3` elements at the beginning using `addAll` operation.
     */
    @Benchmark
    fun addAllFirst_OneThird(): ImmutableList<String> {
        return initialTwoThirds.addAll(0, oneThirdList)
    }

    /**
     * Adds `size / 2` elements to an empty persistent list
     * and then inserts `size - size / 2` elements at the middle using `addAll` operation.
     */
    @Benchmark
    fun addAllMiddle_Half(): ImmutableList<String> {
        return initialHalf.addAll(initialHalf.size / 2, halfList)
    }

    /**
     * Adds `size - size / 3` elements to an empty persistent list builder
     * and then inserts `size / 3` elements at the middle using `addAll` operation.
     */
    @Benchmark
    fun addAllMiddle_OneThird(): ImmutableList<String> {
        return initialTwoThirds.addAll(initialTwoThirds.size / 2, oneThirdList)
    }
}
