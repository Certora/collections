package benchmarks.size

import benchmarks.*
import kotlinx.collections.immutable.*

val sets = sequence {
    yield(SetCase("Empty", 0) { sequenceOf(empty()) })
    scenarioSizes.forEach {
        yield(SetCase("Fresh", it) { sequenceOf((1..it).toSet()) })
    }
    scenarioSizes.forEach { yield(SetCase("IntersectEqual", it) { sequence {
        val fresh = (1..it).toSet()
        yield(fresh)
        yield(fresh intersect (1..it).toSet())
    }})}
    scenarioSizes.forEach { yield(SetCase("IntersectEqualReverse", it) { sequence {
        val fresh = (1..it).toSet()
        yield(fresh)
        yield((1..it).toSet() intersect fresh)
    }})}
    scenarioSizes.forEach { yield(SetCase("IntersectFirstHalf", it) { sequence {
        val fresh = (1..it).toSet()
        yield(fresh)
        yield(fresh intersect (1..it/2).toSet())
    }})}
    scenarioSizes.forEach { yield(SetCase("IntersectFirstHalfReverse", it) { sequence {
        val fresh = (1..it).toSet()
        yield(fresh)
        yield((1..it/2).toSet() intersect fresh)
    }})}
    scenarioSizes.forEach { yield(SetCase("IntersectHalf", it) { sequence {
        val fresh = (1..it).toSet()
        yield(fresh)
        yield(fresh intersect (1..it step 2).toSet())
    }})}
    scenarioSizes.forEach { yield(SetCase("IntersectSparse", it) { sequence {
        val fresh = (1..it).toSet()
        yield(fresh)
        yield(fresh intersect (1..it step 32).toSet())
    }})}
    scenarioSizes.forEach { yield(SetCase("IntersectIntersecting", it) { sequence {
        val fresh = (1..it).toSet()
        yield(fresh)
        yield(fresh intersect (it/2..it+it/2).toSet())
    }})}
    scenarioSizes.forEach { yield(SetCase("UnionSmall", it) { sequence {
        val smalls = manySmall(it, 16)
        yieldAll(smalls)
        yield(smalls.reduce { a, b -> a + b })
    }})}
}

context(SetCase.Context)
private fun IntRange.toSet(): PersistentSet<Any> = empty() + this.map { key(it) }.toSet()

context(SetCase.Context)
private fun manySmall(size: Int, unit: Int) = (1..size step unit).map { (it..it+unit).toSet() }
