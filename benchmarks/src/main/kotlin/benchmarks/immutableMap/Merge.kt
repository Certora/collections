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

    @Param(HAMT_IMPL, TREAP_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE)
    var hashCodeType = ""

    private var lhs = persistentMapOf<IntWrapper, String>()
    private var lhsSmall = persistentMapOf<IntWrapper, String>()
    private var rhs = persistentMapOf<IntWrapper, String>()
    private var rhsSmall = persistentMapOf<IntWrapper, String>()

    @Setup
    fun prepare() {
        val keys = generateKeys(hashCodeType, 2 * size)
        lhs = persistentMapPut(implementation, keys.take(size))
        lhsSmall = persistentMapPut(implementation, keys.take((size / 1000) + 1))
        rhs = persistentMapPut(implementation, keys.takeLast(size))
        rhsSmall = persistentMapPut(implementation, keys.takeLast((size / 1000) + 1))
    }

    @Benchmark
    fun mergeEqualSize() = lhs.merge(rhs) { _, a, b -> a ?: b }

    @Benchmark
    fun mergeSmallIntoLarge() = lhs.merge(rhsSmall) { _, a, b -> a ?: b }

    @Benchmark
    fun mergeLargeIntoSmall() = lhsSmall.merge(rhs) { _, a, b -> a ?: b }

    private fun <K, V> Map<K, V>.merge(m: Map<K, V>, merger: (K, V?, V?) -> V?): Map<K, V> = when(this) {
        is TreapMap<*, *> -> (this as TreapMap<K, V>).merge(m, merger)
        else -> mutableMapOf<K, V>().also { result ->
            this.forEach { (k, v) ->
                when (val vv = merger(k, v, m[k])) {
                    null -> {}
                    else -> result[k] = vv
                }
            }
            m.forEach { (k, v) ->
                if (k !in this) {
                    when (val vv = merger(k, this[k], v)) {
                        null -> {}
                        else -> result[k] = vv
                    }
                }
            }
        }
    }
}
