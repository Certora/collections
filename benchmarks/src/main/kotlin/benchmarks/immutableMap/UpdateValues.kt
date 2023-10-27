package benchmarks.immutableMap

import benchmarks.*
import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlinx.benchmark.*
import kotlinx.collections.immutable.persistentMapOf

@State(Scope.Benchmark)
open class UpdateValues {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param("0.0", "20.0", "100.0")
    var updatePercentage: Double = 0.0

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var map = treapMapOf<IntWrapper, String>()
    private var updateKeys = setOf<IntWrapper>()

    @Setup
    fun prepare() {
        val random = kotlin.random.Random(42)
        val keys = generateKeys(hashCodeType, size)
        updateKeys = keys.filter { random.nextDouble() < updatePercentage / 100 }.toSet()
        map = treapMapPut(generateKeys(hashCodeType, size))
    }

    @Benchmark
    fun update() = map.updateValues { k, v -> if (k in updateKeys) v + "updated" else v }
}
