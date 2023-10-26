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


@Serializable
data class MapSizes(
    val scenario: String,
    val size: Int,
    val hashMap: Long,
    val persistentHashMap: Long,
    val hashTreapMap: Long,
    val sortedTreapMap: Long
)

class MapSizeTestContext(
    val empty: () -> PersistentMap<Any, DummyValue>,
    val key: (Int) -> Any
)

fun mapSizeTest(name: String, size: Int, test: MapSizeTestContext.() -> PersistentMap<Any, DummyValue>) = MapSizes(
    name,
    size,
    hashMap = MapSizeTestContext(empty = { fakePersistentMapOf() }, key = { HashableSizeKey(it) }).test().computeOverhead(),
    persistentHashMap = MapSizeTestContext(empty = { persistentHashMapOf() }, key = { HashableSizeKey(it) }).test().computeOverhead(),
    hashTreapMap = MapSizeTestContext(empty = { treapMapOf() }, key = { HashableSizeKey(it) }).test().computeOverhead(),
    sortedTreapMap = MapSizeTestContext(empty = { treapMapOf() }, key = { ComparableSizeKey(it) }).test().computeOverhead(),
)

val mapSizes = sequence {
    yield(mapSizeTest("Empty", 0) { empty() })
    (1..100).forEach {
        yield(mapSizeTest("Fresh", it) { empty() + (1..it).map { key(it) to DummyValue } })
    }
}


@Serializable
data class SetSizes(
    val scenario: String,
    val size: Int,
    val hashSet: Long,
    val persistentHashSet: Long,
    val hashTreapSet: Long,
    val sortedTreapSet: Long,
)

class SetSizeTestContext(
    val empty: () -> PersistentSet<Any>,
    val key: (Int) -> Any
)

fun setSizeTest(name: String, size: Int, test: SetSizeTestContext.() -> PersistentSet<Any>) = SetSizes(
    name,
    size,
    hashSet = SetSizeTestContext(empty = { fakePersistentSetOf() }, key = { HashableSizeKey(it) }).test().computeOverhead(),
    persistentHashSet = SetSizeTestContext(empty = { persistentHashSetOf() }, key = { HashableSizeKey(it) }).test().computeOverhead(),
    hashTreapSet = SetSizeTestContext(empty = { treapSetOf() }, key = { HashableSizeKey(it) }).test().computeOverhead(),
    sortedTreapSet = SetSizeTestContext(empty = { treapSetOf() }, key = { ComparableSizeKey(it) }).test().computeOverhead(),
)

val setSizes = sequence {
    yield(setSizeTest("Empty", 0) { empty() })
    (1..100).forEach {
        yield(setSizeTest("Fresh", it) { empty() + (1..it).map { key(it) } })
    }
}

fun <K, V> Map<K, V>.computeOverhead(): Long {
    if (this is FakePersistentMap<*, *>) {
        return this.value.computeOverhead()
    } else {
        val keysAndValues = GraphLayout.parseInstance(*(this.keys + this.values).toTypedArray())
        return GraphLayout.parseInstance(this).subtract(keysAndValues).totalSize()
    }
}

fun <T> Set<T>.computeOverhead(): Long {
    if (this is FakePersistentSet<*>) {
        return this.value.computeOverhead()
    } else {
        val keysAndValues = GraphLayout.parseInstance(*(this.toList() as List<Any?>).toTypedArray())
        return GraphLayout.parseInstance(this).subtract(keysAndValues).totalSize()
    }
}

@Serializable
data class Sizes(
    val sets: List<SetSizes>,
    val maps: List<MapSizes>
)

/** Computes the sizes of various sets and maps, for comparison. Invoked by the `sizesBenchmark` Gradle task. */
fun main(args: Array<String>) {
    val outputDir = Path.of(args[0])
    Files.createDirectory(outputDir)

    val sizes = Sizes(
        sets = setSizes.toList(),
        maps = mapSizes.toList()
    )
    val json = Json { prettyPrint = true }.encodeToString(sizes)

    Files.writeString(outputDir.resolve("sizes.json"), json)
    println(json)
}

