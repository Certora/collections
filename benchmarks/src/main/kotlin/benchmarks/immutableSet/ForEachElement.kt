package benchmarks.immutableSet

import benchmarks.*
import com.certora.collect.*
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class ForEachElement {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var treapSet = treapSetOf<IntWrapper>()

    @Setup
    fun prepare() {
        treapSet = treapSetAdd(generateElements(hashCodeType, size))
    }

    @Benchmark
    fun iterate(bh: Blackhole) {
        treapSet.forEachElement {
            bh.consume(it)
        }
    }
}
