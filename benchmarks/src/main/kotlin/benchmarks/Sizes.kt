package benchmarks

import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.openjdk.jol.info.GraphLayout;
import java.nio.file.*

data class ComparableSizeKey(val value: Int) : Comparable<ComparableSizeKey> {
    override fun compareTo(other: ComparableSizeKey): Int = value.compareTo(other.value)
}

data class HashableSizeKey(val value: Int)

object DummyValue


data class Maps(
    val scenario: String,
    val maps: Map<String, Map<Any, Any>>,
    val scenarioSize: Int = maps.values.first().size
)

data class Sets(
    val scenario: String,
    val sets: Map<String, Set<Any>>,
    val scenarioSize: Int = sets.values.first().size
)

class MapSizeTestContext(
    val empty: () -> PersistentMap<Any, Any>,
    val key: (Int) -> Any
)

class SetSizeTestContext(
    val empty: () -> PersistentSet<Any>,
    val key: (Int) -> Any
)

fun map(name: String, test: MapSizeTestContext.() -> PersistentMap<Any, Any>) = Maps(
    name,
    mapOf(
        "HashMap" to MapSizeTestContext(empty = { fakePersistentMapOf() }, key = { HashableSizeKey(it) }).test(),
        "PersistentHashMap" to MapSizeTestContext(empty = { persistentHashMapOf() }, key = { HashableSizeKey(it) }).test(),
        "HashTreapMap" to MapSizeTestContext(empty = { treapMapOf() }, key = { HashableSizeKey(it) }).test(),
        "SortedTreapMap" to MapSizeTestContext(empty = { treapMapOf() }, key = { ComparableSizeKey(it) }).test(),
    )
)

fun set(name: String, test: SetSizeTestContext.() -> PersistentSet<Any>) = Sets(
    name,
    mapOf(
        "HashSet" to SetSizeTestContext(empty = { fakePersistentSetOf() }, key = { HashableSizeKey(it) }).test(),
        "PersistentHashSet" to SetSizeTestContext(empty = { persistentHashSetOf() }, key = { HashableSizeKey(it) }).test(),
        "HashTreapSet" to SetSizeTestContext(empty = { treapSetOf() }, key = { HashableSizeKey(it) }).test(),
        "SortedTreapSet" to SetSizeTestContext(empty = { treapSetOf() }, key = { ComparableSizeKey(it) }).test(),
    )
)

val maps = sequence {
    yield(map("Empty") { empty() })
    (1..100).forEach {
        yield(map("Fresh") { empty() + (1..it).map { key(it) to DummyValue } })
    }
}

val sets = sequence {
    yield(set("Empty") { empty() })
    (1..100).forEach {
        yield(set("Fresh") { empty() + (1..it).map { key(it) } })
    }
}

fun <K, V> Map<K, V>.layout(): GraphLayout {
    if (this is FakePersistentMap<*, *>) {
        return this.value.layout()
    } else {
        val keysAndValues = GraphLayout.parseInstance(*(this.keys + this.values).toTypedArray())
        return GraphLayout.parseInstance(this).subtract(keysAndValues)
    }
}

fun <T> Set<T>.layout(): GraphLayout {
    if (this is FakePersistentSet<*>) {
        return this.value.layout()
    } else {
        val keysAndValues = GraphLayout.parseInstance(*(this.toList() as List<Any?>).toTypedArray())
        return GraphLayout.parseInstance(this).subtract(keysAndValues)
    }
}

fun GraphLayout.process(scenario: String, scenarioSize: Int, outputDir: Path): Long {
    toImage(outputDir.resolve("$scenario.$scenarioSize.png").toString())
    return totalSize()
}

@Serializable
data class MapSizes(
    val scenario: String,
    val scenarioSize: Int,
    val mapSizes: Map<String, Long>,
)

@Serializable
data class SetSizes(
    val scenario: String,
    val scenarioSize: Int,
    val setSizes: Map<String, Long>,
)

@Serializable
data class Sizes(
    val sets: List<SetSizes>,
    val maps: List<MapSizes>
)


/** Computes the sizes of various sets and maps, for comparison. Invoked by the `sizesBenchmark` Gradle task. */
fun main(args: Array<String>) {
    val outputDir = Path.of(args[0])
    Files.createDirectories(outputDir)

    val sizes = Sizes(
        sets = sets.map {
            SetSizes(
                scenario = it.scenario,
                scenarioSize = it.scenarioSize,
                setSizes = it.sets.mapValues { (_, set) -> set.layout().process(it.scenario, it.scenarioSize, outputDir) }
            )
        }.toList(),
        maps = maps.map {
            MapSizes(
                scenario = it.scenario,
                scenarioSize = it.scenarioSize,
                mapSizes = it.maps.mapValues { (_, map) -> map.layout().process(it.scenario, it.scenarioSize, outputDir) }
            )
        }.toList()
    )

    val json = Json { prettyPrint = true }.encodeToString(sizes)

    Files.writeString(outputDir.resolve("sizes.json"), json)
    println(json)
}

