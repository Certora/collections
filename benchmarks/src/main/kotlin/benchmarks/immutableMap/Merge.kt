package benchmarks.immutableMap

import benchmarks.*
import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlinx.benchmark.*
import kotlinx.collections.immutable.persistentMapOf

@State(Scope.Benchmark)
open class Merge {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

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
    fun mergeEqualSize() = lhs.merge(rhs) { _, a, b -> a ?: b }

    @Benchmark
    fun mergeSmallIntoLarge() = lhs.merge(rhsSmall) { _, a, b -> a ?: b }

    @Benchmark
    fun mergeLargeIntoSmall() = lhsSmall.merge(rhs) { _, a, b -> a ?: b }
}
