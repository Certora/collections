package benchmarks.immutableMap

import benchmarks.*
import com.certora.collect.*
import kotlinx.benchmark.*
import kotlinx.collections.immutable.persistentMapOf

@State(Scope.Benchmark)
open class ParallelMerge {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    @Param("0", "100000")
    var parallelWork: Int = 0

    private var lhs = treapMapOf<IntWrapper, String>()
    private var lhsSmall = treapMapOf<IntWrapper, String>()
    private var rhs = treapMapOf<IntWrapper, String>()
    private var rhsSmall = treapMapOf<IntWrapper, String>()

    @Setup
    fun prepare() {
        val keys = generateKeys(hashCodeType, 2 * size)
        lhs = treapMapPut(keys.take(size))
        lhsSmall = treapMapPut(keys.take((size / 1000) + 1))
        rhs = treapMapPut(keys.takeLast(size))
        rhsSmall = treapMapPut(keys.takeLast((size / 1000) + 1))
    }

    @Benchmark
    fun nonParallel(sink: Blackhole) = lhs.merge(rhs) { _, a, b -> parallelWork(sink, a, b) }

    @Benchmark
    fun parallel(sink: Blackhole) = lhs.parallelMerge(rhs) { _, a, b -> parallelWork(sink, a, b) }

    private fun <V> parallelWork(sink: Blackhole, a: V?, b: V?): V? {
        repeat(parallelWork) { sink.consume(it) }
        return a ?: b
    }
}
