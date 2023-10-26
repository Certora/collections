@file:OptIn(ExperimentalSerializationApi::class)
package benchmarks

import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.*
import kotlinx.serialization.csv.Csv
import org.openjdk.jol.info.GraphLayout;
import java.nio.file.*
import java.util.IdentityHashMap

val scenarioSizes = (1..256).asSequence() + sequenceOf(512, 1024, 2048, 4096, 8192, 16384, 32768)

val maps = sequence {
    yield(MapCase("Empty", 0) { sequenceOf(empty()) })
    scenarioSizes.forEach {
        yield(MapCase("Fresh", it) { sequenceOf(empty() + (1..it).map { key(it) to DummyValue }) })
    }
}

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
fun IntRange.toSet(): PersistentSet<Any> = empty() + this.map { key(it) }.toSet()

context(SetCase.Context)
fun manySmall(size: Int, unit: Int) = (1..size step unit).map { (it..it+unit).toSet() }

data class ComparableSizeKey(val value: Int) : Comparable<ComparableSizeKey> {
    override fun compareTo(other: ComparableSizeKey): Int = value.compareTo(other.value)
}

data class HashableSizeKey(val value: Int)

object DummyValue



data class MapCase(
    val scenario: String,
    val scenarioSize: Int,
    val hashMap: Sequence<PersistentMap<Any, Any>>,
    val persistentHashMap: Sequence<PersistentMap<Any, Any>>,
    val hashTreapMap: Sequence<PersistentMap<Any, Any>>,
    val sortedTreapMap: Sequence<PersistentMap<Any, Any>>,
) {
    class Context(
        val empty: () -> PersistentMap<Any, Any>,
        val key: (Int) -> Any
    )

    constructor(scenario: String, scenarioSize: Int, test: Context.() -> Sequence<PersistentMap<Any, Any>>) : this(
        scenario = scenario,
        scenarioSize = scenarioSize,
        hashMap = Context({ fakePersistentMapOf() }, { HashableSizeKey(it) }).test(),
        persistentHashMap = Context({ persistentHashMapOf() }, { HashableSizeKey(it) }).test(),
        hashTreapMap = Context({ treapMapOf() }, { HashableSizeKey(it) }).test(),
        sortedTreapMap = Context({ treapMapOf() }, { ComparableSizeKey(it) }).test(),
    )

    @Serializable
    data class Sizes(
        val scenario: String,
        val scenarioSize: Int,
        val hashMap: Long,
        val persistentHashMap: Long,
        val hashTreapMap: Long,
        val sortedTreapMap: Long,
    )

    private fun Sequence<Map<*, *>>.computeSize(): Long {
        val unwrapped = this.map { (it as? FakePersistentMap<*, *>)?.value ?: it }.toList()

        val keysAndValues = IdentityHashMap<Any, Any>()
        unwrapped.forEach {
            it.keys.forEach { keysAndValues[it] = DummyValue }
            it.values.forEach { keysAndValues[it] = DummyValue }
        }

        return GraphLayout.parseInstance(*unwrapped.toTypedArray())
            .subtract(GraphLayout.parseInstance(*keysAndValues.keys.toTypedArray()))
            .totalSize()
    }

    val sizes get() = Sizes(
        scenario = scenario,
        scenarioSize = scenarioSize,
        hashMap = hashMap.computeSize(),
        persistentHashMap = persistentHashMap.computeSize(),
        hashTreapMap = hashTreapMap.computeSize(),
        sortedTreapMap = sortedTreapMap.computeSize(),
    )
}

data class SetCase(
    val scenario: String,
    val scenarioSize: Int,
    val hashSet: Sequence<PersistentSet<Any>>,
    val persistentHashSet: Sequence<PersistentSet<Any>>,
    val persistentOrderedSet: Sequence<PersistentSet<Any>>,
    val hashTreapSet: Sequence<PersistentSet<Any>>,
    val sortedTreapSet: Sequence<PersistentSet<Any>>,
) {
    class Context(
        val empty: () -> PersistentSet<Any>,
        val key: (Int) -> Any
    )

    constructor(scenario: String, scenarioSize: Int, test: Context.() -> Sequence<PersistentSet<Any>>) : this(
        scenario = scenario,
        scenarioSize = scenarioSize,
        hashSet = Context({ fakePersistentSetOf() }, { HashableSizeKey(it) }).test(),
        persistentHashSet = Context({ persistentHashSetOf() }, { HashableSizeKey(it) }).test(),
        persistentOrderedSet = Context({ persistentSetOf() }, { HashableSizeKey(it) }).test(),
        hashTreapSet = Context({ treapSetOf() }, { HashableSizeKey(it) }).test(),
        sortedTreapSet = Context({ treapSetOf() }, { ComparableSizeKey(it) }).test(),
    )

    @Serializable
    data class Sizes(
        val scenario: String,
        val scenarioSize: Int,
        val hashSet: Long,
        val persistentHashSet: Long,
        val persistentOrderedSet: Long,
        val hashTreapSet: Long,
        val sortedTreapSet: Long,
    )

    private fun Sequence<Set<*>>.computeSize(): Long {
        val unwrapped = this.map { (it as? FakePersistentSet<*>)?.value ?: it }.toList()

        val keys = IdentityHashMap<Any, Any>()
        unwrapped.forEach {
            it.forEach { keys[it] = DummyValue }
        }

        return GraphLayout.parseInstance(*unwrapped.toTypedArray())
            .subtract(GraphLayout.parseInstance(*keys.keys.toTypedArray()))
            .totalSize()
    }

    val sizes get() = Sizes(
        scenario = scenario,
        scenarioSize = scenarioSize,
        hashSet = hashSet.computeSize(),
        persistentHashSet = persistentHashSet.computeSize(),
        persistentOrderedSet = persistentOrderedSet.computeSize(),
        hashTreapSet = hashTreapSet.computeSize(),
        sortedTreapSet = sortedTreapSet.computeSize(),
    )
}

/** Computes the sizes of various sets and maps, for comparison. Invoked by the `sizesBenchmark` Gradle task. */
fun main(args: Array<String>) {
    val outputDir = Path.of(args[0])
    Files.createDirectories(outputDir)

    val csv = Csv { hasHeaderRecord = true }


    println("Computing set sizes...")
    val sizes = sets.map { it.sizes }.toList().groupBy { it.scenario }
    sizes.forEach {
        val setsFile = outputDir.resolve("sets-${it.key}.csv")
        Files.writeString(
            setsFile,
            csv.encodeToString(
                it.value.toList()
            )
        )
        println("Wrote $setsFile")
    }

    println("Computing map sizes...")
    val mapsFile = outputDir.resolve("maps.csv")
    Files.writeString(
        mapsFile,
        csv.encodeToString(
            maps.map { it.sizes }.toList()
        )
    )
    println("Wrote $mapsFile")
}

