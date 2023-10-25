package benchmarks.immutableSet

import benchmarks.*
import kotlinx.collections.immutable.*
import kotlinx.benchmark.*

@State(Scope.Benchmark)
open class SetOperators {
    @Param(BM_1, BM_10, BM_100, BM_1000, BM_10000, BM_100000, BM_1000000)
    var size: Int = 0

    @Param(HAMT_IMPL, TREAP_IMPL)
    var implementation = ""

    @Param(ASCENDING_HASH_CODE, RANDOM_HASH_CODE, COLLISION_HASH_CODE, NON_EXISTING_HASH_CODE)
    var hashCodeType = ""

    private var original = persistentSetOf<IntWrapper>()
    private var disjoint = persistentSetOf<IntWrapper>()
    private var equal = persistentSetOf<IntWrapper>()
    private var subset = persistentSetOf<IntWrapper>()
    private var subsetFromOriginal = persistentSetOf<IntWrapper>()
    private var superset = persistentSetOf<IntWrapper>()
    private var supersetFromOriginal = persistentSetOf<IntWrapper>()
    private var intersecting = persistentSetOf<IntWrapper>()
    private var intersectingFromOriginal = persistentSetOf<IntWrapper>()

    @Setup
    fun prepare() {
        val elements = generateElements(hashCodeType, size)
        val disjointElements = generateElements(hashCodeType, size)

        original = persistentSetAdd(implementation, elements)
        disjoint = persistentSetAdd(implementation, disjointElements)
        equal = persistentSetAdd(implementation, elements)

        subset = persistentSetAdd(implementation, elements.subList(0, size / 2))
        subsetFromOriginal = original - subset

        superset = persistentSetAdd(implementation, elements + disjointElements)
        supersetFromOriginal = original + disjoint

        intersecting = persistentSetAdd(implementation, elements.subList(0, size / 2) + disjointElements)
        intersectingFromOriginal = (original - subset) + disjoint
    }

    @Benchmark fun unionDisjoint() = original union disjoint
    @Benchmark fun unionEqual() = original union equal
    @Benchmark fun unionSubset() = original union subset
    @Benchmark fun unionSubsetFromOriginal() = original union subsetFromOriginal
    @Benchmark fun unionSuperset() = original union superset
    @Benchmark fun unionSupersetFromOriginal() = original union superset
    @Benchmark fun unionIntersecting() = original union intersecting
    @Benchmark fun unionIntersectingFromOriginal() = original union intersectingFromOriginal

    @Benchmark fun intersectDisjoint() = original intersect disjoint
    @Benchmark fun intersectEqual() = original intersect equal
    @Benchmark fun intersectSubset() = original intersect subset
    @Benchmark fun intersectSubsetFromOriginal() = original intersect subsetFromOriginal
    @Benchmark fun intersectSuperset() = original intersect superset
    @Benchmark fun intersectSupersetFromOriginal() = original intersect superset
    @Benchmark fun intersectIntersecting() = original intersect intersecting
    @Benchmark fun intersectIntersectingFromOriginal() = original intersect intersectingFromOriginal

    @Benchmark fun subtractDisjoint() = original subtract disjoint
    @Benchmark fun subtractEqual() = original subtract equal
    @Benchmark fun subtractSubset() = original subtract subset
    @Benchmark fun subtractSubsetFromOriginal() = original subtract subsetFromOriginal
    @Benchmark fun subtractSuperset() = original subtract superset
    @Benchmark fun subtractSupersetFromOriginal() = original subtract superset
    @Benchmark fun subtractIntersecting() = original subtract intersecting
    @Benchmark fun subtractIntersectingFromOriginal() = original subtract intersectingFromOriginal
}
