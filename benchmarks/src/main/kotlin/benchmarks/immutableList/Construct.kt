package benchmarks.immutableList

import benchmarks.*
import kotlinx.collections.immutable.*
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class Construct {
    @Param(KOTLIN_IMPL, TREAP_IMPL, JAVA_IMPL)
    var implementation = ""

    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000, BM_10000000)
    var size: Int = 0

    var toAdd = listOf<Int>()

    @Setup
    fun prepare() {
        toAdd = (1..size).toList()
    }

    @Benchmark
    fun oneAtATime(): ImmutableList<Int> {
        var list = emptyPersistentList<Int>(implementation)
        toAdd.forEach {
            list = list.add(it)
        }
        return list
    }

    @Benchmark
    fun addAll(): ImmutableList<Int> {
        var list = emptyPersistentList<Int>(implementation)
        list = list.addAll(toAdd)
        return list
    }
}
